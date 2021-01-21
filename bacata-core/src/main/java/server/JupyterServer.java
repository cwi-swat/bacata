package server;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import communication.Communication;
import communication.Connection;
import communication.Header;
import entities.Message;
import entities.reply.ContentKernelInfoReply;
import entities.reply.ContentShutdownReply;
import entities.request.ContentCompleteRequest;
import entities.request.ContentExecuteRequest;
import entities.request.ContentIsCompleteRequest;
import entities.request.ContentShutdownRequest;
import entities.util.Content;
import entities.util.ContentStatus;
import entities.util.MessageType;
import entities.util.Status;

import org.apache.commons.codec.binary.Hex;
import org.rascalmpl.repl.ILanguageProtocol;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.UUID;


/**
 * Created by Mauricio on 17/01/2017.
 */
public abstract class JupyterServer {

	// -----------------------------------------------------------------
	// Constants
	// -----------------------------------------------------------------

	public final static String DELIMITER = "<IDS|MSG>";

	public final static String HEARTBEAT_MESSAGE = "ping";

	public final static String ENCODE_CHARSET = "UTF-8";

	public final static String HASH_ALGORITHM = "HmacSHA256";
	
	public final static String SHELL = "Shell";
	
	public final static String CONTROL = "Control";
	
	public final static String PUBLISH = "Publish";
	
	public final static String HEART_BEAT = "Heartbeat"; 

	// -----------------------------------------------------------------
	// Fields
	// -----------------------------------------------------------------

	private Connection connection;

	private Gson parser;

	private Communication communication;

	private ZMQ.Poller poller;
	
	protected ILanguageProtocol language;
	
	protected StringWriter stdout;

	protected StringWriter stderr;
	
	protected int executionNumber;
	
	private Mac sha256;
	
	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------

	public JupyterServer(String connectionFilePath) throws Exception {
		parser = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
		connection = parser.fromJson(new FileReader(connectionFilePath), Connection.class);
		sha256 = Mac.getInstance(HASH_ALGORITHM);
		SecretKeySpec secretKey = new SecretKeySpec(connection.getKey().getBytes(ENCODE_CHARSET), HASH_ALGORITHM);
		sha256.init(secretKey);
		executionNumber = 1;
	}


	// -----------------------------------------------------------------
	// Methods
	// -----------------------------------------------------------------

	public void startServer() throws JsonSyntaxException, JsonIOException, FileNotFoundException, RuntimeException {
		
		try (ZContext context = new ZContext()) {
			communication = new Communication(connection, context);
			// Create the poll to deal with the 4 different sockets
			poller = context.createPoller(4);
			poller.register(communication.getShellSocket(), ZMQ.Poller.POLLIN);
			poller.register(communication.getControlSocket(), ZMQ.Poller.POLLIN);
			poller.register(communication.getIOPubSocket(), ZMQ.Poller.POLLIN);
			poller.register(communication.getHeartbeatSocket(), ZMQ.Poller.POLLIN);
			
			// see if this wakes up the kernel sooner
			heartbeatChannel();

			while (!Thread.currentThread().isInterrupted()) {
				poller.poll();
				if (poller.pollin(0)) {
					Message message = getMessage(communication.getShellSocket());
//					if (message.getHeader().getMsgType().equals(MessageType.KERNEL_INFO_REQUEST)) {
//						statusUpdate(message.getHeader(), Status.STARTING);
//					}
					statusUpdate(message.getHeader(), Status.BUSY);
					
					processShellMessage(message);
					statusUpdate(message.getHeader(), Status.IDLE);
				}
				if (poller.pollin(1)) {
					Message message = getMessage(communication.getControlSocket());					
					processControlMessage(message);
					if (message.getHeader().getMsgType().equals(MessageType.SHUTDOWN_REQUEST)) {
						ContentShutdownRequest content = parser.fromJson(message.getRawContent(), ContentShutdownRequest.class);
						Content contentReply = processShutdownRequest(content);
						Header header = createHeader(message.getHeader().getSession(), MessageType.SHUTDOWN_REPLY);
						sendMessage(communication.getControlSocket(), header, message.getHeader(), contentReply);
					}
				}
				if (poller.pollin(2))
					getMessage(communication.getIOPubSocket());
				if (poller.pollin(3))
					listenHeartbeatSocket();
			}
		
		}
	}

	/**
	 * This method reads the data received from the socket given as parameter and encapsulates it into a Message object.
	 * @param socket
	 * @return Message with the information of the received data.
	 */
	public Message getMessage(ZMQ.Socket socket) throws RuntimeException {
		ZMsg zmsg = ZMsg.recvMsg(socket, false); // Non-blocking recv
		
		ZFrame[] zFrames = new ZFrame[zmsg.size()];
		zmsg.toArray(zFrames);
		
		// Jupyter description says that the client should always send at least 7 chunks of information.
		if (zmsg.size() < 7) {
			throw new RuntimeException("Missing information from the Jupyter client");
		}
		return new Message(zFrames);
	}
	
	public void processControlMessage(Message message) {
		switch (message.getHeader().getMsgType()) {
		case "input_request":
			System.out.println("input_request");
			break;
		default:
			break;
		}
	}
	
	public void processShellMessage(Message message) {
		Content content, contentReply;
		Header header, parentHeader = message.getHeader(); // Parent header for the reply.
		switch (parentHeader.getMsgType()) {
			case MessageType.KERNEL_INFO_REQUEST:
				header = new Header(MessageType.KERNEL_INFO_REPLY, parentHeader);
				contentReply = (ContentKernelInfoReply) processKernelInfoRequest(message);
				sendMessage(communication.getShellSocket(), header, parentHeader, contentReply);
				break;
			case MessageType.SHUTDOWN_REQUEST:
				header = new Header(MessageType.SHUTDOWN_REPLY, parentHeader);
				content = parser.fromJson(message.getRawContent(), ContentShutdownRequest.class);
				contentReply = (ContentShutdownReply) processShutdownRequest((ContentShutdownRequest) content);
				closeAllSockets();
				sendMessage(communication.getShellSocket(), header, parentHeader, contentReply);
				break;
			case MessageType.IS_COMPLETE_REQUEST:
				header = createHeader(message.getHeader().getSession(), MessageType.IS_COMPLETE_REPLY);
				content = parser.fromJson(message.getRawContent(), ContentIsCompleteRequest.class);
				contentReply = processIsCompleteRequest((ContentIsCompleteRequest) content);
				sendMessage(communication.getShellSocket(),header , parentHeader, contentReply);
				break;
			case MessageType.EXECUTE_REQUEST:
				content = parser.fromJson(message.getRawContent(), ContentExecuteRequest.class);
				processExecuteRequest((ContentExecuteRequest) content, message);
				break;
			case MessageType.HISTORY_REQUEST:
				processHistoryRequest(message);
				break;
			case MessageType.COMPLETE_REQUEST:
				header = new Header(MessageType.COMPLETE_REPLY, parentHeader);
				content = parser.fromJson(message.getRawContent(), ContentCompleteRequest.class);
				contentReply = processCompleteRequest((ContentCompleteRequest) content);
				sendMessage(getCommunication().getShellSocket(), header, parentHeader, contentReply);
				break;
			case MessageType.INSPECT_REQUEST:
				break;
			case MessageType.CONNECT_REQUEST:
				System.out.println();
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

	public void closeAllSockets() {
		communication.getControlSocket().close();
		communication.getHeartbeatSocket().close();
		communication.getIOPubSocket().close();
		communication.getShellSocket().close();
		communication.getStdinSocket().close();
		System.exit(-1);
	}
	
	public void listenHeartbeatSocket() {
		@SuppressWarnings("unused")
		byte[] ping;
		while ((ping = communication.getHeartbeatSocket().recv(ZMQ.DONTWAIT)) != null) {
			heartbeatChannel();
		}
	}

	public void sendMessage(ZMQ.Socket socket, Header header, Header parent, Content content) {
		HashMap<String, String> metadata = new HashMap<String, String>();
		sendMessage(socket, header, parent, metadata, content);
	}
	/**
	 * This method sends a message according to the Wire Protocol through the socket received as parameter.
	 */
	public void sendMessage(ZMQ.Socket socket, Header header, Header parent, HashMap<String, String> metadata, Content content) {
		String message = parser.toJson(header) + parser.toJson(parent) + parser.toJson(metadata) + parser.toJson(content);
		String signedMessage = signMessage(message.getBytes());
		
		// Send the message
		socket.sendMore(header.getSession().getBytes());
		socket.sendMore(DELIMITER.getBytes());
		socket.sendMore(signedMessage.getBytes());
		socket.sendMore(parser.toJson(header).getBytes());
		socket.sendMore(parser.toJson(parent));
		socket.sendMore(parser.toJson(metadata));
		socket.send(parser.toJson(content), 0);
	}
	
	public void sendShellMessage(Header header, Content content ) {
		
	}

	public void heartbeatChannel() {
		communication.getHeartbeatSocket().send(HEARTBEAT_MESSAGE);
	}
	
	public Header createHeader(String pSession, String pMessageType) {
		String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
		String msgid = String.valueOf(UUID.randomUUID());
		return new Header(pSession, pMessageType, Header.VERSION, Header.USERNAME, timestamp, msgid);
	}

	public String signMessage(byte[] message) {
		return new String(Hex.encodeHex(sha256.doFinal(message)));
	}

	public Connection getConnection() {
		return connection;
	}

	public Communication getCommunication() {
		return communication;
	}

	public Gson getParser() {
		return parser;
	}
	
	/**
	 * This method updates the kernel status with the value received as a parameter.
	 */
	public void statusUpdate(Header parentHeader, String status) {
		Header header = new Header(parentHeader.getSession(), MessageType.STATUS, parentHeader.getVersion(), parentHeader.getUsername());
		ContentStatus content = new ContentStatus(status);
		sendMessage(communication.getIOPubSocket(), header, parentHeader, content);
	}

	// -----------------------------------------------------------------
	// Abstract methods
	// -----------------------------------------------------------------
	
	/**
	 * This method processes the execute_request message and replies with a execute_reply message.
	 */
	public abstract void processExecuteRequest(ContentExecuteRequest contentExecuteRequest, Message message);

	/**
	 * This method processes the complete_request message and replies with a complete_reply message.
	 */
	public abstract Content processCompleteRequest(ContentCompleteRequest contentCompleteRequest);

	/**
	 * This method processes the history_request message and replies with a history_reply message.
	 */
	public abstract void processHistoryRequest(Message message);

	/**
	 * This method processes the kernel_info_request message and replies with a kernel_info_reply message.
	 */
	public abstract Content processKernelInfoRequest(Message message);

	/**
	 * This method processes the shutdown_request message and replies with a shutdown_reply message.
	 */
	public abstract Content processShutdownRequest(ContentShutdownRequest contentShutdownRequest);

	/**
	 * This method processes the is_complete_request message and replies with a is_complete_reply message.
	 * @param header
	 * @param content
	 */
	public abstract Content processIsCompleteRequest(ContentIsCompleteRequest contentIsCompleteRequest);

	/**
	 * This method creates the interpreter to be used as a REPL
	 * @param source
	 * @param replQualifiedName
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public abstract ILanguageProtocol makeInterpreter(String source, String replQualifiedName, String... salixPath) throws IOException, URISyntaxException, Exception;
}