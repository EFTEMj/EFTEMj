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
import ij.process.ImageProcessor;

import java.io.File;
import java.util.LinkedList;
import java.util.Vector;

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
    private String path_poly = NO_FILE_SELECTED;
    private int subdivision;
    private int oversampling;
    private ImagePlus inputImage;
    private final int binning = 4;

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
     */
    @Override
    public int setup(final String arg, final ImagePlus imp) {
	if (arg == "final") {
	    // TODO Implement final processing
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
	final SR_EELS_CorrectionFunction func = new SR_EELS_CorrectionFunction(path_borders, path_poly);
	final SR_EELS_Correction correction = new SR_EELS_Correction(inputImage, binning, func, subdivision,
		oversampling);
	correction.startCalculation();
	correction.showResult();
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
	findDatasets(searchPath, found_poly, SR_EELS.FILENAME_POLINOMIAL_2D);
	findDatasets(searchPath, found_borders, SR_EELS.FILENAME_BORDERS);
	if (found_poly.size() > 1 | found_borders.size() > 1) {
	    /*
	     * A dialog is presented to select one of the found files.
	     */
	    final GenericDialog gd = new GenericDialog(command + " - Select data set", IJ.getInstance());
	    if (found_poly.size() > 1) {
		String[] files_poly = new String[found_poly.size()];
		files_poly = found_poly.toArray(files_poly);
		gd.addRadioButtonGroup(SR_EELS.FILENAME_POLINOMIAL_2D, files_poly, found_poly.size(), 1,
			found_poly.get(0));
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
		path_poly = gd.getNextRadioButton();
	    }
	    if (found_borders.size() > 1) {
		path_borders = gd.getNextRadioButton();
	    }
	}
	/*
	 * If only one file has been found, this one automatically is passed to the parameters dialog.
	 */
	if (found_poly.size() == 1) {
	    path_poly = found_poly.getFirst();
	}
	if (found_borders.size() == 1) {
	    path_borders = found_borders.getFirst();
	}
	do {
	    if (showParameterDialog(command) == CANCEL) {
		canceled();
		return NO_CHANGES | DONE;
	    }
	} while (!path_poly.contains(".txt") | !path_borders.contains(".txt"));
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
	gd.addFileField(SR_EELS.FILENAME_POLINOMIAL_2D, path_poly);
	gd.addFileField(SR_EELS.FILENAME_BORDERS, path_borders);
	gd.addNumericField("Pixel_subdivision", 10, 0);
	gd.addNumericField("Oversampling", 3, 0);
	gd.setResizable(false);
	gd.showDialog();
	if (gd.wasCanceled()) {
	    return CANCEL;
	}
	path_poly = gd.getNextString();
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

    public static void main(final String[] args) {
	// start ImageJ
	new ImageJ();

	// open the sample stack
	final ImagePlus image = IJ.openImage("C:\\Temp\\SR_EELS.tif");
	image.show();

	// run the plugin
	final Class<?> clazz = SR_EELS_CorrectionPlugin.class;
	IJ.runPlugIn(clazz.getName(), "");
    }

}
