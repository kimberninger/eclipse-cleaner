/**
 *
 */
package fop.project_cleaner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

/**
 * A Helper Class for working with {@link File}s
 *
 * @author Ruben Deisenroth
 *
 */
public final class FileUtils {
	/**
	 * Makes sure all the given directories exist inside the parent directory
	 *
	 * @param parentDir  the Parent Direcory
	 * @param folderName the Folder Names to ensure
	 * @return the Created Folders
	 */
	public static ArrayList<File> ensureDirectories(File parentDir, String... folderName) {
		if (!parentDir.isDirectory()) {
			throw new IllegalArgumentException("parentDir must be a directory");
		}
		ArrayList<File> createdDirs = new ArrayList<>();
		for (String f : folderName) {
			Path wantedFolder = Paths.get(parentDir.getAbsolutePath(), f);
			if (Files.notExists(Paths.get(parentDir.getAbsolutePath(), f))) {
				File newFolder = wantedFolder.toFile();
				if (!newFolder.mkdirs()) {
					System.err.println("Folder " + f + "could not be created");
				}
			}
			createdDirs.add(wantedFolder.toFile());
		}
		return createdDirs;
	}

	/**
	 * Move the content of a folder to a target folder recursively
	 *
	 * @param parentDir the source folder
	 * @param targetDir the destination folder
	 */
	public static void moveFolderContent(File parentDir, File targetDir) {
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
					System.err.println(e.getMessage());
				}
			}
		}
	}

	/**
	 * Copy the contents of a Folder to a target folder
	 *
	 * @param parentDir the source folder
	 * @param targetDir the destination folder
	 */
	public static void copyFolderContent(File parentDir, File targetDir) {
		if (!parentDir.isDirectory()) {
			throw new IllegalArgumentException("parentDir must be a directory");
		}
		if(!targetDir.isDirectory()) {
			throw new IllegalArgumentException("targetDir must be a directory");
		}
		for (File file : parentDir.listFiles()) {
			if (file.isDirectory()) {
				copyFolderContent(file, ensureDirectories(targetDir, file.getName()).get(0));
			} else {
				try {
					Files.copy(file.toPath(), Paths.get(targetDir.getAbsolutePath(), file.getName()),
							StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
			}
		}
	}

	/**
	 * Compares the content of two files
	 *
	 * @param f1 the first file
	 * @param f2 the second file
	 * @return true if contents equal
	 * @throws FileNotFoundException
	 */
	public static boolean filesEqual(File f1, File f2) throws FileNotFoundException {
		Scanner input1 = new Scanner(f1);// read first file
		Scanner input2 = new Scanner(f2);// read second file

		while (input1.hasNextLine() && input2.hasNextLine()) {
			var first = input1.nextLine();
			var second = input2.nextLine();

			if (!first.equals(second)) {
				System.out.println("Differences found: " + "\n" + first + '\n' + second);
				input1.close();
				input2.close();
				return false;
			}
		}
		input1.close();
		input2.close();
		return true;
	}

	/**
	 * remove all the Contents of a given Folder
	 *
	 * @param folder the folder to clear
	 */
	public static void clearFolder(File folder) {
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
	public static void removeFolders(File... folder) {
		for (File f : folder) {
			clearFolder(f);
			// The directory is now empty so we delete it
			f.delete();
		}
	}

	/**
	 * Ensures a Folder is empty, prompts a dialog asking to clear it if not empty
	 *
	 * @param directory the folder to ensure is empty
	 * @param whitelist a List of files that are allowed in the folder
	 * @return true if the folder is empty now, false if not
	 */
	public static boolean EnsureEmpty(File directory, String... whitelist) {
		if (!directory.isDirectory()) {
			System.err.println("parentDir must be a directory");
			return false;
		}
		for (final File f : directory.listFiles()) {
			if (!Arrays.stream(whitelist).anyMatch(x -> x == f.getName())) {
				if (JOptionPane.showConfirmDialog(null, "The directory " + directory.getAbsolutePath()
						+ " contains Files but must be empty inorder to proceed.\n Do you want to clear the folder?",
						"Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					System.out.println("Clearing Folder " + directory.getAbsolutePath());
					clearFolder(directory);
					return true;
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * Extracts all the contents of a ZIP-Archive
	 *
	 * @param zipFile       the ZIP-Archive
	 * @param extractFolder the destination directory
	 */
	public static void extractFolder(String zipFile, String extractFolder) {
		try {
			int BUFFER = 2048;
			File file = new File(zipFile);

			ZipFile zip = new ZipFile(file);
			String newPath = extractFolder;

			new File(newPath).mkdir();
			Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();

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
			System.err.println("ERROR: " + e.getMessage());
		}
	}

	/**
	 * Extracts all the contents of a ZIP-Archive
	 *
	 * @param zipFile       the ZIP-Archive
	 * @param extractFolder the destination directory
	 * @param bar           the Progress bar to track the progress with
	 */
	public static void extractFolder(String zipFile, String extractFolder, JProgressBar bar) {
		try {
			int BUFFER = 2048;
			File file = new File(zipFile);

			ZipFile zip = new ZipFile(file);
			String newPath = extractFolder;

			new File(newPath).mkdir();
			var zipFileEntries = Collections.list(zip.entries());
			bar.setMinimum(0);
			bar.setValue(0);
			bar.setMaximum(zipFileEntries.size());
			bar.setEnabled(true);
			bar.setStringPainted(true);
			bar.setString(String.format("%s/%s Dateien entpackt", bar.getValue(), bar.getMaximum()));
			int done = 0;
			// Process each entry
			for (ZipEntry entry : zipFileEntries) {
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
				bar.setValue(++done);
				bar.setString(String.format("%s/%s Dateien entpackt", bar.getValue(), bar.getMaximum()));
			}
			zip.close();
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
		}
	}

	/**
	 * Creates a Text file and writes (or overwrites) the given String as content of
	 * that file
	 *
	 * @param p the {@link Path} to create the File
	 * @param s the new File Content
	 * @return the created/Modified text-{@link File}
	 */
	public static File createTextFile(Path p, String s) {
		File textFile = new File(p.toAbsolutePath().toString());
		PrintWriter textFileWriter;
		try {
			textFileWriter = new PrintWriter(textFile);
			textFileWriter.print(s);
			textFileWriter.flush();
			textFileWriter.close();
		} catch (FileNotFoundException e) {
			// We should never end up here, since we create the File
			e.printStackTrace();
		}
		return textFile;
	}
}
