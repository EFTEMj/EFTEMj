package sr_eels;

import eftemj.EFTEMj;
import tools.JARTool;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.JavaScriptEvaluator;
import ij.plugin.PlugIn;

public class SR_EELS_PresentResultsPlugin implements PlugIn {

    private String filename_sreels_PresentResults = "SR-EELS_PresentResults.js";

    @Override
    public void run(String arg) {
	String javaScriptCode = new JARTool().getText(EFTEMj.PATH_SCRIPTS_AND_MACROS
		+ filename_sreels_PresentResults );
	JavaScriptEvaluator jse = new JavaScriptEvaluator();
	jse.run(javaScriptCode);
    }

    public static void main(String[] args) {
	// start ImageJ
		new ImageJ();

		// run the plugin
		final Class<?> clazz = SR_EELS_PresentResultsPlugin.class;
		IJ.runPlugIn(clazz.getName(), "");
    }

}
