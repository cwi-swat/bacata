package server;

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

    public final static String DELIMITER = "<IDS|MSG>";


    private Connection connection;

    private Gson parser;

    private Communication communication;

    public JupyterServer(String connectionFilePath) throws Exception {
        parser = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        connection = parser.fromJson(new FileReader(connectionFilePath), Connection.class);
//        connection.printConnectionSettings();
        communication = new Communication(this, connection);


        while (!Thread.currentThread().isInterrupted()) {
            heartbeatChannel();
            String zmqIdentity = new String(communication.getRequests().recv());
            communication.getRequests().recv(); // Delimeter
            String hmacSignature = new String(communication.getRequests().recv());
            Header header = parser.fromJson(new String(communication.getRequests().recv()), Header.class);
            String metadata = new String(communication.getRequests().recv());
            String content = new String(communication.getRequests().recv());
            String extra = new String(communication.getRequests().recv());

            Message message = new Message(zmqIdentity, hmacSignature, header, null, content, metadata);

            if (message.getHeader().getMsgType().equals("kernel_info_request")) {
                JsonObject busy = new JsonObject();
                busy.addProperty("status", "busy");
                sendMessage(communication.getPublish(), createHeader(header.getSession(), "status"), new JsonObject(), new JsonObject(), busy);

                synchronized (communication.getRequests()) {
                    JsonObject parent = new JsonObject();
                    JsonObject cont = new JsonObject();
                    cont.addProperty("protocol_version", "5.0");
                    cont.addProperty("implementation", "javaKernel");
                    cont.addProperty("implementation_version", "1.0");
                    LanguageInfo li = new LanguageInfo("java", "8.0", "application/java", ".java");
                    cont.add("language_info", new Gson().toJsonTree(li));
                    cont.addProperty("banner", "Java kernel banner");

                    sendMessage(communication.getRequests(), createHeader(header.getSession(), "kernel_info_reply"), parent, parent, cont);
                }
            }
        }
        communication.getRequests().close();
        communication.getPublish().close();
        communication.getControl().close();
        communication.getContext().term();
    }

    public void sendMessage(ZMQ.Socket socket, Header header, JsonObject parent, JsonObject metadata, JsonObject content) {
//        socket.sendMore(header.getSession());
        socket.sendMore(DELIMITER);
//        String header = parser.toJson(createHeader(session, "kernel_info_reply"));
        socket.sendMore(signMessage(parser.toJson(header), String.valueOf(parent), String.valueOf(parent), parser.toJson(content)));
        socket.sendMore(parser.toJson(header));
        socket.sendMore(String.valueOf(parent));
        socket.sendMore(String.valueOf(parent));
        System.out.println("SEND: " + socket.send(parser.toJson(content)));

    }

    public void heartbeatChannel() {
        byte[] ping = communication.getHeartbeat().recv();
        communication.getHeartbeat().send("ok", 0);
    }

    public static void main(String[] args) throws Exception {
        try {
            System.out.println("Kernel started");
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
        String tmp = header + parentHeader + metadata + content;
//        System.out.println("MESSAGE TO SIGN:" +  tmp);
        return encode(tmp);
//        return encode(key, key)+encode(key, header)+encode(key, parentHeader)+encode(key, metadata)+encode(key, content);
    }


    public Connection getConnection() {
        return connection;
    }
}
