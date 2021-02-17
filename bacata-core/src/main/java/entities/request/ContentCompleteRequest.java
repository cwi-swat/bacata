package entities.request;

import entities.util.Content;
import entities.util.MessageType;

public class ContentCompleteRequest extends Content{
	
    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------
	
	/**
	 * The code context in which completion is requested
	 */
	private String code;
	
	/**
	 * The cursor position within 'code' (in unicode characters) where completion is requested.
	 */
	private int cursorPos;

    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------
	
	/**
	 * 
	 * @return
	 */
	public String getCode() {
		return code;
	}

	/**
	 * 
	 * @return
	 */
	public int getCursorPosition() {
		return cursorPos;
	}

	@Override
	public String getMessageType() {
		return MessageType.COMPLETE_REQUEST;
	}
	
}
