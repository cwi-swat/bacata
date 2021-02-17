package entities.reply;

import entities.util.Content;
import entities.util.MessageType;

import java.util.List;

/**
 * Created by Mauricio on 31/01/2017.
 */
public class ContentExecuteReplyError extends Content {

    @SuppressWarnings("unused")
	private String status;

    /**
     * This field represents the exception name as a String
     */
    @SuppressWarnings("unused")
	private String ename;

    /**
     * This field represents the exception value, as a String
     */
    @SuppressWarnings("unused")
	private String evalue;

    /**
     * This field represents the traceback frames as Strings
     */
    @SuppressWarnings("unused")
	private List<String> traceback;

    public ContentExecuteReplyError(String ename, String evalue, List<String> traceback) {
        this.status = "error";
        this.ename = ename;
        this.evalue = evalue;
        this.traceback = traceback;
    }

    @Override
    public String getMessageType() {
        return MessageType.EXECUTE_REPLY;
    }
}
