package project_cleaner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.w3c.dom.*;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class SubmissionsExtractor {
	private File submissionFile;
	private File outputDir;
	private PrintStream log = System.out;
	private PrintStream err = System.err;
	private File solutionArchive;
	private File fileList;

	private enum FileReadMode {
		ASSERT_EXISTS, OVERWRITE_ALWAYS, COPY_IF_NOT_EXISTS, ASSERT_NOT_EXISTS, IGNORE
	}

	public SubmissionsExtractor(File submissionFile, File outputDir) {
		this.submissionFile = submissionFile;
		this.outputDir = outputDir;
	}

	public SubmissionsExtractor(File submissionFile, File outputDir, PrintStream log, PrintStream err) {
		this(submissionFile, outputDir);
		this.log = log;
		this.err = err;
	}

	public SubmissionsExtractor(File submissionFile, File outputDir, File solutionArchive, PrintStream log,
			PrintStream err) {
		this(submissionFile, outputDir, log, err);
		this.solutionArchive = solutionArchive;
	}

	public SubmissionsExtractor(File submissionFile, File outputDir, File solutionArchive, File fileList,
			PrintStream log, PrintStream err) {
		this(submissionFile, outputDir, solutionArchive, log, err);
		this.fileList = fileList;
	}

	public void extract() {
		log.println("Vorbereitung...");
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
		File solutionFolder = null;
		ArrayList<Path> filesToAssertExist = new ArrayList<>();
		ArrayList<Path> filesToOverwrite = new ArrayList<>();
		ArrayList<Path> filesToCopyIfNotExist = new ArrayList<>();
		ArrayList<Path> filesToAssertNotExist = new ArrayList<>();
		ArrayList<Path> filesToIgnore = new ArrayList<>();
		if (solutionArchive != null) {
			// Extract the Solution Project
			extractFolder(solutionArchive.getAbsolutePath(), outputDir.getAbsolutePath());
			if (!Stream.of(outputDir.listFiles()).anyMatch(x -> x.getName().endsWith("SOLUTION"))) {
				System.err.println("Faulty solution");
				return;
			}
			solutionFolder = Stream.of(outputDir.listFiles()).filter(x -> x.getName().endsWith("SOLUTION")).findFirst()
					.get();
			if (fileList != null) {
				try {
					String solPath = solutionFolder.getAbsolutePath();
					BufferedReader fileListReader = new BufferedReader(new FileReader(fileList));
					String currentLine;
					int lineNumber = 0;
					FileReadMode currentMode = FileReadMode.ASSERT_EXISTS;
					// Read File Line By Line
					while ((currentLine = fileListReader.readLine()) != null) {
						lineNumber++;
						// Comments
						if (currentLine.startsWith("#"))
							continue;
						// Mode switcher
						if (currentLine.startsWith("[")) {
							if (!currentLine.endsWith("]")) {
								System.err.println("Faulty FileList Line:" + lineNumber);
							}
							switch (currentLine.toLowerCase()) { // Case insensitive matching
							case "[assert_exists]":
								currentMode = FileReadMode.ASSERT_EXISTS;
								continue;
							case "[overwrite_always]":
								currentMode = FileReadMode.OVERWRITE_ALWAYS;
								continue;
							case "[copy_if_not_exists]":
								currentMode = FileReadMode.COPY_IF_NOT_EXISTS;
								continue;
							case "[assert_not_exists]":
								currentMode = FileReadMode.ASSERT_NOT_EXISTS;
								continue;
							case "[ignore]":
								currentMode = FileReadMode.IGNORE;
								continue;
							default:
								System.err.println("Unknown File Read Mode: " + currentLine);
								continue;
							}
						}
						// Reading
						switch (currentMode) {
						case ASSERT_EXISTS:
							filesToAssertExist.add(Paths.get(solPath, currentLine));
							break;
						case ASSERT_NOT_EXISTS:
							filesToAssertNotExist.add(Paths.get(solPath, currentLine));
							break;
						case OVERWRITE_ALWAYS:
							filesToOverwrite.add(Paths.get(solPath, currentLine));
							break;
						case COPY_IF_NOT_EXISTS:
							filesToCopyIfNotExist.add(Paths.get(solPath, currentLine));
							break;
						case IGNORE:
							filesToIgnore.add(Paths.get(solPath, currentLine));
							break;
						default:
							break;
						}
					}
					System.out.println("Files to Assert exist: " + filesToAssertExist.toString());
					System.out.println("Files to Assert not exist: " + filesToAssertNotExist.toString());
					System.out.println("Files to overwrite: " + filesToOverwrite.toString());
					System.out.println("Files to ignore: " + filesToIgnore.toString());

					// Close the input stream
					fileListReader.close();
					// filesToEvaluate = (ArrayList<Path>) Files.lines(fileList.toPath()).filter(x
					// -> !x.startsWith("#"))
					// .map(x -> Paths.get(solPath,
					// x).toAbsolutePath()).collect(Collectors.toList());
				} catch (Exception e) {
					err.print(e.getMessage());
					fileList = null;
				}
			}
		}
		// Extract The individual submissions
		int fileCount = 0;
		int successfullCount = 0;
		log.println("Extracting Projects...");
		for (File submission : tempAllSubsFolder.listFiles()) {
			fileCount++;
			if (!submission.isDirectory() || submission.listFiles().length != 1) {
				err.println("Cannot Extract submission " + submission.getName()
						+ " (maybe you didn't download compressed submissions?)");
				moveFolderContent(submission, faultyDir);
				continue;
			}
			String submittorName = submission.getName().split("_")[0];
			log.println("Extracting Submission from " + submittorName);
			// Extract current Submission to tempCurrentSubFolder
			File submissionZip = submission.listFiles()[0];
			if (!submissionZip.getName().endsWith(".zip")) {
				err.println("Cannot Extract submission " + submission.getName()
						+ " (Not a Zip, maybe you didn't download compressed submissions?)");
				moveFolderContent(submission, faultyDir);
				continue;
			}
			// Naming convention check 1 (now unnecessarty)
			/*
			 * if(!submissionZip.getName().matches(
			 * "H[0-9]+_(?!(?i)NACHNAME_VORNAME(?-i))[a-zA-Z\\-]+(_[a-zA-Z\\-]+)+.zip")) {
			 * log.
			 * println("Namenskonvention leicht verletzt, KEIN PUNKTABZUG(abgabearchiv): " +
			 * submission.getName()); moveFolderContent(submission, faultyDir); continue; }
			 */
			clearFolder(tempCurrentSubFolder);
			extractFolder(submissionZip.getAbsolutePath(), tempCurrentSubFolder.getAbsolutePath());
			// Naming convention check 2
			if (tempCurrentSubFolder.listFiles().length == 0) {
				err.println("Abgabeverzeichnis von " + submittorName + " leer: " + submission.getName());
				moveFolderContent(submission, faultyDir);
				continue;
			}
			File submissionProjectFolder = tempCurrentSubFolder.listFiles()[0];
			/*
			 * Unnecessary if (!submissionProjectFolder.getName().matches(
			 * "H[0-9]+_(?!(?i)NACHNAME_VORNAME(?-i))[a-zA-Z\\-]+(_[a-zA-Z\\-]+)+")) {
			 * err.println("Namenskonvention verletzt in " + submission.getName() + ": " +
			 * submissionProjectFolder.getName()); moveFolderContent(submission, faultyDir);
			 * continue; }
			 */
			// Final Naming Convention Check and compatibility check
			boolean hadProjectFile = true;
			if (!Arrays.stream(submissionProjectFolder.listFiles()).anyMatch(x -> x.getName().equals(".project"))) {
				err.println("keine .project Datei bei " + submittorName);
				try {
					// Copy .project from Solution
					if (solutionArchive != null) {
						err.println("Kopiere .project Datei von Musterlösung");
						Files.copy(Paths.get(solutionFolder.getAbsolutePath(), ".project"),
								Paths.get(submissionProjectFolder.getAbsolutePath(), ".project"),
								StandardCopyOption.REPLACE_EXISTING);
					} else {
						moveFolderContent(submission, faultyDir);
						continue;
					}
				} catch (IOException e) {
					err.println("Error during .project copy: " + e.getMessage());
					continue;
				}
				hadProjectFile = false;
			}
			File projectFile = Paths.get(submissionProjectFolder.getAbsolutePath(), ".project").toFile();
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder;
			Document document;
			try {
				documentBuilder = documentBuilderFactory.newDocumentBuilder();
				document = documentBuilder.parse(projectFile);
				var projectName = document.getElementsByTagName("name").item(0);
				if (!projectName.getTextContent()
						.matches("H[0-9]+_(?!(?i)NACHNAME_VORNAME(?-i))[a-zA-Z\\-]+(_[a-zA-Z\\-]+)+")) {
					if (hadProjectFile) {
						err.println(
								"Namenskonvention verletzt bei " + submittorName + ": " + projectName.getTextContent());
					} else {
						err.println("passe Namenskonvention für " + submittorName + " an...");
					}
					// Get correct project name
					String newProjectName = submittorName.replace(" ", "_").replace("ä", "ae").replace("ö", "oe")
							.replace("ü", "ue").replace("ß", "ss");
					String hausuebungsprefix = solutionFolder == null ? "HXX_"
							: solutionFolder.getName().split("_")[0] + "_";
					err.println("Projekt nach " + hausuebungsprefix + newProjectName + " umbenannt");
					projectName.setTextContent(hausuebungsprefix + newProjectName);
					// Overwrite .project File
					// 4- Save the result to a new XML doc
					Transformer xformer = TransformerFactory.newInstance().newTransformer();
					xformer.transform(new DOMSource(document), new StreamResult(projectFile));

				}
				if (fileList != null && filesToAssertExist != null) {
					mergeProjectContent(solutionFolder, submissionProjectFolder, filesToAssertExist,
							filesToAssertNotExist, filesToOverwrite, filesToCopyIfNotExist, filesToIgnore);
				}
			} catch (Exception e) {
				err.println(e.getMessage());
			}
			// Project is ready to import, make sure foldername doesn't exist already
			if (Stream.of(outputDir.listFiles())
					.anyMatch(x -> x.isDirectory() && x.getName().equals(submissionProjectFolder.getName()))) {
				err.println("Folder named " + submissionProjectFolder.getName() + " already exists. renaming to: "
						+ submissionProjectFolder.getName() + "(1)");
				File newProjectFolder = Paths
						.get(tempCurrentSubFolder.getAbsolutePath(), submissionProjectFolder.getName() + "(1)")
						.toFile();
				if (!submissionProjectFolder.renameTo(newProjectFolder)) {
					err.println("Could not rename, moving to faulty");
					moveFolderContent(tempCurrentSubFolder, faultyDir);
					continue;
				}
			}
			moveFolderContent(tempCurrentSubFolder, outputDir);
			successfullCount++;
			continue;
		}
		log.println("Cleanup...");
		removeFolders(tempCurrentSubFolder, tempAllSubsFolder);
		log.println("Done :)");
		log.println(String.format("Converted %s file(s):\n%s sucessfull and %s faulty", fileCount, successfullCount,
				fileCount - successfullCount));
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

	/**
	 * Recursively merges projects
	 * 
	 * @param solutionDir     the solution directory
	 * @param targetDir       the target directory
	 * @param assertExist     the List of Files to assert exist
	 * @param assertNotExist  the List of Files to assert not exist
	 * @param overwrite       the List of files to overwrite
	 * @param copyIfNotExists the List of Files to copy if not present in target
	 *                        directory
	 * @param ignore          the list of files to skip checking (can contain
	 *                        folders, will prevent subfolder checking)
	 */
	private void mergeProjectContent(File solutionDir, File targetDir, ArrayList<Path> assertExist,
			ArrayList<Path> assertNotExist, ArrayList<Path> overwrite, ArrayList<Path> copyIfNotExists,
			ArrayList<Path> ignore) {
		if (!solutionDir.isDirectory() || !targetDir.isDirectory()) {
			throw new IllegalArgumentException("parentDir must be a directory");
		}
		for (File file : solutionDir.listFiles()) {
			Path filePath = file.toPath().toAbsolutePath();
			// ignore mode
			if (ignore.contains(filePath)) {
				continue;
			}
			var assertedExistsTriggered = false;
			var assertedNotExistsTriggered = false;
			// assert exist mode
			if (assertExist.contains(filePath)) {
				if (!Paths.get(targetDir.getAbsolutePath(), file.getName()).toFile().exists()) {
					err.println("File " + file.getName() + " missing...");
					assertedExistsTriggered = true;
//					continue;
				}
			}
			// assert Not Exist Mode
			if (assertNotExist.contains(filePath)) {
				if (Paths.get(targetDir.getAbsolutePath(), file.getName()).toFile().exists()) {
					err.println("File " + file.getName() + " existing in Submission...");
					assertedNotExistsTriggered = true;
					// continue;
				}
			}
			if (file.isDirectory()) {
				// Copy_if_not_exists and overwrite_always mode for directories
				if (overwrite.contains(file.toPath().toAbsolutePath())) {
					if (assertedNotExistsTriggered) {
						err.println("Overwriting file that should not have existed:" + file.getName());
					}
					copyFolderContent(file, Paths.get(targetDir.getAbsolutePath(), file.getName()).toFile());
					continue;
				}
				if (assertedExistsTriggered) {
					if(!copyIfNotExists.contains(filePath)) {
						continue; // If the directory doesn't exist subfiles wont exist neither
					} else {
						err.println("WARNUNG: Verzeichnis das in Abgabe existieren sollte wird aus der Lösung kopiert:" + file.getName() + " DIESES VERZEICHNIS NICHT BEWERTEN");
					}
				}
				mergeProjectContent(file, ensureDirectories(targetDir, file.getName()).get(0), assertExist,
						assertNotExist, overwrite, copyIfNotExists, ignore);
			} else {
				try {
					Path target = Paths.get(targetDir.getAbsolutePath(), file.getName());
					// Copy_if_not_exists and overwrite_always mode for files
					if (!target.toFile().exists()) {
						if(assertedExistsTriggered) {
							if(!copyIfNotExists.contains(filePath)) {
								continue;
							} else {
								err.println("WARNUNG: Datei die in Abgabe existieren sollte wird aus der Lösung kopiert:" + file.getName() + " DIESE DATEI NICHT BEWERTEN");
							}
						}
//						System.out.println("Copying file " + file.getName());
						Files.copy(file.toPath(), Paths.get(targetDir.getAbsolutePath(), file.getName()));
					} else if (overwrite.contains(filePath)) {
						if(assertedNotExistsTriggered) {
							err.println("Overwriting file that should not have existed:" + file.getName());
						}
						Files.copy(file.toPath(), Paths.get(targetDir.getAbsolutePath(), file.getName()),
								StandardCopyOption.REPLACE_EXISTING);
					}
					/*
					 * else if(!filesEqual(file, target.toFile())) { System.err.println("File " +
					 * file.getName() + "was Modified"); Files.copy(file.toPath(),
					 * Paths.get(targetDir.getAbsolutePath(), file.getName())); }
					 */
				} catch (IOException e) {
					err.println(e.getMessage());
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

	private void copyFolderContent(File parentDir, File targetDir) {
		if (!parentDir.isDirectory() || !targetDir.isDirectory()) {
			throw new IllegalArgumentException("parentDir must be a directory");
		}
		for (File file : parentDir.listFiles()) {
			if (file.isDirectory()) {
				copyFolderContent(file, ensureDirectories(targetDir, file.getName()).get(0));
			} else {
				try {
					Files.copy(file.toPath(), Paths.get(targetDir.getAbsolutePath(), file.getName()),
							StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					err.println(e.getMessage());
				}
			}
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
