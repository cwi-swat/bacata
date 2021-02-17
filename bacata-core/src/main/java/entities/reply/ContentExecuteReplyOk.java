package entities.reply;

import entities.util.Content;
import entities.util.MessageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Mauricio on 27/01/2017.
 */
public class ContentExecuteReplyOk extends Content {

    private String status;

    /**
     * This field represents the global kernel counter that increases by one with each request that stores history
     */
    private int executionCount;

    /**
     * This field represents a way to trigger frontend actions from the kernel.
     * (Payloads are considered deprecated, though their replacement is not yet implemented.)
     */
    @SuppressWarnings("unused")
	private List<Map<String, String>> payload;

    /**
     * This field represents the results for the user_expressions
     */
    @SuppressWarnings("unused")
	private Map<String, String> userExpressions;

    public ContentExecuteReplyOk(int executionCount) {
        this.status = "ok";
        this.executionCount = executionCount;
        this.payload = new ArrayList<Map<String, String>>();
        this.userExpressions = new HashMap<String, String>();
    }

    public String getStatus() {
        return status;
    }

    public int getExecutionCount() {
        return executionCount;
    }

    @Override
    public String getMessageType() {
        return MessageType.EXECUTE_REPLY;
    }
}
