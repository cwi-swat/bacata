package entities.reply;

import entities.util.Content;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mauricio on 31/01/2017.
 */
public class ContentHistoryReply extends Content {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

    private List<String> history;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------
    public ContentHistoryReply() {
        this.history = new ArrayList<String>();
    }
    
}
