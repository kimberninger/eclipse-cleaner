package project_cleaner;

/**
 * A Model for performing Actions on Submissions
 * 
 * @author Ruben Deisenroth
 *
 */
public class ActionSetModel {
	private int sheet_number;
	private LanguageMode language_mode;
	private boolean check_naming_convention = true, fix_naming_convention = true;

	/**
	 * @return the sheet_number
	 */
	public int getSheet_number() {
		return sheet_number;
	}

	/**
	 * @param sheet_number the sheet_number to set
	 */
	public void setSheet_number(int sheet_number) {
		this.sheet_number = sheet_number;
	}

	/**
	 * @return the language_mode
	 */
	public LanguageMode getLanguage_mode() {
		return language_mode;
	}

	/**
	 * @param language_mode the language_mode to set
	 */
	public void setLanguage_mode(LanguageMode language_mode) {
		this.language_mode = language_mode;
	}

	/**
	 * @return the fix_naming_convention
	 */
	public boolean shouldFix_naming_convention() {
		return fix_naming_convention;
	}

	/**
	 * @param fix_naming_convention the fix_naming_convention to set
	 */
	public void setFix_naming_convention(boolean fix_naming_convention) {
		this.fix_naming_convention = fix_naming_convention;
	}

	/**
	 * @return the check_naming_convention
	 */
	public boolean shouldCheck_naming_convention() {
		return check_naming_convention;
	}

	/**
	 * @param check_naming_convention the check_naming_convention to set
	 */
	public void setCheck_naming_convention(boolean check_naming_convention) {
		this.check_naming_convention = check_naming_convention;
	}

}
