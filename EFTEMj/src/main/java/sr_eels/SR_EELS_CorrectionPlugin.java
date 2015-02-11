/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 *
 * Copyright (c) 2014, Michael Entrup b. Epping <michael.entrup@wwu.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package sr_eels;

import gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import libs.lma.LMA;
import sr_eels.testing.SR_EELS_Polynomial_2D;
import sr_eels.testing.SR_EELS_CharacterisationPlugin;

/**
 * <p>
 * This plugin is used to correct SR-EELS images.
 * </p>
 *
 * <p>
 * As it implements {@link ExtendedPlugInFilter} you have to apply it to an image. Additionally there has to be a
 * SR-EELS characterisation data set. Each data set contains the files <code>Borders.txt</code> and
 * <code>Width.txt</code> that are necessary to run the correction. The plugin assumes that the data set is can be found
 * in a sub folder of where the SR-EELS image is stored. If there is more than one characterisation data set, the plugin
 * presents a dialog to choose the preferred data set.
 * </p>
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 *
 */
public class SR_EELS_CorrectionPlugin implements ExtendedPlugInFilter {

    /**
     * Use this to enable the debug mode.
     *
     * For example multithreading is disabled when running in debug mode.
     */
    private static final boolean DEBUG = false;
    /**
     * The plugin will be aborted.
     */
    private final int CANCEL = 0;
    /**
     * The plugin will continue with the next step.
     */
    private final int OK = 1;
    /**
     * <code>DOES_32 | NO_CHANGES | FINAL_PROCESSING</code>
     */
    private final int FLAGS = DOES_32 | NO_CHANGES | FINAL_PROCESSING;
    /**
     * A {@link String} for the file field of the {@link GenericDialogPlus}.
     */
    private final String NO_FILE_SELECTED = "No file selected.";
    /**
     * The path where <code>Borders.txt</code> can be found.
     */
    private String pathBorders = NO_FILE_SELECTED;
    /**
     * The path where <code>Width.txt</code> can be found.
     */
    private String pathWidth = NO_FILE_SELECTED;
    /**
     * <p>
     * An {@link ImagePlus} that contains the image that will be corrected.
     * </p>
     *
     * <p>
     * This images will not be changed!
     * </p>
     */
    private ImagePlus inputImage;
    /**
     * <p>
     * An {@link ImagePlus} that contains the image that is the result of the correction.
     * </p>
     */
    private ImagePlus outputImage;
    /**
     * This field indicates the progress. A static method is used to increase the value by 1. It is necessary to use
     * volatile because different {@link Thread}s call the related method.
     */
    private static volatile int progress;
    /**
     * Number of steps until the correction is finished.
     */
    private static int progressSteps;

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
     */
    @Override
    public int setup(final String arg, final ImagePlus imp) {
	/*
	 * This will be called when the run method has finished.
	 */
	if (arg == "final") {
	    outputImage.show();
	    return NO_CHANGES | DONE;
	}
	return FLAGS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
     */
    @Override
    public void run(final ImageProcessor ip) {
	final CameraSetup camSetup = new CameraSetup(inputImage);
	/*
	 * Each correction contains of implementations of the abstract classes CoordinateCorrector and a
	 * IntensityCorrector that can be can be combined as you want.
	 * 
	 * Ba using getFunctionWidth() and getFunctionBorders() the characterisation results are loaded and an
	 * implementation of the Levenberg–Marquardt algorithm (LMA) is used to fit functions to the discrete values.
	 */
	final SimpleCoordinateCorrection coordinateCorrection = new SimpleCoordinateCorrection(
		getFunctionWidth(camSetup), getFunctionBorders(camSetup), camSetup);
	// final AnalyticalCoordinateCorrection coordinateCorrection = new AnalyticalCoordinateCorrection(
	// getFunctionWidth(), getFunctionBorders(), camSetup);
	final NoIntensityCorrection intensityCorrection = new NoIntensityCorrection(
		(FloatProcessor) inputImage.getProcessor(), coordinateCorrection, camSetup);
	/*
	 * TODO: Add the used correction methods to the image title.
	 */
	outputImage = new ImagePlus(inputImage.getTitle() + "_corrected", new FloatProcessor(inputImage.getWidth(),
		inputImage.getHeight(), new double[inputImage.getWidth() * inputImage.getHeight()]));
	final FloatProcessor output = (FloatProcessor) outputImage.getProcessor();
	/*
	 * Each line of the image is a step that is visualise by the progress bar of ImageJ.
	 */
	progressSteps = inputImage.getHeight();
	if (DEBUG) {
	    for (int x2 = 0; x2 < output.getHeight(); x2++) {
		for (int x1 = 0; x1 < inputImage.getWidth(); x1++) {
		    final float intensity = intensityCorrection.getIntensity(x1, x2);
		    output.setf(x1, x2, intensity);
		}
		updateProgress();
	    }
	} else {
	    /*
	     * The ExecutorService is used to handle the multithreading. see
	     * http://www.vogella.com/tutorials/JavaConcurrency/article.html#threadpools
	     */
	    final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
		    .availableProcessors());
	    for (int x2 = 0; x2 < inputImage.getHeight(); x2++) {
		final int x2Temp = x2;
		executorService.execute(new Runnable() {

		    @Override
		    public void run() {
			for (int x1 = 0; x1 < inputImage.getWidth(); x1++) {
			    final float intensity = intensityCorrection.getIntensity(x1, x2Temp);
			    output.setf(x1, x2Temp, intensity);
			}
			updateProgress();
		    }
		});
	    }
	    executorService.shutdown();
	    try {
		executorService.awaitTermination(30, TimeUnit.MINUTES);
	    } catch (final InterruptedException e) {
		e.printStackTrace();
	    }
	}
    }

    public ImagePlus correctImage(final ImagePlus input_image, final String path_borders, final String path_width) {
	this.inputImage = input_image;
	this.pathBorders = path_borders;
	this.pathWidth = path_width;
	run(null);
	return outputImage;
    }

    /**
     * <p>
     * The results of the {@link SR_EELS_CharacterisationPlugin} plugin are parsed.
     * </p>
     *
     * <p>
     * This function extracts the values that describe the pathway of the borders of a spectrum.
     * </p>
     *
     * @param camSetup
     *            is used to shift the point 0,0 to the centre of the camera.
     * @return a polynomial that fits the given data points
     */
    private SR_EELS_Polynomial_2D getFunctionBorders(final CameraSetup camSetup) {
	final DataImporter importer = new DataImporter(pathBorders, true);
	final double[][] vals = new double[importer.vals.length][importer.vals[0].length];
	for (int i = 0; i < vals.length; i++) {
	    // y value (this is a position on the x2 axis)
	    vals[i][0] = importer.vals[i][0] - camSetup.getCameraOffsetX2();
	    // x1 value
	    vals[i][1] = importer.vals[i][1] - camSetup.getCameraOffsetX1();
	    // x2 value
	    vals[i][2] = importer.vals[i][2] - camSetup.getCameraOffsetX2();
	}
	/*
	 * Define the orders of the 2D polynomial.
	 */
	final int m = 3;
	final int n = 2;
	final SR_EELS_Polynomial_2D func = new SR_EELS_Polynomial_2D(m, n);
	final double[] a_fit = new double[(m + 1) * (n + 1)];
	Arrays.fill(a_fit, 1.);
	final LMA lma = new LMA(func, a_fit, vals);
	lma.fit();
	if (true) {
	    IJ.log(func.compareWithGnuplot(SR_EELS_Polynomial_2D.BORDERS, camSetup));
	}
	return new SR_EELS_Polynomial_2D(m, n, a_fit);
    }

    /**
     * <p>
     * The results of the {@link SR_EELS_CharacterisationPlugin} plugin are parsed.
     * </p>
     *
     * <p>
     * This function extracts the values that describe the width of a spectrum depending on its position on the camera.
     * </p>
     *
     * @param camSetup
     *            is used to shift the point 0,0 to the centre of the camera.
     * @return a polynomial that fits the given data points
     */
    private SR_EELS_Polynomial_2D getFunctionWidth(final CameraSetup camSetup) {
	final DataImporter importer = new DataImporter(pathWidth, false);
	final double[][] vals = new double[importer.vals.length][importer.vals[0].length];
	for (int i = 0; i < vals.length; i++) {
	    // y value (the width is a difference of two x2 values)
	    vals[i][0] = importer.vals[i][0];
	    // x1 value
	    vals[i][1] = importer.vals[i][1] - camSetup.getCameraOffsetX1();
	    // x2 value
	    vals[i][2] = importer.vals[i][2] - camSetup.getCameraOffsetX2();
	}
	/*
	 * Define the orders of the 2D polynomial.
	 */
	final int m = 2;
	final int n = 2;
	final SR_EELS_Polynomial_2D func = new SR_EELS_Polynomial_2D(m, n);
	final double[] b_fit = new double[(m + 1) * (n + 1)];
	Arrays.fill(b_fit, 1.);
	final LMA lma = new LMA(func, b_fit, vals);
	lma.fit();
	if (true) {
	    IJ.log(func.compareWithGnuplot(SR_EELS_Polynomial_2D.WIDTH_VS_POS, camSetup));
	}
	return new SR_EELS_Polynomial_2D(m, n, b_fit);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.ExtendedPlugInFilter#showDialog(ij.ImagePlus, java.lang.String,
     * ij.plugin.filter.PlugInFilterRunner)
     */
    @Override
    public int showDialog(final ImagePlus imp, final String command, final PlugInFilterRunner pfr) {
	final String searchPath = IJ.getDirectory("image");
	final LinkedList<String> found_poly = new LinkedList<String>();
	final LinkedList<String> found_borders = new LinkedList<String>();
	findDatasets(searchPath, found_poly, SR_EELS.FILENAME_WIDTH);
	findDatasets(searchPath, found_borders, SR_EELS.FILENAME_BORDERS);
	if (found_poly.size() > 1 | found_borders.size() > 1) {
	    /*
	     * A dialog is presented to select one of the found files.
	     */
	    final GenericDialog gd = new GenericDialog(command + " - Select data set", IJ.getInstance());
	    if (found_poly.size() > 1) {
		String[] files_poly = new String[found_poly.size()];
		files_poly = found_poly.toArray(files_poly);
		gd.addRadioButtonGroup(SR_EELS.FILENAME_WIDTH, files_poly, found_poly.size(), 1, found_poly.get(0));
	    }
	    if (found_borders.size() > 1) {
		String[] files_borders = new String[found_borders.size()];
		files_borders = found_borders.toArray(files_borders);
		gd.addRadioButtonGroup(SR_EELS.FILENAME_BORDERS, files_borders, found_borders.size(), 1,
			found_borders.get(0));
	    }
	    gd.setResizable(false);
	    gd.showDialog();
	    if (gd.wasCanceled()) {
		canceled();
		return NO_CHANGES | DONE;
	    }
	    if (found_poly.size() > 1) {
		pathWidth = gd.getNextRadioButton();
	    }
	    if (found_borders.size() > 1) {
		pathBorders = gd.getNextRadioButton();
	    }
	}
	/*
	 * If only one file has been found, this one automatically is passed to the parameters dialog.
	 */
	if (found_poly.size() == 1) {
	    pathWidth = found_poly.getFirst();
	}
	if (found_borders.size() == 1) {
	    pathBorders = found_borders.getFirst();
	}
	do {
	    if (showParameterDialog(command) == CANCEL) {
		canceled();
		return NO_CHANGES | DONE;
	    }
	} while (!pathWidth.contains(".txt") | !pathBorders.contains(".txt"));
	inputImage = imp;
	return FLAGS;
    }

    /**
     * Searches the given folder for a data set file. Recursion is used to search in subfolders.
     *
     * @param searchPath
     *            the folder to search in.
     * @param found
     *            a {@link Vector} that stores all found file paths.
     * @param filename
     *            is the full name of the file we search for.
     */
    private void findDatasets(final String searchPath, final LinkedList<String> found, final String filename) {
	final String[] entries = new File(searchPath).list();
	for (final String entrie : entries) {
	    if (entrie.equals(filename)) {
		found.add(searchPath + entrie);
	    }
	    if (new File(searchPath + entrie).isDirectory()) {
		findDatasets(searchPath + entrie + File.separatorChar, found, filename);
	    }
	}
    }

    /**
     * Creates and shows the {@link GenericDialog} that is used to set the parameters for elemental mapping.
     *
     * @param title
     * @return The constant <code>OK</code> or <code>CANCEL</code>.
     */
    private int showParameterDialog(final String title) {
	final GenericDialogPlus gd = new GenericDialogPlus(title + " - set parameters", IJ.getInstance());
	gd.addFileField(SR_EELS.FILENAME_WIDTH, pathWidth);
	gd.addFileField(SR_EELS.FILENAME_BORDERS, pathBorders);
	// TODO Add drop down menu for correction method.
	gd.setResizable(false);
	gd.showDialog();
	if (gd.wasCanceled()) {
	    return CANCEL;
	}
	pathWidth = gd.getNextString();
	pathBorders = gd.getNextString();
	return OK;
    }

    /**
     * Cancel the plugin and show a status message.
     */
    private void canceled() {
	IJ.showStatus("SR-EELS correction has been canceled.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.ExtendedPlugInFilter#setNPasses(int)
     */
    @Override
    public void setNPasses(final int nPasses) {
	// This method is not used.
    }

    private static void updateProgress() {
	progress++;
	IJ.showProgress(progress, progressSteps);
    }

    /**
     * <p>
     * This main method is used for testing. It starts ImageJ, loads a test image and starts the plugin.
     * </p>
     *
     * <p>
     * User interaction is necessary, as the plugin uses a GUI.
     * </p>
     * <p>
     * <a href='https://github.com/imagej/minimal-ij1-plugin/blob/master/src/main/java/Process_Pixels.java'>see
     * minimal-ij1-plugin on GitHub</a>
     * </p>
     *
     * @param args
     */
    public static void main(final String[] args) {
	/*
	 * start ImageJ
	 */
	new ImageJ();

	/*
	 * open the test image
	 */
	final ImagePlus image = IJ.openImage("C:/Temp/20140507 SM315 -11%/SR-EELS_testImage_small.tif");
	image.show();

	/*
	 * run the plugin
	 */
	final Class<?> clazz = SR_EELS_CorrectionPlugin.class;
	IJ.runPlugIn(clazz.getName(), "");
    }

    /**
     * <p>
     * This class is used to load a data file that contains a data set for the fit of a 2D polynomial. For each y-value
     * there is are pairs of x-values that is stored at a 2D array.
     * </p>
     *
     * <p>
     * The data file must contain one data point at each line. Each data point contains of x1, x2 and y separated by
     * whitespace. Lines that contain a '#' are regarded as comments.
     * </p>
     *
     * <p>
     * The Plugin {@link SR_EELS_CharacterisationPlugin} creates files that can be processed by this class.
     * </p>
     *
     * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
     *
     */
    private static class DataImporter {

	/**
	 * <p>
	 * The first index will iterate through all the data points.
	 * </p>
	 * <p>
	 * The second index defines y-value (index 0), the x1-value (index 1) and the x2-value (index 2).
	 * </p>
	 */
	protected double[][] vals;
	/**
	 * When loading the file <code>Borders.txt</code> there is a fourth column, that contains the weight. This value
	 * is stored in a separate array-
	 */
	protected double[] weights;

	/**
	 * <p>
	 * Create a new data set by loading it from a file.
	 * </p>
	 *
	 * <p>
	 * This method sopports the files <code>Borders.txt</code> and <code>width.txt</code> that are created by
	 * {@link SR_EELS_CharacterisationPlugin}.
	 * </p>
	 *
	 * @param dataFilePath
	 *            is the path to the file that contains the data set.
	 *
	 * @param readWeights
	 *            is used to disable the readout of the fourth column that contains weights.
	 */
	public DataImporter(final String dataFilePath, final boolean readWeights) {
	    /*
	     * 'Borders.txt' contains a fourth column and has to be handled different than 'Width.txt'.
	     */
	    boolean isBordersTxt = false;
	    /*
	     * First we read the file and store the values a a vector.
	     */
	    final File file = new File(dataFilePath);
	    final Vector<Double[]> values = new Vector<Double[]>();
	    try {
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		boolean containsData = true;
		do {
		    final String line = reader.readLine();
		    if (line == null) {
			containsData = false;
		    } else {
			/*
			 * Only read the line if if does not contain any comment.
			 */
			if (line.indexOf('#') == -1) {
			    /*
			     * This RegExpr splits the line at whitespace characters.
			     */
			    final String[] splitLine = line.split("\\s+");
			    if (readWeights == true) {
				if (splitLine.length >= 4) {
				    isBordersTxt = true;
				    final Double[] point = { Double.valueOf(splitLine[0]),
					    Double.valueOf(splitLine[1]), Double.valueOf(splitLine[2]),
					    Double.valueOf(splitLine[3]) };
				    values.add(point);
				}
			    } else {
				if (splitLine.length >= 3) {
				    final Double[] point = { Double.valueOf(splitLine[0]),
					    Double.valueOf(splitLine[1]), Double.valueOf(splitLine[2]) };
				    values.add(point);
				}
			    }
			}
		    }
		} while (containsData);
		reader.close();
	    } catch (final FileNotFoundException exc) {
		exc.printStackTrace();
	    } catch (final IOException exc) {
		exc.printStackTrace();
	    }
	    /*
	     * Reading the file is done now.
	     */
	    vals = new double[values.size()][3];
	    weights = new double[values.size()];
	    for (int i = 0; i < values.size(); i++) {
		if (isBordersTxt) {
		    vals[i][0] = values.get(i)[2];
		} else {
		    vals[i][0] = values.get(i)[2];
		}
		vals[i][1] = values.get(i)[0];
		vals[i][2] = values.get(i)[1];
		if (readWeights == true) {
		    weights[i] = values.get(i)[3];
		}
	    }
	}
    }

}
