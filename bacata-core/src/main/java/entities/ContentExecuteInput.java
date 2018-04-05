package entities;

import entities.util.Content;

public class ContentExecuteInput extends Content{

	@SuppressWarnings("unused")
	private String code;
	
	@SuppressWarnings("unused")
	private int executionCount;

	public ContentExecuteInput(String code, int executionCount) {
		this.code = code;
		this.executionCount = executionCount;
	}
	
}
