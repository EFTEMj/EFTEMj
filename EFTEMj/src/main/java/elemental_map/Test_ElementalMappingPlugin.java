package elemental_map;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class Test_ElementalMappingPlugin {

    public static void main(String[] args) {
	// set the plugins.dir property to make the plugin appear in the Plugins
	// menu
	Class<?> clazz = ElementalMappingPlugin.class;
	String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
	String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
	System.setProperty("plugins.dir", pluginsDir);

	// start ImageJ
	new ImageJ();

	// open the sample stack
	String path = IJ.getDirectory("imagej");
	// the string is shortened by 2 to remove the tailing '\\'
	path = path.substring(0, path.substring(0, path.length() - 2).lastIndexOf("\\"))
		+ "\\test-images\\EFTEM-Stack_Fe_50counts.tif";
	System.out.println(path);
	ImagePlus image = IJ.openImage(path);
	image.show();

	// run the plugin
	IJ.runPlugIn(clazz.getName(), "");
    }

}
