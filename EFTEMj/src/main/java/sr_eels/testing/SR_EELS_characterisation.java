package sr_eels.testing;

import eftemj.EFTEMj;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;
import tools.JARTool;

public class SR_EELS_characterisation implements PlugIn {

    private final String filename_ijm_characterisation = "SR-EELS_characterisation.ijm";

    @Override
    public void run(final String arg) {
	IJ.runMacro(new JARTool().getText(EFTEMj.PATH_MACROS + filename_ijm_characterisation));
	// TODO Create the tutorial.
    }

    public static void main(final String[] args) {
	// start ImageJ
	new ImageJ();

	// run the plugin
	final Class<?> clazz = SR_EELS_characterisation.class;
	IJ.runPlugIn(clazz.getName(), "");
    }

}
