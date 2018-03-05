package testImplementations;

import communication.Header;
import entities.reply.*;
import entities.request.ContentCompleteRequest;
import entities.request.ContentExecuteRequest;
import entities.request.ContentIsCompleteRequest;
import entities.request.ContentShutdownRequest;
import entities.util.MessageType;
import entities.util.Status;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.rascalmpl.repl.ILanguageProtocol;
import org.zeromq.ZMQ;
import server.JupyterServer;

/**
 * Created by Mauricio Verano Merino on 2/15/17.
 */
public class TestCaseServer extends JupyterServer {


    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------
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
    public void processShutdownRequest(ZMQ.Socket socket, Header parentHeader, ContentShutdownRequest contentShutdown, Map<String, String> metadata) {
        //TODO: verify if its a restarting or a final shutdown command
        sendMessage(socket, createHeader(parentHeader.getSession(), MessageType.SHUTDOWN_REPLY), parentHeader, metadata, new ContentShutdownReply(contentShutdown.getRestart()));
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
    public void processIsCompleteRequest(Header header, ContentIsCompleteRequest content, Map<String, String> metadata) {
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
        sendMessage(getCommunication().getRequests(), createHeader(header.getSession(), MessageType.IS_COMPLETE_REPLY), header, metadata, new ContentIsCompleteReply(status, indent));
    }
    @Override
    public void processKernelInfoRequest(Header parentHeader, Map<String, String> metadata){
		sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.KERNEL_INFO_REPLY), parentHeader, metadata, new ContentKernelInfoReply());
	}

    /**
     * This method processes the execute_request message and replies with a execute_reply message.
     */
    @Override
    public void processExecuteRequest(Header parentHeader, ContentExecuteRequest contentExecuteRequest, Map<String, String> metadata) {
        System.out.println("PROCESS: " + getParser().toJson(contentExecuteRequest));
        if (contentExecuteRequest.isStoreHistory())
            executionNumber++;
        // TODO evaluate user Expressions

        processExecuteResult(parentHeader, contentExecuteRequest.getCode(), executionNumber, metadata);
        sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.EXECUTE_REPLY), parentHeader, metadata, new ContentExecuteReplyOk(executionNumber));
    }
    
    /**
     * 
     */
	public void processExecuteResult(Header parentHeader, String code, int executionNumber, Map<String, String> metadata) {
		System.out.println("EXECUTE_RESULT");
        Map<String, String> data = new HashMap<>();
        data.put("text/plain", "kernel answer");
        data.put("text/plain", code);
        ContentExecuteResult content = new ContentExecuteResult(executionNumber, data, new HashMap<String, String>());
        sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.DISPLAY_DATA), parentHeader, metadata, content);
        sendMessage(getCommunication().getPublish(), createHeader(parentHeader.getSession(), MessageType.EXECUTE_RESULT), parentHeader, metadata, content);
	}

    /**
     * This method processes the history_request message and replies with a history_reply message.
     */
    @Override
    public void processHistoryRequest(Header parentHeader, Map<String, String> metadata) {
        sendMessage(getCommunication().getRequests(), createHeader(parentHeader.getSession(), MessageType.HISTORY_REPLY), parentHeader, metadata, new ContentHistoryReply());
    }
    
    /**
     * 
     */
	@Override
	public void processCompleteRequest(Header parentHeader, ContentCompleteRequest request, Map<String, String> metadata) {
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
            new TestCaseServer(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	@Override
	public ILanguageProtocol makeInterpreter(String source, String moduleName, String variableName, String... salixPath) throws IOException, URISyntaxException {
		// TODO Auto-generated method stub
		return null;
	}
}
