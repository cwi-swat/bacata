package entities;

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
        this.protocolVersion = PROTOCOL_VERSION;
        this.implementation = IMPLEMENTATION;
        this.implementationVersion = IMPLEMENTATION_VERSION;
        this.languageInformation = new LanguageInfo();
        this.banner = BANNER;
    }
}
