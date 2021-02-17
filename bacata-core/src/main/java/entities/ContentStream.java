package entities;

import entities.util.Content;
import entities.util.MessageType;

public class ContentStream extends Content {
	public final static String STD_ERR = "stderr";
	public final static String STD_OUT = "stdout";
	
	private final String name;
	private final String text;

	public ContentStream(String name, String text) {
		super();
		this.name = name;
		this.text = text;
	}
	
	public String getName() {
		return name;
	}

	public String getText() {
		return text;
	}

	@Override
	public String getMessageType() {
		return MessageType.STREAM;
	}
}
