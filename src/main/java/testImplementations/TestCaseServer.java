package testImplementations;

import com.google.gson.JsonObject;
import communication.Header;
import entities.reply.*;
import entities.request.ContentCompleteRequest;
import entities.request.ContentExecuteRequest;
import entities.request.ContentIsCompleteRequest;
import entities.request.ContentShutdownRequest;
import entities.util.MessageType;
import entities.util.Status;

import java.util.HashMap;
import java.util.Map;

import org.zeromq.ZMQ;
import server.JupyterServer;

/**
 * Created by Mauricio Verano Merino on 2/15/17.
 */
public class TestCaseServer extends JupyterServer {


    private int executionNumber;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------
    
    public TestCaseServer(String connectionFilePath) throws Exception {
        super(connectionFilePath);
        executionNumber=0;
    }

    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------

    /**
     * This method processes the shutdown_request message and replies with a shutdown_reply message.
     */
    @Override
    public void processShutdownRequest(ZMQ.Socket socket, Header parentHeader, ContentShutdownRequest contentShutdown) {
        //TODO: verify if its a restarting or a final shutdown command
        sendMessage(socket, createHeader(parentHeader.getSession(), MessageType.SHUTDOWN_REPLY), parentHeader, new JsonObject(), new ContentShutdownReply(contentShutdown.getRestart()));
        getCommunication().getRequests().close();
        getCommunication().getPublish().close();
        getCommunication().getControl().close();
        getCommunication().getContext().close();
        getCommunication().getContext().term();
        System.exit(-1);
    }

    /**
     * This method processes the is_complete_request message and replies with a is_complete_reply message.
     * <Auto-complete>
     * @param header
     * @param content
     */
    @Override
    public void processIsCompleteRequest(Header header, ContentIsCompleteRequest content) {
        System.out.println("CODE: " + content.getCode());
        String status, indent = "";
        if (content.getCode().endsWith(";"))
            status = Status.COMPLETE;
        else if (content.getCode().endsWith("-")) {
            status = Status.INCOMPLETE;
            indent = "\t \t";
        }
        else
            status = Status.UNKNOWN;
        sendMessage(getCommunication().getRequests(), createHeader(header.getSession(), MessageType.IS_COMPLETE_REPLY), header, new JsonObject(), new ContentIsCompleteReply(status, indent));
    }

    /**
     * This method processes the execute_request message and replies with a execute_reply message.
     */
    @Override
    public void processExecuteRequest(Header parentHeader, ContentExecuteRequest contentExecuteRequest) {
        System.out.println("PROCESS: " + getParser().toJson(contentExecuteRequest));
        if (contentExecuteRequest.isStoreHistory())
            executionNumber++;
        // TODO evaluate user Expressions

        processExecuteResult(parentHeader, contentExecuteRequest.getCode(), executionNumber);
        sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.EXECUTE_REPLY), parentHeader, new JsonObject(), new ContentExecuteReplyOk(executionNumber, null, null));
    }
    
    /**
     * 
     */
	@Override
	public void processExecuteResult(Header parentHeader, String code, int executionNumber) {
		System.out.println("EXECUTE_RESULT");
        Map<String, String> data = new HashMap<>();
        data.put("text/plain", "kernel answer");
        data.put("text/plain", code);
        ContentExecuteResult content = new ContentExecuteResult(executionNumber, data, new HashMap<String, String>());
        sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.DISPLAY_DATA), parentHeader, new JsonObject(), content);
        sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.EXECUTE_RESULT), parentHeader, new JsonObject(), content);
	}

    /**
     * This method processes the history_request message and replies with a history_reply message.
     */
    @Override
    public void processHistoryRequest(Header parentHeader) {
        sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.HISTORY_REPLY), parentHeader, new JsonObject(), new ContentHistoryReply());
    }
    
    /**
     * 
     */
	@Override
	public void processCompleteRequest(Header parentHeader, ContentCompleteRequest request) {
		// TODO 
		
	}
    
    // -----------------------------------------------------------------
    // Execution
    // -----------------------------------------------------------------
    /**
     * This method runs the application.
     * @param args application parameters
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        try {
            System.out.println("Java Kernel started");
            TestCaseServer jupyterServer = new TestCaseServer(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
