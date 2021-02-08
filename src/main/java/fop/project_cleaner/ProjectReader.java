package fop.project_cleaner;

import java.io.IOException;

public interface ProjectReader<T> extends AutoCloseable {
    T getNextEntry() throws IOException;
    int readEntry(byte[] b) throws IOException;
}
