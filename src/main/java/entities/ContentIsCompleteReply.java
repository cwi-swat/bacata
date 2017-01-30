package entities;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentIsCompleteReply extends Content {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

    private String status;

    private String indent;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public ContentIsCompleteReply(String status, String indent) {
        this.status = status;
        this.indent = indent;
    }
}
