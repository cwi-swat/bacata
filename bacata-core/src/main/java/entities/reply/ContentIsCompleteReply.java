package entities.reply;

import entities.util.Content;
import entities.util.MessageType;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentIsCompleteReply extends Content {

	/**
	 * One of: 'complete', 'incomplete', 'invalid' or 'unknown'
	 */
	private final String status;

    /**
     * If status is 'incomplete' this field should contain the characters to indent next line. 
     * This is only a hint: front-ends may ignore it and use their own auto-indentation rules. 
     * For other statuses, this field does not exist.
     */
	private final String indent;

    public ContentIsCompleteReply(String status, String indent) {
        this.status = status;
        this.indent = indent;
    }

    @Override
    public String getMessageType() {
        return MessageType.IS_COMPLETE_REPLY;
    }

    public String getStatus() {
        return status;
    }

    public String getIndent() {
        return indent;
    }
}
