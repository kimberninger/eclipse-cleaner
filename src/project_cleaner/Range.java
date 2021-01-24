package project_cleaner;

/**
 * Stores a range between two {@link Integer}s
 * 
 * @author Ruben Deisenroth
 *
 */
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

	/**
	 * Returns true if an {@link Integer} lies between start and end
	 * 
	 * @param number the number to check
	 * @return true if a number lies between start and end
	 */
	public boolean contains(int number) {
		return (number >= start && number <= end);
	}

	/**
	 * Returns true if another {@link Range} is contained by this range
	 * 
	 * @param range the other {@link Range}
	 * @return true if another {@link Range} is contained by this range
	 */
	public boolean contains(Range range) {
		return contains(range.start) && contains(range.end);
	}
}