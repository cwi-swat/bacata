package communication;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Header {

    // -----------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------

    public final static String VERSION = "5.3";

    public final static String USERNAME = "Bacata";

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
    
    public Header(String session, String msgType, String version, String username) {
		this.session = session;
		this.msgType = msgType;
        this.version = version;
        this.username = username;
        
        this.date = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        this.msgId = String.valueOf(UUID.randomUUID());
    }
    
    public Header(String msgType, Header parentHeader) {
		this.session = parentHeader.getSession();
		this.msgType = msgType;
        this.version = parentHeader.getVersion();
        this.username = parentHeader.getUsername();
        
        this.date = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        this.msgId = String.valueOf(UUID.randomUUID());
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
