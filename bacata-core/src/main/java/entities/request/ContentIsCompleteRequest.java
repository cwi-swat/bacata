package entities.request;

import entities.util.Content;
import entities.util.MessageType;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentIsCompleteRequest extends Content {
    private String code;

    public ContentIsCompleteRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String getMessageType() {
        return MessageType.IS_COMPLETE_REQUEST;
    }
}
