package entities.reply;

import entities.util.Content;
import entities.util.MessageType;

/**
 * Created by Mauricio on 31/01/2017.
 */
public class ContentExecuteReplyAbort extends Content {
    private String status;

    public ContentExecuteReplyAbort() {
        this.setStatus("abort");
    }

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String getMessageType() {
		return MessageType.EXECUTE_REPLY;
	}
}
