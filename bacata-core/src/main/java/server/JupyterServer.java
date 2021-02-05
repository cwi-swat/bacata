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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
 * Created by Mauricio on 17/01/2017.
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

	private Connection connection;
	private Gson parser;
	private Communication communication;
	private ZMQ.Poller poller;

	private final ILanguageProtocol language;
	private final LanguageInfo info;

	// TODO: figure out if we can do without the StringWriters
	private final StringWriter stdout = new StringWriter();
	private final StringWriter stderr = new StringWriter();
	private final OutputStream outStream = new WriterOutputStream(stdout, UTF8, 4096, true);
	private final OutputStream errStream = new WriterOutputStream(stderr, UTF8, 4096, true);

	private int executionNumber;

	private final SecretKeySpec secret;

	public JupyterServer(String connectionFilePath, ILanguageProtocol language, LanguageInfo info) throws Exception {
		parser = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
		connection = parser.fromJson(new FileReader(connectionFilePath), Connection.class);
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

			statusUpdate(new Header(), Status.STARTING);

			while (true) {
				poller.poll();

				if (poller.pollin(0)) {
					Message message = getMessage(communication.getShellSocket());
					System.err.println("received shell: " + message);
					processShellMessage(message);
				}

				if (poller.pollin(1)) {
					Message message = getMessage(communication.getControlSocket());
					System.err.println("received control: " + message);
					processControlMessage(message);
				}

				if (poller.pollin(2)) {
					Message message = getMessage(communication.getIOPubSocket());
					System.err.println("received IO: " + message);
				}

				if (poller.pollin(3)) {
					System.err.println("received heartbeat");
					listenHeartbeatSocket();
				}
			}
		}
	}

	/**
	 * This method reads the data received from the socket given as parameter and
	 * encapsulates it into a Message object.
	 * 
	 * @param socket
	 * @return Message with the information of the received data.
	 */
	private Message getMessage(ZMQ.Socket socket) throws RuntimeException {
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

	private void processControlMessage(Message message) {
		// TODO?
		if (message.getHeader().getMsgType().equals(MessageType.SHUTDOWN_REQUEST)) {
			ContentShutdownRequest content = parser.fromJson(message.getRawContent(), ContentShutdownRequest.class);
			Content contentReply = processShutdownRequest(content);
			Header header = new Header(MessageType.SHUTDOWN_REPLY, message.getHeader());
			sendMessage(communication.getControlSocket(), header, message.getHeader(), contentReply);
		}
	}

	private void processShellMessage(Message message) {
		Content content, replyContent;
		Header replyHeader;
		Header requestHeader = message.getHeader(); // Parent header for the reply.
		switch (requestHeader.getMsgType()) {
			case MessageType.KERNEL_INFO_REQUEST:
				statusUpdate(requestHeader, Status.BUSY);
				replyHeader = new Header(MessageType.KERNEL_INFO_REPLY, requestHeader);
				replyContent = new ContentKernelInfoReply(info);
				sendMessage(communication.getShellSocket(), replyHeader, requestHeader, replyContent);
				statusUpdate(requestHeader, Status.IDLE);
				break;
			case MessageType.SHUTDOWN_REQUEST:
				replyHeader = new Header(MessageType.SHUTDOWN_REPLY, requestHeader);
				content = parser.fromJson(message.getRawContent(), ContentShutdownRequest.class);
				replyContent = (ContentShutdownReply) processShutdownRequest((ContentShutdownRequest) content);
				sendMessage(communication.getShellSocket(), replyHeader, requestHeader, replyContent);

				if (!((ContentShutdownRequest) content).getRestart()) {
					closeAllSockets();
					System.exit(0);
				}

				break;
			case MessageType.IS_COMPLETE_REQUEST:
				replyHeader = new Header(MessageType.IS_COMPLETE_REPLY, message.getHeader());
				content = parser.fromJson(message.getRawContent(), ContentIsCompleteRequest.class);
				replyContent = processIsCompleteRequest((ContentIsCompleteRequest) content);
				sendMessage(communication.getShellSocket(), replyHeader, requestHeader, replyContent);
				break;
			case MessageType.EXECUTE_REQUEST:
				statusUpdate(requestHeader, Status.BUSY);
				content = parser.fromJson(message.getRawContent(), ContentExecuteRequest.class);
				processExecuteRequest((ContentExecuteRequest) content, message);
				statusUpdate(requestHeader, Status.IDLE);
				break;
			case MessageType.HISTORY_REQUEST:
				processHistoryRequest(message);
				break;
			case MessageType.COMPLETE_REQUEST:
				replyHeader = new Header(MessageType.COMPLETE_REPLY, requestHeader);
				content = parser.fromJson(message.getRawContent(), ContentCompleteRequest.class);
				replyContent = processCompleteRequest((ContentCompleteRequest) content);
				sendMessage(communication.getShellSocket(), replyHeader, requestHeader, replyContent);
				break;
			case MessageType.INSPECT_REQUEST:
				break;
			case MessageType.CONNECT_REQUEST:
				System.out.println("Deprecated message");
				break;
			case MessageType.COMM_INFO_REQUEST:
				System.out.println("COMM_INFO_REQUEST");
				System.out.println("CONTENT: " + message.getRawContent());
				break;
			default:
				System.out.println("NEW_MESSAGE_TYPE_REQUEST: " + message.getHeader().getMsgType());
				System.out.println("CONTENT: " + message.getRawContent());
				break;
		}
	}

	private void closeAllSockets() {
		communication.getControlSocket().close();
		communication.getHeartbeatSocket().close();
		communication.getIOPubSocket().close();
		communication.getShellSocket().close();
		communication.getStdinSocket().close();
	}

	private void listenHeartbeatSocket() {
		@SuppressWarnings("unused")
		byte[] ping;
		while ((ping = communication.getHeartbeatSocket().recv(ZMQ.DONTWAIT)) != null) {
			heartbeatChannel();
		}
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
			String jsonHeader = parser.toJson(header);
			String jsonParent = parser.toJson(parent);
			String jsonMetaData = parser.toJson(metadata);
			String jsonContent = parser.toJson(content);

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

	private void heartbeatChannel() {
		System.err.println("replying heartbeat");
		communication.getHeartbeatSocket().send(HEARTBEAT_MESSAGE);
	}

	private void statusUpdate(Header parentHeader, String status) {
		statusUpdate(communication.getIOPubSocket(), parentHeader, status);
	}

	private void statusUpdate(Socket socket, Header parentHeader, String status) {
		Header header = new Header(MessageType.STATUS, parentHeader);
		ContentStatus content = new ContentStatus(status);
		sendMessage(socket, header, parentHeader, content);
	}

	private void replyExecuteRequest(Header parentHeader, String session, Map<String, InputStream> data,
			Map<String, String> metadata) {
		Map<String, String> res = data.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> convertStreamToString(e.getValue())));

		ContentExecuteResult content = new ContentExecuteResult(executionNumber, res, metadata);
		sendMessage(communication.getIOPubSocket(), new Header(MessageType.EXECUTE_RESULT, parentHeader), parentHeader,
				content);
	}

	private void processExecuteRequest(ContentExecuteRequest contentExecuteRequest, Message message) {
		Header header, parentHeader = message.getHeader();
		Map<String, String> metadata = message.getMetadata();
		Map<String, InputStream> data = new HashMap<>();
		String session = message.getHeader().getSession();

		if (!contentExecuteRequest.isSilent()) {
			if (contentExecuteRequest.isStoreHistory()) {
				header = new Header(MessageType.EXECUTE_INPUT, parentHeader);
				sendMessage(communication.getIOPubSocket(), header, parentHeader,
						new ContentExecuteInput(contentExecuteRequest.getCode(), executionNumber));
				try {
					this.language.handleInput(contentExecuteRequest.getCode(), data, metadata); // Execute user's code

					sendMessage(communication.getShellSocket(), new Header(MessageType.EXECUTE_REPLY, parentHeader),
							parentHeader, new ContentExecuteReplyOk(executionNumber));

					processStreams(parentHeader, data, metadata, session); // stdout writing

					if (!data.isEmpty()) {
						replyExecuteRequest(parentHeader, session, data, metadata); // // Returns the result
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			executionNumber++;
		} else {
			// No broadcast output on the IOPUB channel.
			// Don't have an execute_result.
			header = new Header(MessageType.EXECUTE_REPLY, parentHeader);
			sendMessage(communication.getShellSocket(), header, parentHeader,
					new ContentExecuteReplyOk(executionNumber));
		}
	}

	@SuppressWarnings("resource")
	private String convertStreamToString(java.io.InputStream inputStream) {
		Scanner s = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	private void processStreams(Header parentHeader, Map<String, InputStream> data, Map<String, String> metadata,
			String session) {
		if (!stdout.toString().trim().equals("")) {
			processStreamsReply(ContentStream.STD_OUT, parentHeader, data, metadata, session);
		}
		if (!stderr.toString().trim().equals("")) {
			processStreamsReply(ContentStream.STD_ERR, parentHeader, data, metadata, session);
		}
	}

	private void processStreamsReply(String stream, Header parentHeader, Map<String, InputStream> data,
			Map<String, String> metadata, String session) {
		String logs = stream.equals(ContentStream.STD_OUT) ? stdout.toString() : stderr.toString();
		if (logs.contains("http://")) {
			metadata.put(MIME_TYPE_HTML,
					stream.equals(ContentStream.STD_OUT) ? createDiv(STD_OUT_DIV, replaceLocs2html(logs))
							: createDiv(STD_ERR_DIV, replaceLocs2html(logs)));
			sendMessage(communication.getIOPubSocket(), new Header(MessageType.DISPLAY_DATA, parentHeader), parentHeader,
					new ContentDisplayData(metadata, metadata, new HashMap<String, String>()));
		} else {
			sendMessage(communication.getIOPubSocket(), new Header(MessageType.STREAM, parentHeader), parentHeader,
					new ContentStream(stream, stdout.toString()));
		}
		flushStreams();
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

	private String createDiv(String clazz, String body) {
		return "<div class = \"" + clazz + "\">" + (body.equals("") || body == null ? "</div>" : body + "</div>");
	}

	private void flushStreams() {
		stdout.getBuffer().setLength(0);
		stdout.flush();
		stderr.getBuffer().setLength(0);
		stderr.flush();
	}

	private void processHistoryRequest(Message message) {
		// TODO This is only for clients to explicitly request history from a kernel
		// TODO add history to ILanguageProtocol or implement a generic version here
	}

	private ContentShutdownReply processShutdownRequest(ContentShutdownRequest contentShutdown) {
		boolean restart = false;
		if (contentShutdown.getRestart()) {
			restart = true;
		} else {
			this.language.stop();
		}
		return new ContentShutdownReply(restart);
	}

	/**
	 * This method is executed when the kernel receives a is_complete_request message.
	 */
	private Content processIsCompleteRequest(ContentIsCompleteRequest request) {
		String status, indent="";
		if (this.language.isStatementComplete(request.getCode())) {
			status = Status.COMPLETE;
		} else {
			status = Status.INCOMPLETE;
			indent = "??????";
		}
		return new ContentIsCompleteReply(status, indent);
	}

	private Content processCompleteRequest(ContentCompleteRequest content) {
		int cursorStart = 0;
		ArrayList<String> suggestions;
		
		if (content.getCode().startsWith("import "))
			cursorStart=7;
		
		CompletionResult result =this.language.completeFragment(content.getCode(), content.getCursorPosition());
		if (result != null) {
			suggestions = (ArrayList<String>)result.getSuggestions();
		}
		else {
			suggestions = null;
		}
		
		return new ContentCompleteReply(suggestions, cursorStart, content.getCode().length(), new HashMap<String, String>(), Status.OK);
	}
}