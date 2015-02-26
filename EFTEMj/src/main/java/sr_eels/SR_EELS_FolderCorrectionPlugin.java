package sr_eels;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.io.File;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SR_EELS_FolderCorrectionPlugin implements PlugIn {

    @Override
    public void run(final String arg) {
	final String path = "Q:\\Aktuell\\SR-EELS Calibration measurements\\";
	// final String path = IJ.getDirectory("Select a Folder...");
	final File folder = new File(path);
	if (!folder.isDirectory())
	    return;
	final String[] list = folder.list();
	for (final String item : list) {
	    if (item.contains("SM") & item.contains("%")) {
		processDataSet(path + item);
	    }
	}
	IJ.log("Finished!");
    }

    private void processDataSet(final String paramPath) {
	String path;
	if (paramPath.endsWith(File.separator))
	    path = paramPath;
	else
	    path = paramPath + File.separator;
	final File folder = new File(path);
	final String[] list = folder.list();
	final Vector<String> dataSets = new Vector<String>();
	String image = "";
	for (final String item : list) {
	    final File subFile = new File(path + item);
	    if (subFile.isDirectory()) {
		final String pattern = "results_\\d+\\w+";
		final Pattern r = Pattern.compile(pattern);
		final Matcher m = r.matcher(item);
		if (m.find()) {
		    String tempPath;
		    if (item.endsWith(File.separator))
			tempPath = path + item;
		    else
			tempPath = path + item + File.separator;
		    dataSets.add(tempPath);
		    IJ.log(tempPath);
		}
	    } else {
		if (image.equals("") & item.endsWith(".tif")) {
		    image = path + item;
		    IJ.log(image);
		}
	    }
	}
	IJ.run("Image Sequence...", "open=[" + image + "] file=Cal_ sort");
	final ImagePlus stack = IJ.getImage();
	IJ.run(stack, "Z Project...", "projection=[Sum Slices]");
	stack.close();
	final ImagePlus projection = IJ.getImage();
	IJ.save(projection, path + "projection.tif");
	final SR_EELS_CorrectionPlugin correction = new SR_EELS_CorrectionPlugin();
	for (int i = 0; i < dataSets.size(); i++) {
	    final String[] split = dataSets.get(i).split("_");
	    final String params = split[split.length - 1];
	    // ImagePlus projection = IJ.openImage(image);
	    final ImagePlus result = correction.correctImage(projection, dataSets.get(i) + "Borders.txt",
		    dataSets.get(i) + "Width.txt");
	    IJ.save(result, path + "result_" + params.substring(0, params.length() - 1) + ".tif");
	}
	projection.close();

    }

    public static void main(final String[] args) {
	// start ImageJ
	new ImageJ();

	// run the plugin
	final Class<?> clazz = SR_EELS_FolderCorrectionPlugin.class;
	IJ.runPlugIn(clazz.getName(), "");
    }
}
