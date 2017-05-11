package entities;

import entities.util.Content;

public class ContentStream extends Content{
	
	// -----------------------------------------------------------------
	// Fields
	// -----------------------------------------------------------------
	private String name;
	
	private String text;

	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------
	public ContentStream(String name, String text) {
		super();
		this.name = name;
		this.text = text;
	}
	
}
