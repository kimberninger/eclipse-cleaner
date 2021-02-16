package fop.project_cleaner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import com.google.gson.Gson;

import static fop.project_cleaner.FileUtils.EnsureEmpty;
import static fop.project_cleaner.FileUtils.clearFolder;
import static fop.project_cleaner.FileUtils.copyFolderContent;
import static fop.project_cleaner.FileUtils.ensureDirectories;
import static fop.project_cleaner.FileUtils.extractFolder;
import static fop.project_cleaner.FileUtils.moveFolderContent;
import static fop.project_cleaner.FileUtils.removeFolders;

public class SubmissionsExtractor extends SwingWorker<String, Object> {

	// -- Attributes --\\

	private File submissionFile;
	private File outputDir;
	private PrintStream log = System.out;
	private PrintStream err = System.err;
	private File solutionArchive;
	private File fileList;
	private JProgressBar pb;
	private LanguageMode languageMode = LanguageMode.JAVA;
	private ActionSetModel instructionSet;
	private RacoAdapter raco;

	private enum FileReadMode {
		ASSERT_EXISTS, OVERWRITE_ALWAYS, COPY_IF_NOT_EXISTS, ASSERT_NOT_EXISTS, IGNORE
	}

	// -- Constructors --\\

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

	// -- Getters+Setters --\\

	@SuppressWarnings("exports")
	public void setProgressBar(JProgressBar pb) {
		this.pb = pb;
	}

	@SuppressWarnings("exports")
	public JProgressBar getProgressBar() {
		return pb;
	}

	public void setLanguageMode(LanguageMode languageMode) {
		this.languageMode = languageMode;
	}

	public LanguageMode getLanguageMode() {
		return languageMode;
	}

	// -- Main Methods --\\

	public void extract() {
		log.println("Vorbereitung...");
		Thread.yield();
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
		switch (languageMode) {
		case JAVA:
			// Extract the Solution Project
			extractFolder(solutionArchive.getAbsolutePath(), outputDir.getAbsolutePath());
			if (!Stream.of(outputDir.listFiles()).anyMatch(x -> x.getName().endsWith("SOLUTION"))) {
				System.err.println("Faulty solution");
				return;
			}
			solutionFolder = Stream.of(outputDir.listFiles()).filter(x -> x.getName().endsWith("SOLUTION")).findFirst()
					.get();
			if (solutionArchive != null) {
				if (fileList != null) {
					// Read instruction Set
					try {
						if (fileList.getName().endsWith(".json")) {
							// New Fancy format
							Gson gson = new Gson();
							instructionSet = gson.fromJson(Files.readString(fileList.toPath()),
									JavaActionSetModel.class);
							JavaActionSetModel javaInstructionSet = (JavaActionSetModel) instructionSet;
							javaInstructionSet.convertToAbsolutePaths(solutionFolder);
							System.out.println(
									"Files to Assert exist: " + javaInstructionSet.getAssert_exists().toString());
							System.out.println("Files to Assert not exist: "
									+ javaInstructionSet.getAssert_not_exists().toString());
							System.out.println(
									"Files to overwrite: " + javaInstructionSet.getOverwrite_always().toString());
							System.out.println("Files to ignore: " + javaInstructionSet.getIgnore().toString());
						} else if (fileList.getName().endsWith(".txt")) {
							// Legacy Java Support
							if (languageMode != LanguageMode.JAVA) {
								throw new Error(
										"Only Java has legacy instruction Set support. Please use the new .json Format");
							}
							String solPath = solutionFolder.getAbsolutePath();
							ArrayList<Path> filesToAssertExist = new ArrayList<>();
							ArrayList<Path> filesToOverwrite = new ArrayList<>();
							ArrayList<Path> filesToCopyIfNotExist = new ArrayList<>();
							ArrayList<Path> filesToAssertNotExist = new ArrayList<>();
							ArrayList<Path> filesToIgnore = new ArrayList<>();
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
							instructionSet = new JavaActionSetModel();
							JavaActionSetModel javaInstructionSet = (JavaActionSetModel) instructionSet;
							javaInstructionSet.setAssert_exists(filesToAssertExist);
							javaInstructionSet.setAssert_not_exists(filesToAssertNotExist);
							javaInstructionSet.setOverwrite_always(filesToOverwrite);
							javaInstructionSet.setIgnore(filesToIgnore);
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

						} else {
							throw new Error("File List format invalid");
						}

					} catch (Exception e) {
						err.print("Exception durung Java JSON Reading.");
						e.printStackTrace();
						// e.printStackTrace();
						fileList = null;
					}
				}
			}
			break;

		case RACKET:
			try {
				solutionFolder = Files
						.copy(solutionArchive.toPath().toAbsolutePath(),
								Paths.get(outputDir.toPath().toAbsolutePath().toString(), solutionArchive.getName()))
						.toFile();
				if (fileList != null) {
					// Read instruction Set
					try {
						if (fileList.getName().endsWith(".json")) {
							// New Fancy format
							Gson gson = new Gson();
							instructionSet = gson.fromJson(Files.readString(fileList.toPath()),
									RacketActionSetModel.class);
							RacketActionSetModel racketInstructionSet = (RacketActionSetModel) instructionSet;
							System.out.println("Extraction Settings:");
							System.out.println(
									"- Should remove student tests:" + racketInstructionSet.isRemove_student_tests());
							System.out.println("- Should fix naming convention:"
									+ racketInstructionSet.shouldFix_naming_convention());
							System.out.println("- Should do tests:" + racketInstructionSet.isDo_tests());
						} else {
							throw new Error("✗ File List format invalid");
						}

					} catch (Exception e) {
						err.print("Exception durung Racket JSON Reading:" + e.getMessage());
						// e.printStackTrace();
						fileList = null;
					}
				}
			} catch (IOException e1) {
				System.err.println("✗ Unable to copy soluition to target: " + e1.getMessage());
				// e1.printStackTrace();
				return;
			}
			raco = new RacoAdapter(outputDir);
			raco.verify();
		}

		// Extract The individual submissions
		int fileCount = 0;
		int successfullCount = 0;
		var submissions = tempAllSubsFolder.listFiles();
		if (!verifyDownloadArchiveStructure(submissions)) {
			err.println("✗ Die Option \"Als Verzeichnis Herunterladen\" wurde nicht verwendet. Breche ab");
			return;
		} else {
			System.out.println("✓ Ordnerstruktur verifiziert");
		}
		if (pb != null) {
			pb.setMinimum(0);
			pb.setMaximum(submissions.length);
			pb.setValue(0);
			pb.setString(String.format("%s/%s Abgaben fertig", pb.getValue(), pb.getMaximum()));
			pb.setEnabled(true);
		}
		log.println("Extracting Projects...");

		// -- Individual submissions --\\
		for (File submission : submissions) {
			fileCount++;
			switch (languageMode) {
			case JAVA:
				if (processJavaSubmission(submission, faultyDir, tempCurrentSubFolder, solutionFolder)) {
					successfullCount++;
				}
				break;
			case RACKET:

				if (processRacketSubmission(submission, faultyDir, tempCurrentSubFolder, solutionFolder)) {
					successfullCount++;
				}
				break;
			}
			if (pb != null) {
				pb.setValue(fileCount);
				pb.setString(String.format("%s/%s Abgaben fertig", pb.getValue(), pb.getMaximum()));
			}
		}
		log.println("Cleanup...");
		removeFolders(tempCurrentSubFolder, tempAllSubsFolder);
		log.println("Done :)");
		log.println(String.format("Converted %s file(s):\n%s sucessfull and %s faulty", fileCount, successfullCount,
				fileCount - successfullCount));
	}

	/**
	 * Schaut ob die obtion "in Verzeichnis herunterladen" verwendet wurde
	 *
	 * @param submissions the files from the directory (only on the top layer, no
	 *                    recursive file list)
	 * @return true if the structure Matches
	 */
	private static boolean verifyDownloadArchiveStructure(File[] submissions) {
		for (File f : submissions) {
			if (!f.isDirectory() || !f.getName().endsWith("_assignsubmission_file_")) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Process a single Submission File
	 *
	 * @param submission           the Submission File (Zip or .rkt)
	 * @param faultyDir            the directory to move if faulty
	 * @param tempCurrentSubFolder the folder to extraxt the current submission to
	 *                             isolated
	 * @param solutionFolder       the solution folder or file
	 * @return true if processed sucessfully
	 */
	private boolean processJavaSubmission(File submission, File faultyDir, File tempCurrentSubFolder,
			File solutionFolder) {
		if (!submission.isDirectory() || submission.listFiles().length != 1) {
			err.println("Cannot Extract submission " + submission.getName()
					+ " (maybe you didn't choose the correct Language mode?)");
			moveFolderContent(submission, faultyDir);
			return false;
		}
		String submittorName = submission.getName().split("_")[0];
		log.println("Extracting Submission from " + submittorName);
		// Extract current Submission to tempCurrentSubFolder
		File submissionZip = submission.listFiles()[0];
		if (!submissionZip.getName().endsWith(".zip")) {
			err.println("Cannot Extract submission " + submission.getName()
					+ " (Not a Zip, maybe you didn't download compressed submissions?)");
			moveFolderContent(submission, faultyDir);
			return false;
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
			return false;
		}
		if (Stream.of(tempCurrentSubFolder.listFiles())
				.anyMatch(x -> x.isDirectory() && x.getName().equals("__MACOSX"))) {
			System.out.println("removing __MACOSX folder");
			File macosxFolder = Stream.of(tempCurrentSubFolder.listFiles())
					.filter(x -> x.isDirectory() && x.getName().equals("__MACOSX")).findFirst().get();
			clearFolder(macosxFolder);
			if (macosxFolder.delete()) {
				// System.out.println("Removed" + " __MACOSX-Folder");
			} else {
				System.out.println("Cannot remove" + " __MACOSX-Folder");
			}
		}
		File submissionProjectFolder = tempCurrentSubFolder.listFiles()[0];
		/*
		 * Unnecessary if (!submissionProjectFolder.getName().matches(
		 * "H[0-9]+_(?!(?i)NACHNAME_VORNAME(?-i))[a-zA-Z\\-]+(_[a-zA-Z\\-]+)+")) {
		 * err.println("Namenskonvention verletzt in " + submission.getName() + ": " +
		 * submissionProjectFolder.getName()); moveFolderContent(submission, faultyDir);
		 * continue; }
		 */
		if(!checkJavaNamingConvention(submissionZip, submissionProjectFolder, submittorName, solutionFolder, faultyDir)){
			moveFolderContent(submission, faultyDir);
			return false;
		}
		// Project is ready to import, make sure foldername doesn't exist already
		if(Stream.of(outputDir.listFiles())
				.anyMatch(x -> x.isDirectory() && x.getName().equals(submissionProjectFolder.getName()))) {
			int existCounter=1;
			err.println("Folder named " + submissionProjectFolder.getName() + " already exists. renaming to: "
					+ submissionProjectFolder.getName() + "(" + existCounter + ")");
			while (true) {
				int curExistsC = existCounter;
				if(!Stream.of(outputDir.listFiles())
					.anyMatch(x -> x.isDirectory() && x.getName().equals(submissionProjectFolder.getName()  + "(" + curExistsC + ")"))) {
					break;
				}
				existCounter++;
				err.println("Folder named " + submissionProjectFolder.getName() + "(" + (curExistsC - 1) + ")" + " already exists. renaming to: "
						+ submissionProjectFolder.getName() + "(" + (existCounter) + ")");
			}
			File newProjectFolder = Paths
					.get(tempCurrentSubFolder.getAbsolutePath(), submissionProjectFolder.getName() + "(" + existCounter + ")").toFile();
			if (!submissionProjectFolder.renameTo(newProjectFolder)) {
				err.println("Could not rename, moving to faulty");
				moveFolderContent(tempCurrentSubFolder, faultyDir);
				return false;
			}
		}
		moveFolderContent(tempCurrentSubFolder, outputDir);
		return true;
	}

	/**
	 * Process a single Submission File
	 *
	 * @param submission           the Submission Directory (containing a .rkt file)
	 * @param faultyDir            the directory to move if faulty
	 * @param tempCurrentSubFolder the folder to extraxt the current submission to
	 *                             isolated
	 * @param solutionFile       the solution file
	 * @return true if processed sucessfully
	 */
	private boolean processRacketSubmission(File submission, File faultyDir, File tempCurrentSubFolder,
			File solutionFile) {
		if (!submission.isDirectory()) {
			err.println("✗ Cannot Extract submission " + submission.getName()
					+ " (maybe you didn't choose the correct Language mode?)");
			moveFolderContent(submission, faultyDir);
			return false;
		}
		String submittorName = submission.getName().split("_")[0];
		RacketActionSetModel racketInstructionSet = (RacketActionSetModel) instructionSet;
		if (racketInstructionSet.isDo_tests()) {
			log.println("------------------------------------------------------------------------------");
			log.println("Submission from: " + submittorName);
			log.println("------------------------------------------------------------------------------");

		} else {
			log.println("Extracting Submission from: " + submittorName);
		}
		// Move current Submission to tempCurrentSubFolder
		clearFolder(tempCurrentSubFolder);
		copyFolderContent(submission, tempCurrentSubFolder);
		// Naming convention check 2
		if (tempCurrentSubFolder.listFiles().length == 0) {
			err.println("✗ Abgabeverzeichnis von " + submittorName + " leer: " + submission.getName());
			System.err.println("Moving to faultyDir...");
			moveFolderContent(tempCurrentSubFolder, faultyDir);
			return false;
		}
		Path submissionProjectFile = tempCurrentSubFolder.listFiles()[0].toPath().toAbsolutePath();
		String submissionContent = "";
		try {
			submissionContent = Files.readString(submissionProjectFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Convert WXME-Submissions
		if (submissionContent.startsWith("#reader(lib\"read.ss\"\"wxme\")WXME0108 ## ")
				|| submissionContent.startsWith("#reader(lib\"read.ss\"\"wxme\")WXME0109 ## ")) {
			System.out.print("Converting WXME-Submission...");
			submissionProjectFile = raco.convertWxmeSubmission(submissionProjectFile.toFile()).toPath();
			try {
				submissionContent = Files.readString(submissionProjectFile);
			} catch (IOException e) {
				System.out.println();
				e.printStackTrace();
			}
			System.out.println("Done");
		}
		// Testing Phase
		String projectName = submissionProjectFile.toFile().getName();
		String submissionContentWithoutComments = RacoAdapter.removeCommentsFromCode(submissionContent);
		if (racketInstructionSet.shouldCheck_naming_convention()
				&& !checkRacketNamingConvention(submissionProjectFile.toFile().getName(),
						submissionContentWithoutComments, submittorName)) {
			System.err.println("Moving to faultyDir...");
			moveFolderContent(submission, faultyDir);
			return false;
		}
		if (racketInstructionSet.isDo_tests()) {
			String codeWithoutTestsAndComments = RacoAdapter.removeTests(submissionContentWithoutComments);
			if(codeWithoutTestsAndComments == null) {
				moveFolderContent(submission, faultyDir);
				return false;
			}
			if (!raco.racoTest(codeWithoutTestsAndComments).ok()) {
				System.err.println(
						"✗ The code of the student does not run successfully without tests, so it cannot be tested automatically.");
				moveFolderContent(submission, faultyDir);
				return false;
			}
			int passed = 0;
			int testCount = 0;
			var tasks = racketInstructionSet.getTasks();
			ArrayList<RacketTest> tests = new ArrayList<>();
			for(var task : tasks) {
				tests.addAll(task.getTests());
			}
			System.out.print("❯  running Tests...");
			var results = raco.racoTest(codeWithoutTestsAndComments, tests);
			System.out.println("Done");
			for (RacketTask task : tasks) {
				System.out.println();
				System.out.println(task.getTitle());
				System.out.println("-----------------------------------------------------");
				if (task.getAnnotation() != null) {
					System.out.println(task.getAnnotation());
				}
				for (RacketTest test : task.getTests()) {
					testCount++;
//					if (test.getRepeat() == 1) {
//						System.out.println("❯  running \"" + test.getTitle() + "\"...");
//					} else {
//						System.out.println("❯  running \"" + test.getTitle() + "\" " + test.getRepeat() + " time(s)");
//
//					}
					RacketTestResult result = results.stream().filter(x -> x.getTest().equals(test)).findFirst().orElse(null);
					if(result == null) {
						System.err.println("Something went wrong with the quick testing method.");
						continue;
					}
					if (result.hasPassed()) {
						passed++;
						System.out.println("✓ " + test.getTitle() + " has passed");
					} else {
						System.err.println("✗ Test \"" + test.getTitle() + "\" did not pass:");
						System.err.println(result.getResultString());
					}
				}
			}

			System.out.format("Passed %s of %s tests \n", passed, testCount);
//			System.err.println("✗ Automated testing is planned but not yet implemented.");
		}
		// Project is ready to import, make sure fileName doesn't exist already
//		Path finalProjectPath = submissionProjectFile.toAbsolutePath();
		if (Stream.of(outputDir.listFiles()).anyMatch(x -> !x.isDirectory() && x.getName().equals(projectName))) {
			err.println("Project File  named " + submissionProjectFile.toFile().getName()
					+ " already exists. renaming to: " + projectName + "(1)");
			File newProjectFile = Paths.get(tempCurrentSubFolder.getAbsolutePath(), projectName + "(1)").toFile();
			if (!submissionProjectFile.toFile().renameTo(newProjectFile)) {
				err.println("Could not rename, moving to faulty");
				System.err.println("Moving to faultyDir...");
				moveFolderContent(submission, faultyDir);
				return false;
			}
//			finalProjectPath = newProjectFile.toPath().toAbsolutePath();
		}
		// Move Project to main Target dir
		try {
//			Files.copy(finalProjectPath, outputDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
			moveFolderContent(tempCurrentSubFolder, outputDir);
			return true;
		} catch (Exception e) {
			System.err.println("✗ Could not move the fixed Project to target directory");
			e.printStackTrace();
			moveFolderContent(tempCurrentSubFolder, faultyDir);
			return false;
		}
	}

	private boolean checkRacketNamingConvention(String fileName, String fileContent, String submittorName) {
		// Filename
		if (languageMode != LanguageMode.RACKET) {
			err.println("✗ Method checkRacketNamingConvention() was called in non-racket-Mode");
			return false;
		}
		RacketActionSetModel racketInstructionSet = (RacketActionSetModel) instructionSet;
		if (!fileName.matches("H[0-9]+_(?!(?i)NACHNAME_VORNAME(?-i))[a-zA-Z\\-]+(_[a-zA-Z\\-]+)+.rkt")) {
			err.println("✗ Namenskonvention verletzt bei " + submittorName + ": " + fileName);
			if (racketInstructionSet.shouldFix_naming_convention()) {
				// Get correct project name
				String newProjectName = submittorName.replace(" ", "_").replace("ä", "ae").replace("ö", "oe")
						.replace("ü", "ue").replace("ß", "ss");
				String hausuebungsprefix = "H" + racketInstructionSet.getSheet_number();
				err.println("Projekt nach " + hausuebungsprefix + newProjectName + " umbenannt");
			} else {
				return false;
			}
		} else {
			System.out.println("✓ Namenskonvention eingehalten");
		}
		ArrayList<String> check_contained = racketInstructionSet.getVerify_strings_contained();
		if (check_contained != null && !check_contained.isEmpty()) {
			for (String contains : check_contained) {
				if (!fileContent.contains(contains)) {
					System.err.println("✗ Submission from " + submittorName
							+ " does not contain the following keyword: " + contains);
					return false;
				}
			}
			System.out.println("✓ All required Keywords are present");
		}
		ArrayList<String> check__not_contained = racketInstructionSet.getVerify_strings_not_contained();
		if (check__not_contained != null && !check__not_contained.isEmpty()) {
			for (String contains : check__not_contained) {
				if (fileContent.contains(contains)) {
					System.err.println("✗ Submission from " + submittorName
							+ " contains the following forbidden keyword: " + contains);
					return false;
				}
			}
			System.out.println("✓ No Forbidden functions/keywords used");
		}
		return true;
	}

	@SuppressWarnings("unused")
	private boolean checkRacketNamingConvention(File rktFile, String SubmittorName) throws IOException {
		return checkRacketNamingConvention(rktFile.getName(), SubmittorName, Files.readString(rktFile.toPath()));
	}

	private boolean checkJavaNamingConvention(File extractedSubmissionFileOrFolder, File submissionProjectFolder,
			String submittorName, File solutionFolder, File faultyDir) {
		// Final Naming Convention Check and compatibility check
		if(Arrays.stream(submissionProjectFolder.listFiles()).anyMatch(x -> x.getName().equals("pom.xml"))){
			err.println("Abgabe verwendet Maven-Version. Da niemand motiviert war Maven zu implementieren wird die Abgabe ins \"faulty\"-Verzeichnis verschoben.");
			return false;
		}
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
					moveFolderContent(extractedSubmissionFileOrFolder, faultyDir);
					return false;
				}
			} catch (IOException e) {
				err.println("Error during .project copy: " + e.getMessage());
				return false;
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
			if (!projectName.getTextContent().trim()
					.matches("H[0-9]+_(?!(?i)NACHNAME_VORNAME(?-i))[a-zA-Z\\-]+(_[a-zA-Z\\-]+)+")) {
				if (hadProjectFile) {
					err.println("Namenskonvention verletzt bei " + submittorName + ": " + projectName.getTextContent());
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
			JavaActionSetModel javaInstructionSet = (JavaActionSetModel) instructionSet;
			if (instructionSet != null) {
				mergeProjectContent(solutionFolder, submissionProjectFolder, javaInstructionSet.getAssert_exists(),
						javaInstructionSet.getAssert_not_exists(), javaInstructionSet.getOverwrite_always(),
						javaInstructionSet.getCopy_if_not_exists(), javaInstructionSet.getIgnore());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
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
					File copyTargetDir = Paths.get(targetDir.getAbsolutePath(), file.getName()).toFile();
					if (assertedNotExistsTriggered) {
						err.println("Overwriting file that should not have existed:" + file.getName());
					} else {
						copyTargetDir.mkdirs();
					}
					copyFolderContent(file, copyTargetDir);
					continue;
				}
				if (assertedExistsTriggered) {
					if (!copyIfNotExists.contains(filePath)) {
						continue; // If the directory doesn't exist subfiles wont exist neither
					} else {
						err.println("WARNUNG: Verzeichnis das in Abgabe existieren sollte wird aus der Lösung kopiert:"
								+ file.getName() + " DIESES VERZEICHNIS NICHT BEWERTEN");
					}
				}
				mergeProjectContent(file, ensureDirectories(targetDir, file.getName()).get(0), assertExist,
						assertNotExist, overwrite, copyIfNotExists, ignore);
			} else {
				try {
					Path target = Paths.get(targetDir.getAbsolutePath(), file.getName());
					// Copy_if_not_exists and overwrite_always mode for files
					if (!target.toFile().exists()) {
						if (assertedExistsTriggered) {
							if (!copyIfNotExists.contains(filePath)) {
								continue;
							} else {
								err.println(
										"WARNUNG: Datei die in Abgabe existieren sollte wird aus der Lösung kopiert:"
												+ file.getName() + " DIESE DATEI NICHT BEWERTEN");
							}
						}
//						System.out.println("Copying file " + file.getName());
						Files.copy(file.toPath(), Paths.get(targetDir.getAbsolutePath(), file.getName()));
					} else if (overwrite.contains(filePath)) {
						if (assertedNotExistsTriggered) {
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

	// -- Swing Worker Stuff --\\

	@Override
	protected String doInBackground() throws Exception {
		extract();
		return null;
	}
}
