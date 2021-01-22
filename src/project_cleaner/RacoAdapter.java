package project_cleaner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
			var stderr = error.toString();
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

	public String removeCommentsFromCode(String racketCodeWithComments) {
		StringBuilder newCode = new StringBuilder();
		boolean first = true;
		String newLine = System.getProperty("line.separator");
		var StringMode = false;
		for (String line : racketCodeWithComments.split(newLine)) {
			if (first) {
				first = false;
			} else {
				newCode.append(newLine);
			}
			Pattern stringRegex = Pattern.compile("(?<!\\)(?:\\\\)*\"(\\.|[^\"])*\"");
			Matcher stringMatcher = stringRegex.matcher(line);
			Pattern semicolonRegex = Pattern.compile("(?<!\\)(?:\\\\)*;");
			Matcher semicolonMatcher = semicolonRegex.matcher(line);
			var matches = stringMatcher.results();
			if(semicolonMatcher.results().anyMatch(x -> stringMatcher.results().anyMatch(y -> x.start() >= y.start() && x.start() + x.end() <= y.start()+y.end()))) {
				var commentMatch = semicolonMatcher.results().filter(x -> stringMatcher.results().anyMatch(y -> x.start() >= y.start() && x.start() + x.end() <= y.start()+y.end())).findFirst().get();
				line  = line.substring(0, commentMatch.start());
			}
			if((int) stringMatcher.results().count() %2 != 0) {
				
			}
		}
		return newCode.toString();
	}
}
