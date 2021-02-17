package entities.reply;

import entities.util.Content;
import entities.util.MessageType;

import java.util.Map;

public class ContentExecuteResult extends Content {
	private final int executionCount;
    private final Map<String, String> data;
    private final Map<String, String> metadata;

    public ContentExecuteResult(int executionCount, Map<String, String> data, Map<String, String> metadata) {
        this.executionCount = executionCount;
        this.data = data;
        this.metadata = metadata;
    }

    @Override
    public String getMessageType() {
        return MessageType.EXECUTE_RESULT;
    }

    public int getExecutionCount() {
        return executionCount;
    }

    public Map<String, String> getData() {
        return data;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
