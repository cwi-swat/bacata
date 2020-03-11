package entities.request;

import entities.util.Content;

import java.util.Map;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentExecuteRequest extends Content {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

	/**
	 * Source code to be executed by the kernel, one or more lines.
	 */
    private String code;

    /**
     * This flag signals the kernel to execute this code as quietly as possible.
     */
    private boolean silent;

    /**
     * Boolean flag which, if True, signals the kernel to populate history.
     * The default is True if silent is False.  If silent is True, store_history is forced to be False.
     */
    private boolean storeHistory;

    /**
     * A dict mapping names to expressions to be evaluated in the user's dict.
     */
    private Map<String, String> userExpressions;

    private boolean allowStdin;

    private boolean stopOnError;

    /**
     * Represents the cell id of a code cell. This is used for the execution graph.
     */
    private String cellId;
    
    /**
     * Represents the current cell in the execution graph.
     */
    private String currentCell;
    

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

	public String getCellId() {
		return cellId;
	}

	public String getCurrentCell() {
		return currentCell;
	}
	
}
