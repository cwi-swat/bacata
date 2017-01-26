package Messages.Content;

import Messages.LanguageInfo;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentKernelInfoReply extends Content {

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

    private String protocolVersion;

    private String implementation;

    private String implementationVersion;

    private LanguageInfo languageInformation;

    private String banner;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public ContentKernelInfoReply(String protocolVersion, String implementation, String implementationVersion, LanguageInfo languageInformation, String banner) {
        this.protocolVersion = protocolVersion;
        this.implementation = implementation;
        this.implementationVersion = implementationVersion;
        this.languageInformation = languageInformation;
        this.banner = banner;
    }

    public ContentKernelInfoReply() {
        this.protocolVersion = "ss";
        this.implementation = "S";
        this.implementationVersion = "";
        this.languageInformation = new LanguageInfo("", "", "", "");
        this.banner = "";
    }
}
