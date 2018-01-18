package entities.reply;

import entities.util.Content;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentIsCompleteReply extends Content {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

	/**
	 * One of: 'complete', 'incomplete', 'invalid' or 'unknown'
	 */
    private String status;

    /**
     * If status is 'incomplete' this field should contain the characters to indent next line. 
     * This is only a hint: front-ends may ignore it and use their own auto-indentation rules. 
     * For other statuses, this field does not exist.
     */
    private String indent;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public ContentIsCompleteReply(String status, String indent) {
        this.status = status;
        this.indent = indent;
    }
}
