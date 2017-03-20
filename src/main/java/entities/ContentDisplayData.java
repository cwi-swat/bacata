package entities;

import entities.util.Content;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentDisplayData extends Content {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

	/**
	 * Contains key/value pairs, where the keys are MIME types and the values are the raw data of the representation in that format.
	 */
    private Map<String, String> data;

    /**
     * Metadata that describes the data
     */
    private Map<String, String> metadata;
    
    /**
     * Information not to be persisted to a notebook or other documents. Intended to live only during a live kernel session.
     */
    @SerializedName("transient")
    private Map<String, String> transients;
    

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public ContentDisplayData(Map<String, String> data, Map<String, String> metadata, Map<String, String> transients) {
        this.data = data;
        this.metadata = metadata;
        this.transients = transients;
    }
    
    public ContentDisplayData(){
    	this.data = new HashMap<String, String>();
    	this.metadata = new HashMap<String, String>();
    	this.transients = new HashMap<String, String>();
    }
    
    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------
    
    public void addData(String key, String value){
    	data.put(key, value);
    }

	public Map<String, String> getData() {
		return data;
	}
    
}
