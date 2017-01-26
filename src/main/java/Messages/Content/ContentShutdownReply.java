package Messages.Content;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentShutdownReply extends Content {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------
    private boolean restart;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public ContentShutdownReply(boolean pRestart) {
        this.restart = pRestart;
    }
}
