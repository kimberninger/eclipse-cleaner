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
        if (submissionsFile.isDirectory()) {
            cleanSubmissionDirectory();
        } else if (submissionsFile.getName().endsWith(".zip")) {
            cleanSubmissionArchive();
        } else {
            System.err.println("Moodle-Abgaben konnten nicht geöffnet werden.");
        }
    }

    private void cleanSubmissionDirectory() {
        try (var projects = Files.find(
            submissionsFile.toPath(), Integer.MAX_VALUE, (path, attributes) ->
                path.getFileName().toString().toLowerCase().endsWith(".zip"))) {
            projects.forEach(this::cleanProjectWithSolutionDirectory);
        } catch (IOException e) {
            System.err.format(
                "Das Verzeichnis %s konnte nicht durchsucht werden",
                submissionsFile);
        }
    }
    
    private void cleanProjectWithSolutionDirectory(Path projectArchive) {
        System.out.println(projectArchive.getFileName());
    }
    
    private void cleanProjectWithSolutionArchive(Path projectArchive) {
        /*System.out.println(projectArchive.getFileName());
        var buffer = new byte[2048];

        try (
            var studentStream = new ZipInputStream(
                new BufferedInputStream(
                    new FileInputStream(projectArchive.toFile())));
            var solutionStream = new ZipInputStream(
                new BufferedInputStream(
                    new FileInputStream(solutionFile)));
            var outputStream = new ZipOutputStream(
                new BufferedOutputStream(
                    new FileOutputStream(cleanedFile)))) {
            ZipEntry inputEntry;

            while ((inputEntry = inputStream.getNextEntry()) != null) {
                var outputEntry = new ZipEntry(inputEntry.getName());
                outputStream.putNextEntry(outputEntry);
                
                int entryLength;
                while ((entryLength = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, entryLength);
                }
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }*/
    }

    private void cleanSubmissionArchive() {
        System.out.println("Zip!");
        /*var buffer = new byte[2048];
        try (
            var inputStream = new ZipInputStream(
                new BufferedInputStream(
                    new FileInputStream(solutionFile)));
            var outputStream = new ZipOutputStream(
                new BufferedOutputStream(
                    new FileOutputStream(cleanedFile)))) {
            ZipEntry inputEntry;

            while ((inputEntry = inputStream.getNextEntry()) != null) {
                var outputEntry = new ZipEntry(inputEntry.getName());
                outputStream.putNextEntry(outputEntry);
                
                int entryLength;
                while ((entryLength = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, entryLength);
                }
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }*/
    }

    public static void main(String[] args) {
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
        /*
        try {
            run(args);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        */
    }

    private static void run(String[] args) throws IOException {
        var cleaner = new ProjectCleaner(
            new File(args[0]),
            new File(args[1]), 
            new File(args[2]),
            new File(args[3]));
        
        cleaner.cleanProjects(
            new File("/Users/kimberninger/Downloads/cleanertest/H04_SOLUTION.zip"),
            new File("/Users/kimberninger/Downloads/cleanertest/extracted"));
    }

    private void cleanProjects(File inputFile, File outputFile) {
        try (
            var reader = new ProjectArchiveReader(inputFile);
            var writer = new ProjectDirectoryWriter(outputFile)
        ) {
            cleanProject(reader, writer, entry -> Path.of(entry.getName()));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private <I, O> void cleanProject(
        ProjectReader<I> reader,
        ProjectWriter<O> writer,
        Function<? super I, ? extends O> entryMapper
    ) throws IOException {
        byte[] buffer = new byte[2048];
        while (reader.hasEntry()) {
            var entry = reader.getNextEntry();
            writer.putNextEntry(entryMapper.apply(entry));

            int entryLength;
            while ((entryLength = reader.read(buffer)) > 0) {
                writer.write(buffer, 0, entryLength);
            }
        }
    }
}
