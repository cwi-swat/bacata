package entities.reply;

import java.util.Collections;
import java.util.List;

import entities.util.Content;
import entities.util.HelpLinks;
import entities.util.LanguageInfo;
import entities.util.MessageType;

/**
 * Created by Mauricio on 26/01/2017.
 */

public class ContentKernelInfoReply extends Content {
    private final String status;
	private final String protocolVersion;
	private final String implementation;
	private final String implementationVersion;
    private final LanguageInfo languageInfo;
	private final String banner;
    private final List<HelpLinks> helpLinks;

    public ContentKernelInfoReply(LanguageInfo language) {
        this.status = "ok";
        this.protocolVersion = "5.2";
        this.implementation = "bacata";
        this.implementationVersion = "0.1";
        this.languageInfo = language;
        this.banner = language.getName();
        this.helpLinks = Collections.emptyList();
    }

    @Override
    public String getMessageType() {
        return MessageType.KERNEL_INFO_REPLY;
    }

    public String getStatus() {
        return status;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public String getImplementation() {
        return implementation;
    }

    public String getImplementationVersion() {
        return implementationVersion;
    }

    public LanguageInfo getLanguageInfo() {
        return languageInfo;
    }

    public String getBanner() {
        return banner;
    }

    public List<HelpLinks> getHelpLinks() {
        return helpLinks;
    }
}
