package project_cleaner;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ProjectArchiveReader implements ProjectReader<ZipEntry> {
    private final ZipInputStream inputStream;

    public ProjectArchiveReader(File zipFile) throws IOException {
        inputStream = new ZipInputStream(
            new BufferedInputStream(
                new FileInputStream(zipFile)));
    }

    @Override
    public ZipEntry getNextEntry() throws IOException {
        return inputStream.getNextEntry();
    }

    @Override
    public int readEntry(byte[] b) throws IOException {
        return inputStream.read(b);
    }

    @Override
    public void close() throws Exception {
        inputStream.close();
    }
}
