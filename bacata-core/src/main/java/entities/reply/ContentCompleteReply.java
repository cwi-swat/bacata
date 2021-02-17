package entities.reply;

import java.util.List;
import java.util.Map;

import entities.util.Content;
import entities.util.MessageType;

public class ContentCompleteReply extends Content{
	
    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------
	
	private List<String> matches;
	
	private int cursorStart;
	
	private int cursorEnd;
	
	private Map<String, String> metadata;
	
	private String status;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------
	
	/**
	 * 
	 * @param matches
	 * @param cursorStart
	 * @param cursorEnd
	 * @param metadata
	 * @param status
	 */
	public ContentCompleteReply(List<String> matches, int cursorStart, int cursorEnd, Map<String, String> metadata,
			String status) {
		this.matches = matches;
		this.cursorStart = cursorStart;
		this.cursorEnd = cursorEnd;
		this.metadata = metadata;
		this.status = status;
	}

    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------
	
	public List<String> getMatches() {
		return matches;
	}

	public int getCursorStart() {
		return cursorStart;
	}

	public int getCursorEnd() {
		return cursorEnd;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public String getStatus() {
		return status;
	}

	@Override
	public String getMessageType() {
		return MessageType.COMPLETE_REPLY;
	}
	
}
