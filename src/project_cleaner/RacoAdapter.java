package project_cleaner;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RacoAdapter {
	private File raco;
	private Path executionDirectory;
	private String defaultTempFilename = "temp.rkt";

	public RacoAdapter(File raco, Path executionDirectory) {
		this.raco = raco;
		this.setExecutionDirectory(executionDirectory);
	}

	public RacoAdapter(Path executionDirectory) {
		this(findRaco(), executionDirectory);
	}

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
	 * Verifies the loaded raco to be compatible with this racoAdapter
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
		String s;
		String t;
		StringBuilder output = new StringBuilder();
		StringBuilder error = new StringBuilder();
		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
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

	public RacketTestResult racoTest(File rktFile) {
		CommandResult result = executeShellComand(raco.getAbsolutePath(), "test", "--quiet", rktFile.getAbsolutePath());
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

	public RacketTestResult racoTest(String racketCode, Path path) {
		File rktFile = FileUtils.createTextFile(path, racketCode);
		return racoTest(rktFile);
	}

	public RacketTestResult racoTest(String racketCode) {
		return racoTest(racketCode, Paths.get(executionDirectory.toAbsolutePath().toString(), defaultTempFilename));
	}

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
		Pattern stringRegex = Pattern.compile("(?<!\\\\)(?:\\\\\\\\)*\"(\\\\.|[^\"])*(?<!\\\\)(?:\\\\\\\\)*\"");
		Matcher stringMatcher = stringRegex.matcher(racketCodeWithComments);
		// THis one did not
		Pattern semicolonRegex = Pattern.compile("(?<![\\\\])(?:\\\\\\\\)*;");
		Matcher semicolonMatcher = semicolonRegex.matcher(racketCodeWithComments);

		Pattern multilineCommentStarterRegex = Pattern.compile("[ \\(\\)\\{\\}\\[\\],][#][|]");
		Matcher multilineCommentStarterMatcher = multilineCommentStarterRegex.matcher(racketCodeWithComments);

		Pattern multilineCommentStarterNoPrefixRegex = Pattern.compile("[#][|]");
		Matcher multilineCommentStarterNoPrefixMatcher = multilineCommentStarterNoPrefixRegex
				.matcher(racketCodeWithComments);

		Pattern multilineCommentEnderRegex = Pattern.compile("[|][#]");
		Matcher multilineCommentEnderMatcher = multilineCommentEnderRegex.matcher(racketCodeWithComments);
		var stringMatcherResults = stringMatcher.results().collect(Collectors.toList());
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
		var semikolonOutsideStrings = semikolonMatcherResults.stream()
				.filter(x -> !stringMatcherResults.stream().anyMatch(y -> x.start() >= y.start() && x.end() <= y.end()))
				.collect(Collectors.toList());
		var multilineCommentStarterOutsideString = multilineCommentStarterMatcherResults.stream()
				.filter(x -> !stringMatcherResults.stream().anyMatch(y -> x.start() >= y.start() && x.end() <= y.end()))
				.collect(Collectors.toList());
		;
//		printMatchList(semikolonOutsideStrings, "semikolonOutsideStrings: ");
//		printMatchList(multilineCommentStarterOutsideString, "multilineCommentStarterOutsideString: ");
		var semikolonCommentIndexes = semikolonOutsideStrings.stream().map(x -> x.end() - 1)
				.collect(Collectors.toList());
		var multilineCommentStarterIndexes = multilineCommentStarterOutsideString.stream().map(x -> x.end() - 2)
				.collect(Collectors.toList());
//		System.out.println(semikolonCommentIndexes.toString());
//		System.out.println(multilineCommentStarterIndexes.toString());
		String newLine = System.getProperty("line.separator");
		int multilineCommentDepth = 0;
		int multilineCommentStartIndex = -1;
		int multilineCommentEndIndex = -1;
		boolean first = true;
		int index = 0;
		int lineNumber = 0;
		for (String aline : racketCodeWithComments.split(newLine)) {
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
			var mlcsInLine = multilineCommentStarterMatcherResults.stream().map(x -> x.end() - 2)
					.filter(x -> x >= lineStart && x < lineEnd).collect(Collectors.toList());
			var mlcsNoPrefixInLine = multilineCommentStarterMatcherNoPrefixResults.stream().map(x -> x.end() - 2)
					.filter(x -> x >= lineStart && x < lineEnd).collect(Collectors.toList());
			var mlcsOutsideOfStringInThisLine = multilineCommentStarterIndexes.stream()
					.filter(x -> x >= lineStart && x < lineEnd).collect(Collectors.toList());
			var mlceInLine = multilineCommentEnderMatcherResults.stream().map(x -> x.end() - 2)
					.filter(x -> x >= lineStart && x < lineEnd).collect(Collectors.toList());
			var scOutsideOfStringInThisLine = semikolonCommentIndexes.stream()
					.filter(x -> x >= lineStart && x <= lineEnd).collect(Collectors.toList());
			if (mlcsNoPrefixInLine.contains(lineStart)) { // Fix for the mlcs regex
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
								&& !(stli >= mlcsInLine.size() && enli >= mlceInLine.size());) {
							var sti = stli >= mlcsInLine.size() ? -1 : mlcsInLine.get(stli);
							var eni = enli >= mlceInLine.size() ? -1 : mlceInLine.get(enli);
							while (sti <= mlcs && stli < mlcsInLine.size() - 1) {
								stli++;
								sti = mlcsInLine.get(stli);
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
								if (stli < mlcsInLine.size()) {
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
						&& !(stli >= mlcsInLine.size() && enli >= mlceInLine.size());) {
					var sti = stli >= mlcsInLine.size() ? -1 : mlcsInLine.get(stli);
					var eni = enli >= mlceInLine.size() ? -1 : mlceInLine.get(enli);

					int currentIndex; // The openand currently processed
					boolean opening = false;
					// Select left most opening or closing multilene Comment operand
					if (sti < eni) {
						if (stli < mlcsInLine.size()) {
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
									&& !(stli >= mlcsInLine.size() && enli >= mlceInLine.size());) {
								var sti = stli >= mlcsInLine.size() ? -1 : mlcsInLine.get(stli);
								var eni = enli >= mlceInLine.size() ? -1 : mlceInLine.get(enli);

								while (sti <= mlcs && stli < mlcsInLine.size() - 1) {
									stli++;
									sti = mlcsInLine.get(stli);
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
									if (stli < mlcsInLine.size()) {
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
				String after = aline.substring(r.getEnd() - lineStart - eraseOffset);
				aline = before + after;
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

	@SuppressWarnings("unused") // Used for debugging only
	private static void printMatchList(List<MatchResult> matches, String name) {
		System.out.println(matches.stream().map(x -> String.format("{start: %s, end: %s}", x.start(), x.end()))
				.collect(Collectors.joining(",", name + "[", "]")));
	}
}
