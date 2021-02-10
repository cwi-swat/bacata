package server;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.output.WriterOutputStream;
import org.rascalmpl.repl.CompletionResult;
import org.rascalmpl.repl.ILanguageProtocol;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import communication.Communication;
import communication.Connection;
import communication.Header;
import entities.ContentDisplayData;
import entities.ContentExecuteInput;
import entities.ContentStream;
import entities.Message;
import entities.reply.ContentCompleteReply;
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
import entities.util.LanguageInfo;
import entities.util.MessageType;
import entities.util.Status;

/**
 * This server mediates between a Jupyter notebook application and an implementation
 * of the Bacatá/Rascal ILanguageProtocol. An ILanguageProtocol is a
 * Java interface to a language REPL interface with typical actions
 * such as executing input code and auto-completing code fragments.
 * 
 * The server uses ILanguageInfo to supply meta-data about the language
 * which is implements by the ILanguageProtocol.
 * 
 * @author Mauricio Verano on 17/01/2017.
 */
public class JupyterServer {
	private static final Map<String, String> EMPTY_MAP = (Map<String, String>) Collections.EMPTY_MAP;
	private static final String DELIMITER = "<IDS|MSG>";
	private static final String HEARTBEAT_MESSAGE = "ping";
	private static final String ENCODE_CHARSET = "UTF-8";
	private static final String HASH_ALGORITHM = "HmacSHA256";

	private static final String STD_ERR_DIV = "output_stderr";
	private static final String STD_OUT_DIV = "output_stdout";
	private static final Charset UTF8 = Charset.forName("UTF8");
	private static final String MIME_TYPE_HTML = "text/html";
	private static final String MIME_TYPE_PLAIN = "text/plain";

	private Connection connection;
	private Gson gson;
	private Communication communication;
	private ZMQ.Poller poller;

	private final ILanguageProtocol language;
	private final LanguageInfo info;

	private final StringWriter stdout = new StringWriter();
	private final StringWriter stderr = new StringWriter();
	private final OutputStream outStream = new WriterOutputStream(stdout, UTF8, 4096, true);
	private final OutputStream errStream = new WriterOutputStream(stderr, UTF8, 4096, true);

	private int executionNumber;

	private final SecretKeySpec secret;

	public JupyterServer(String connectionFilePath, ILanguageProtocol language, LanguageInfo info) throws Exception {
		gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
		connection = gson.fromJson(new FileReader(connectionFilePath), Connection.class);
		connection.printConnectionSettings();
		this.secret = new SecretKeySpec(connection.getKey().getBytes(ENCODE_CHARSET), HASH_ALGORITHM);
		this.language = language;
		this.info = info;
		this.language.initialize(new ByteArrayInputStream(new byte[0]), outStream, errStream);
		executionNumber = 1;
	} 

	public void startServer() throws JsonSyntaxException, JsonIOException, FileNotFoundException, RuntimeException {
		try (ZContext context = new ZContext(2)) {
			communication = new Communication(connection, context);
			// Create the poll to deal with the 4 different sockets
			poller = context.createPoller(4);

			poller.register(communication.getShellSocket(), ZMQ.Poller.POLLIN);
			poller.register(communication.getControlSocket(), ZMQ.Poller.POLLIN);
			poller.register(communication.getIOPubSocket(), ZMQ.Poller.POLLIN);
			poller.register(communication.getHeartbeatSocket(), ZMQ.Poller.POLLIN);

			sendStatus(new Header(), Status.STARTING);

			while (true) {
				poller.poll();

				if (poller.pollin(0)) {
					Message message = receiveMessage(communication.getShellSocket());
					System.err.println("received shell: " + message);
					processShellMessage(message);
				}

				if (poller.pollin(1)) {
					Message message = receiveMessage(communication.getControlSocket());
					System.err.println("received control: " + message);
					processControlMessage(message);
				}

				if (poller.pollin(2)) {
					Message message = receiveMessage(communication.getIOPubSocket());
					System.err.println("received IO: " + message);
				}

				if (poller.pollin(3)) {
					System.err.println("received heartbeat");
					processHeartbeat();
				}
			}
		}
	}

	private void processControlMessage(Message message) {
		Header requestHeader = message.getHeader(); // Parent header for the reply.

		switch (requestHeader.getMsgType()) {
			case MessageType.SHUTDOWN_REQUEST:
			processShutdownRequest(communication.getControlSocket(), content(ContentShutdownRequest.class, message), requestHeader);
			break;
		}
	}

	private void processShellMessage(Message message) {
		Header requestHeader = message.getHeader(); // Parent header for the reply.

		switch (requestHeader.getMsgType()) {
			case MessageType.KERNEL_INFO_REQUEST:
				processKernelInfoRequest(requestHeader);
				break;
			case MessageType.SHUTDOWN_REQUEST:
				processShutdownRequest(communication.getShellSocket(), content(ContentShutdownRequest.class, message), requestHeader);
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
		sendMessage(
			communication.getShellSocket(), 
			new Header(MessageType.KERNEL_INFO_REPLY, parent), 
			parent, 
			new ContentKernelInfoReply(info));
		sendStatus(parent, Status.IDLE);
	}

	private void processIsCompleteRequest(ContentIsCompleteRequest content, Header parent) {
		boolean isComplete = language.isStatementComplete(content.getCode());

		sendMessage(
			communication.getShellSocket(), 
			new Header(MessageType.IS_COMPLETE_REPLY, parent), 
			parent, 
			new ContentIsCompleteReply(isComplete ? Status.COMPLETE : Status.INCOMPLETE, isComplete ? "" : "??????"));
	}

	private void processExecuteRequest(ContentExecuteRequest contentExecuteRequest, Header parent) {
		sendStatus(parent, Status.BUSY);

		if (!contentExecuteRequest.isSilent()) {
			if (contentExecuteRequest.isStoreHistory()) { // why this condition?
				sendMessage(
					communication.getIOPubSocket(), 
					new Header(MessageType.EXECUTE_INPUT, parent), 
					parent,
					new ContentExecuteInput(contentExecuteRequest.getCode(), executionNumber));

				try {
					Map<String, String> metadata = new HashMap<>();
					Map<String, InputStream> data = new HashMap<>();
			
					this.language.handleInput(contentExecuteRequest.getCode(), data, metadata); 

					sendStreamData(parent); 

					if (!data.isEmpty()) {
						Map<String, String> output = data.entrySet().stream()
								.collect(Collectors.toMap(e -> e.getKey(), e -> convertStreamToString(e.getValue())));

						sendMessage(
							communication.getIOPubSocket(), 
							new Header(MessageType.EXECUTE_RESULT, parent), 
							parent, 
							new ContentExecuteResult(executionNumber, output, metadata));
					}

					sendMessage(
						communication.getShellSocket(), 
						new Header(MessageType.EXECUTE_REPLY, parent),
						parent, 
						new ContentExecuteReplyOk(executionNumber));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			executionNumber++;
		} else {
			// No broadcast output on the IOPUB channel.
			// Don't have an execute_result.
			sendMessage(
				communication.getShellSocket(), 
				new Header(MessageType.EXECUTE_REPLY, parent), 
				parent,
				new ContentExecuteReplyOk(executionNumber));
		}

		sendStatus(parent, Status.IDLE);
	}

	private void processShutdownRequest(Socket socket, ContentShutdownRequest shutdown, Header parent) {
		boolean restart = shutdown.getRestart();

		if (!restart) {
			language.stop();
		}
				
		sendMessage(
			socket, 
			new Header(MessageType.SHUTDOWN_REPLY, parent), 
			parent, 
			new ContentShutdownReply(restart));

		if (!restart) {
			closeAllSockets();
			System.exit(0);
		}
	}

	private void processCompleteRequest(ContentCompleteRequest content, Header parent) {
		Content reply;

		CompletionResult result = this.language.completeFragment(content.getCode(), content.getCursorPosition());
		
		if (result != null) {
			reply = new ContentCompleteReply(
				result.getSuggestions().stream().collect(Collectors.toList()), 
				result.getOffset(), 
				Math.max(content.getCursorPosition(), result.getOffset()), 
				EMPTY_MAP, 
				Status.OK
			);
		}
		else {
			reply = new ContentCompleteReply(
				Collections.emptyList(), 
				content.getCursorPosition(),
				content.getCursorPosition(), 
				Collections.emptyMap(), 
				Status.OK);
		}
		
		
		sendMessage(
			communication.getShellSocket(), 
			new Header(MessageType.COMPLETE_REPLY, parent), 
			parent, 
			reply);
	}

	private void processHeartbeat() {
		@SuppressWarnings("unused")
		byte[] ping;
		while ((ping = communication.getHeartbeatSocket().recv(ZMQ.DONTWAIT)) != null) {
			communication.getHeartbeatSocket().send(HEARTBEAT_MESSAGE);
		}
	}

	/**
	 * Utility to parse content of a message
	 */
	private <T extends Content> T content(Class<T> clazz, Message msg) {
		return gson.fromJson(msg.getRawContent(), clazz);
	}

	/**
	 * This method reads the data received from the socket given as parameter and
	 * encapsulates it into a Message object.
	 * 
	 * @param socket
	 * @return Message with the information of the received data.
	 */
	private Message receiveMessage(ZMQ.Socket socket) throws RuntimeException {
		ZMsg zmsg = ZMsg.recvMsg(socket, false); // Non-blocking recv

		ZFrame[] zFrames = new ZFrame[zmsg.size()];
		zmsg.toArray(zFrames);

		// Jupyter description says that the client should always send at least 7 chunks
		// of information.
		if (zmsg.size() < 7) {
			throw new RuntimeException("Missing information from the Jupyter client");
		}
		return new Message(zFrames);
	}

	private void sendMessage(ZMQ.Socket socket, Header header, Header parent, Content content) {
		Map<String, String> metadata = EMPTY_MAP;
		sendMessage(socket, header, parent, metadata, content);
	}

	/**
	 * This method sends a message according to the Wire Protocol through the socket
	 * received as parameter.
	 */
	private void sendMessage(ZMQ.Socket socket, Header header, Header parent, Map<String, String> metadata, Content content) {
		try {
			// Serialize the message as JSON
			String jsonHeader = gson.toJson(header);
			String jsonParent = gson.toJson(parent);
			String jsonMetaData = gson.toJson(metadata);
			String jsonContent = gson.toJson(content);

			System.err.println("sending message..." 
				+ "\n\theader: " + jsonHeader 
				+ "\n\tparent: " + jsonParent 
				+ "\n\tcontent: " + jsonContent);

			// Sign the message
			Mac encoder = Mac.getInstance(HASH_ALGORITHM);
			encoder.init(secret);
			encoder.update(jsonHeader.getBytes());
			encoder.update(jsonParent.getBytes());
			encoder.update(jsonMetaData.getBytes());
			encoder.update(jsonContent.getBytes());
			String signedMessage = new String(Hex.encodeHex(encoder.doFinal()));

			// Send the message
			socket.sendMore(header.getSession().getBytes());
			socket.sendMore(DELIMITER.getBytes());
			socket.sendMore(signedMessage.getBytes());
			socket.sendMore(jsonHeader.getBytes());
			socket.sendMore(jsonParent);
			socket.sendMore(jsonMetaData);
			socket.send(jsonContent, ZMQ.DONTWAIT);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new RuntimeException("this should never happen", e);
		} 
	}

	private void sendStatus(Header parentHeader, String status) {
		sendMessage(
			communication.getIOPubSocket(), 
			new Header(MessageType.STATUS, parentHeader), 
			parentHeader, 
			new ContentStatus(status));
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
			
		sendMessage(
			communication.getIOPubSocket(), 
			new Header(MessageType.DISPLAY_DATA, parentHeader), 
			parentHeader,
			new ContentDisplayData(Collections.singletonMap(MIME_TYPE_PLAIN, output), EMPTY_MAP, EMPTY_MAP));

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
			logs = logs.replaceAll("\n", "<br>");
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

	private void closeAllSockets() {
		communication.getControlSocket().close();
		communication.getHeartbeatSocket().close();
		communication.getIOPubSocket().close();
		communication.getShellSocket().close();
		communication.getStdinSocket().close();
	}
}