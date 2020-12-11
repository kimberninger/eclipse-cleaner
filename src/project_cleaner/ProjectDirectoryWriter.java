package project_cleaner;

import java.io.File;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.file.Path;
import java.util.Optional;

public class ProjectDirectoryWriter implements ProjectWriter<Path> {
    private Path directory;
    private OutputStream outputStream;

    public ProjectDirectoryWriter(File directory) {
        this.directory = directory.toPath();
    }

    @Override
    public void putNextEntry(Path entry) throws IOException {
        if (outputStream != null) {
            outputStream.close();
        }

        var filePath = directory.resolve(
            entry.subpath(1, entry.getNameCount()));
        
        Optional.ofNullable(filePath.getParent())
            .ifPresent(p -> p.toFile().mkdirs());

        var file = filePath.toFile();
        file.createNewFile();
        outputStream = new BufferedOutputStream(new FileOutputStream(file));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
    }

    @Override
    public void close() throws Exception {
        outputStream.close();
    }
}
