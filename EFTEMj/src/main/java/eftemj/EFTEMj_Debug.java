package eftemj;

import ij.ImageJ;

public class EFTEMj_Debug {

    /**
     * Set the plugins.dir property to make the EFTEMj appear in the Plugins menu. Finally start ImageJ.
     * 
     * @param args
     *            Not used
     */
    public static void main(String[] args) {
	Class<?> clazz = EFTEMj_Debug.class;
	String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
	// this is the path were maven creates the jar-file
	String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length()
		- "classes/".length());
	System.setProperty("plugins.dir", pluginsDir);

	// start ImageJ
	new ImageJ();
    }

}
