package server;

import communication.Header;

/**
 * Created by Mauricio on 18/01/2017.
 */
public class Message {

//    b'u-u-i-d',         # zmq identity(ies)
//    b'<IDS|MSG>',       # delimiter
//    b'baddad42',        # HMAC signature
//    b'{header}',        # serialized header dict
//    b'{parent_header}', # serialized parent header dict
//    b'{metadata}',      # serialized metadata dict
//    b'{content}',       # serialized content dict
//    b'\xf0\x9f\x90\xb1' # extra raw data buffer(s)

    private String zmqIdentity;

    private String hmacSignature;

    private Header header;

    private String parentHeader;

    private String content;

    private String metadata;

    public Message() {

    }

    public Message(String zmqIdentity, String hmacSignature, Header header, String parentHeader, String content, String metadata) {
        this.zmqIdentity = zmqIdentity;
        this.hmacSignature = hmacSignature;
        this.header = header;
        this.parentHeader = parentHeader;
        this.content = content;
        this.metadata = metadata;
    }

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

    public String getParentHeader() {
        return parentHeader;
    }

    public void setParentHeader(String parentHeader) {
        this.parentHeader = parentHeader;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
