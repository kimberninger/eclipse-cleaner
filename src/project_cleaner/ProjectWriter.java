package project_cleaner;

import java.io.IOException;

public interface ProjectWriter<T> extends AutoCloseable {
    void putNextEntry(T entry) throws IOException;
    void write(byte[] b, int off, int len) throws IOException;
}
