package Messages.Content;

import java.util.Map;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentExecuteRequest extends Content {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

    private String code;

    private boolean silent;

    private boolean storeHistory;

    private Map<String, String> userExpressions;

    private boolean allowStdin;

    private boolean stopOnError;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public ContentExecuteRequest() {

    }

    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------

    public String getCode() {
        return code;
    }

    public boolean isSilent() {
        return silent;
    }

    public boolean isStoreHistory() {
        return storeHistory;
    }

    public Map<String, String> getUserExpressions() {
        return userExpressions;
    }

    public boolean isAllowStdin() {
        return allowStdin;
    }

    public boolean isStopOnError() {
        return stopOnError;
    }
}
