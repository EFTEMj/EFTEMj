package tutorials;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ij.IJ;
import ij.plugin.PlugIn;

/**
 * This Plugin is a tutorial on how to use the drift correction.<br />
 * It will automatically create an example stack and instructs the user on how to correct the drift.
 * 
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 *
 */
public class Tutorial_Drift implements PlugIn {

    private String path_macros = "/macros/";
    private String ijm_create = "create_Drift-Stack.ijm";

    @Override
    public void run(String arg) {
	IJ.runMacro(getText(path_macros + ijm_create));
	// TODO Create the tutorial.
    }

    /**
     * Loads a text file from within a JAR file using getResourceAsStream().<br />
     * Taken from <url>http://imagej.nih.gov/ij/plugins/download/JAR_Resources_Demo.java</url>.
     * 
     * @param path
     *            Path to the text file.
     * @return Content of the text file.
     */
    private String getText(String path) {
	String text = "";
	try {
	    // get the text resource as a stream
	    InputStream is = getClass().getResourceAsStream(path);
	    if (is == null) {
		IJ.showMessage("Load macro from JAR", "File not found in JAR at " + path);
		return "";
	    }
	    InputStreamReader isr = new InputStreamReader(is);
	    StringBuffer sb = new StringBuffer();
	    char[] b = new char[8192];
	    int n;
	    // read a block and append any characters
	    while ((n = isr.read(b)) > 0)
		sb.append(b, 0, n);
	    // display the text in a TextWindow
	    text = sb.toString();
	} catch (IOException e) {
	    String msg = e.getMessage();
	    if (msg == null || msg.equals(""))
		msg = "" + e;
	    IJ.showMessage("Load macro from JAR", msg);
	}
	return text;
    }
}