package server;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.io.output.WriterOutputStream;
import org.rascalmpl.repl.CompletionResult;
import org.rascalmpl.repl.ILanguageProtocol;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import communication.Communication;
import communication.Connection;
import communication.Header;
import entities.ContentDisplayData;
import entities.ContentExecuteInput;
import entities.ContentStream;
import entities.Message;
import entities.reply.ContentCompleteReply;
import entities.reply.ContentExecuteReplyError;
import entities.reply.ContentExecuteReplyOk;
import entities.reply.ContentExecuteResult;
import entities.reply.ContentIsCompleteReply;
import entities.reply.ContentKernelInfoReply;
import entities.reply.ContentShutdownReply;
import entities.request.ContentCompleteRequest;
import entities.request.ContentExecuteRequest;
import entities.request.ContentIsCompleteRequest;
import entities.request.ContentShutdownRequest;
import entities.util.Content;
import entities.util.ContentStatus;
import entities.util.GSON;
import entities.util.LanguageInfo;
import entities.util.MessageType;
import entities.util.Status;

/**
 * This server mediates between a Jupyter notebook application and an
 * implementation of the Bacatá/Rascal ILanguageProtocol. An ILanguageProtocol
 * is a Java interface to a language REPL interface with typical actions such as
 * executing input code and auto-completing code fragments.
 * 
 * The server uses ILanguageInfo to supply meta-data about the language which is
 * implements by the ILanguageProtocol.
 * 
 * @author Mauricio Verano on 17/01/2017.
 */
public class JupyterServer {
	private static final Charset UTF8 = Charset.forName("UTF8");
	private static final String MIME_TYPE_HTML = "text/html";
	private static final String MIME_TYPE_PLAIN = "text/plain";

	private final ILanguageProtocol language;
	private final LanguageInfo info;
	private final Connection connection;

	private final StringWriter stdout = new StringWriter();
	private final StringWriter stderr = new StringWriter();
	private final OutputStream outStream = new WriterOutputStream(stdout, UTF8, 4096, true);
	private final OutputStream errStream = new WriterOutputStream(stderr, UTF8, 4096, true);

	private Communication comms;
	private int executionNumber;

	public JupyterServer(String connectionFilePath, ILanguageProtocol language, LanguageInfo info) throws Exception {
		this.connection = GSON.fromJson(new FileReader(connectionFilePath), Connection.class);
		this.language = language;
		this.info = info;
		this.language.initialize(new ByteArrayInputStream(new byte[0]), outStream, errStream);
		executionNumber = 1;
	}

	public void startServer() throws JsonSyntaxException, JsonIOException, FileNotFoundException, RuntimeException, UnsupportedEncodingException {
		try (ZContext context = new ZContext(2)) {
			comms = new Communication(connection, context);
			// Create the poll to deal with the 4 different sockets
			ZMQ.Poller poller = comms.createPoller();

			sendStatus(new Header(MessageType.STATUS), Status.STARTING);

			while (true) {
				poller.poll();

				if (poller.pollin(0)) {
					Message message = comms.receiveShellMessage();
					System.err.println("received shell: " + message);
					processShellMessage(message);
				}

				if (poller.pollin(1)) {
					Message message = comms.receiveControlMessage();
					System.err.println("received control: " + message);
					processControlMessage(message);
				}

				if (poller.pollin(2)) {
					Message message = comms.receiveIOMessage();
					System.err.println("received IO: " + message);
				}

				if (poller.pollin(3)) {
					System.err.println("received heartbeat");
					comms.processHeartbeat();
				}
			}
		}
	}

	private void processControlMessage(Message message) {
		Header requestHeader = message.getHeader(); // Parent header for the reply.

		switch (requestHeader.getMsgType()) {
			case MessageType.SHUTDOWN_REQUEST:
				processShutdownRequest(content(ContentShutdownRequest.class, message), requestHeader);
				break;
		}
	}

	private void processShellMessage(Message message) {
		Header requestHeader = message.getHeader(); // Parent header for the reply.

		switch (requestHeader.getMsgType()) {
			case MessageType.KERNEL_INFO_REQUEST:
				processKernelInfoRequest(requestHeader);
				break;
			case MessageType.IS_COMPLETE_REQUEST:
				processIsCompleteRequest(content(ContentIsCompleteRequest.class, message), requestHeader);
				break;
			case MessageType.EXECUTE_REQUEST:
				processExecuteRequest(content(ContentExecuteRequest.class, message), requestHeader);
				break;
			case MessageType.COMPLETE_REQUEST:
				processCompleteRequest(content(ContentCompleteRequest.class, message), requestHeader);
				break;
			case MessageType.HISTORY_REQUEST:
				// Not yet implemented
				break;
			case MessageType.INSPECT_REQUEST:
				// Not yet implemented
				break;
			case MessageType.CONNECT_REQUEST:
				// Ignored for backward compatibility
				break;
			case MessageType.COMM_INFO_REQUEST:
				// Not yet implemented
				break;
			default:
				// Ignored for robustness' sake
				break;
		}
	}

	private void processKernelInfoRequest(Header parent) {
		sendStatus(parent, Status.BUSY);

		comms.replyShellMessage(
			parent,
			new ContentKernelInfoReply(info)
		);

		sendStatus(parent, Status.IDLE);
	}

	private void processIsCompleteRequest(ContentIsCompleteRequest content, Header parent) {
		boolean isComplete = language.isStatementComplete(content.getCode());

		comms.replyShellMessage(
			parent,
			new ContentIsCompleteReply(isComplete ? Status.COMPLETE : Status.INCOMPLETE, isComplete ? "" : "??????")
		);
	}

	private void processExecuteRequest(ContentExecuteRequest contentExecuteRequest, Header parent) {
		sendStatus(parent, Status.BUSY);

		comms.sendIOMessage(
			parent,
			new ContentExecuteInput(contentExecuteRequest.getCode(), executionNumber)
		);

		try {
			Map<String, String> metadata = new HashMap<>();
			Map<String, InputStream> data = new HashMap<>();

			this.language.handleInput(contentExecuteRequest.getCode(), data, metadata);

			if (!contentExecuteRequest.isSilent()) {
				sendStreamData(parent);
			}

			if (!data.isEmpty()) {
				Map<String, String> output = data.entrySet().stream()
						.collect(Collectors.toMap(e -> e.getKey(), e -> convertStreamToString(e.getValue())));

				comms.sendIOMessage(
					parent, 
					new ContentExecuteResult(executionNumber, output, metadata)
				);
			}

			comms.replyShellMessage(
				parent,
				new ContentExecuteReplyOk(executionNumber)
			);
		} 
		catch (InterruptedException e) {
			comms.replyShellMessage(
				parent,
				new ContentExecuteReplyError("interrupted", e.getMessage(), Collections.emptyList())
			);
		}
		finally {
			executionNumber++;
			sendStatus(parent, Status.IDLE);
		}
	}

	private void processShutdownRequest(ContentShutdownRequest shutdown, Header parent) {
		boolean restart = shutdown.getRestart();

		if (!restart) {
			language.stop();
		}

		comms.replyControlMessage(parent, new ContentShutdownReply(restart));

		if (!restart) {
			comms.close();
			System.exit(0);
		}
	}

	private void processCompleteRequest(ContentCompleteRequest content, Header parent) {
		Content reply;

		CompletionResult result = this.language.completeFragment(content.getCode(), content.getCursorPosition());

		if (result != null) {
			reply = new ContentCompleteReply(result.getSuggestions().stream().collect(Collectors.toList()),
					result.getOffset(), Math.max(content.getCursorPosition(), result.getOffset()),
					Collections.emptyMap(), Status.OK);
		} else {
			reply = new ContentCompleteReply(Collections.emptyList(), content.getCursorPosition(),
					content.getCursorPosition(), Collections.emptyMap(), Status.OK);
		}

		comms.replyShellMessage(parent, reply);
	}

	/**
	 * Utility to parse content of a message
	 */
	private <T extends Content> T content(Class<T> clazz, Message msg) {
		return GSON.fromJson(msg.getRawContent(), clazz);
	}

	private void sendStatus(Header parentHeader, String status) {
		comms.sendIOMessage(
			parentHeader,
			new ContentStatus(status)
		);
	}

	private void sendStreamData(Header parentHeader) {
		stdout.flush();
		if (!stdout.toString().trim().equals("")) {
			sendOutputStream(ContentStream.STD_OUT, parentHeader);
		}

		stderr.flush();
		if (!stderr.toString().trim().equals("")) {
			sendOutputStream(ContentStream.STD_ERR, parentHeader);
		}
	}

	private void sendOutputStream(String stream, Header parentHeader) {
		boolean isStdOut = stream.equals(ContentStream.STD_OUT);
		String output = isStdOut ? stdout.toString() : stderr.toString();

		if (output.contains("http://")) {
			output = replaceLocs2html(output);
		}

		comms.sendIOMessage(
			parentHeader,
			new ContentDisplayData(Collections.singletonMap(isStdOut ? MIME_TYPE_PLAIN : MIME_TYPE_HTML, output),
					Collections.emptyMap(), Collections.emptyMap()
				)
			);

		flushStreams();
	}

	@SuppressWarnings("resource")
	private String convertStreamToString(java.io.InputStream inputStream) {
		Scanner s = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	private String replaceLocs2html(String logs) {
		String pattern = "(?s)(.*)(\\|)(.+)(\\|)(.*$)";
		if (logs.matches(pattern)) {
			String prefix = logs.replaceAll(pattern, "$1");
			String url = logs.replaceAll(pattern, "$3");
			String suffix = logs.replaceAll(pattern, "$5");
			return prefix + "<a href=\"" + url + "\" target=\"_blank\">" + url + "</a>" + suffix;
		}
		return logs;
	}

	private void flushStreams() {
		stdout.getBuffer().setLength(0);
		stdout.flush();
		stderr.getBuffer().setLength(0);
		stderr.flush();
	}
}