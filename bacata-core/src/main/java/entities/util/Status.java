package entities.util;

/**
 * Created by Mauricio on 31/01/2017.
 */
public class Status {

    // -----------------------------------------------------------------
    // Request constants
    // -----------------------------------------------------------------

    /**
     * This constant is used when the code is ready to be executed
     */
    public final static String COMPLETE = "complete";

    /**
     * This constant is used when the code should prompt for another line
     */
    public final static String INCOMPLETE = "incomplete";

    /**
     * This constant is used when the code will typically be sent for execution, so that the user sees the error soonest
     */
    public final static String INVALID = "invalid";

    /**
     * This constant is used if the kernel is not able to determine this
     */
    public final static String UNKNOWN = "unknown";

    /**
     * This constant is used if the kernel is busy
     */
    public final static String BUSY = "busy";
    
    /**
     * This constant is used if the kernel is once at process startup.
     */
    public final static String STARTING = "starting";

    /**
     * This constant is used if the kernel is idle
     */
    public final static String IDLE = "idle";
    
    /**
     * This constant is used to inform an ok answer.
     */
    public final static String OK = "ok";
}
