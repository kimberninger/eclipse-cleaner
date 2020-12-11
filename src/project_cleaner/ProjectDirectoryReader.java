package project_cleaner;

import java.util.List;
import java.util.stream.Collectors;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;

public class ProjectDirectoryReader implements ProjectReader<Path> {
    private List<Path> entries;
    private InputStream inputStream;

    private int index = 0;

    public ProjectDirectoryReader(File directory) throws IOException {
        try (var files = Files.walk(directory.toPath())) {
            entries = files.collect(Collectors.toList());
        }
    }

    @Override
    public Path getNextEntry() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
        if (index + 1 >= entries.size()) {
            return null;
        }

        index++;

        var entry = entries.get(index);
        inputStream = new BufferedInputStream(
            new FileInputStream(entry.toFile()));
        return entry;
    }

    @Override
    public int readEntry(byte[] b) throws IOException {
        return inputStream.read(b);
    }

    @Override
    public void close() throws Exception {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}
