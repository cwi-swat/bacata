package server;

import Messages.Content.*;
import Messages.LanguageInfo;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import communication.Communication;
import communication.Connection;
import communication.Header;
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
import java.util.UUID;


/**
 * Created by Mauricio on 17/01/2017.
 */
public class JupyterServer {

    // -----------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------

    public final static String DELIMITER = "<IDS|MSG>";

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

    private Connection connection;

    private Gson parser;

    private Communication communication;

    private LanguageInfo languageInformation;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------
    public JupyterServer(String connectionFilePath) throws Exception {
        parser = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        connection = parser.fromJson(new FileReader(connectionFilePath), Connection.class);
        communication = new Communication(this);

        while (!Thread.currentThread().isInterrupted()) {

            String zmqIdentity;
            while ((zmqIdentity = communication.getRequests().recvStr(ZMQ.DONTWAIT)) != null) {
                communication.getRequests().recv(); // Delimeter
                String hmacSignature = communication.getRequests().recvStr();
                Header header = parser.fromJson(communication.getRequests().recvStr(), Header.class);
                String metadata = communication.getRequests().recvStr();
                String content = communication.getRequests().recvStr();
                String extra = communication.getRequests().recvStr();


                if (header.getMsgType().equals("kernel_info_request")) {
//                    Message message = new Message(zmqIdentity, hmacSignature, header, header, new ContentKernelInfoReply(), metadata);
                    processKernelInfoRequest(header);
                } else if (header.getMsgType().equals("shutdown_request")) {
                    System.out.println("SHUTDOWN REQUEST");
                    ContentShutdownRequest contentShutdownRequest = parser.fromJson(content, ContentShutdownRequest.class);
                    processShutdownRequest(header, contentShutdownRequest);
                } else if (header.getMsgType().equals("is_complete_request")) {
                    System.out.println("CODEEEEE: " + content);
                    ContentIsCompleteRequest actualCode = parser.fromJson(content, ContentIsCompleteRequest.class);
                    processIsCompleteRequest(header, actualCode);
                } else if (header.getMsgType().equals("history_request")) {
                    System.out.println("HISTORY: " + parser.toJson(content));
                } else {
                    System.out.println("NUEVO TIPO DE MENSAJE: " + header.getMsgType());
                }
            }

            String ctrlInput;
            while ((ctrlInput = communication.getControl().recvStr(ZMQ.DONTWAIT)) != null) {
                System.out.println("CONTROL MESSAGE RECEIVED: " + ctrlInput);
            }

            String ping;
            while ((ping = communication.getHeartbeat().recvStr(ZMQ.DONTWAIT)) != null) {
                heartbeatChannel();
            }
        }
        communication.getRequests().close();
        communication.getPublish().close();
        communication.getControl().close();
        communication.getContext().term();
    }

    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------

    public void processKernelInfoRequest(Header parentHeader) {
        Content content = new ContentKernelInfoReply("5.0", "javaKernel", "0.1", languageInformation, "Java Kernel Banner");
        sendMessage(communication.getRequests(), createHeader(parentHeader.getSession(), "kernel_info_reply"), parentHeader, new JsonObject(), content);
    }

    public void processShutdownRequest(Header parentHeader, ContentShutdownRequest contentShutdown) {
        //TODO: verify if its a restarting or a final shutdown command
        sendMessage(communication.getRequests(), createHeader(parentHeader.getSession(), "shutdown_reply"), parentHeader, new JsonObject(), new ContentShutdownReply(contentShutdown.getRestart()));
//        System.exit(0);
    }

    /**
     * complete code is ready to be executed
     * incomplete code should prompt for another line
     * invalid code will typically be sent for execution, so that the user sees the error soonest.
     * unknown - if the kernel is not able to determine this.
     *
     * @param header
     * @param content
     */
    public void processIsCompleteRequest(Header header, ContentIsCompleteRequest content) {
        System.out.println("CODE: " + content.getCode());
        ContentIsCompleteReply contentReply;
        if (content.getCode() == "" || content.getCode() == null) {
            contentReply = new ContentIsCompleteReply("unknown", "");
        } else if (content.getCode().endsWith(";"))
            contentReply = new ContentIsCompleteReply("complete", "");
        else
            contentReply = new ContentIsCompleteReply("incomplete", "");

        sendMessage(communication.getRequests(), createHeader(header.getSession(), "is_complete_reply"), header, new JsonObject(), contentReply);
    }

    public void listenControlChannel() {
        System.out.println("CONTROL: " + communication.getControl().recvStr());
    }

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
        communication.getHeartbeat().send("ok", 0);
    }

    public static void main(String[] args) throws Exception {
        try {
            System.out.println("Java Kernel started");
            JupyterServer jupyterServer = new JupyterServer(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Header createHeader(String pSession, String pMessageType) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        String msgid = String.valueOf(UUID.randomUUID());
        return new Header(pSession, pMessageType, Header.VERSION, "JavaKernel", timestamp, msgid);
    }

    public String encode(String data) {
        try {
            Mac sha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(connection.getKey().getBytes("UTF-8"), "HmacSHA256");
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
}