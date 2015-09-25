
package tutorials;

import java.io.InputStream;

import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.PlugIn;

/**
 * This Plugin is a tutorial on how to create an elemental map from energy
 * filtered images.<br />
 * It will automatically open example images/stacks and instruct the user on how
 * to process them using EFTEMj.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 */
public class Tutorial_ESI implements PlugIn {

	private final String path = "/example-images/";
	private final String stack_tif = "EFTEM-Stack_Fe_50counts.tif";

	@Override
	public void run(final String arg) {
		// Open a tiff-file that is inside the jar-file (taken from
		// http://imagej.nih.gov/ij/plugins/download/JAR_Resources_Demo.java)
		final InputStream is = getClass().getResourceAsStream(path + stack_tif);
		if (is != null) {
			final Opener opener = new Opener();
			final ImagePlus imp = opener.openTiff(is, stack_tif);
			if (imp != null) {
				imp.show();
			}
		}
		// TODO Create the tutorial.
	}

}
