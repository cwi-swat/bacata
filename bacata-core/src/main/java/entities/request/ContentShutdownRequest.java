package entities.request;

import entities.util.Content;
import entities.util.MessageType;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentShutdownRequest extends Content {
    /**
     * False if final shutdown, or True if shutdown precedes a restart
     */
	private boolean restart;

    public ContentShutdownRequest(boolean pRestart) {
        this.restart = pRestart;
    }

    /**
     * This method returns False if final shutdown, or True if shutdown precedes a restart.
     * @return
     */
    public boolean getRestart() {
        return restart;
    }

    @Override
    public String getMessageType() {
        return MessageType.SHUTDOWN_REQUEST;
    }

}
