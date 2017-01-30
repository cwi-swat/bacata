package entities;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentShutdownRequest extends Content {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------
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
    public boolean getRestart() {
        return restart;
    }
}
