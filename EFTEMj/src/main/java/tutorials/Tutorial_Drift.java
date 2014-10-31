package tutorials;

import ij.IJ;
import ij.plugin.PlugIn;
import tools.JARTool;

/**
 * This Plugin is a tutorial on how to use the drift correction.<br />
 * It will automatically create an example stack and instructs the user on how to correct the drift.
 * 
 * @author Michael Entrup b. Epping <entrup@arcor.de>
 *
 */
public class Tutorial_Drift implements PlugIn {

    private String path_macros = "/macros/";
    private String ijm_create = "create_Drift-Stack.ijm";

    @Override
    public void run(String arg) {
	IJ.runMacro(new JARTool(path_macros + ijm_create).getText());
	// TODO Create the tutorial.
    }
}