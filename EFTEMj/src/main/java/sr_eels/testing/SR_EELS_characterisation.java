package sr_eels.testing;

import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;
import tools.JARTool;

public class SR_EELS_characterisation implements PlugIn {

    private String path_macros = "/macros/";
    private String ijm_characterisation = "SR-EELS_characterisation.ijm";

    public SR_EELS_characterisation() {
	// TODO Auto-generated constructor stub
    }

    @Override
    public void run(String arg) {
	IJ.runMacro(new JARTool(path_macros + ijm_characterisation).getText());
	// TODO Create the tutorial.
    }

    public static void main(String[] args) {
	// start ImageJ
	new ImageJ();

	// run the plugin
	Class<?> clazz = SR_EELS_characterisation.class;
	IJ.runPlugIn(clazz.getName(), "");
    }

}
