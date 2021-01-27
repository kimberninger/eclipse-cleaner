package project_cleaner;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A fully Functional Adapter for Handling The Raco. It supports code execution,
 * testing, converting from wxme, and some parsing line removing all Comments
 * from a file
 * 
 * @author Ruben Deisenroth
 *
 */
public class RacoAdapter {
	private File raco;
	private Path executionDirectory;
	private String defaultTempFilename = "temp.rkt";

	/**
	 * Create a new {@link RacoAdapter}
	 * 
	 * @param raco               the raco {@link File}
	 * @param executionDirectory the Execution Directory {@link Path}
	 */
	public RacoAdapter(File raco, Path executionDirectory) {
		this.raco = raco;
		this.setExecutionDirectory(executionDirectory);
	}

	/**
	 * Create a new {@link RacoAdapter}
	 * 
	 * @param executionDirectory the Execution Directory {@link Path}
	 */
	public RacoAdapter(Path executionDirectory) {
		this(findRaco(), executionDirectory);
	}

	/**
	 * Create a new {@link RacoAdapter}
	 * 
	 * @param executionDirectory the Execution Directory {@link File}
	 */
	public RacoAdapter(File executionDirectory) {
		this(findRaco(), executionDirectory.toPath().toAbsolutePath());
	}

	/**
	 * @return the raco
	 */
	public File getRaco() {
		return raco;
	}

	/**
	 * @param raco the raco to set
	 */
	public void setRaco(File raco) {
		this.raco = raco;
	}

	/**
	 * @return the executionDirectory
	 */
	public Path getExecutionDirectory() {
		return executionDirectory;
	}

	/**
	 * @param executionDirectory the executionDirectory to set
	 */
	public void setExecutionDirectory(Path executionDirectory) {
		this.executionDirectory = executionDirectory;
	}

	/**
	 * Verifies the loaded raco to be compatible with this {@link RacoAdapter}
	 * 
	 * @return true if the Raco is compatible
	 */
	public boolean verify() {
		String racketCode = ";; The first three lines of this file were inserted by DrRacket. They record metadata\n"
				+ ";; about the language level of this file in a form that our tools can easily process.\n"
				+ "#reader(lib \"htdp-advanced-reader.ss\" \"lang\")((modname racoTest) (read-case-sensitive #t) (teachpacks ()) (htdp-settings #(#t constructor repeating-decimal #f #t none #f () #t)))\n"
				+ "(define (minimum lst) (foldr (lambda (x y) (if (< x y) x y)) (first lst) (rest lst)))\n" + "\n"
				+ "(minimum '(12 54 31 2 -1 124))\n" + "\n" + "(check-expect (+ 1 2) 3)";
		try {
			RacketTestResult testResult = racoTest(racketCode,
					Paths.get(executionDirectory.toAbsolutePath().toString(), "racoTest.rkt").toAbsolutePath());
			if (testResult.hasPassed()) {
				System.out.println("✓ raco verified");
				return true;
			} else {
				System.err.println("✗ Raco is invalid");
				return false;
			}
		} catch (Exception e) {
			System.err.println("✗ Could not verify Raco:");
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Tries to find the Raco file for Executing Racket Files
	 * 
	 * @return the Raco File
	 */
	public static File findRaco() {
		File raco = null;
		String osName = System.getProperty("os.name");
		System.out.println("OS: " + osName);
		if (osName.toLowerCase().contains("windows")) {
			raco = Paths.get(System.getenv("SystemDrive"), "Program Files", "Racket", "raco.exe").toFile();
		} else if (osName.toLowerCase().contains("linux")) {
			CommandResult whichRacoCommand = executeShellComand("which", "raco");
			if (whichRacoCommand.wasExecuted() && whichRacoCommand.getExitCode() == 0) {
				raco = Paths.get(whichRacoCommand.getResultString().toString().trim()).toFile();
			}
		} else {
			System.out.println("✗ I can't find raco");
		}
		if (raco != null) {
			System.out.println("✓ raco found in: \"" + raco.getAbsolutePath() + "\"");
		}
		return raco;
	}

	/**
	 * Executes a Shell command and handels occuring errors
	 * 
	 * @param command The command to be executed
	 * @return a {@link CommandResult} that stores information about the execution
	 */
	public static CommandResult executeShellComand(String... command) {
		return executeShellComand(-1, command);
	}

	/**
	 * Executes a Shell command and handels occuring errors
	 * 
	 * @param timeoutInSeconds The Timeout in Seconds
	 * @param command          The command to be executed
	 * @return a {@link CommandResult} that stores information about the execution
	 */
	public static CommandResult executeShellComand(int timeoutInSeconds, String... command) {
		String s;
		String t;
		StringBuilder output = new StringBuilder();
		StringBuilder error = new StringBuilder();
		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			if (timeoutInSeconds > 0) {
				if (!p.waitFor(timeoutInSeconds, TimeUnit.SECONDS)) {
					p.destroyForcibly();
					return new CommandResult(
							"The Execution tool longer than the time Limit of " + timeoutInSeconds + " second(s).", -1,
							false, null);
				}
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader be = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			s = br.readLine();
			boolean first = true;
			while (s != null) {
				if (!first) {
					output.append(System.getProperty("line.separator"));
				} else {
					first = false;
				}
				output.append(s);
				s = br.readLine();
			}
			first = true;
			t = be.readLine();
			while (t != null) {
				if (!first) {
					error.append(System.getProperty("line.separator"));
				} else {
					first = false;
				}
				error.append(t);
				t = be.readLine();
			}
			p.waitFor();
			p.destroy();
			return new CommandResult(output.toString(), p.exitValue(), true, new Error(error.toString()));
		} catch (Exception e) {
			return new CommandResult(output.toString(), -1, false, e);
		}
	}

	/**
	 * Executes the raco test command on a given {@link File}
	 * 
	 * @param rktFile the Racket Code {@link File}
	 * @return the {@link RacketTestResult}
	 */
	public RacketTestResult racoTest(File rktFile) {
		return racoTest(rktFile, -1);
	}

	/**
	 * Executes the raco test command on a given {@link File}
	 * 
	 * @param rktFile the Racket Code {@link File}
	 * @param Timeout The Timeout in Seconds
	 * @return the {@link RacketTestResult}
	 */
	public RacketTestResult racoTest(File rktFile, int timeout) {
		CommandResult result = executeShellComand(timeout, raco.getAbsolutePath(), "test", "--quiet",
				rktFile.getAbsolutePath());
		if (result.ok()) {
			String testResult = result.getResultString();
			if (testResult.toLowerCase().contains("test passed") || testResult.toLowerCase().contains("tests passed")) {
				return new RacketTestResult(testResult, true);
			} else {
				return new RacketTestResult(testResult, false);
			}
		}
		return new RacketTestResult(result.getResultString(), false);
	}

	/**
	 * Executes the raco test command on a given {@link String} in a given
	 * {@link Path} and deletes the File afterwards
	 * 
	 * @param racketCode the Racket-Code-{@link String}
	 * @param path       the execution {@link Path}
	 * @return the {@link RacketTestResult}
	 */
	public RacketTestResult racoTest(String racketCode, Path path) {
		return racoTest(racketCode, path, -1);
	}

	/**
	 * Executes the raco test command on a given {@link String} in a given
	 * {@link Path} and deletes the File afterwards
	 * 
	 * @param racketCode the Racket-Code-{@link String}
	 * @param path       the execution {@link Path}
	 * @return the {@link RacketTestResult}
	 */
	public RacketTestResult racoTest(String racketCode, Path path, int timeout) {
		File rktFile = FileUtils.createTextFile(path, racketCode);
		RacketTestResult result = racoTest(rktFile, timeout);
		rktFile.delete();
		return result;
	}

	/**
	 * Invocation of {@link #racoTest(String, Path)} with
	 * {@link #executionDirectory} and {@link #defaultTempFilename}
	 * 
	 * @param racketCode the Racket-Code-{@link String}
	 * @return the {@link RacketTestResult}
	 */
	public RacketTestResult racoTest(String racketCode) {
		return racoTest(racketCode, -1);
	}

	/**
	 * Invocation of {@link #racoTest(String, Path)} with
	 * {@link #executionDirectory} and {@link #defaultTempFilename}
	 * 
	 * @param racketCode the Racket-Code-{@link String}
	 * @param Timeout    The Timeout in Seconds
	 * @return the {@link RacketTestResult}
	 */
	public RacketTestResult racoTest(String racketCode, int timeout) {
		return racoTest(racketCode, Paths.get(executionDirectory.toAbsolutePath().toString(), defaultTempFilename),
				timeout);
	}

	/**
	 * Invocation of {@link #racoTest(String)} with a given {@link RacketTest}
	 * appended to the Code and stored in the result
	 * 
	 * @param racketCode the Racket-Code-{@link String}
	 * @param test       the {@link RacketTest} to execute on the given Code
	 * @return the {@link RacketTestResult} with the {@link RacketTest} stored
	 */
	public RacketTestResult racoTest(String racketCode, RacketTest test) {
		String testCode = racketCode + test.getCode();
		RacketTestResult result = racoTest(testCode,
				Paths.get(executionDirectory.toAbsolutePath().toString(), defaultTempFilename),
				test.getMaxEcecTimeInSeconds());
		result.setTest(test);
		return result;
	}

	public ArrayList<RacketTestResult> racoTest(String racketCode, List<RacketTest> tests) {
		ArrayList<RacketTestResult> result = new ArrayList<>();
		StringBuilder modifiedRacketCodeBuilder = new StringBuilder(racketCode);
		String newLine = System.getProperty("line.separator");
		modifiedRacketCodeBuilder.append(newLine);
		String testIdentifier = "\"------------Test_Start---------)\"";
		modifiedRacketCodeBuilder.append("(print " + testIdentifier + ")");
		for (RacketTest t : tests) {
			modifiedRacketCodeBuilder.append(newLine);
			modifiedRacketCodeBuilder.append(t.getCode());
		}
		String modifiedRacketCode = removeEmptyLines(modifiedRacketCodeBuilder.toString());
//		String[] racketLines = modifiedRacketCode.split(newLine);
		TreeMap<Range, RacketTest> testsInCode = findTests(modifiedRacketCode, tests);
		RacketTestResult totalTestResult = racoTest(modifiedRacketCode, 60);
		String totalTestResultString = totalTestResult.getResultString();
		int startIndex = totalTestResultString.lastIndexOf(testIdentifier) + testIdentifier.length();
		totalTestResultString = totalTestResultString.substring(startIndex);
		var lines = totalTestResultString.lines().collect(Collectors.toList());
		String firstLine = totalTestResultString.lines().findFirst().orElse("");
		if (totalTestResultString.toLowerCase().contains("test passed")
				|| totalTestResultString.toLowerCase().contains("tests passed")) {
			for (RacketTest t : tests) {
				result.add(new RacketTestResult(totalTestResultString, true, t));
			}
		} else if (firstLine.matches("Ran [0-9]+ check[s]?.")) {
			int numberOfTests = Integer.parseInt(firstLine.replaceAll("[^\\d]+", ""));
			int failedTests = Integer.parseInt(lines.get(1).replaceAll("[^\\d]+", " ").trim().split(" ")[0]);
			if (failedTests == 0) {
				// No Test passed (in this case the string says 0 tests passed)
				failedTests = numberOfTests;
			}
			int resultstartline = 2;
			int resultendline = -1;
			for (int i = 0; i < failedTests; i++) {
				resultendline = resultstartline;
				while (!lines.get(resultendline).trim().matches("In .+ at line [0-9]+ column [0-9]+")) {
					resultendline++;
				}
				String[] numbersInLine = lines.get(resultendline).replaceAll("[^\\d]+", " ").trim().split(" ");
				int lineNumber = Integer.parseInt(numbersInLine[numbersInLine.length - 2]);
				int Column = Integer.parseInt(numbersInLine[numbersInLine.length - 1]);
				int pos = lineNumberAndColToPos(lineNumber, Column, modifiedRacketCode);
				// Get test
				if (!testsInCode.keySet().stream().anyMatch(x -> x.contains(pos))) {
					System.err.println("Code contains test that was not found");
				}
				Range testRange = testsInCode.keySet().stream().filter(x -> x.contains(pos)).findFirst().get();
				RacketTest test = testsInCode.get(testRange);
				StringBuilder testResultBuilder = new StringBuilder();
				for (int j = resultstartline; j < resultendline; j++) {
					testResultBuilder.append(newLine);
					testResultBuilder.append(lines.get(j));
				}
				String testResult = removeEmptyLines(testResultBuilder.toString());
				result.add(new RacketTestResult(testResult, false, test));
				resultstartline = resultendline + 1;
				resultendline = -1;
			}
			// The other Tests Passed
			for (var t : tests) {
				if (!result.stream().anyMatch(x -> x.getTest().equals(t))) {
					result.add(new RacketTestResult("The Test passed", true, t));
				}
			}
		} else {
			System.err.println("Someth");
		}
		return result;
	}

	private int lineNumberAndColToPos(int lineNumber, int col, String s) {
		String newLine = System.getProperty("line.separator");
		var lines = s.split(newLine);
		int lineStart = 0;
		for (int currLineNumber = 1; currLineNumber < lineNumber; currLineNumber++) {
			lineStart += lines[currLineNumber - 1].length();
			lineStart += newLine.length();
		}
		return lineStart + col;
	}

	/**
	 * Converts a WXME Submission to a readable text file
	 * 
	 * @param wxmeFile the {@link File} to convert
	 * @return the converted {@link File}
	 */
	public File convertWxmeSubmission(File wxmeFile) {
		File WxmeConverter = FileUtils.createTextFile(
				Paths.get(executionDirectory.toAbsolutePath().toString(), "convertWxme.rkt"),
				"#lang racket/base\n" + "(require wxme)\n" + "(define files (list \"" + wxmeFile.getAbsolutePath()
						+ "\"))\n" + "(for ([name files])\n"
						+ "  (define name-convert (string-append (substring name 0 (- (string-length name) 4)) \"-converted.rkt\"))\n"
						+ "  (when (file-exists? name-convert) (delete-file name-convert))\n"
						+ "  (define my-in (wxme-port->text-port (open-input-file name)))\n"
						+ "  (define my-out (open-output-file name-convert))\n"
						+ "  (write-string (read-string 300000 my-in) my-out)\n" + ")");
		var result = racoTest(WxmeConverter);
		if (result.ok()) {
			return Paths.get(wxmeFile.getAbsolutePath().substring(0, wxmeFile.getAbsolutePath().lastIndexOf('.'))
					+ "-converted.rkt").toFile();
		} else {
			return wxmeFile;
		}
	}

	/**
	 * @return the defaultTempFilename
	 */
	public String getDefaultTempFilename() {
		return defaultTempFilename;
	}

	/**
	 * @param defaultTempFilename the defaultTempFilename to set
	 */
	public void setDefaultTempFilename(String defaultTempFilename) {
		this.defaultTempFilename = defaultTempFilename;
	}

	/**
	 * Invokation of {@link #removeCommentsFromCode(String, boolean, boolean)} with
	 * removeEmptyLines and allowDrRacketComments both set to {@code true}
	 * 
	 * @param racketCodeWithComments the Racket Code that likely Contains Comments
	 * @return the Code with all Comments removed
	 */
	public static String removeCommentsFromCode(String racketCodeWithComments) {
		return removeCommentsFromCode(racketCodeWithComments, true, true);
	}

	/**
	 * Removes All Comments (Single Line Comments with ; and even nested Multiline
	 * Comments with #||#)
	 * 
	 * @param racketCodeWithComments           the Racket Code that likely Contains
	 *                                         Comments
	 * @param removeEmptyLines                 If Lines that are empty or contain
	 *                                         only spaces should be removed
	 * @param allowDrRacketCommentsAtFileStart If the first two Lines of Comments
	 *                                         generated by drracket should be
	 *                                         allowed (DrRacket can't read the file
	 *                                         properly without that, but raco can)
	 * @return the Code with all Comments removed
	 */
	public static String removeCommentsFromCode(String racketCodeWithComments, boolean removeEmptyLines,
			boolean allowDrRacketCommentsAtFileStart) {
		// This Method took WAAAAAAAAAAY longer than i would ever admit
		StringBuilder newCode = new StringBuilder();
		System.out.println("Removing Comments");
		// This regex took forever to make:

//		Pattern stringRegex = Pattern.compile("(?<!\\\\)(?:\\\\\\\\)*\"(\\\\.|[^\"])*(?<!\\\\)(?:\\\\\\\\)*\"");
//		Matcher stringMatcher = stringRegex.matcher(racketCodeWithComments);
		Pattern quotationMarkRegex = Pattern.compile("(?<!\\\\)(?:[\\\\]{2})*\"");
		Matcher quotationMarkMatcher = quotationMarkRegex.matcher(racketCodeWithComments);
		// THis one did not
		Pattern semicolonRegex = Pattern.compile("(?<![\\\\])(?:\\\\\\\\)*;");
		Matcher semicolonMatcher = semicolonRegex.matcher(racketCodeWithComments);

		Pattern multilineCommentStarterRegex = Pattern.compile("[ \\(\\)\\{\\}\\[\\],][#][|]");
		Matcher multilineCommentStarterMatcher = multilineCommentStarterRegex.matcher(racketCodeWithComments);

		Pattern multilineCommentStarterNoPrefixRegex = Pattern.compile("(?<![|])[#][|]");
		Matcher multilineCommentStarterNoPrefixMatcher = multilineCommentStarterNoPrefixRegex
				.matcher(racketCodeWithComments);

		Pattern multilineCommentEnderRegex = Pattern.compile("[|][#]");
		Matcher multilineCommentEnderMatcher = multilineCommentEnderRegex.matcher(racketCodeWithComments);

//		var stringMatcherResults = stringMatcher.results().collect(Collectors.toList());
		var quotationMarkMatcherResults = quotationMarkMatcher.results().collect(Collectors.toList());
//		printMatchList(quotationMarkMatcherResults, "quotationMarkMatcherResults: ");
		List<MatchResult> ignoreRanges = new ArrayList<>();
		List<Range> quoteRanges = getQuoteRanges(quotationMarkMatcherResults, ignoreRanges);
		var semikolonMatcherResults = semicolonMatcher.results().collect(Collectors.toList());
		var multilineCommentStarterMatcherResults = multilineCommentStarterMatcher.results()
				.collect(Collectors.toList());
		var multilineCommentStarterMatcherNoPrefixResults = multilineCommentStarterNoPrefixMatcher.results()
				.collect(Collectors.toList());
		var multilineCommentEnderMatcherResults = multilineCommentEnderMatcher.results().collect(Collectors.toList());
//		printMatchList(stringMatcherResults, "stringMatcherResults: ");
//		printMatchList(semikolonMatcherResults, "semikolonMatcherResults: ");
//		printMatchList(multilineCommentStarterMatcherResults, "multilineCommentStarterMatcherResults: ");
//		printMatchList(multilineCommentStarterMatcherNoPrefixResults,
//				"multilineCommentStarterMatcherNoPrefixResults: ");
//		printMatchList(multilineCommentEnderMatcherResults, "multilineCommentEnderMatcherResults: ");

		String newLine = System.getProperty("line.separator");
		int multilineCommentDepth = 0;
		int multilineCommentStartIndex = -1;
		int multilineCommentEndIndex = -1;
		boolean first = true;
		int index = 0;
		int lineNumber = 0;
		var lines = racketCodeWithComments.split(newLine);
		for (int lineCounter = 0; lineCounter < lines.length; lineCounter++) {
			String aline = lines[lineCounter];
			lineNumber++;
			if (first) {
				first = false;
			} else {
				newCode.append(newLine);
			}
			int lineStart = index;
			int lineEnd = index + aline.length();
			// Make sure files are still entirely readable by DrRacket (if wanted)
			if (allowDrRacketCommentsAtFileStart && ((lineNumber == 1 && aline.equalsIgnoreCase(
					";; The first three lines of this file were inserted by DrRacket. They record metadata"))
					|| (lineNumber == 2 && aline.equalsIgnoreCase(
							";; about the language level of this file in a form that our tools can easily process.")))) {
				newCode.append(aline);
				index = lineEnd + newLine.length();
				continue;
			}
			final List<Range> currentQuoteRanges = quoteRanges;
			List<Integer> mlcsInLine = multilineCommentStarterMatcherResults.stream().map(x -> x.end() - 2)
					.filter(x -> x >= lineStart && x < lineEnd).collect(Collectors.toList());
			List<Integer> mlcsNoPrefixInLine = multilineCommentStarterMatcherNoPrefixResults.stream()
					.map(x -> x.end() - 2).filter(x -> x >= lineStart && x < lineEnd).collect(Collectors.toList());
			List<Integer> mlcsOutsideOfStringInThisLine = multilineCommentStarterMatcherResults.stream()
					.map(x -> x.end() - 2).filter(x -> !currentQuoteRanges.stream().anyMatch(y -> y.contains(x))
							&& x >= lineStart && x < lineEnd)
					.collect(Collectors.toList());
			List<Integer> mlceInLine = multilineCommentEnderMatcherResults.stream().map(x -> x.end() - 2)
					.filter(x -> x >= lineStart && x < lineEnd).collect(Collectors.toList());
			List<Integer> scOutsideOfStringInThisLine = semikolonMatcherResults.stream().map(x -> x.end() - 1).filter(
					x -> !currentQuoteRanges.stream().anyMatch(y -> y.contains(x)) && x >= lineStart && x <= lineEnd)
					.collect(Collectors.toList());
			if (mlcsNoPrefixInLine.contains(lineStart)
					&& !currentQuoteRanges.stream().anyMatch(x -> x.contains(lineStart))) { // Fix for the mlcs regex
				mlcsOutsideOfStringInThisLine.add(0, lineStart);
				mlcsInLine.add(0, lineStart);
			}
			List<Range> erase = new ArrayList<Range>();
			if (multilineCommentDepth == 0) {
				int semicolonCommentIndex = -1;
				if (!scOutsideOfStringInThisLine.isEmpty()) {
					semicolonCommentIndex = scOutsideOfStringInThisLine.get(0);
				}
				for (var mlcs : mlcsOutsideOfStringInThisLine) {
					if (semicolonCommentIndex == -1 || mlcs < semicolonCommentIndex) {
						multilineCommentDepth++;
						multilineCommentStartIndex = mlcs;
						for (int stli = 0, enli = 0; multilineCommentDepth != 0
								&& !(stli >= mlcsNoPrefixInLine.size() && enli >= mlceInLine.size());) {
							var sti = stli >= mlcsNoPrefixInLine.size() ? -1 : mlcsNoPrefixInLine.get(stli);
							var eni = enli >= mlceInLine.size() ? -1 : mlceInLine.get(enli);
							while (sti <= mlcs && stli < mlcsNoPrefixInLine.size() - 1) {
								stli++;
								sti = mlcsNoPrefixInLine.get(stli);
							}
							while (eni <= mlcs && enli < mlceInLine.size() - 1) {
								enli++;
								eni = mlceInLine.get(enli);
							}
							if (sti <= mlcs && eni <= mlcs) {
								break;
							}

							int currentIndex; // The openand currently processed
							boolean opening = false;
							// Select left most opening or closing multilene Comment operand
							if (sti < eni && sti > mlcs) {
								if (stli < mlcsNoPrefixInLine.size()) {
									currentIndex = sti;
									stli++;
									opening = true;
								} else {
									currentIndex = eni;
									enli++;
									opening = false;
								}
							} else {
								if (enli < mlceInLine.size()) {
									currentIndex = eni;
									enli++;
									opening = false;
								} else {
									currentIndex = sti;
									stli++;
									opening = true;
								}
							}
							// Adjust multiline Comment depth
							if (opening) {
								multilineCommentDepth++;
							} else {
								multilineCommentDepth--;
							}
							if (multilineCommentDepth == 0) {
								multilineCommentEndIndex = currentIndex + 2;
							}
						}
						if (multilineCommentDepth == 0) {
							Range currentMLCRange = new Range(multilineCommentStartIndex, multilineCommentEndIndex);
							erase.add(currentMLCRange);
							if (currentMLCRange.contains(semicolonCommentIndex)) {
								scOutsideOfStringInThisLine.remove(Integer.valueOf(semicolonCommentIndex));
								semicolonCommentIndex = -1;
								if (!scOutsideOfStringInThisLine.isEmpty()) {
									semicolonCommentIndex = scOutsideOfStringInThisLine.get(0);
								}
							}
						} else {
							// Line ends with Multiline Comment active
							erase.add(new Range(multilineCommentStartIndex, lineEnd));
						}
					}
				}
				if (multilineCommentDepth == 0 && semicolonCommentIndex != -1
						&& semicolonCommentIndex > multilineCommentEndIndex) {
					erase.add(new Range(semicolonCommentIndex, lineEnd));
				}
			} else {

				int semicolonCommentIndex = -1;
				if (!scOutsideOfStringInThisLine.isEmpty()) {
					semicolonCommentIndex = scOutsideOfStringInThisLine.get(0);
				}

				// See if whole line is Still mlc
				for (int stli = 0, enli = 0; multilineCommentDepth != 0
						&& !(stli >= mlcsNoPrefixInLine.size() && enli >= mlceInLine.size());) {
					var sti = stli >= mlcsNoPrefixInLine.size() ? -1 : mlcsNoPrefixInLine.get(stli);
					var eni = enli >= mlceInLine.size() ? -1 : mlceInLine.get(enli);

					int currentIndex; // The openand currently processed
					boolean opening = false;
					// Select left most opening or closing multilene Comment operand
					if (sti < eni) {
						if (stli < mlcsNoPrefixInLine.size()) {
							currentIndex = sti;
							stli++;
							opening = true;
						} else {
							currentIndex = eni;
							enli++;
							opening = false;
						}
					} else {
						if (enli < mlceInLine.size()) {
							currentIndex = eni;
							enli++;
							opening = false;
						} else {
							currentIndex = sti;
							stli++;
							opening = true;
						}
					}
					// Adjust multiline Comment depth
					if (opening) {
						multilineCommentDepth++;
					} else {
						multilineCommentDepth--;
					}
					if (multilineCommentDepth == 0) {
						multilineCommentEndIndex = currentIndex + 2;
					}
				}

				if (multilineCommentDepth != 0) {// Whole line was mlc
					erase.add(new Range(lineStart, lineEnd));
				} else {
					Range currentMLCRange = new Range(lineStart, multilineCommentEndIndex);
					erase.add(currentMLCRange);
					if (currentMLCRange.contains(semicolonCommentIndex)) {
						scOutsideOfStringInThisLine.remove(Integer.valueOf(semicolonCommentIndex));
						semicolonCommentIndex = -1;
						if (!scOutsideOfStringInThisLine.isEmpty()) {
							semicolonCommentIndex = scOutsideOfStringInThisLine.get(0);
						}
					}

					// Look for further Comments in line, copy of first case//

					for (var mlcs : mlcsOutsideOfStringInThisLine) {
						if (semicolonCommentIndex == -1 || mlcs < semicolonCommentIndex) {
							multilineCommentDepth++;
							multilineCommentStartIndex = mlcs;
							for (int stli = 0, enli = 0; multilineCommentDepth != 0
									&& !(stli >= mlcsNoPrefixInLine.size() && enli >= mlceInLine.size());) {
								var sti = stli >= mlcsNoPrefixInLine.size() ? -1 : mlcsNoPrefixInLine.get(stli);
								var eni = enli >= mlceInLine.size() ? -1 : mlceInLine.get(enli);

								while (sti <= mlcs && stli < mlcsNoPrefixInLine.size() - 1) {
									stli++;
									sti = mlcsNoPrefixInLine.get(stli);
								}
								while (eni <= mlcs && enli < mlceInLine.size() - 1) {
									enli++;
									eni = mlceInLine.get(enli);
								}
								if (sti <= mlcs && eni <= mlcs) {
									break;
								}

								int currentIndex; // The openand currently processed
								boolean opening = false;
								// Select left most opening or closing multilene Comment operand
								if (sti < eni && sti > mlcs) {
									if (stli < mlcsNoPrefixInLine.size()) {
										currentIndex = sti;
										stli++;
										opening = true;
									} else {
										currentIndex = eni;
										enli++;
										opening = false;
									}
								} else {
									if (enli < mlceInLine.size()) {
										currentIndex = eni;
										enli++;
										opening = false;
									} else {
										currentIndex = sti;
										stli++;
										opening = true;
									}
								}
								// Adjust multiline Comment depth
								if (opening) {
									multilineCommentDepth++;
								} else {
									multilineCommentDepth--;
								}
								if (multilineCommentDepth == 0) {
									multilineCommentEndIndex = currentIndex + 2;
								}
							}
							if (multilineCommentDepth == 0) {
								Range currentMLCRange2 = new Range(multilineCommentStartIndex,
										multilineCommentEndIndex);
								erase.add(currentMLCRange2);
								if (currentMLCRange2.contains(semicolonCommentIndex)) {
									scOutsideOfStringInThisLine.remove(Integer.valueOf(semicolonCommentIndex));
									semicolonCommentIndex = -1;
									if (!scOutsideOfStringInThisLine.isEmpty()) {
										semicolonCommentIndex = scOutsideOfStringInThisLine.get(0);
									}
								}
							} else {
								// Line ends with Multiline Comment active
								erase.add(new Range(multilineCommentStartIndex, lineEnd));
							}
						}
					}
					if (multilineCommentDepth == 0 && semicolonCommentIndex != -1
							&& semicolonCommentIndex > multilineCommentEndIndex) {
						erase.add(new Range(semicolonCommentIndex, lineEnd));
					}

				}

			}
			// Handle erase
			int origLineLength = aline.length();
			int eraseOffset = 0;
			for (Range r : erase) {
				String before = aline.substring(0, r.getStart() - lineStart - eraseOffset);
//				String between = aline.substring(r.getStart() - lineStart - eraseOffset,
//						r.getEnd() - lineStart - eraseOffset);
				String after = aline.substring(r.getEnd() - lineStart - eraseOffset);
				aline = before + after;
				// Fix string starters in Comments
				if (quotationMarkMatcherResults.stream().anyMatch(x -> r.contains(x.end() - 1))) {
					// Reevaluate String Positions
					ignoreRanges.addAll(quotationMarkMatcherResults.stream().filter(x -> r.contains(x.end() - 1))
							.collect(Collectors.toList()));
					quoteRanges = getQuoteRanges(quotationMarkMatcherResults, ignoreRanges);
				}
				eraseOffset += r.length();
			}
			if (aline.trim().length() > 0 || (origLineLength == 0 && !removeEmptyLines)) {
				newCode.append(aline);
			} else {
				first = true;
			}
			index = lineEnd + newLine.length();
		}
//		System.out.println("Modified:");
//		System.out.println(newCode.toString());

		return newCode.toString();
	}

	/**
	 * Gets the Ranges where Quotation Marks are in
	 * 
	 * @param quotationMarkMatches the Regex match result for quotes
	 * @param ignore               the quotes to ignore (probably in comments)
	 * @return the Ranges where Quotation Marks are in
	 */
	private static List<Range> getQuoteRanges(List<MatchResult> quotationMarkMatches, List<MatchResult> ignore) {
		boolean inQuote = false;
		int quoteStart = -1;
		int quoteEnd = -1;
		List<Range> quoteRanges = new ArrayList<>();
		for (var match : quotationMarkMatches) {
			if (ignore.contains(match)) {
				continue;
			}
			if (inQuote == false) {
				quoteStart = match.end() - 1;
			} else {
				quoteEnd = match.end() - 1;
				quoteRanges.add(new Range(quoteStart, quoteEnd));
				quoteStart = -1;
				quoteEnd = -1;
			}
			inQuote = !inQuote;
		}
		return quoteRanges;
	}

	/**
	 * Invocation of {@link #getQuoteRanges(List, List)} with no ignore Cases
	 * 
	 * @param quotationMarkMatches the Regex match result for quotes
	 * @return the Ranges where Quotation Marks are in
	 */
	public static List<Range> getQuoteRanges(List<MatchResult> quotationMarkMatches) {
		return getQuoteRanges(quotationMarkMatches, new ArrayList<MatchResult>());
	}

	@SuppressWarnings("unused") // Used for debugging only
	private static void printMatchList(List<MatchResult> matches, String name) {
		System.out.println(matches.stream().map(x -> String.format("{start: %s, end: %s}", x.start(), x.end()))
				.collect(Collectors.joining(",", name + "[", "]")));
	}

	/**
	 * Returns true if a range List contains a given number
	 * 
	 * @param ranges the Range list
	 * @param i      the number to check is contained
	 * @return true if contained
	 */
	private static boolean rangeListContains(List<Range> ranges, int i) {
		return ranges.stream().anyMatch(x -> x.contains(i));
	}

	/**
	 * Returns true if a range List contains a given range
	 * 
	 * @param ranges the Range list
	 * @param r      the range to check is contained
	 * @return true if contained
	 */
	private static boolean rangeListContains(List<Range> ranges, Range r) {
		return ranges.stream().anyMatch(x -> x.contains(r));
	}

	/**
	 * Gets the {@link Range}s of Tests in a given Racket Code
	 * 
	 * @param racketCode the Racket Code to match
	 * @return the {@link Range}s of Tests in a given Racket Code
	 */
	public static List<Range> getTestRanges(String racketCode) {
		// Code must not contain Comments for this string matcher to work:
		// Quotes
		Pattern quotationMarkRegex = Pattern.compile("(?<!\\\\)(?:[\\\\]{2})*\"");
		Matcher quotationMarkMatcher = quotationMarkRegex.matcher(racketCode);
		var quotationMarkMatcherResults = quotationMarkMatcher.results().collect(Collectors.toList());
		List<Range> quotes = getQuoteRanges(quotationMarkMatcherResults);

		// Test Constructs
		var openingBraces = List.of('(', '[', '{');
		var closingBraces = List.of(')', ']', '}');

		Pattern testConstructRegex = Pattern.compile(
				"(check-expect|check-within|check-member-of|check-satisfied|check-range|check-error|check-property)");
		Matcher testConstructMatcher = testConstructRegex.matcher(racketCode);
		// Only get Test constructs outside of strings
		var testConstructs = testConstructMatcher.results()
				.filter(x -> !rangeListContains(quotes, new Range(x.start(), x.end()))).collect(Collectors.toList());
		List<Range> tests = new ArrayList<>();
		for (var test_construct : testConstructs) {
			// Find Left brace
			int startIndex = test_construct.start();
			while (startIndex >= 0 && !openingBraces.contains(racketCode.charAt(startIndex))) {
				startIndex--;
			}
			if (startIndex < 0) {
				System.err.println("Could not find end for test at index " + test_construct.start());
				System.err.println("Skipping...");
				continue;
			}
			Character openingBraceType = racketCode.charAt(startIndex);
			Character closingBraceType = closingBraces.get(openingBraces.indexOf(openingBraceType));
			int paranthesis_depth = 1;
			int endIndex = test_construct.end();
			while (paranthesis_depth > 0 && endIndex < racketCode.length()) {
				Character currentChar = racketCode.charAt(endIndex);
				if (currentChar.equals(openingBraceType) && !rangeListContains(quotes, endIndex)) {
					paranthesis_depth++;
				} else if (currentChar.equals(closingBraceType) && !rangeListContains(quotes, endIndex)) {
					paranthesis_depth--;
				}
				endIndex++;
			}
			if (endIndex > racketCode.length()) {
				System.err.println("Could not remove test at index " + test_construct.start());
				break;
			}
			tests.add(new Range(startIndex, endIndex));
		}
		return tests;
	}

	/**
	 * Removes the Tests from a Racket Code
	 * 
	 * @param racketCode the Racket Code
	 * @return the Code with the tests Removed
	 */
	public static String removeTests(String racketCode) {
		// Code must not contain Comments for this string matcher to work:
		System.out.print("Removing Students Tests...");
		List<Range> tests = getTestRanges(racketCode);
		// actually remove the tests
		int eraseOffset = 0;
//		List<String> testStrings = new ArrayList<>();
		for (Range test : tests) {
			if (tests.stream().anyMatch(x -> x != test && x.contains(test))) {
				System.out.println("\nFound overlapping test, aborting (probably a syntax error)");
				return null;
			}
			String before = racketCode.substring(0, test.getStart() - eraseOffset);
//			String between = racketCode.substring(test.getStart() - eraseOffset, test.getEnd() - eraseOffset);
			String after = racketCode.substring(test.getEnd() - eraseOffset);
//			testStrings.add(between);
			racketCode = before + after;
			eraseOffset += test.length();
		}
		System.out.println("Done");
		return racketCode;
	}

	/**
	 * Removes empty Lines from a given {@link String}
	 * 
	 * @param s the {@link String} to remove all empty lines from
	 * @return the {@link String} with all Empty Lines removed
	 */
	public static String removeEmptyLines(String s) {
		return s.replaceAll("(?m)^[ \\t]*\\r?\\n", "");
	}

	/**
	 * Finds the Line Numbers of tests known to be in the Code
	 * 
	 * @param RacketCode the Code Containing the given tests
	 * @param tests      the {@link RacketTest}s in the same order as in the code
	 * @return the A {@link HashMap} with the Tests and their coresponding line
	 *         Numbers
	 */
	public static TreeMap<Range, RacketTest> findTests(String RacketCode, List<RacketTest> tests) {
		// tests must have the same order as in Code
		TreeMap<Range, RacketTest> result = new TreeMap<>();
		var testRanges = getTestRanges(RacketCode);
		TreeMap<Range, String> testsInCode = new TreeMap<>();
		for (var range : testRanges) {
			testsInCode.put(range, RacketCode.substring(range.getStart(), range.getEnd()));
		}
		for (int i = 0; i < tests.size(); i++) {
			var test = tests.get(i);
			// Validate test order
			var estimatedRange = testRanges.get(i);
			var foundTestCode = testsInCode.get(estimatedRange);
			if (!test.getCode().contains(foundTestCode)) {
				System.err.println("Test Missmatch, skipping test");
				continue;
			}
			result.put(estimatedRange, test);
		}
		return result;
	}
}
