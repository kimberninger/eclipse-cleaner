/**
 * 
 */
package project_cleaner;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Ruben Deisenroth
 *
 */
public class racketUtils {
	public static File findRaco() {
		File raco = null;
		String osName = System.getProperty("os.name");
		System.out.println("OS: " + osName);
		if (osName.toLowerCase().contains("windows")) {
			raco = Paths.get(System.getenv("SystemDrive"), "Program Files", "Racket", "raco.exe").toFile();
		} else if (osName.toLowerCase().contains("linux")) {
			String s;
			StringBuilder output = new StringBuilder();
			Process p;
			try {
				p = Runtime.getRuntime().exec("which raco");
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				while ((s = br.readLine()) != null)
					output.append(s);
				p.waitFor();
				p.destroy();
				if(p.exitValue() == 0) {
					raco = Paths.get(output.toString().trim()).toFile();
				} else {
					System.err.println("process exited with Code:" + p.exitValue());
				}
			} catch (Exception e) {
			}
		} else {
			System.out.println("✗ I can't find raco");
		}
		if(raco != null) {
			System.out.println("✓ raco found in: \"" + raco.getAbsolutePath()+ "\"");
		}
		return raco;
	}
}
