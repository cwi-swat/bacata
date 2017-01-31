package entities.util;

/**
 * Created by Mauricio on 19/01/2017.
 */
public class LanguageInfo {


    // -----------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------

    public static final String LANGUAGE_NAME = "Java";

    public static final String LANGUAGE_VERSION = "8.0";

    public static final String LANGUAGE_MIMETYPE = "application/java";

    public static final String LANGUAGE_EXTENSION = ".java";

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

    private String name;

    private String version;

    private String mimetype;

    private String file_extension;

    private String pygmentsLexer;

    private String codemirroMode;

    private String nbconvertExporter;

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------

    public LanguageInfo() {
        this.name = LANGUAGE_NAME;
        this.version = LANGUAGE_VERSION;
        this.mimetype = LANGUAGE_MIMETYPE;
        this.file_extension = LANGUAGE_EXTENSION;
        this.pygmentsLexer = null;
        this.codemirroMode = null;
        this.nbconvertExporter = null;
    }

    public LanguageInfo(String name, String version, String mimetype, String file_extension) {
        this.name = name;
        this.version = version;
        this.mimetype = mimetype;
        this.file_extension = file_extension;
        this.pygmentsLexer = null;
        this.codemirroMode = null;
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
        return file_extension;
    }

    public String getPygmentsLexer() {
        return pygmentsLexer;
    }

    public String getCodemirroMode() {
        return codemirroMode;
    }

    public String getNbconvertExporter() {
        return nbconvertExporter;
    }

    public void setPygmentsLexer(String pygmentsLexer) {
        this.pygmentsLexer = pygmentsLexer;
    }

    public void setCodemirroMode(String codemirroMode) {
        this.codemirroMode = codemirroMode;
    }

    public void setNbconvertExporter(String nbconvertExporter) {
        this.nbconvertExporter = nbconvertExporter;
    }
}
