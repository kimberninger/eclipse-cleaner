package project_cleaner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String line = "asdf \"asd\" f";
//		Pattern stringRegex = Pattern.compile("(?<!\\\\)(?:\\\\\\\\)*\\\"(\\\\.|[^\\\"])*\\\"");
//		Matcher stringMatcher = stringRegex.matcher(line);
//		Pattern semicolonRegex = Pattern.compile("(?<!\\)(?:\\\\)*;");
//		Matcher semicolonMatcher = semicolonRegex.matcher(line);
//		var matches = stringMatcher.results();
//		if(semicolonMatcher.results().anyMatch(x -> stringMatcher.results().anyMatch(y -> x.start() >= y.start() && x.start() + x.end() <= y.start()+y.end()))) {
//			var commentMatch = semicolonMatcher.results().filter(x -> stringMatcher.results().anyMatch(y -> x.start() >= y.start() && x.start() + x.end() <= y.start()+y.end())).findFirst().get();
//			line  = line.substring(0, commentMatch.start());
//		}
		line.replaceAll("(?<!\\)(?:\\\\)*\"(\\.|[^\"])*\"", "");
		System.out.println(line);
	}

}
