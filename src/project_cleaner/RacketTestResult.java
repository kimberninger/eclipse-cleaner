package project_cleaner;

public class RacketTestResult extends CommandResult {
	private boolean passed = false;

	public RacketTestResult(String resultString, boolean passed) {
		super(resultString, 0);
		this.passed = passed;
	}

	public boolean hasPassed() {
		return ok() && passed;
	}
}
