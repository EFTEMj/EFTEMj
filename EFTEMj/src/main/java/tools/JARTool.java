package tools;

import ij.IJ;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class is used to load data that is stored at the JAR file of this plugin package.
 *
 * @author Michael Entrup b. Epping <entrup@arcor.de>
 *
 */
public class JARTool {

    public JARTool() {
	super();
    }

    /**
     * Loads a text file from within a JAR file using getResourceAsStream().<br />
     * Taken from <url>http://imagej.nih.gov/ij/plugins/download/JAR_Resources_Demo.java</url>.
     *
     * @return Content of the text file.
     */
    public String getText(final String path) {
	String text = "";
	try {
	    // get the text resource as a stream
	    final InputStream is = getClass().getResourceAsStream(path);
	    if (is == null) {
		IJ.showMessage("Load macro from JAR", "File not found in JAR at " + path);
		return "";
	    }
	    final InputStreamReader isr = new InputStreamReader(is);
	    final StringBuffer sb = new StringBuffer();
	    final char[] b = new char[8192];
	    int n;
	    // read a block and append any characters
	    while ((n = isr.read(b)) > 0)
		sb.append(b, 0, n);
	    // display the text in a TextWindow
	    text = sb.toString();
	} catch (final IOException e) {
	    String msg = e.getMessage();
	    if (msg == null || msg.equals(""))
		msg = "" + e;
	    IJ.showMessage("Load macro from JAR", msg);
	}
	return text;
    }

}
