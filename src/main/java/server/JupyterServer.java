package server;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import communication.Communication;
import communication.Connection;
import communication.Header;
import entities.*;
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

    private int executionNumber;
//    private LanguageInfo languageInformation;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------
    public JupyterServer(String connectionFilePath) throws Exception {
        parser = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        connection = parser.fromJson(new FileReader(connectionFilePath), Connection.class);
        communication = new Communication(this);
        executionNumber = 0;


        while (!Thread.currentThread().isInterrupted()) {

            String zmqIdentity;
            while ((zmqIdentity = communication.getRequests().recvStr(ZMQ.DONTWAIT)) != null) {
                String delimeter = communication.getRequests().recvStr(); // Delimeter
                String hmacSignature = communication.getRequests().recvStr();
                Header header = parser.fromJson(communication.getRequests().recvStr(), Header.class);
                Header parentHeader = parser.fromJson(communication.getRequests().recvStr(), Header.class);
                String metadata = communication.getRequests().recvStr();
                String content = communication.getRequests().recvStr();

//                System.out.println("%%%z"+zmqIdentity);
//                System.out.println("%%%d"+delimeter);
//                System.out.println("%%%h"+hmacSignature);
//                System.out.println("%%%h"+parser.toJson(header));
//                System.out.println("%%%m"+metadata);
//                System.out.println("%%%c"+content);

                statusUpdate(header, "busy");
                if (header.getMsgType().equals("kernel_info_request")) {
                    System.out.println("KERNEL INFO REQUEST: ");
                    processKernelInfoRequest(header);
                } else if (header.getMsgType().equals("shutdown_request")) {
                    System.out.println("SHUTDOWN REQUEST");
                    ContentShutdownRequest contentShutdownRequest = parser.fromJson(content, ContentShutdownRequest.class);
                    processShutdownRequest(header, contentShutdownRequest);
                } else if (header.getMsgType().equals("is_complete_request")) {
                    System.out.println("IS_COMPLETE_REQUEST: ");
                    processIsCompleteRequest(header, parser.fromJson(content, ContentIsCompleteRequest.class));
                } else if (header.getMsgType().equals("history_request")) {
                    System.out.println("HISTORY: ");
                } else if (header.getMsgType().equals("execute_request")) {
                    System.out.println("EXECUTE_REQUEST: ");
                    processExecuteRequest(header, parser.fromJson(content, ContentExecuteRequest.class));
                } else {
                    System.out.println("NUEVO TIPO DE MENSAJE: " + header.getMsgType());
                }
                statusUpdate(header, "idle");
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
    }


    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------

    public void processExecuteRequest(Header parentHeader, ContentExecuteRequest contentExecuteRequest) {
        System.out.println("PROCESS: " + parser.toJson(contentExecuteRequest));
        if (contentExecuteRequest.isStoreHistory())
            executionNumber++;

        sendMessage(communication.getRequests(), createHeader(parentHeader.getSession(), "execute_reply"), parentHeader, new JsonObject(), new ContentExecuteReply("ok", executionNumber));
    }

    public void statusUpdate(Header parentHeader, String status) {
        sendMessage(communication.getPublish(), createHeader(parentHeader.getSession(), "status"), parentHeader, new JsonObject(), new ContentStatus(status));
    }


    public void processKernelInfoRequest(Header parentHeader) {
        sendMessage(communication.getRequests(), createHeader(parentHeader.getSession(), "kernel_info_reply"), parentHeader, new JsonObject(), new ContentKernelInfoReply());
    }

    public void processShutdownRequest(Header parentHeader, ContentShutdownRequest contentShutdown) {
        //TODO: verify if its a restarting or a final shutdown command
        sendMessage(communication.getRequests(), createHeader(parentHeader.getSession(), "shutdown_reply"), parentHeader, new JsonObject(), new ContentShutdownReply(contentShutdown.getRestart()));
        communication.getRequests().close();
        communication.getPublish().close();
        communication.getControl().close();
        communication.getContext().term();
        communication.getContext().close();
        System.exit(-1);
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
        String status;
        if (content.getCode().endsWith(";"))
            status = "complete";
        else
            status = "incomplete";
        sendMessage(communication.getRequests(), createHeader(header.getSession(), "is_complete_reply"), header, new JsonObject(), new ContentIsCompleteReply(status, ""));
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