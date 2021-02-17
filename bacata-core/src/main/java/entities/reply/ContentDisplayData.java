package entities.reply;

import java.util.Map;

import entities.util.Content;
import entities.util.MessageType;

public class ContentDisplayData extends Content{
	
    @SuppressWarnings("unused")
	private Map<String, String> data;

    @SuppressWarnings("unused")
	private Map<String, String> metadata;
    
    @SuppressWarnings("unused")
	private Map<String, String> trancient;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public ContentDisplayData(Map<String, String> data, Map<String, String> metadata, Map<String, String> trancient) {
        this.data = data;
        this.metadata = metadata;
        this.trancient = trancient;
    }

    @Override
    public String getMessageType() {
        return MessageType.DISPLAY_DATA;
    }

}
