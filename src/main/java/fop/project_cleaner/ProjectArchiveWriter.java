package fop.project_cleaner;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ProjectArchiveWriter implements ProjectWriter<ZipEntry> {
    private ZipOutputStream outputStream;

    public ProjectArchiveWriter(File zipFile) throws IOException {
        outputStream = new ZipOutputStream(
            new BufferedOutputStream(
                new FileOutputStream(zipFile)));
    }

    @Override
    public void putNextEntry(ZipEntry entry) throws IOException {
        outputStream.putNextEntry(entry);
    }

    @Override
    public void writeEntry(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
    }

    @Override
    public void close() throws Exception {
        outputStream.close();
    }
}
