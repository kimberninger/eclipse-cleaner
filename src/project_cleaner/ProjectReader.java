package project_cleaner;

import java.io.IOException;

public interface ProjectReader<T> extends AutoCloseable {
    boolean hasEntry();
    T getNextEntry() throws IOException;
    int read(byte[] b) throws IOException;
}
