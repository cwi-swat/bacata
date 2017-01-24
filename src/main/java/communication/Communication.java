package communication;

import org.zeromq.ZMQ;
import server.JupyterServer;

/**
 * Created by Mauricio on 23/01/2017.
 */
public class Communication {

    private ZMQ.Context context;
    private ZMQ.Socket publish;
    private ZMQ.Socket requests;
    private ZMQ.Socket control;
    private ZMQ.Socket stdin;
    private ZMQ.Socket heartbeat;

    private JupyterServer server;

    public Communication(JupyterServer jupyterServer, Connection connection) {
        server = jupyterServer;
        context = ZMQ.context(1);
        publish = context.socket(ZMQ.PUB);
        requests = context.socket(ZMQ.ROUTER);
        control = context.socket(ZMQ.ROUTER);
        stdin = context.socket(ZMQ.ROUTER);
        heartbeat = context.socket(ZMQ.REP);

        publish.bind(toUri(connection.getIoPubPort()));
        requests.bind(toUri(connection.getShellPort()));
        control.bind(toUri(connection.getControlPort()));
        stdin.bind(toUri(connection.getStdinPort()));
        heartbeat.bind(toUri(connection.getHbPort()));
    }

    public ZMQ.Context getContext() {
        return context;
    }

    public ZMQ.Socket getPublish() {
        return publish;
    }

    public ZMQ.Socket getRequests() {
        return requests;
    }

    public ZMQ.Socket getControl() {
        return control;
    }

    public ZMQ.Socket getStdin() {
        return stdin;
    }

    public ZMQ.Socket getHeartbeat() {
        return heartbeat;
    }

    public String toUri(Long pPort) {
        return server.getConnection().getTransport() + "://" + server.getConnection().getIp() + ":" + pPort;
    }
}
