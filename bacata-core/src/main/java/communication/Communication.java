package communication;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;


public class Communication {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

	/**
	 * This socket is the ‘broadcast channel’ where the kernel publishes all side effects (stdout, stderr, etc.) as well as the requests coming from any client over the shell socket and its own requests on the stdin socket
	 */
    private ZMQ.Socket IOPub;

    /**
     * This single ROUTER socket allows multiple incoming connections from frontends, and this is the socket where requests for code execution, object information, prompts, etc. are made to the kernel by any frontend. 
     * The communication on this socket is a sequence of request/reply actions from each frontend and the kernel
     */
    private ZMQ.Socket shell;

    /**
     * This channel is identical to Shell, but operates on a separate socket, to allow important messages to avoid queueing behind execution requests (e.g. shutdown or abort).
     */
    private ZMQ.Socket control;

    /**
     * this ROUTER socket is connected to all frontends, and it allows the kernel to request input from the active frontend when raw_input() is called.
     */
    private ZMQ.Socket stdin;

    /**
     * This socket allows for simple bytestring messages to be sent between the frontend and the kernel to ensure that they are still connected.
     */
    private ZMQ.Socket heartBeat;
    
    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public Communication(Connection connection, ZContext context) {
    	// Create sockets in the context received as parameter.
        this.IOPub = context.createSocket(SocketType.XPUB);
        this.shell = context.createSocket(SocketType.ROUTER);
        this.control = context.createSocket(SocketType.ROUTER);
        this.stdin = context.createSocket(SocketType.ROUTER);
        this.heartBeat = context.createSocket(SocketType.REP);
        
        // Bind each socket to their corresponding URI.
        this.IOPub.bind(connection.getIOPubURI());
        this.shell.bind(connection.getShellURI());
        this.control.bind(connection.getControlURI());
        this.stdin.bind(connection.getStdinURI());
        this.heartBeat.bind(connection.getHbURI());
    }

    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------

    public ZMQ.Socket getIOPubSocket() {
        return IOPub;
    }

    public ZMQ.Socket getShellSocket() {
        return shell;
    }

    public ZMQ.Socket getControlSocket() {
        return control;
    }

    public ZMQ.Socket getStdinSocket() {
        return stdin;
    }

    public ZMQ.Socket getHeartbeatSocket() {
        return heartBeat;
    }

}
