package entities.util;

/**
 * Created by Mauricio on 27/01/2017.
 */
public class ContentStatus extends Content {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------
    @SuppressWarnings("unused")
	private String executionState;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------
    public ContentStatus(String executionState) {
        this.executionState = executionState;
    }
}
