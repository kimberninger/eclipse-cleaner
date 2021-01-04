package project_cleaner;

import java.util.Set;
import java.util.function.Function;
import java.util.Map;
import java.util.HashMap;

import java.util.stream.Collectors;

import java.util.zip.ZipEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.SwingUtilities;

import project_cleaner.ui.MainGui;
import project_cleaner.ui.ProjectCleanerDialog;

public class ProjectCleaner {
	private File submissionsFile;
	private File solutionFile;
	private File outputDirectory;

	private Set<File> files;

	private boolean compressProjects = false;

	private Map<String, Integer> fileNameOccurrences = new HashMap<>();

	public ProjectCleaner(File submissionsFile, File solutionFile, File fileListFile, File outputDirectory)
			throws IOException {
		this.submissionsFile = submissionsFile;
		this.solutionFile = solutionFile;
		this.outputDirectory = outputDirectory;

		files = Files.readAllLines(fileListFile.toPath()).stream().map(File::new).collect(Collectors.toSet());
	}

	public void setCompressProjects(boolean compressProjects) {
		this.compressProjects = compressProjects;
	}

	public void cleanSubmissions() {
		if (submissionsFile.isDirectory()) {
			cleanSubmissionDirectory();
		} else if (submissionsFile.getName().endsWith(".zip")) {
		} else {
			System.err.println("Moodle-Abgaben konnten nicht geöffnet werden.");
		}
	}

	private void cleanSubmissionDirectory() {
		try (var projects = Files.find(submissionsFile.toPath(), Integer.MAX_VALUE,
				(path, attributes) -> path.getFileName().toString().toLowerCase().endsWith(".zip"))) {
			projects.forEach(this::cleanProjectWithSolutionDirectory);
		} catch (IOException e) {
			System.err.format("Das Verzeichnis %s konnte nicht durchsucht werden", submissionsFile);
		}
	}

	private void cleanProjectWithSolutionDirectory(Path projectArchive) {
		System.out.println(projectArchive.getFileName());
	}

	public static void main(String[] args) {

		switch (args.length) {
		case 0:
			try {
				for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
					if ("Nimbus".equals(info.getName())) {
						javax.swing.UIManager.setLookAndFeel(info.getClassName());
						break;
					}
				}
			} catch (ClassNotFoundException ex) {
				java.util.logging.Logger.getLogger(MainGui.class.getName()).log(java.util.logging.Level.SEVERE, null,
						ex);
			} catch (InstantiationException ex) {
				java.util.logging.Logger.getLogger(MainGui.class.getName()).log(java.util.logging.Level.SEVERE, null,
						ex);
			} catch (IllegalAccessException ex) {
				java.util.logging.Logger.getLogger(MainGui.class.getName()).log(java.util.logging.Level.SEVERE, null,
						ex);
			} catch (javax.swing.UnsupportedLookAndFeelException ex) {
				java.util.logging.Logger.getLogger(MainGui.class.getName()).log(java.util.logging.Level.SEVERE, null,
						ex);
			}
//                var dialog = new ProjectCleanerDialog();
//                System.setOut(dialog.getOut());
//                System.setErr(dialog.getErr());
//                SwingUtilities.invokeLater(dialog::start);
			java.awt.EventQueue.invokeLater(new Runnable() {
				public void run() {
					new MainGui().setVisible(true);
				}
			});
			break;
		case 4:
			var extractor = new SubmissionsExtractor(new File(args[0]), new File(args[1]), new File(args[2]),
					new File(args[3]), System.out, System.err);
			extractor.extract();
		default:
		}
	}

	private void cleanProjects(File inputFile, File outputFile) {
		try (var reader = new ProjectDirectoryReader(inputFile); var writer = new ProjectArchiveWriter(outputFile)) {
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

	private <I, O> void cleanProject(ProjectReader<I> reader, ProjectWriter<O> writer,
			Function<? super I, ? extends O> entryMapper) throws IOException {
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
}