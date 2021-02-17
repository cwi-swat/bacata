package entities.reply;

import entities.util.Content;
import entities.util.MessageType;

import java.util.ArrayList;
import java.util.List;

public class ContentHistoryReply extends Content {
	private final List<String> history;

    public ContentHistoryReply() {
        this.history = new ArrayList<String>();
    }
    
    @Override
    public String getMessageType() {
        return MessageType.HISTORY_REPLY;
    }

    public List<String> getHistory() {
        return history;
    }
}
