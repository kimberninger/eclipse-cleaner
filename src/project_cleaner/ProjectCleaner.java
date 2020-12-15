package project_cleaner;

import java.util.Set;
import java.util.function.Function;
import java.util.Map;
import java.util.HashMap;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;

import javax.swing.SwingUtilities;

import project_cleaner.ui.ProjectCleanerDialog;

public class ProjectCleaner {
    private File submissionsFile;
    private File solutionFile;
    private File outputDirectory;

    private Set<File> files;

    private boolean compressProjects = false;

    private Map<String, Integer> fileNameOccurrences = new HashMap<>();

    public ProjectCleaner(
        File submissionsFile,
        File solutionFile,
        File fileListFile,
        File outputDirectory
    ) throws IOException {
        this.submissionsFile = submissionsFile;
        this.solutionFile = solutionFile;
        this.outputDirectory = outputDirectory;
        
        files = Files.readAllLines(fileListFile.toPath()).stream()
            .map(File::new)
            .collect(Collectors.toSet());
    }

    public void setCompressProjects(boolean compressProjects) {
        this.compressProjects = compressProjects;
    }

    public void cleanSubmissions() {
        try (var projects = Files.find(
            submissionsFile.toPath(), Integer.MAX_VALUE, (path, attributes) ->
                path.getFileName().toString().toLowerCase().endsWith(".zip"))) {
            projects.forEach(project -> {
                if (solutionFile.isDirectory()) {
                    try (var solutionReader = new ProjectDirectoryReader(solutionFile)) {
                        
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (solutionFile.getName().toLowerCase().endsWith(".zip")) {
                    try (var solutionReader = new ProjectArchiveReader(solutionFile)) {
                        
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            System.err.format(
                "Das Verzeichnis %s konnte nicht durchsucht werden",
                submissionsFile);
        }
    }

    private <I, O> void cleanProject(
        Path projectPath,
        ProjectReader<I> solutionReader,
        Function<? super I, ? extends O> entryMapper
    ) {
        try {
            var submissionReader = new ProjectArchiveReader(projectPath.toFile());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private <I, O> void hurr(
        ProjectReader<I> reader,
        ProjectWriter<O> writer,
        Function<? super I, ? extends O> entryMapper
    ) throws IOException {
        byte[] buffer = new byte[2048];
        I entry;
        while ((entry = reader.getNextEntry()) != null) {
            writer.putNextEntry(entryMapper.apply(entry));

            int entryLength;
            while ((entryLength = reader.readEntry(buffer)) > 0) {
                writer.writeEntry(buffer, 0, entryLength);
            }
        }
    }

    public static void main(String[] args) {
        /*
        switch (args.length) {
            case 0:
                var dialog = new ProjectCleanerDialog();
                System.setOut(dialog.getOut());
                System.setErr(dialog.getErr());
                SwingUtilities.invokeLater(dialog::start);
                break;
            case 4:
                try {
                    var cleaner = new ProjectCleaner(
                        new File(args[0]),
                        new File(args[1]), 
                        new File(args[2]),
                        new File(args[3]));
                    cleaner.cleanSubmissions();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            default:                
        }
        */
        try {
            run(args);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void run(String[] args) throws IOException {
        var cleaner = new ProjectCleaner(
            new File(args[0]),
            new File(args[1]), 
            new File(args[2]),
            new File(args[3]));
        
        cleaner.cleanProjects(
            new File("/Users/kimberninger/Downloads/cleanertest/extracted"),
            new File("/Users/kimberninger/Downloads/cleanertest/compressed.zip"));
    }

    private void cleanProjects(File inputFile, File outputFile) {
        try (
            var reader = new ProjectDirectoryReader(inputFile);
            var writer = new ProjectArchiveWriter(outputFile)
        ) {
            cleanProject(reader, writer, entry -> {
                var fileName = entry.toString();
                if (entry.toFile().isDirectory()) {
                    fileName += "/";
                }
                return new ZipEntry(fileName);
            });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
