package server;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import communication.Communication;
import communication.Connection;
import communication.Header;
import entities.reply.*;
import entities.request.ContentExecuteRequest;
import entities.request.ContentIsCompleteRequest;
import entities.request.ContentShutdownRequest;
import entities.util.Content;
import entities.util.ContentStatus;
import entities.util.MessageType;
import entities.util.Status;
import org.apache.commons.codec.binary.Hex;
import org.zeromq.ZMQ;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
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

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

    private Connection connection;

    private Gson parser;

    private Communication communication;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public JupyterServer(String connectionFilePath) throws Exception {
        parser = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        connection = parser.fromJson(new FileReader(connectionFilePath), Connection.class);
        communication = new Communication(this);

        while (!Thread.currentThread().isInterrupted()) {
            listenShellSocket();
            listenControlSocket();
            listenHeartbeatSocket();
        }
    }


    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------

    public void listenShellSocket() {
        String zmqIdentity;
        while ((zmqIdentity = communication.getRequests().recvStr(ZMQ.DONTWAIT)) != null) {
            String delimeter = communication.getRequests().recvStr(); // Delimeter
            String hmacSignature = communication.getRequests().recvStr();
            Header header = parser.fromJson(communication.getRequests().recvStr(), Header.class);
            Header parentHeader = parser.fromJson(communication.getRequests().recvStr(), Header.class);
            String metadata = communication.getRequests().recvStr();
            String content = communication.getRequests().recvStr();

            statusUpdate(header, Status.BUSY);
            processShellMessageType(header, content);
            statusUpdate(header, Status.IDLE);
        }
    }

    public void processShellMessageType(Header header, String content) {
        switch (header.getMsgType()) {
            case MessageType.KERNEL_INFO_REQUEST:
                System.out.println("KERNEL INFO REQUEST: ");
                processKernelInfoRequest(header);
                break;
            case MessageType.SHUTDOWN_REQUEST:
                System.out.println("SHUTDOWN REQUEST");
                processShutdownRequest(communication.getRequests(), header, parser.fromJson(content, ContentShutdownRequest.class));
                break;
            case MessageType.IS_COMPLETE_REQUEST:
                System.out.println("IS_COMPLETE_REQUEST: ");
                processIsCompleteRequest(header, parser.fromJson(content, ContentIsCompleteRequest.class));
                break;
            case MessageType.EXECUTE_REQUEST:
                System.out.println("EXECUTE_REQUEST: ");
                processExecuteRequest(header, parser.fromJson(content, ContentExecuteRequest.class));
                break;
            case MessageType.HISTORY_REQUEST:
                System.out.println("HISTORY: ");
                processHistoryRequest(header);
                break;
            case MessageType.COMPLETE_REQUEST:
                System.out.println("COMPLETE_REQUEST: ");
                break;
            case MessageType.INSPECT_REQUEST:
                System.out.println("INSPECT_REQUEST: ");
                break;
            default:
                System.out.println("NEW_MESSAGE_TYPE_REQUEST: " + header.getMsgType());
                break;
        }
    }

    public void listenHeartbeatSocket() {
        String ping;
        while ((ping = communication.getHeartbeat().recvStr(ZMQ.DONTWAIT)) != null) {
            heartbeatChannel();
        }
    }

    public void listenControlSocket() {
        String ctrlInput;
        while ((ctrlInput = communication.getControl().recvStr(ZMQ.DONTWAIT)) != null) {
            System.out.println("CONTROL MESSAGE RECEIVED: " + ctrlInput);
            String delimeter = communication.getRequests().recvStr(); // Delimeter
            String hmacSignature = communication.getRequests().recvStr();
            Header header = parser.fromJson(communication.getRequests().recvStr(), Header.class);
            Header parentHeader = parser.fromJson(communication.getRequests().recvStr(), Header.class);
            String metadata = communication.getRequests().recvStr();
            String content = communication.getRequests().recvStr();
            if (header.getMsgType().equals(MessageType.SHUTDOWN_REQUEST)) {
                processShutdownRequest(communication.getControl(), header, parser.fromJson(content, ContentShutdownRequest.class));
            }
        }
    }

    /**
     * This method processes the execute_request message and replies with a execute_reply message.
     */
    public abstract void processExecuteRequest(Header parentHeader, ContentExecuteRequest contentExecuteRequest);

    /**
     * This method processes the history_request message and replies with a history_reply message.
     */
    public abstract void processHistoryRequest(Header parentHeader);

    /**
     * This method updates the kernel status with the value received as a parameter.
     */
    public void statusUpdate(Header parentHeader, String status) {
        sendMessage(communication.getPublish(), createHeader(parentHeader.getSession(), MessageType.STATUS), parentHeader, new JsonObject(), new ContentStatus(status));
    }

    /**
     * This method processes the kernel_info_request message and replies with a kernel_info_reply message.
     */
    public void processKernelInfoRequest(Header parentHeader){
    	sendMessage(communication.getRequests(), createHeader(parentHeader.getSession(), MessageType.KERNEL_INFO_REPLY), parentHeader, new JsonObject(), new ContentKernelInfoReply());
    }

    /**
     * This method processes the shutdown_request message and replies with a shutdown_reply message.
     */
    public abstract void processShutdownRequest(ZMQ.Socket socket, Header parentHeader, ContentShutdownRequest contentShutdown);

    /**
     * This method processes the is_complete_request message and replies with a is_complete_reply message.
     * @param header
     * @param content
     */
    public abstract void processIsCompleteRequest(Header header, ContentIsCompleteRequest content);

    public abstract void processExecuteResult(Header parentHeader, String code, int executionNumber);

    /**
     * This method sends a message according to The Wire Protocol through the socket received as parameter.
     */
    public void sendMessage(ZMQ.Socket socket, Header header, Header parent, JsonObject metadata, Content content) {
        socket.sendMore(header.getSession().getBytes());
        socket.sendMore(DELIMITER.getBytes());
        socket.sendMore(signMessage(parser.toJson(header), parser.toJson(parent), String.valueOf(metadata), parser.toJson(content)).getBytes());
        socket.sendMore(parser.toJson(header).getBytes());
        socket.sendMore(parser.toJson(parent));
        socket.sendMore(String.valueOf(metadata).getBytes());
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


}