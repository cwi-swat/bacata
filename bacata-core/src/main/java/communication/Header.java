package communication;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Header {
    private final String session;
    private final String msgType;
    private final String version;
    private final String username;
    private final String date;
    private final String msgId;

    public Header(String msgType) {
        this.session = "";
        this.msgType = msgType;
        this.version = "5.3";
        this.username = "guest";
        this.date = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        this.msgId = String.valueOf(UUID.randomUUID());
    }
    /**
     * Create header with specific msgType, inheriting properties
     * like session, version and username from the parentHeader.
     * Date and msgId are generated by now() and UUID.
     */
    public Header(String msgType, Header parentHeader) {
		this.session = parentHeader.getSession();
		this.msgType = msgType;
        this.version = parentHeader.getVersion();
        this.username = parentHeader.getUsername();
        
        this.date = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        this.msgId = String.valueOf(UUID.randomUUID());
    }

    public Header(String msgType, Header parentHeader, String msgId) {
		this.session = parentHeader.getSession();
		this.msgType = msgType;
        this.version = parentHeader.getVersion();
        this.username = parentHeader.getUsername();
        
        this.date = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        this.msgId = msgId;
    }

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

    @Override
    public String toString() {
       return "Header[" + "session=" + session + ", type=" + msgType + ", version=" + version + ", username=" + username + ", date=" + date + ", msgId=" + msgId + "]";
    }
}
