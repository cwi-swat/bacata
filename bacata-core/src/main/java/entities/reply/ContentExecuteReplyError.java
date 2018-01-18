package entities.reply;

import entities.util.Content;

import java.util.List;

/**
 * Created by Mauricio on 31/01/2017.
 */
public class ContentExecuteReplyError extends Content {

    private String status;

    /**
     * This field represents the exception name as a String
     */
    private String ename;

    /**
     * This field represents the exception value, as a String
     */
    private String evalue;

    /**
     * This field represents the traceback frames as Strings
     */
    private List<String> traceback;

    public ContentExecuteReplyError(String ename, String evalue, List<String> traceback) {
        this.status = "error";
        this.ename = ename;
        this.evalue = evalue;
        this.traceback = traceback;
    }
}
