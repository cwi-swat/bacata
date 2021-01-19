package org.rascalmpl.notebook;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

// TODO: UTF8 character encoding
public class StringWriterOutputStream extends OutputStream {
    private final StringWriter writer;

    StringWriterOutputStream(StringWriter writer) {
        this.writer = writer;
    }
    
    @Override
    public void write(int b) throws IOException {
        writer.write((char) b);
    }

}
