package project_cleaner;

public class Range {
	private int start;
	private int end;

	public Range(int low, int high) {
		this.setStart(low);
		this.setEnd(high);
	}

	/**
	 * @return the start
	 */
	public int getStart() {
		return start;
	}

	/**
	 * @param start the start to set
	 */
	public void setStart(int start) {
		this.start = start;
	}

	/**
	 * @return the end
	 */
	public int getEnd() {
		return end;
	}

	/**
	 * @param end the end to set
	 */
	public void setEnd(int end) {
		this.end = end;
	}

	/**
	 * Gets the length of the Range
	 * 
	 * @return
	 */
	public int length() {
		return Math.abs(end - start);
	}

	public boolean contains(int number) {
		return (number >= start && number <= end);
	}
}