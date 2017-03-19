package entities;

import entities.util.Content;

public class ContentExecuteInput extends Content{

	private String code;
	
	private int executionCount;

	public ContentExecuteInput(String code, int executionCount) {
		this.code = code;
		this.executionCount = executionCount;
	}
	
	
}
