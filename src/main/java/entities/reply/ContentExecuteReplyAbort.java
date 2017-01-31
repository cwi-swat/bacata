package entities.reply;

import entities.util.Content;

/**
 * Created by Mauricio on 31/01/2017.
 */
public class ContentExecuteReplyAbort extends Content {

    private String status;

    public ContentExecuteReplyAbort() {
        this.status = "abort";
    }
}
