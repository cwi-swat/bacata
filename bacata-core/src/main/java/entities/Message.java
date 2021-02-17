package entities;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.zeromq.ZFrame;

import communication.Header;


public class Message {
	
	//	  b'u-u-i-d',         # zmq identity(ies)
	//	  b'<IDS|MSG>',       # delimiter
	//	  b'baddad42',        # HMAC signature
	//	  b'{header}',        # serialized header dict
	//	  b'{parent_header}', # serialized parent header dict
	//	  b'{metadata}',      # serialized metadata dict
	//	  b'{content}',       # serialized content dict
	//	  b'\xf0\x9f\x90\xb1' # extra raw data buffer(s)
	private class MessageParts {
        public static final int sessionId = 0;
        // public static final int DELIMETER = 1;
        public static final int HMAC = 2;
        public static final int HEADER = 3;
        public static final int PARENT_HEADER = 4;
        public static final int METADATA = 5;
        public static final int CONTENT = 6;
        // public static final int EXTRA = 7;
    }  

    private final String sessionId;
    private final byte[] hmacSignature;
    private final Header header;
    private final Header parentHeader;
    private final String rawContent;
    private final Map<String,String> metadata;

    @SuppressWarnings("unchecked")
	public Message(ZFrame[] zframes) {
    	Gson parser = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    	// Some required pre-processing
    	String rawMetadata = new String(zframes[MessageParts.METADATA].getData());
    	String rawParent = new String(zframes[MessageParts.PARENT_HEADER].getData());
    	String rawHeader = new String(zframes[MessageParts.HEADER].getData());
    	
    	this.sessionId = new String(zframes[MessageParts.sessionId].getData());
    	this.hmacSignature = zframes[MessageParts.HMAC].getData();
        this.header = parser.fromJson(rawHeader, Header.class);
        this.parentHeader = parser.fromJson(rawParent, Header.class);
    	this.metadata = parser.fromJson(rawMetadata, new HashMap<String,String>().getClass());		
    	this.rawContent = new String(zframes[MessageParts.CONTENT].getData());
    }

    public String getSessionId() {
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
