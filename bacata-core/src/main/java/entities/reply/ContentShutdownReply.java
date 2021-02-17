package entities.reply;

import entities.util.Content;
import entities.util.MessageType;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentShutdownReply extends Content {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------
    @SuppressWarnings("unused")
	private boolean restart;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public ContentShutdownReply(boolean pRestart) {
        this.restart = pRestart;
    }

    @Override
    public String getMessageType() {
        return MessageType.SHUTDOWN_REPLY;
    }
    
}
