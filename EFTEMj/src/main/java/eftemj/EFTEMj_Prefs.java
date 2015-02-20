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

    public static void main(String[] args) {
	/*
	 * start ImageJ
	 */
	new ImageJ();

	Properties props = Prefs.getControlPanelProperties();
	Set<Object> set = props.keySet();
	String searchWord = "dispersion";
	IJ.showMessage("Size: " + set.size() + "\n" + "dir: " + Prefs.getPrefsDir());
	Vector<String> found = new Vector<String>();
	for (Object i : set) {
	    String str = (String) i;
	    if (str.contains("EFTEMj"))
		if (str.contains(searchWord))
		    found.add(str);
	}
	for (String i : found) {
	    IJ.log(i + " = " + props.getProperty(i));
	}
    }

    public static Vector<String> getAllKeys() {
	Vector<String> keys = new Vector<String>();
	Properties props = Prefs.getControlPanelProperties();
	Set<Object> set = props.keySet();
	for (Object obj : set) {
	    String key = (String) obj;
	    key = key.substring(1, key.length());
	    keys.add(key);
	}
	return keys;
    }

    public static Vector<String> getAllKeys(String searchWord) {
	Vector<String> keys = getAllKeys();
	Vector<String> found = new Vector<String>();
	for (String key : keys) {
	    if (key.contains(searchWord)) {
		found.add(key);
	    }
	}
	return found;
    }

    public static String getLogFile() {
	String path = Prefs.get(EFTEMj.PREFS_PREFIX + PREFS_LOG_FILE_PATH, LOG_FILE_PATH);
	return path;
    }

}
