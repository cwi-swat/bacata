package entities;

import java.util.HashMap;
import java.util.Map;

import org.zeromq.ZFrame;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import communication.Header;
import entities.util.Content;


public class Message {
	
	//	  b'u-u-i-d',         # zmq identity(ies)
	//	  b'<IDS|MSG>',       # delimiter
	//	  b'baddad42',        # HMAC signature
	//	  b'{header}',        # serialized header dict
	//	  b'{parent_header}', # serialized parent header dict
	//	  b'{metadata}',      # serialized metadata dict
	//	  b'{content}',       # serialized content dict
	//	  b'\xf0\x9f\x90\xb1' # extra raw data buffer(s)
	class MessageParts {
		
        public static final int UUID = 0;
        
        public static final int DELIMETER = 1;
        
        public static final int HMAC = 2;
        
        public static final int HEADER = 3;
        
        public static final int PARENT_HEADER = 4;
        
        public static final int METADATA = 5;
        
        public static final int CONTENT = 6;
        
        public static final int EXTRA = 7;
        
    }  

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

    private String UUID;
    
    @SuppressWarnings("unused")
	private byte[] delimeter;

    private byte[] hmacSignature;

    private Header header;

    private Header parentHeader;

    private String rawContent;

    private Map<String,String> metadata;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public Message() {

    }

    public Message(Header header, Header parentHeader, String content, Map<String,String> metadata) {
        this.UUID = java.util.UUID.randomUUID().toString();
        this.header = header;
        this.parentHeader = parentHeader;
        this.rawContent = content;
        this.metadata = metadata;
    }
    
    @SuppressWarnings("unchecked")
	public Message(ZFrame[] zframes) {
    	Gson parser = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    	// Some required pre-processing
    	String rawMetadata = new String(zframes[MessageParts.METADATA].getData());
    	String rawParent = new String(zframes[MessageParts.PARENT_HEADER].getData());
    	String rawHeader = new String(zframes[MessageParts.HEADER].getData());
    	
    	this.UUID = new String(zframes[MessageParts.UUID].getData());
    	this.delimeter = zframes[MessageParts.DELIMETER].getData();
    	this.hmacSignature = zframes[MessageParts.HMAC].getData();
    	this.header = parser.fromJson(rawHeader, Header.class);
    	this.parentHeader = parser.fromJson(rawParent, Header.class);
    	this.metadata = parser.fromJson(rawMetadata, new HashMap<String,String>().getClass());		
    	this.rawContent = new String(zframes[MessageParts.CONTENT].getData());
    }

    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------

    public String getUUID() {
        return UUID;
    }

    public byte[] getHmacSignature() {
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

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String content) {
        this.rawContent = content;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
       return "Message[header=" + header + ", content=" + rawContent.substring(0, Math.min(16, rawContent.length())) + "]";
    }
}
