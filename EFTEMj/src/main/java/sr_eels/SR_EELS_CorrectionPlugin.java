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

import libs.lma.LMA;
import sr_eels.testing.SR_EELS_Polynomial_2D;
import sr_eels.testing.SR_EELS_characterisation;

/**
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 *
 */
public class SR_EELS_CorrectionPlugin implements ExtendedPlugInFilter {

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
    private final String NO_FILE_SELECTED = "No file selected.";
    private String path_borders = NO_FILE_SELECTED;
    private String path_width = NO_FILE_SELECTED;
    private int subdivision;
    private int oversampling;
    private ImagePlus inputImage;
    private ImagePlus outputImage;
    private final int binning = 1;
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
	final SimpleCoordinateCorrection coordinateCorrection = new SimpleCoordinateCorrection(getFunctionWidth(),
		getFunctionBorders());
	final NoIntensityCorrection intensityCorrection = new NoIntensityCorrection(
		(FloatProcessor) inputImage.getProcessor(), coordinateCorrection);
	outputImage = new ImagePlus(inputImage.getTitle() + "_corrected", new FloatProcessor(inputImage.getWidth(),
		inputImage.getHeight(), new double[inputImage.getWidth() * inputImage.getHeight()]));
	final FloatProcessor output = (FloatProcessor) outputImage.getProcessor();
	progressSteps = inputImage.getHeight();
	for (int x2 = 0; x2 < inputImage.getHeight(); x2++) {
	    for (int x1 = 0; x1 < inputImage.getWidth(); x1++) {
		output.setf(x2 * output.getWidth() + x1, intensityCorrection.getIntensity(x1, x2));
	    }
	    updateProgress();
	}
    }

    /**
     * The parameters of two 3D polynomials are parsed from a file and stored at individual arrays.
     */
    private SR_EELS_Polynomial_2D getFunctionBorders() {
	final DataImporter importer = new DataImporter(path_borders, true);
	final double[][] vals = importer.vals;
	final int m = 3;
	final int n = 2;
	final SR_EELS_Polynomial_2D func = new SR_EELS_Polynomial_2D(m, n);
	final double[] a_fit = new double[(m + 1) * (n + 1)];
	Arrays.fill(a_fit, 1.);
	final LMA lma = new LMA(func, a_fit, vals);
	lma.fit();
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		System.out.printf("a%d%d = %g\n", i, j, a_fit[i * m + j]);
	    }
	}
	return new SR_EELS_Polynomial_2D(m, n, a_fit);
    }

    /**
     * The parameters of two 3D polynomials are parsed from a file and stored at individual arrays.
     */
    private SR_EELS_Polynomial_2D getFunctionWidth() {
	final DataImporter importer = new DataImporter(path_width, false);
	final double[][] vals = importer.vals;
	final int m = 3;
	final int n = 2;
	final SR_EELS_Polynomial_2D func = new SR_EELS_Polynomial_2D(m, n);
	final double[] b_fit = new double[(m + 1) * (n + 1)];
	Arrays.fill(b_fit, 1.);
	final LMA lma = new LMA(func, b_fit, vals);
	lma.fit();
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		System.out.printf("b%d%d = %g\n", i, j, b_fit[i * m + j]);
	    }
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
		String[] files_borders = new String[found_poly.size()];
		files_borders = found_poly.toArray(files_borders);
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
		path_width = gd.getNextRadioButton();
	    }
	    if (found_borders.size() > 1) {
		path_borders = gd.getNextRadioButton();
	    }
	}
	/*
	 * If only one file has been found, this one automatically is passed to the parameters dialog.
	 */
	if (found_poly.size() == 1) {
	    path_width = found_poly.getFirst();
	}
	if (found_borders.size() == 1) {
	    path_borders = found_borders.getFirst();
	}
	do {
	    if (showParameterDialog(command) == CANCEL) {
		canceled();
		return NO_CHANGES | DONE;
	    }
	} while (!path_width.contains(".txt") | !path_borders.contains(".txt"));
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
	gd.addFileField(SR_EELS.FILENAME_WIDTH, path_width);
	gd.addFileField(SR_EELS.FILENAME_BORDERS, path_borders);
	gd.addNumericField("Pixel_subdivision", 10, 0);
	gd.addNumericField("Oversampling", 3, 0);
	gd.setResizable(false);
	gd.showDialog();
	if (gd.wasCanceled()) {
	    return CANCEL;
	}
	path_width = gd.getNextString();
	path_borders = gd.getNextString();
	subdivision = (int) gd.getNextNumber();
	oversampling = (int) gd.getNextNumber();
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

    public static void main(final String[] args) {
	// start ImageJ
	new ImageJ();

	// open the sample stack
	final ImagePlus image = IJ.openImage("C:\\Temp\\SR_EELS_superposition.tif");
	image.show();

	// run the plugin
	final Class<?> clazz = SR_EELS_CorrectionPlugin.class;
	IJ.runPlugIn(clazz.getName(), "");
    }

    /**
     * <p>
     * This class is used to load a data file that contains a data set for the fit of a 2D polynomial. For each y-value
     * there is are pairs of x-values that are stored at a 2D array.
     * </p>
     *
     * <p>
     * The data file must contain one data point at each line. Each data point contains of x1, x2 and y separated by
     * whitespace. Lines that contain a '#' are regarded as comments.
     * </p>
     *
     * <p>
     * The Plugin {@link SR_EELS_characterisation} creates files that can be processed by this class.
     * </p>
     *
     * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
     *
     */
    private static class DataImporter {

	protected double[][] vals;
	protected double[] weights;

	/**
	 * Create a new data set by loading it from a file.
	 *
	 * @param dataFilePath
	 *            is the path to the file that contains the data set.
	 */
	public DataImporter(final String dataFilePath, final boolean readWeights) {
	    boolean isBordersTxt = false;
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
	    vals = new double[values.size()][3];
	    weights = new double[values.size()];
	    for (int i = 0; i < values.size(); i++) {
		if (isBordersTxt) {
		    vals[i][0] = values.get(i)[2] - 2048;
		} else {
		    vals[i][0] = values.get(i)[2];
		}
		vals[i][1] = values.get(i)[0] - 2048;
		vals[i][2] = values.get(i)[1] - 2048;
		if (readWeights == true) {
		    weights[i] = values.get(i)[3];
		}
	    }
	}
    }

}
