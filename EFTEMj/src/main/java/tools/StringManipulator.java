package tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringManipulator {

    public static String removeExtensionFromTitle(String title) {
	String pattern = "(.*)\\.\\w{2,3}$";
	Pattern r = Pattern.compile(pattern);
	Matcher m = r.matcher(title);
	if (m.matches()) {
	    return m.group(1);
	}
	return title;
    }

}
