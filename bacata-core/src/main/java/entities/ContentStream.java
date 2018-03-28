package entities;

import entities.util.Content;

public class ContentStream extends Content{

	// -----------------------------------------------------------------
	// Constants
	// -----------------------------------------------------------------
	public final static String STD_ERR = "stderr";
	public final static String STD_OUT = "stdout";
	
	// -----------------------------------------------------------------
	// Fields
	// -----------------------------------------------------------------
	@SuppressWarnings("unused")
	private String name;
	
	@SuppressWarnings("unused")
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
