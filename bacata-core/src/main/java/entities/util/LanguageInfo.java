package entities.util;

/**
 * Created by Mauricio on 19/01/2017.
 */
public class LanguageInfo {


    // -----------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------

    public static final String LANGUAGE_NAME = "Rascal";

    public static final String LANGUAGE_VERSION = "0.19.3-SNAPSHOT";

    public static final String LANGUAGE_MIMETYPE = "text/plain";

    public static final String LANGUAGE_EXTENSION = ".rsc";

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

    private String name;

    private String version;

    private String mimetype;

    private String fileExtension;

    private String pygmentsLexer;

    @SuppressWarnings("unused")
	private CodeMirrorMode codemirrorMode;

    private String nbconvertExporter;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public LanguageInfo(String codemirrorName) {
        this.name = LANGUAGE_NAME;
        this.version = LANGUAGE_VERSION;
        this.mimetype = LANGUAGE_MIMETYPE;
        this.fileExtension = LANGUAGE_EXTENSION;
        this.pygmentsLexer = null;
        this.codemirrorMode = new CodeMirrorMode(codemirrorName);
        this.nbconvertExporter = null;
    }

    public LanguageInfo(String name, String version, String mimetype, String file_extension) {
        this.name = name;
        this.version = version;
        this.mimetype = mimetype;
        this.fileExtension = file_extension;
        this.pygmentsLexer = null;
        this.codemirrorMode = null;
        this.nbconvertExporter = null;
    }

    // -----------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------

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

    public String getPygmentsLexer() {
        return pygmentsLexer;
    }

    public String getNbconvertExporter() {
        return nbconvertExporter;
    }

    public void setPygmentsLexer(String pygmentsLexer) {
        this.pygmentsLexer = pygmentsLexer;
    }

    public void setNbconvertExporter(String nbconvertExporter) {
        this.nbconvertExporter = nbconvertExporter;
    }
}
