/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 *
 * Copyright (c) 2014, Michael Entrup b. Epping
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

package tools;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

/**
 * A simple plugin that will transfer the calibration of one image to another.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 */
public class CalibrationTransferPlugin implements ExtendedPlugInFilter {

	/**
	 * <code>FLAGS = DOES_ALL | NO_CHANGES | FINAL_PROCESSING</code>
	 */
	private final int FLAGS = DOES_ALL | NO_CHANGES | FINAL_PROCESSING;
	/**
	 * The {@link ImagePlus} the {@link Calibration} is taken from.
	 */
	private ImagePlus impSource;
	/**
	 * The {@link ImagePlus} the {@link Calibration} is transfered to.
	 */
	private ImagePlus impTarget;

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	@Override
	public int setup(final String arg, final ImagePlus imp) {
		if (arg.equals("final")) {
			impTarget.updateAndRepaintWindow();
			impTarget.changes = true;
			return DONE;
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
		final Calibration cal = impSource.getCalibration();
		impTarget.setCalibration(cal);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.filter.ExtendedPlugInFilter#showDialog(ij.ImagePlus, java.lang.String,
	 * ij.plugin.filter.PlugInFilterRunner)
	 */
	@Override
	public int showDialog(final ImagePlus imp, final String command,
		final PlugInFilterRunner pfr)
	{
		final int[] wList = WindowManager.getIDList();
		if (wList.length == 1) {
			IJ.showMessage("Two or more images are necessary to use this tool.");
			return DONE;
		}
		final String[] titles = new String[wList.length];
		for (int i = 0; i < wList.length; i++) {
			final ImagePlus temp = WindowManager.getImage(wList[i]);
			if (temp != null) titles[i] = temp.getTitle();
			else titles[i] = "";
		}
		final GenericDialog gd = new GenericDialog(command + " - setup", IJ
			.getInstance());
		gd.addChoice("Source_image:", titles, titles[0]);
		gd.addChoice("Target_image:", titles, titles[1]);
		gd.setResizable(false);
		final String help =
			"<html><h3>Transfer calibration</h3><p>description</p></html>";
		gd.addHelp(help);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return DONE;
		}
		final int index1 = gd.getNextChoiceIndex();
		final int index2 = gd.getNextChoiceIndex();
		if (index1 == index2) {
			IJ.showMessage("It is not possible to transfer the calibration." + "\n" +
				"You have to choose two different images.");
			return DONE;
		}
		impSource = WindowManager.getImage(wList[index1]);
		impTarget = WindowManager.getImage(wList[index2]);
		return FLAGS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.filter.ExtendedPlugInFilter#setNPasses(int)
	 */
	@Override
	public void setNPasses(final int nPasses) {
		// this method is not used.
	}

	/**
	 * Main method for debugging. For debugging, it is convenient to have a method
	 * that starts ImageJ, loads an image and calls the plugin, e.g. after setting
	 * breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(final String[] args) {
		// start ImageJ
		new ImageJ();

		// open the sample
		final ImagePlus input = IJ.createImage("input", 256, 256, 1, 32);
		IJ.run(input, "Properties...",
			"channels=1 slices=1 frames=1 unit=unit pixel_width=0.001 pixel_height=1.234 voxel_depth=1 origin=127,127");
		final ImagePlus output = IJ.createImage("output", 256, 256, 1, 32);
		input.show();
		output.show();

		// run the plugin
		final Class<?> clazz = CalibrationTransferPlugin.class;
		IJ.runPlugIn(clazz.getName(), "");
		if (output.getCalibration().equals(input.getCalibration())) {
			IJ.showMessage("Test: Ok");
		}
		else {
			IJ.showMessage("Test: Failed");
		}
	}

}
