package communication;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMsg;

import entities.Message;
import entities.util.Content;
import entities.util.GSON;

public class Communication {
    private static final String DELIMITER = "<IDS|MSG>";
    private static final String HEARTBEAT_MESSAGE = "ping";
    private static final String ENCODE_CHARSET = ZMQ.CHARSET.name();
    private static final String HASH_ALGORITHM = "HmacSHA256";

    /**
     * This socket is the ‘broadcast channel’ where the kernel publishes all side
     * effects (stdout, stderr, etc.) as well as the requests coming from any client
     * over the shell socket and its own requests on the stdin socket
     */
    private ZMQ.Socket IOPub;

    /**
     * This single ROUTER socket allows multiple incoming connections from
     * frontends, and this is the socket where requests for code execution, object
     * information, prompts, etc. are made to the kernel by any frontend. The
     * communication on this socket is a sequence of request/reply actions from each
     * frontend and the kernel
     */
    private ZMQ.Socket shell;

    /**
     * This channel is identical to Shell, but operates on a separate socket, to
     * allow important messages to avoid queueing behind execution requests (e.g.
     * shutdown or abort).
     */
    private ZMQ.Socket control;

    /**
     * this ROUTER socket is connected to all frontends, and it allows the kernel to
     * request input from the active frontend when raw_input() is called.
     */
    private ZMQ.Socket stdin;

    /**
     * This socket allows for simple bytestring messages to be sent between the
     * frontend and the kernel to ensure that they are still connected.
     */
    private ZMQ.Socket heartBeat;

    private final SecretKeySpec secret;
    private final ZContext context;
    private String shellRouterId = "NO_ID";

    public Communication(Connection connection, ZContext context) throws UnsupportedEncodingException {
        this.secret = new SecretKeySpec(connection.getKey().getBytes(ENCODE_CHARSET), HASH_ALGORITHM);;
        this.context = context;

    	// Create sockets in the context received as parameter.
        this.IOPub = context.createSocket(SocketType.PUB);
        this.shell = context.createSocket(SocketType.ROUTER);
        this.shell.setRouterMandatory(true); // to help debugging the identity aspect of ROUTER
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

    public Poller createPoller() {
        Poller poller = context.createPoller(4);

        poller.register(getShellSocket(), ZMQ.Poller.POLLIN);
        poller.register(getControlSocket(), ZMQ.Poller.POLLIN);
        poller.register(getIOPubSocket(), ZMQ.Poller.POLLIN);
        poller.register(getHeartbeatSocket(), ZMQ.Poller.POLLIN);

        return poller;
    }
    
    private ZMQ.Socket getIOPubSocket() {
        return IOPub;
    }

    private ZMQ.Socket getShellSocket() {
        return shell;
    }

    private ZMQ.Socket getControlSocket() {
        return control;
    }

    private ZMQ.Socket getStdinSocket() {
        return stdin;
    }

    private ZMQ.Socket getHeartbeatSocket() {
        return heartBeat;
    }

    public void processHeartbeat() {
		@SuppressWarnings("unused")
		byte[] ping;
		while ((ping = getHeartbeatSocket().recv(ZMQ.DONTWAIT)) != null) {
			getHeartbeatSocket().send(HEARTBEAT_MESSAGE);
		}
	}

    public void replyShellMessage(Header parent, Content content) {
        sendMessage(getShellSocket(), new Header(content.getMessageType(), parent), parent, content);
    }

    public void replyControlMessage(Header parent, Content content) {
        sendMessage(getControlSocket(), new Header(content.getMessageType(), parent), parent, content);
    }

    private void sendMessage(ZMQ.Socket socket, Header header, Header parent, Content content) {
		Map<String, String> metadata = Collections.emptyMap();
		sendMessage(socket, header, parent, metadata, content);
	}

    private void sendMessage(ZMQ.Socket socket, Header header, Header parent, Map<String, String> metadata, Content content) {
        try {
            // Serialize the message as JSON
            String jsonHeader = GSON.toJson(header);
            String jsonParent = GSON.toJson(parent);
            String jsonMetaData = GSON.toJson(metadata);
            String jsonContent = GSON.toJson(content);

            System.err.println("sending message..." + "\n\theader: " + jsonHeader + "\n\tparent: " + jsonParent
                    + "\n\tcontent: " + jsonContent);

            // Sign the message
            Mac encoder = Mac.getInstance(HASH_ALGORITHM);
            encoder.init(secret);
            encoder.update(jsonHeader.getBytes(ENCODE_CHARSET));
            encoder.update(jsonParent.getBytes(ENCODE_CHARSET));
            encoder.update(jsonMetaData.getBytes(ENCODE_CHARSET));
            encoder.update(jsonContent.getBytes(ENCODE_CHARSET));
            String signedMessage = new String(Hex.encodeHex(encoder.doFinal()));

            // Send the message
            System.err.println("Shell router id: " + shellRouterId);
            socket.sendMore(shellRouterId);
            socket.sendMore(DELIMITER);
            socket.sendMore(signedMessage);
            socket.sendMore(jsonHeader);
            socket.sendMore(jsonParent);
            socket.sendMore(jsonMetaData);
            socket.send(jsonContent);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalStateException
                | UnsupportedEncodingException e) {
            throw new RuntimeException("this should never happen", e);
        }
    }

    public Message receiveShellMessage() throws RuntimeException, UnsupportedEncodingException {
        Message msg = receiveMessage(getShellSocket());
        this.shellRouterId = msg.getSessionId();
        getShellSocket().setIdentity(msg.getSessionId().getBytes(ENCODE_CHARSET));
        return msg;
    }

    public Message receiveControlMessage() throws RuntimeException {
        return receiveMessage(getControlSocket());
    }

    /**
	 * This method reads the data received from the socket given as parameter and
	 * encapsulates it into a Message object.
	 * 
	 * @param socket
	 * @return Message with the information of the received data.
	 */
	public Message receiveMessage(ZMQ.Socket socket) throws RuntimeException {
		ZMsg zmsg = ZMsg.recvMsg(socket, false); // Non-blocking recv

		if (zmsg == null) {
			throw new RuntimeException("receiveMessage was interrupted?");
		}

		ZFrame[] zFrames = new ZFrame[zmsg.size()];
		zmsg.toArray(zFrames);

		// Jupyter description says that the client should always send at least 7 chunks
		// of information.
		if (zmsg.size() < 7) {
			throw new RuntimeException("Missing information from the Jupyter client");
		}
		return new Message(zFrames);
	}

	public void close() {
        getControlSocket().close();
		getHeartbeatSocket().close();
		getIOPubSocket().close();
		getShellSocket().close();
		getStdinSocket().close();
	}

	public void sendIOMessage(Header parent, Content content) {
        sendMessage(
			getIOPubSocket(), 
			new Header(content.getMessageType(), parent), 
			parent,
			content
		);
	}

	public Message receiveIOMessage() {
		return receiveMessage(getIOPubSocket());
	}
}