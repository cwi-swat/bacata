package server;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import communication.Communication;
import communication.Connection;
import communication.Header;
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
import org.zeromq.ZMQ;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
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
	
	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------

	public JupyterServer(String connectionFilePath) throws Exception {
		parser = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
		connection = parser.fromJson(new FileReader(connectionFilePath), Connection.class);
		communication = new Communication(this);
		poller = new ZMQ.Poller(4);
		poller.register(communication.getRequests(), ZMQ.Poller.POLLIN);
		poller.register(communication.getControl(), ZMQ.Poller.POLLIN);
		poller.register(communication.getPublish(), ZMQ.Poller.POLLIN);
		poller.register(communication.getHeartbeat(), ZMQ.Poller.POLLIN);
	}


	// -----------------------------------------------------------------
	// Methods
	// -----------------------------------------------------------------

	public void startServer(){
		while (!Thread.currentThread().isInterrupted()) {
			poller.poll();
			if(poller.pollin(0))
				listenSocket(communication.getRequests(), SHELL);
			if(poller.pollin(1))
				listenSocket(communication.getControl(), CONTROL);
			if(poller.pollin(2))
				listenSocket(communication.getPublish(), PUBLISH);
			if(poller.pollin(3))
				listenHeartbeatSocket();
		}
	}
	
	/**
	 * This method receives the information received from the socket given as parameter.
	 * @param socket
	 * @param socketType
	 */
	private void listenSocket(ZMQ.Socket socket, String socketType)
	{
		socket.recvStr(ZMQ.DONTWAIT); // ZMQIdentity
		socket.recvStr(); // Delimeter
		socket.recvStr(); // HmacSignature
		Header header = parser.fromJson(communication.getRequests().recvStr(), Header.class); // Header
		parser.fromJson(communication.getRequests().recvStr(), Header.class); // Parent Header
		Map<String,String> map = new HashMap<String,String>();
		@SuppressWarnings("unchecked")
		Map<String, String> metadata = parser.fromJson(socket.recvStr(), map.getClass());
		String content = socket.recvStr(); // Content
		
		if(socketType.equals(SHELL))
		{
			statusUpdate(header, Status.BUSY);
			processShellMessageType(header, content, metadata);
			statusUpdate(header, Status.IDLE);
		}
		else if(socketType.equals(CONTROL))
		{
			if (header.getMsgType().equals(MessageType.SHUTDOWN_REQUEST)) {
				processShutdownRequest(socket, header, parser.fromJson(content, ContentShutdownRequest.class), metadata);
			}
		}
		
	}

	public void processShellMessageType(Header header, String content, Map<String, String> metadata) {
		switch (header.getMsgType()) {
		case MessageType.KERNEL_INFO_REQUEST:
			processKernelInfoRequest(header, metadata);
			break;
		case MessageType.SHUTDOWN_REQUEST:
			processShutdownRequest(communication.getRequests(), header, parser.fromJson(content, ContentShutdownRequest.class), metadata);
			break;
		case MessageType.IS_COMPLETE_REQUEST:
			processIsCompleteRequest(header, parser.fromJson(content, ContentIsCompleteRequest.class), metadata);
			break;
		case MessageType.EXECUTE_REQUEST:
			processExecuteRequest(header, parser.fromJson(content, ContentExecuteRequest.class), metadata);
			break;
		case MessageType.HISTORY_REQUEST:
			processHistoryRequest(header, metadata);
			break;
		case MessageType.COMPLETE_REQUEST:
			processCompleteRequest(header, parser.fromJson(content, ContentCompleteRequest.class), metadata);
			break;
		case MessageType.INSPECT_REQUEST:
			break;
		default:
			System.out.println("NEW_MESSAGE_TYPE_REQUEST: " + header.getMsgType());
			System.out.println("CONTENT: "+ content);
			break;
		}
	}

	public void listenHeartbeatSocket() {
		@SuppressWarnings("unused")
		String ping;
		while ((ping = communication.getHeartbeat().recvStr(ZMQ.DONTWAIT)) != null) {
			heartbeatChannel();
		}
	}

	/**
	 * This method sends a message according to the Wire Protocol through the socket received as parameter.
	 */
	public void sendMessage(ZMQ.Socket socket, Header header, Header parent, Map<String, String> metadata, Content content) {
		socket.sendMore(header.getSession().getBytes());
		socket.sendMore(DELIMITER.getBytes());
		socket.sendMore(signMessage(parser.toJson(header), parser.toJson(parent), parser.toJson(metadata), parser.toJson(content)).getBytes());
		socket.sendMore(parser.toJson(header).getBytes());
		socket.sendMore(parser.toJson(parent));
		socket.sendMore(parser.toJson(metadata));
		socket.send(parser.toJson(content), 0);
	}

	public void heartbeatChannel() {
		communication.getHeartbeat().send(HEARTBEAT_MESSAGE, 0);
	}

	public Header createHeader(String pSession, String pMessageType) {
		String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
		String msgid = String.valueOf(UUID.randomUUID());
		return new Header(pSession, pMessageType, Header.VERSION, Header.USERNAME, timestamp, msgid);
	}

	public String encode(String data) {
		try {
			Mac sha256 = Mac.getInstance(HASH_ALGORITHM);
			SecretKeySpec secretKey = new SecretKeySpec(connection.getKey().getBytes(ENCODE_CHARSET), HASH_ALGORITHM);
			sha256.init(secretKey);
			return new String(Hex.encodeHex(sha256.doFinal(data.getBytes())));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String signMessage(String header, String parentHeader, String metadata, String content) {
		String message = header + parentHeader + metadata + content;
		return encode(message);
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
		sendMessage(communication.getPublish(), createHeader(parentHeader.getSession(), MessageType.STATUS), parentHeader, new HashMap<String, String>(), new ContentStatus(status));
	}

	// -----------------------------------------------------------------
	// Abstract methods
	// -----------------------------------------------------------------
	
	/**
	 * This method processes the execute_request message and replies with a execute_reply message.
	 */
	public abstract void processExecuteRequest(Header parentHeader, ContentExecuteRequest contentExecuteRequest, Map<String, String> metadata);

	/**
	 * This method processes the complete_request message and replies with a complete_reply message.
	 */
	public abstract void processCompleteRequest(Header parentHeader, ContentCompleteRequest request, Map<String, String> metadata);

	/**
	 * This method processes the history_request message and replies with a history_reply message.
	 */
	public abstract void processHistoryRequest(Header parentHeader, Map<String, String> metadata);

	/**
	 * This method processes the kernel_info_request message and replies with a kernel_info_reply message.
	 */
	public abstract void processKernelInfoRequest(Header parentHeader, Map<String, String> metadata);

	/**
	 * This method processes the shutdown_request message and replies with a shutdown_reply message.
	 */
	public abstract void processShutdownRequest(ZMQ.Socket socket, Header parentHeader, ContentShutdownRequest contentShutdown, Map<String, String> metadata);

	/**
	 * This method processes the is_complete_request message and replies with a is_complete_reply message.
	 * @param header
	 * @param content
	 */
	public abstract void processIsCompleteRequest(Header header, ContentIsCompleteRequest content, Map<String, String> metadata);

	/**
	 * This method creates the interpreter to be used as a REPL
	 * @param source
	 * @param moduleName
	 * @param variableName
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public abstract ILanguageProtocol makeInterpreter(String source, String moduleName, String variableName, String... salixPath) throws IOException, URISyntaxException;
}