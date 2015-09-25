
package tutorials;

import eftemj.EFTEMj;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;
import tools.JARTool;

/**
 * This Plugin is a tutorial on how to use the drift correction.<br />
 * It will automatically create an example stack and instructs the user on how
 * to correct the drift.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 */
public class Tutorial_Drift implements PlugIn {

	private final String filename_ijm_create = "create_Drift-Stack.ijm";

	@Override
	public void run(final String arg) {
		IJ.runMacro(new JARTool().getText(EFTEMj.PATH_SCRIPTS_AND_MACROS +
			filename_ijm_create));
		// TODO Create the tutorial.
	}

	public static void main(final String[] args) {
		// start ImageJ
		new ImageJ();

		// run the plugin
		final Class<?> clazz = Tutorial_Drift.class;
		IJ.runPlugIn(clazz.getName(), "");
	}
}
