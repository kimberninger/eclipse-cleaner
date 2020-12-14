package project_cleaner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JOptionPane;

public class SubmissionsExtractor {
	private File submissionFile;
	private File outputDir;
	private PrintStream log = System.out;
	private PrintStream err = System.err;

	public SubmissionsExtractor(File submissionFile, File outputDir) {
		this.submissionFile = submissionFile;
		this.outputDir = outputDir;
	}

	public SubmissionsExtractor(File submissionFile, File outputDir, PrintStream log, PrintStream err) {
		this(submissionFile, outputDir);
		this.log = log;
		this.err = err;
	}

	public void extract() {
		log.println("Extracting Projects...");
		if (!EnsureEmpty(outputDir)) {
			err.println("Target directory not empty, aborting");
			return;
		}
		// Give us space to work with
		var tempDirs = ensureDirectories(outputDir, "tempCurrentSub", "tempAllSubs", "faulty");
		File tempCurrentSubFolder = tempDirs.get(0);
		File tempAllSubsFolder = tempDirs.get(1);
		File faultyDir = tempDirs.get(2);
		// Extract the main Zip File
		extractFolder(submissionFile.getAbsolutePath(), tempAllSubsFolder.getAbsolutePath());
		// Extract The individual submissions
		for (File submission : tempAllSubsFolder.listFiles()) {
			if (!submission.isDirectory() || submission.listFiles().length != 1) {
				err.println("Cannot Extract submission " + submission.getName()
						+ " (maybe you didn't download compressed submissions?)");
				moveFolderContent(submission, faultyDir);
				continue;
			}
			// Extract current Submission to tempCurrentSubFolder
			File submissionZip = submission.listFiles()[0];
			if (!submissionZip.getName().endsWith(".zip")) {
				err.println("Cannot Extract submission " + submission.getName()
						+ " (Not a Zip, maybe you didn't download compressed submissions?)");
				moveFolderContent(submission, faultyDir);
				continue;
			}
			// Naming convention check 1
			if (submissionZip.getName().contains("NACHNAME_VORNAME")
					|| !submissionZip.getName().matches("H[0-9]+_[a-zA-Z\\-]+(_[a-zA-Z\\-]+)+.zip")) {
				err.println("Namenskonvention verletzt: " + submission.getName());
				moveFolderContent(submission, faultyDir);
				continue;
			}
			clearFolder(tempCurrentSubFolder);
			extractFolder(submissionZip.getAbsolutePath(), tempCurrentSubFolder.getAbsolutePath());
			// Naming convention check 2
			if (tempCurrentSubFolder.listFiles().length == 0) {
				err.println("Abgabeverzeichnis leer: " + submission.getName());
				moveFolderContent(submission, faultyDir);
				continue;
			}
			var submissionProjectFolder = tempCurrentSubFolder.listFiles()[0];
			if (submissionProjectFolder.getName().contains("NACHNAME_VORNAME")
					|| !submissionProjectFolder.getName().matches("H[0-9]+_[a-zA-Z\\-]+(_[a-zA-Z\\-]+)+")) {
				err.println("Namenskonvention verletzt in " + submission.getName() + ": "
						+ submissionProjectFolder.getName());
				moveFolderContent(submission, faultyDir);
				continue;
			}
			moveFolderContent(tempCurrentSubFolder, outputDir);
			continue;
		}
		log.println("Cleanup...");
		removeFolders(tempCurrentSubFolder, tempAllSubsFolder);
		log.println("Done :)");
	}

	private void extractFolder(String zipFile, String extractFolder) {
		try {
			int BUFFER = 2048;
			File file = new File(zipFile);

			ZipFile zip = new ZipFile(file);
			String newPath = extractFolder;

			new File(newPath).mkdir();
			var zipFileEntries = zip.entries();

			// Process each entry
			while (zipFileEntries.hasMoreElements()) {
				// grab a zip file entry
				ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
				String currentEntry = entry.getName();

				File destFile = new File(newPath, currentEntry);
				// destFile = new File(newPath, destFile.getName());
				File destinationParent = destFile.getParentFile();

				// create the parent directory structure if needed
				destinationParent.mkdirs();

				if (!entry.isDirectory()) {
					BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
					int currentByte;
					// establish buffer for writing file
					byte data[] = new byte[BUFFER];

					// write the current file to disk
					FileOutputStream fos = new FileOutputStream(destFile);
					BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);

					// read and write until last byte is encountered
					while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
						dest.write(data, 0, currentByte);
					}
					dest.flush();
					dest.close();
					is.close();
				}

			}
			zip.close();
		} catch (Exception e) {
			err.println("ERROR: " + e.getMessage());
		}

	}

	private boolean EnsureEmpty(File directory, String... whitelist) {
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("parentDir must be a directory");
		}
		for (final File f : directory.listFiles()) {
			if (!Arrays.stream(whitelist).anyMatch(x -> x == f.getName())) {
				if (JOptionPane.showConfirmDialog(null, "The directory " + directory.getAbsolutePath()
						+ " contains Files but must be empty inorder to proceed.\n Do you want to clear the folder?",
						"Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					log.println("Clearing Folder " + directory.getAbsolutePath());
					clearFolder(directory);
					return true;
				}
				return false;
			}
		}
		return true;
	}

	private void clearFolder(File folder) {
		if (!folder.isDirectory()) {
			throw new IllegalArgumentException("parentDir must be a directory");
		}
		for (File file : folder.listFiles()) {
			if (file.isDirectory())
				clearFolder(file);
			file.delete();
		}
	}

	/**
	 * remove given Folder(s) and their contents
	 * 
	 * @param folder the folders to remove
	 */
	private void removeFolders(File... folder) {
		for (File f : folder) {
			clearFolder(f);
			// The directory is now empty so we delete it
			f.delete();
		}
	}

	private void moveFolderContent(File parentDir, File targetDir) {
		if (!parentDir.isDirectory() || !targetDir.isDirectory()) {
			throw new IllegalArgumentException("parentDir must be a directory");
		}
		for (File file : parentDir.listFiles()) {
			if (file.isDirectory()) {
				moveFolderContent(file, ensureDirectories(targetDir, file.getName()).get(0));
				file.delete();
			} else {
				try {
					Files.move(file.toPath(), Paths.get(targetDir.getAbsolutePath(), file.getName()),
							StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					err.println(e.getMessage());
				}
			}
		}
	}

	/**
	 * Makes sure all the given directories exist inside the parent directory
	 * 
	 * @param parentDir  the Parent Direcory
	 * @param folderName the Folder Names to ensure
	 * @return the Created Folders
	 */
	private ArrayList<File> ensureDirectories(File parentDir, String... folderName) {
		if (!parentDir.isDirectory()) {
			throw new IllegalArgumentException("parentDir must be a directory");
		}
		ArrayList<File> createdDirs = new ArrayList<>();
		for (String f : folderName) {
			Path wantedFolder = Paths.get(parentDir.getAbsolutePath(), f);
			if (Files.notExists(Paths.get(parentDir.getAbsolutePath(), f))) {
				File newFolder = wantedFolder.toFile();
				if (!newFolder.mkdirs()) {
					err.println("Folder " + f + "could not be created");
				}
			}
			createdDirs.add(wantedFolder.toFile());
		}
		return createdDirs;
	}
}
