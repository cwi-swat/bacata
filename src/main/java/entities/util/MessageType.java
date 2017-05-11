package entities.util;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class MessageType {

    // -----------------------------------------------------------------
    // Request constants
    // -----------------------------------------------------------------

    public final static String KERNEL_INFO_REQUEST = "kernel_info_request";

    public final static String SHUTDOWN_REQUEST = "shutdown_request";

    public final static String IS_COMPLETE_REQUEST = "is_complete_request";

    public final static String HISTORY_REQUEST = "history_request";

    public final static String EXECUTE_REQUEST = "execute_request";

    public final static String COMPLETE_REQUEST = "complete_request";

    public final static String INSPECT_REQUEST = "inspect_request";
    
    // -----------------------------------------------------------------
    // Reply constants
    // -----------------------------------------------------------------

    public final static String KERNEL_INFO_REPLY = "kernel_info_reply";

    public final static String SHUTDOWN_REPLY = "shutdown_reply";

    public final static String IS_COMPLETE_REPLY = "is_complete_reply";

    public final static String HISTORY_REPLY = "history_reply";

    public final static String EXECUTE_REPLY = "execute_reply";

    public final static String COMPLETE_REPLY = "complete_reply";

    public final static String INSPECT_REPLY = "inspect_reply";

    public final static String EXECUTE_RESULT = "execute_result";

    public final static String DISPLAY_DATA = "display_data";

    // -----------------------------------------------------------------
    // Common constants
    // -----------------------------------------------------------------
    public final static String STATUS = "status";
    
    public final static String EXECUTE_INPUT = "execute_input";
    
    public final static String STREAM = "stream";


}
