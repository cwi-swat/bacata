package entities.reply;

import entities.util.Content;

import java.util.Map;

/**
 * Created by Mauricio on 31/01/2017.
 */
public class ContentExecuteResult extends Content {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

    private int executionCount;

    private Map<String, String> data;

    private Map<String, String> metadata;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public ContentExecuteResult(int executionCount, Map<String, String> data, Map<String, String> metadata) {
        this.executionCount = executionCount;
        this.data = data;
        this.metadata = metadata;
    }


    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------
}
