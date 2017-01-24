package Messages;

/**
 * Created by Mauricio on 19/01/2017.
 */
public class LanguageInfo {

    private String name;

    private String version;

    private String mimetype;

    private String file_extension;

    private String pygmentsLexer;

    private String codemirroMode;

    private String nbconvertExporter;


    public LanguageInfo(String name, String version, String mimetype, String file_extension) {
        this.name = name;
        this.version = version;
        this.mimetype = mimetype;
        this.file_extension = file_extension;
        this.pygmentsLexer = null;
        this.codemirroMode = null;
        this.nbconvertExporter = null;
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
