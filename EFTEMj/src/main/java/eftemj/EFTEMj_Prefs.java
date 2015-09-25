
package eftemj;

import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import ij.IJ;
import ij.ImageJ;
import ij.Prefs;

public class EFTEMj_Prefs {

	private final static String PREFS_LOG_FILE_PATH = "path.logFile";
	private final static String LOG_FILE_PATH = "C:/temp/EFTEMj.log";

	public static void main(final String[] args) {
		/*
		 * start ImageJ
		 */
		new ImageJ();

		final Properties props = Prefs.getControlPanelProperties();
		final Set<Object> set = props.keySet();
		final String searchWord = "dispersion";
		IJ.showMessage("Size: " + set.size() + "\n" + "dir: " + Prefs
			.getPrefsDir());
		final Vector<String> found = new Vector<String>();
		for (final Object i : set) {
			final String str = (String) i;
			if (str.contains("EFTEMj")) if (str.contains(searchWord)) found.add(str);
		}
		for (final String i : found) {
			IJ.log(i + " = " + props.getProperty(i));
		}
	}

	public static Vector<String> getAllKeys() {
		final Vector<String> keys = new Vector<String>();
		final Properties props = Prefs.getControlPanelProperties();
		final Set<Object> set = props.keySet();
		for (final Object obj : set) {
			String key = (String) obj;
			key = key.substring(1, key.length());
			keys.add(key);
		}
		return keys;
	}

	public static Vector<String> getAllKeys(final String searchWord) {
		final Vector<String> keys = getAllKeys();
		final Vector<String> found = new Vector<String>();
		for (final String key : keys) {
			if (key.contains(searchWord)) {
				found.add(key);
			}
		}
		return found;
	}

	public static String getLogFile() {
		final String path = Prefs.get(EFTEMj.PREFS_PREFIX + PREFS_LOG_FILE_PATH,
			LOG_FILE_PATH);
		return path;
	}

}
