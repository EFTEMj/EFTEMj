
package sr_eels;

import eftemj.EFTEMj;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;
import tools.JARTool;

public class SR_EELS_CharacterisationPlugin implements PlugIn {

	private final String filename_ijm_Characterisation =
		"SR-EELS_Characterisation.ijm";

	@Override
	public void run(final String arg) {
		IJ.runMacro(new JARTool().getText(EFTEMj.PATH_SCRIPTS_AND_MACROS +
			filename_ijm_Characterisation));
	}

	public static void main(final String[] args) {
		// start ImageJ
		new ImageJ();

		// run the plugin
		final Class<?> clazz = SR_EELS_CharacterisationPlugin.class;
		IJ.runPlugIn(clazz.getName(), "");
	}

}
