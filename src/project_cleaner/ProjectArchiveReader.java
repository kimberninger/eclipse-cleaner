package project_cleaner;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ProjectArchiveReader implements ProjectReader<ZipEntry> {
    ZipInputStream inputStream;
    ZipEntry entry;

    public ProjectArchiveReader(File zipFile) throws IOException {
        inputStream = new ZipInputStream(
            new BufferedInputStream(
                new FileInputStream(zipFile)));
        entry = inputStream.getNextEntry();
    }

    @Override
    public boolean hasEntry() {
        return entry != null;
    }

    @Override
    public ZipEntry getNextEntry() throws IOException {
        inputStream.closeEntry();
        return entry = inputStream.getNextEntry();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return inputStream.read(b);
    }

    @Override
    public void close() throws Exception {
        inputStream.close();
    }
}
