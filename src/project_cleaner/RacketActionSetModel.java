package project_cleaner;

import java.util.ArrayList;

/**
 * 
 * @see ActionSetModel
 * @author Ruben Deisenroth
 *
 */
public class RacketActionSetModel extends ActionSetModel {

	private boolean remove_student_tests = true, do_tests = true, verify_that_code_runs = true;
	private ArrayList<String> verify_strings_contained, verify_strings_not_contained;

	/**
	 * @return the remove_student_tests
	 */
	public boolean isRemove_student_tests() {
		return remove_student_tests;
	}

	/**
	 * @param remove_student_tests the remove_student_tests to set
	 */
	public void setRemove_student_tests(boolean remove_student_tests) {
		this.remove_student_tests = remove_student_tests;
	}

	/**
	 * @return the do_tests
	 */
	public boolean isDo_tests() {
		return do_tests;
	}

	/**
	 * @param do_tests the do_tests to set
	 */
	public void setDo_tests(boolean do_tests) {
		this.do_tests = do_tests;
	}

	/**
	 * @return the verifyCodeRuns
	 */
	public boolean isVerifyCodeRuns() {
		return verify_that_code_runs;
	}

	/**
	 * @param verifyCodeRuns the verifyCodeRuns to set
	 */
	public void setVerifyCodeRuns(boolean verifyCodeRuns) {
		this.verify_that_code_runs = verifyCodeRuns;
	}

	/**
	 * @return the verify_strings_contained
	 */
	public ArrayList<String> getVerify_strings_contained() {
		return verify_strings_contained;
	}

	/**
	 * @param verify_strings_contained the verify_strings_contained to set
	 */
	public void setVerify_strings_contained(ArrayList<String> verify_strings_contained) {
		this.verify_strings_contained = verify_strings_contained;
	}

	/**
	 * @return the verify_strings_not_contained
	 */
	public ArrayList<String> getVerify_strings_not_contained() {
		return verify_strings_not_contained;
	}

	/**
	 * @param verify_strings_not_contained the verify_strings_not_contained to set
	 */
	public void setVerify_strings_not_contained(ArrayList<String> verify_strings_not_contained) {
		this.verify_strings_not_contained = verify_strings_not_contained;
	}

}
