package Messages;

/**
 * Created by Mauricio on 26/01/2017.
 */
public class ContentKernelInfoReply extends Content {
    private String protocolVersion;

    private String implementation;

    private String implementationVersion;

    private LanguageInfo languageInformation;

    private String banner;

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
//        cont.addProperty("implementation_version", "1.0");
//    LanguageInfo li = new LanguageInfo("java", "8.0", "application/java", ".java");
//        cont.add("language_info", new Gson().toJsonTree(li));
//        cont.addProperty("banner", "Java kernel banner");
}
