module project_cleaner {
    requires java.desktop;
	requires java.xml;
	requires java.logging;
	requires com.google.gson;
	exports project_cleaner to com.google.gson;
	opens project_cleaner to com.google.gson;
}