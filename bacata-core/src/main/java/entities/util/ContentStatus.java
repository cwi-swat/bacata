package entities.util;

/**
 * Created by Mauricio on 27/01/2017.
 */
public class ContentStatus extends Content {
    @SuppressWarnings("unused")
	private String executionState;

    public ContentStatus(String executionState) {
        this.executionState = executionState;
    }

    @Override
    public String getMessageType() {
        return MessageType.STATUS;
    }
}
