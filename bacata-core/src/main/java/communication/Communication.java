package communication;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import server.JupyterServer;

/**
 * Created by Mauricio on 23/01/2017.
 */
public class Communication {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

//    private ZMQ.Context context;

    private ZMQ.Socket publish;

    private ZMQ.Socket requests;

    private ZMQ.Socket control;

    private ZMQ.Socket stdin;

    private ZMQ.Socket heartbeat;
    
    private Connection connection;

//    private JupyterServer server;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public Communication(Connection connection, ZContext context) {
//        server = jupyterServer;
//    	this.context = context;
    	this.connection = connection;
        this.publish = context.createSocket(SocketType.PUB);
        this.requests = context.createSocket(SocketType.ROUTER);
        this.control = context.createSocket(SocketType.ROUTER);
        this.stdin = context.createSocket(SocketType.ROUTER);
        this.heartbeat = context.createSocket(SocketType.REP);

//        this.publish.connect(connection.getIOPubURI());
//        this.requests.connect(connection.getShellURI());
//        this.control.connect(connection.getControlURI());
//        this.stdin.connect(connection.getStdinURI());
//        this.heartbeat.connect(connection.getHbURI());
        
        this.publish.bind(connection.getIOPubURI());
        this.requests.bind(connection.getShellURI());
        this.control.bind(connection.getControlURI());
        this.stdin.bind(connection.getStdinURI());
        this.heartbeat.bind(connection.getHbURI());
    }

    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------

//    public ZMQ.Context getContext() {
//        return context;
//    }

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

}
