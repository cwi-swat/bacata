import com.google.gson.Gson;
import org.zeromq.ZMQ;

import java.io.FileReader;


/**
 * Created by Mauricio on 17/01/2017.
 */
public class JupyterServer {

    private Connection connection;
    private Gson parser;

    public JupyterServer(String connectionFilePath) throws Exception {
        parser = new Gson();
        connection = new Connection();
        connect(connectionFilePath);


        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket publish = context.socket(ZMQ.PUB);
        ZMQ.Socket requests = context.socket(ZMQ.ROUTER);
        ZMQ.Socket control = context.socket(ZMQ.ROUTER);
        ZMQ.Socket stdin = context.socket(ZMQ.ROUTER);
        ZMQ.Socket heartbeat = context.socket(ZMQ.REP);

        publish.bind(toUri(connection.getIoPubPort()));
        requests.bind(toUri(connection.getShellPort()));
        control.bind(toUri(connection.getControlPort()));
        stdin.bind(toUri(connection.getStdinPort()));
        heartbeat.bind(toUri(connection.getHbPort()));

        while (!Thread.currentThread().isInterrupted()) {

//            b'u-u-i-d',         # zmq identity(ies)
//                    b'<IDS|MSG>',       # delimiter
//            b'baddad42',        # HMAC signature
//            b'{header}',        # serialized header dict
//            b'{parent_header}', # serialized parent header dict
//            b'{metadata}',      # serialized metadata dict
//            b'{content}',       # serialized content dict


            byte[] zmqIdentity = requests.recv();
            byte[] delimeter = requests.recv();
            byte[] hmacSignature = requests.recv();
            byte[] header = requests.recv();
            byte[] metadata = requests.recv();
            byte[] content = requests.recv();
            byte[] extra = requests.recv();
//            System.out.println("ReqMsg: " + new String(zmqIdentity));
//            System.out.println("ReqMsg: " + new String(delimeter));
//            System.out.println("ReqMsg: " + new String(hmacSignature));
            System.out.println("ReqMsg: " + new String(header));
//            System.out.println("ReqMsg: " + new String(metadata));
//            System.out.println("ReqMsg: " + new String(content));
//            System.out.println("ReqMsg: " + new String(extra));

//            System.out.println("ReqMsg: " + requests.recvStr());
            //requestsMsg = control.recv();
            //System.out.println("CtrlMsg: " + new String(requestsMsg));
            //requestsMsg = stdin.recv();
            //System.out.println("STDInMsg: " + new String(requestsMsg));

            byte[] ping = heartbeat.recv();
            String str = new String(ping);
            System.out.println("HbMsg: " + str);

            //Do something
//            Thread.sleep(1000);

            String answer = "ok";
            heartbeat.send(answer.getBytes(), 0);
        }
//            responder.close();
        context.term();
    }

    public String toUri(Long pPort) {
        return connection.getTransport() + "://" + connection.getIp() + ":" + pPort;
    }

    public void connect(String connectionFilePath) {
        try {
            connection = parser.fromJson(new FileReader(connectionFilePath), Connection.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printConnectionSettings() {
        System.out.println("SHELL PORT: " + connection.getShellPort());
        System.out.println("IO pub PORT: " + connection.getIoPubPort());
        System.out.println("HB PORT: " + connection.getHbPort());
        System.out.println("Control PORT: " + connection.getControlPort());
        System.out.println("STDIN PORT: " + connection.getStdinPort());
        System.out.println("IP: " + connection.getIp());
        System.out.println("Transport: " + connection.getTransport());
        System.out.println("key: " + connection.getKey());
        System.out.println("kernel name: " + connection.getKernelName());
        System.out.println("signature scheme: " + connection.getSignatureScheme());
    }

    public static void main(String[] args) throws Exception {
        try {
            System.out.println("Kernel started");
            JupyterServer jupyterServer = new JupyterServer(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
