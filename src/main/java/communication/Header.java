package communication;

/**
 * Created by Mauricio on 18/01/2017.
 */
public class Header {

    // -----------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------

    public final static String VERSION = "5.0";

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

    private String session;

    private String msgType;

    private String version;

    private String username;

    private String date;

    private String msgId;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public Header() {

    }

    public Header(String session, String msgType, String version, String username, String date, String msgId) {
        this.session = session;
        this.msgType = msgType;
        this.version = version;
        this.username = username;
        this.date = date;
        this.msgId = msgId;
    }

    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------

    public String getSession() {
        return session;
    }

    public String getMsgType() {
        return msgType;
    }

    public String getVersion() {
        return version;
    }

    public String getUsername() {
        return username;
    }

    public String getDate() {
        return date;
    }

    public String getMsgId() {
        return msgId;
    }
}
