package project_cleaner;

/**
 * 
 * @author Ruben Deisenroth
 *
 */
public class RacketTest {
	private String title;
	private String description;
	private int repeat = 1;
	private String code;
	private int maxEcecTimeInSeconds = 30;

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
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the repeat
	 */
	public int getRepeat() {
		return repeat;
	}

	/**
	 * @param repeat the repeat to set
	 */
	public void setRepeat(int repeat) {
		this.repeat = repeat;
	}

	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return the maxEcecTimeInSeconds
	 */
	public int getMaxEcecTimeInSeconds() {
		return maxEcecTimeInSeconds;
	}

	/**
	 * @param maxEcecTimeInSeconds the maxEcecTimeInSeconds to set
	 */
	public void setMaxEcecTimeInSeconds(int maxEcecTimeInSeconds) {
		this.maxEcecTimeInSeconds = maxEcecTimeInSeconds;
	}
}
