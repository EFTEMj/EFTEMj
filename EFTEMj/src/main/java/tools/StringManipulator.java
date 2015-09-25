
package tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringManipulator {

	public static String removeExtensionFromTitle(final String title) {
		final String pattern = "(.*)\\.\\w{2,3}$";
		final Pattern r = Pattern.compile(pattern);
		final Matcher m = r.matcher(title);
		if (m.matches()) {
			return m.group(1);
		}
		return title;
	}

}
