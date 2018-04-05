package entities.reply;

import entities.util.Content;

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
    
}
