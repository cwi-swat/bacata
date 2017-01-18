import org.json.simple.JSONObject;
import org.zeromq.ZMQ;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;


/**
 * Created by Mauricio on 17/01/2017.
 */
public class JupyterServer {

    public static void main(String[] args) throws Exception {
        JSONParser parser = new JSONParser();
        try {
            System.out.println("Kernel started");
            JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(args[0]));
            Long shellPort = (Long) jsonObject.get("shell_port");
            Long iopubPort = (Long) jsonObject.get("iopub_port");
            Long hbPort = (Long) jsonObject.get("hb_port");
            Long controlPort = (Long) jsonObject.get("control_port");
            Long stdinPort = (Long) jsonObject.get("stdin_port");
            String ip = (String) jsonObject.get("ip");
            String transport = (String) jsonObject.get("transport");
            String key = (String) jsonObject.get("key");
            String kernelName = (String) jsonObject.get("kernel_name");
            String signatureScheme = (String) jsonObject.get("signature_scheme");

            System.out.println("SHELL PORT: " + shellPort);
            System.out.println("IO pub PORT: " + iopubPort);
            System.out.println("HB PORT: " + hbPort);
            System.out.println("Control PORT: " + controlPort);
            System.out.println("STDIN PORT: " + stdinPort);
            System.out.println("IP: " + ip);
            System.out.println("Transport: " + transport);
            System.out.println("key: " + key);
            System.out.println("kernel name: " + kernelName);
            System.out.println("signature scheme: " + signatureScheme);

            ZMQ.Context context = ZMQ.context(1);
            ZMQ.Socket publish = context.socket(ZMQ.PUB);
            ZMQ.Socket requests = context.socket(ZMQ.ROUTER);
            ZMQ.Socket control = context.socket(ZMQ.ROUTER);
            ZMQ.Socket stdin = context.socket(ZMQ.ROUTER);
            ZMQ.Socket heartbeat = context.socket(ZMQ.REP);

            publish.bind(transport + "://" + ip + ":" + iopubPort);
            requests.bind(transport + "://" + ip + ":" + shellPort);
            control.bind(transport + "://" + ip + ":" + controlPort);
            stdin.bind(transport + "://" + ip + ":" + stdinPort);
            heartbeat.bind(transport + "://" + ip + ":" + hbPort);


            while (!Thread.currentThread().isInterrupted()) {
                byte[] request = heartbeat.recv();
                String str = new String(request);
                System.out.println("Received: " + str);

                //Do something
                Thread.sleep(1000);

                String answer = "ok";
                heartbeat.send(answer.getBytes(), 0);
            }
//            responder.close();
            context.term();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
