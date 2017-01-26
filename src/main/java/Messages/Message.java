package Messages;

import communication.Header;

/**
 * Created by Mauricio on 18/01/2017.
 */
public class Message {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

    private String zmqIdentity;

    private String hmacSignature;

    private Header header;

    private Header parentHeader;

    private Content content;

    private String metadata;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public Message() {

    }

    public Message(String zmqIdentity, String hmacSignature, Header header, Header parentHeader, Content content, String metadata) {
        this.zmqIdentity = zmqIdentity;
        this.hmacSignature = hmacSignature;
        this.header = header;
        this.parentHeader = parentHeader;
        this.content = content;
        this.metadata = metadata;
    }

    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------

    public String getZmqIdentity() {
        return zmqIdentity;
    }

    public String getHmacSignature() {
        return hmacSignature;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Header getParentHeader() {
        return parentHeader;
    }

    public void setParentHeader(Header parentHeader) {
        this.parentHeader = parentHeader;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
