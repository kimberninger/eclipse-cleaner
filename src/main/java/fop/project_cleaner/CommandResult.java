package fop.project_cleaner;

/**
 * Stores Information about an Executed Command such as if the execution was
 * sucessfull and the output
 *
 * @author Ruben Deisenroth
 *
 */
public class CommandResult {
	private String resultString;
	private int exitCode;
	private boolean wasExecuted;
	private Throwable error;

	/**
	 * Create a {@link Command Result}
	 *
	 * @param resultString the Output
	 * @param exitCode     the exit code
	 * @param wasExecuted  if the command could be executed
	 * @param error        the error during the execution (can be null)
	 */
	public CommandResult(String resultString, int exitCode, boolean wasExecuted, Throwable error) {
		this.resultString = resultString;
		this.exitCode = exitCode;
		this.wasExecuted = wasExecuted;
		this.error = error;
	}

	/**
	 * Create a {@link Command Result}, invocation with wasExecuted=true and error =
	 * null
	 *
	 * @param resultString the Output
	 * @param exitCode     the exit code
	 */
	public CommandResult(String resultString, int exitCode) {
		this(resultString, exitCode, true, null);
	}

	/**
	 *
	 * @return the result String
	 */
	public String getResultString() {
		return resultString;
	}

	/**
	 *
	 * @return the Exit Code (0 if sucessfull)
	 */
	public int getExitCode() {
		return exitCode;
	}

	public boolean wasExecuted() {
		return wasExecuted;
	}

	public Throwable getError() {
		return error;
	}

	public boolean ok() {
		return exitCode == 0 && wasExecuted;
	}
}
