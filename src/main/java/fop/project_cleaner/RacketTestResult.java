package fop.project_cleaner;

public class RacketTestResult extends CommandResult {
	private boolean passed = false;
	private RacketTest test;

	public RacketTestResult(String resultString, boolean passed) {
		super(resultString, 0);
		this.passed = passed;
	}

	public RacketTestResult(String resultString, boolean passed, RacketTest test) {
		this(resultString, passed);
		this.setTest(test);
	}

	public boolean hasPassed() {
		return ok() && passed;
	}

	/**
	 * @return the test
	 */
	public RacketTest getTest() {
		return test;
	}

	/**
	 * @param test the test to set
	 */
	public void setTest(RacketTest test) {
		this.test = test;
	}
}
