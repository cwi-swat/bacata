package entities.request;

import entities.util.Content;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentShutdownRequest extends Content {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------
	
    /**
     * False if final shutdown, or True if shutdown precedes a restart
     */
	private boolean restart;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------
    public ContentShutdownRequest(boolean pRestart) {
        this.restart = pRestart;
    }

    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------
    /**
     * This method returns False if final shutdown, or True if shutdown precedes a restart.
     * @return
     */
    public boolean getRestart() {
        return restart;
    }
}
