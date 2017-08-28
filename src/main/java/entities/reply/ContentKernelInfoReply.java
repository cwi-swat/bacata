package entities.reply;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entities.util.Content;
import entities.util.HelpLinks;
import entities.util.LanguageInfo;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentKernelInfoReply extends Content {

    // -----------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------

    public final static String PROTOCOL_VERSION = "5.0";

    public final static String IMPLEMENTATION = "Java kernel";

    public final static String IMPLEMENTATION_VERSION = "0.1";

    public final static String BANNER = "Java Kernel Banner";

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

    private String protocolVersion;

    private String implementation;

    private String implementationVersion;

    private LanguageInfo languageInfo;

    private String banner;
    
    private List<HelpLinks> helpLinks;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public ContentKernelInfoReply(String protocolVersion, String implementation, String implementationVersion, LanguageInfo languageInformation, String banner, List<HelpLinks> links) {
        this.protocolVersion = protocolVersion;
        this.implementation = implementation;
        this.implementationVersion = implementationVersion;
        this.languageInfo = languageInformation;
        this.banner = banner;
        this.helpLinks = links;
    }

    /**
     * Kernel information for the Rascal Language.
     * @param codemirror
     */
    public ContentKernelInfoReply() {
        this.protocolVersion = PROTOCOL_VERSION;
        this.implementation = IMPLEMENTATION;
        this.implementationVersion = IMPLEMENTATION_VERSION;
        this.languageInfo = new LanguageInfo("rascal");
        this.banner = BANNER;
        this.helpLinks = new ArrayList<HelpLinks>();
        this.helpLinks.add(new HelpLinks("Rascal", "http://rascal-mpl.org/help/"));
        this.helpLinks.add(new HelpLinks("Rascal documentation", "http://tutor.rascal-mpl.org/Rascal/Rascal.html"));
        this.helpLinks.add(new HelpLinks("Rascal recipes", "http://tutor.rascal-mpl.org/Rascal/Recipes.html"));
        this.helpLinks.add(new HelpLinks("Rascal StackOverflow", "http://stackoverflow.com/questions/tagged/rascal"));
    }
    
    public ContentKernelInfoReply(LanguageInfo language) {
        this.protocolVersion = PROTOCOL_VERSION;
        this.implementation = IMPLEMENTATION;
        this.implementationVersion = IMPLEMENTATION_VERSION;
        this.languageInfo = language;
        this.banner = BANNER;
    }
}
