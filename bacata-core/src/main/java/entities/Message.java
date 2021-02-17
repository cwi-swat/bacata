package entities;

import java.util.Map;

import communication.Header;


public class Message {
    private final byte[] sessionId;
    private final byte[] hmacSignature;
    private final Header header;
    private final Header parentHeader;
    private final String rawContent;
    private final Map<String,String> metadata;

    public Message(byte[] sessionId, byte[] hmacSignature, Header header, Header parentHeader, String rawContent, Map<String,String> metadata) {
        this.sessionId = sessionId;
        this.hmacSignature = hmacSignature;
        this.header = header;
        this.parentHeader = parentHeader;
        this.rawContent = rawContent;
        this.metadata = metadata;
    }

    public byte[] getSessionId() {
        return sessionId;
    }

    public byte[] getHmacSignature() {
        return hmacSignature;
    }

    public Header getHeader() {
        return header;
    }

    public Header getParentHeader() {
        return parentHeader;
    }

    public String getRawContent() {
        return rawContent;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
       return "Message[header=" + header + ", content=" + rawContent.substring(0, Math.min(16, rawContent.length())) + "]";
    }
}
