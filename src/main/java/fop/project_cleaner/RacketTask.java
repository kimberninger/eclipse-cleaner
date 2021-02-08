package fop.project_cleaner;

import java.util.ArrayList;

/**
 *
 * @author Ruben Deisenroth
 *
 */
public class RacketTask {
	private String title;
	private String annotation;
	private ArrayList<RacketTest> tests;
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the annotation
	 */
	public String getAnnotation() {
		return annotation;
	}
	/**
	 * @param annotation the annotation to set
	 */
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}
	/**
	 * @return the tests
	 */
	public ArrayList<RacketTest> getTests() {
		return tests;
	}
	/**
	 * @param tests the tests to set
	 */
	public void setTests(ArrayList<RacketTest> tests) {
		this.tests = tests;
	}
}
