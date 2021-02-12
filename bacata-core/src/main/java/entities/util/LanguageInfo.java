package entities.util;

/**
 * Created by Mauricio on 19/01/2017.
 */
public class LanguageInfo {

    private final String name;
    private final String version;
    private final String mimetype;
    private final String fileExtension;

    // private final String pygmentsLexer;

	private final CodeMirrorMode codemirrorMode;

    // private String nbconvertExporter;

    public LanguageInfo(String name, String version, String mimetype, String file_extension) {
        this.name = name;
        this.version = version;
        this.mimetype = mimetype;
        this.fileExtension = file_extension;
        // this.pygmentsLexer = null;
        this.codemirrorMode = null;
        // this.nbconvertExporter = null;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getMimetype() {
        return mimetype;
    }

    public String getFile_extension() {
        return fileExtension;
    }

    public CodeMirrorMode getCodemirrorMode() {
        return codemirrorMode;
    }

    // public String getPygmentsLexer() {
    //     return pygmentsLexer;
    // }

    // public String getNbconvertExporter() {
    //     return nbconvertExporter;
    // }

    // public void setPygmentsLexer(String pygmentsLexer) {
    //     this.pygmentsLexer = pygmentsLexer;
    // }

    // public void setNbconvertExporter(String nbconvertExporter) {
    //     this.nbconvertExporter = nbconvertExporter;
    // }
}
