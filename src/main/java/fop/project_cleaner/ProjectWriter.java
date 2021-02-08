package fop.project_cleaner;

import java.io.IOException;

public interface ProjectWriter<T> extends AutoCloseable {
    void putNextEntry(T entry) throws IOException;
    void writeEntry(byte[] b, int off, int len) throws IOException;
}
