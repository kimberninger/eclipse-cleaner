package fop.project_cleaner;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @see ActionSetModel
 * @author Ruben Deisenroth
 *
 */
public class JavaActionSetModel extends ActionSetModel {

	// In order to have easy json Conversion, we use Strings internally
	private ArrayList<String> assert_exists;
	private ArrayList<String> assert_not_exists;
	private ArrayList<String> overwrite_always;
	private ArrayList<String> ignore;
	private ArrayList<String> copy_if_not_exists;
	private String solutionFile;

	public static ArrayList<String> pathListToStringList(ArrayList<Path> paths) {
		return (ArrayList<String>) paths.stream().map(x -> x.toAbsolutePath().toString()).collect(Collectors.toList());
	}

	public static ArrayList<Path> stringListToPathList(ArrayList<String> paths){
		return (ArrayList<Path>) paths.stream().map(x -> Paths.get(x)).collect(Collectors.toList());
	}

	/**
	 * @return the assert_exists
	 */
	public ArrayList<Path> getAssert_exists() {
		return stringListToPathList(assert_exists);
	}

	/**
	 * @param assert_exists the assert_exists to set
	 */
	public void setAssert_exists(ArrayList<Path> assert_exists) {
		this.assert_exists = pathListToStringList(assert_exists);
	}

	/**
	 * @return the assert_not_exists
	 */
	public ArrayList<Path> getAssert_not_exists() {
		return stringListToPathList(assert_not_exists);
	}

	/**
	 * @param assert_not_exists the assert_not_exists to set
	 */
	public void setAssert_not_exists(ArrayList<Path> assert_not_exists) {
		this.assert_not_exists = pathListToStringList(assert_not_exists);
	}

	/**
	 * @return the overwrite_always
	 */
	public ArrayList<Path> getOverwrite_always() {
		return stringListToPathList(overwrite_always);
	}

	/**
	 * @param overwrite_always the overwrite_always to set
	 */
	public void setOverwrite_always(ArrayList<Path> overwrite_always) {
		this.overwrite_always = pathListToStringList(overwrite_always);
	}

	/**
	 * @return the copy_if_not_exists
	 */
	public ArrayList<Path> getCopy_if_not_exists() {
		return stringListToPathList(copy_if_not_exists);
	}

	/**
	 * @param copy_if_not_exists the copy_if_not_exists to set
	 */
	public void setCopy_if_not_exists(ArrayList<Path> copy_if_not_exists) {
		this.copy_if_not_exists = pathListToStringList(copy_if_not_exists);
	}

	/**
	 * @return the ignore
	 */
	public ArrayList<Path> getIgnore() {
		return stringListToPathList(ignore);
	}

	/**
	 * @param ignore the ignore to set
	 */
	public void setIgnore(ArrayList<Path> ignore) {
		this.ignore = pathListToStringList(ignore);
	}

	public void convertToAbsolutePaths(File ParentFolder) {
		for (ArrayList<String> list : List.of(assert_exists, assert_not_exists, overwrite_always, ignore,
				copy_if_not_exists)) {
			for (int i = 0; i < list.size(); i++) {
				list.set(i, Paths.get(ParentFolder.getAbsolutePath(), list.get(i)).toAbsolutePath().toString());
			}
		}
		setSolutionFile(ParentFolder);
	}

	/**
	 * @return the solutionFile
	 */
	public File getSolutionFile() {
		return Paths.get(solutionFile).toFile();
	}

	/**
	 * @param solutionFile the solutionFile to set
	 */
	public void setSolutionFile(File solutionFile) {
		this.solutionFile = solutionFile.getAbsolutePath();
	}
}
