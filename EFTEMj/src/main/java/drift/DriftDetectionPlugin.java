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

package drift;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.util.Arrays;

import javax.naming.InitialContext;

import eftemj.EFTEMj;
import gui.ExtendedWaitForUserDialog;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import tools.ExtendedImagesToStack;

/**
 * This plugin can measure the drift between all images of a stack. The output
 * is the drift of all other images compared to the selected reference image.
 * <p />
 * The automatic drift detection calculates the normalised cross-correlation
 * coefficients for all possible shifts (can be limited for faster processing)
 * to determine the drift.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 */
public class DriftDetectionPlugin implements ExtendedPlugInFilter {

	/**
	 * The plugin will be aborted.
	 */
	private final int CANCEL = 0;
	/**
	 * The plugin will continue with the next step.
	 */
	private final int OK = 1;
	/**
	 * <code>DOES_32 | NO_UNDO | FINAL_PROCESSING</code>
	 */
	private final int FLAGS = DOES_32 | NO_UNDO | FINAL_PROCESSING;
	/**
	 * An {@link ImagePlus} that contains an {@link ImageStack}. If the initial
	 * {@link ImagePlus} is a stack then this is the same {@link ImagePlus}. A new
	 * stack will be created if the {@link InitialContext} {@link ImagePlus} is no
	 * stack.
	 */
	private ImagePlus stack;
	/**
	 * This is the index (one-based) of the slice that is used as reference image.
	 */
	private int referenceIndex = OK;
	/**
	 * Set this to <code>false</code> for only detecting the drift and not
	 * performing the image shift.
	 */
	private boolean performShift;
	/**
	 * Defines if the shift values get optimised.
	 */
	private boolean optimiseShift;
	/**
	 * Defines if a new {@link ImagePlus} is created that contains the shifted
	 * images.
	 */
	private boolean createNew;
	/**
	 * The maximum image shift in x-direction that will be tested by the automatic
	 * drift detection
	 */
	private int deltaX;
	/**
	 * The maximum image shift in y-direction that will be tested by the automatic
	 * drift detection
	 */
	private int deltaY;
	/**
	 * A copy of the {@link Roi}s bounding {@link Rectangle}, that has been placed
	 * during the preparation of the automatic drift detection.
	 */
	private Rectangle roi;
	/**
	 * An array to store the detected drift.
	 */
	private Point[] driftArray;
	/**
	 * An array to store the processed shift.
	 */
	private Point[] shiftArray;
	/**
	 * The methods that can be used to fill the border.
	 */
	private OptimisedStackShifter.MODES mode;
	/**
	 * the {@link Calibration} of the input stack.
	 */
	private Calibration calibration;

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	@Override
	public int setup(final String arg, final ImagePlus imp) {
		if (arg == "final") {
			final ResultsTable result = new ResultsTable();
			// only integer values are used
			result.setPrecision(0);
			for (int i = 0; i < driftArray.length; i++) {
				result.incrementCounter();
				result.addLabel(stack.getStack().getShortSliceLabel(i + 1));
				result.addValue("drift.x", driftArray[i].x);
				result.addValue("drift.y", driftArray[i].y);
				if (performShift == true) {
					result.addValue("shift.x", shiftArray[i].x);
					result.addValue("shift.y", shiftArray[i].y);
				}
			}
			result.show("Drift of " + stack.getShortTitle());
			if (createNew == true) {
				return NO_CHANGES | DONE;
			}
			return DONE;
		}
		// No setup is done here. See showDialog() for the setup procedure.
		return FLAGS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	@Override
	public void run(final ImageProcessor ip) {
		final NormCrossCorrelation[] ccArray = prepareCC();
		driftArray = new Point[ccArray.length];
		for (int i = 0; i < ccArray.length; i++) {
			if (ccArray[i] != null) {
				ccArray[i].startCalculation();
				driftArray[i] = NormCrossCorrelation.findMax(ccArray[i]
					.getCrossCorrelationMap());
			}
			else {
				driftArray[i] = new Point(0, 0);
			}
		}
		// OptimisedStackShifter will modify the Array
		shiftArray = Arrays.copyOf(driftArray, driftArray.length);
		if (performShift == true) {
			final ImagePlus correctedStack = OptimisedStackShifter.shiftImages(stack,
				shiftArray, mode, true, optimiseShift, createNew);
			if (createNew == true) {
				correctedStack.show();
				correctedStack.setCalibration(calibration);
			}
			else {
				correctedStack.changes = true;
				correctedStack.updateAndRepaintWindow();
			}
		}
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
		// Check if imp is a stack.
		if (imp.getStackSize() <= 1) {
			// ExtendedStackToImage is a plugin
			new ExtendedImagesToStack().convertImagesToStack();
			if (IJ.getImage().getStackSize() <= 1) {
				canceled();
				return NO_CHANGES | DONE;
			}
			stack = IJ.getImage();
		}
		else {
			stack = imp;
		}
		calibration = imp.getCalibration();
		/*
		 * command is an empty String if the plugin is called from within the main method of this class (testing). In
		 * this case we add the name of this class as title.
		 */
		final String title = (command != "" ? command : "Test " + EFTEMj
			.getNameWithoutPackage(this));
		if (imp.getRoi() == null) {
			/*
			 * Show the ROI dialog again until the user cancels it or places a ROI.
			 */
			do {
				referenceIndex = showRoiDialog(title);
			}
			while (referenceIndex != CANCEL & imp.getRoi() == null);
		}
		if (referenceIndex == CANCEL) {
			canceled();
			return NO_CHANGES | DONE;
		}
		roi = (Rectangle) imp.getRoi().getBounds().clone();
		if (showParameterDialog(title) == CANCEL) {
			canceled();
			return NO_CHANGES | DONE;
		}
		if (createNew == true) {
			return FLAGS | NO_CHANGES;
		}
		return FLAGS;
	}

	/**
	 * For each image except the reference image an instance of
	 * {@link NormCrossCorrelation} is created. The input images are copied and
	 * cropped before committed to {@link NormCrossCorrelation}.
	 */
	private NormCrossCorrelation[] prepareCC() {
		final NormCrossCorrelation[] ccArray = new NormCrossCorrelation[stack
			.getStackSize()];
		roi.x = roi.x - deltaX;
		roi.y = roi.y - deltaY;
		roi.width = roi.width + 2 * deltaX;
		roi.height = roi.height + 2 * deltaY;
		FloatProcessor reference = new FloatProcessor(stack.getWidth(), stack
			.getHeight());
		reference.copyBits(stack.getStack().getProcessor(referenceIndex), 0, 0,
			Blitter.COPY);
		reference.setRoi(roi);
		reference = (FloatProcessor) reference.crop();
		for (int i = 0; i < stack.getStackSize(); i++) {
			if (i != referenceIndex - 1) {
				FloatProcessor fp = new FloatProcessor(stack.getWidth(), stack
					.getHeight());
				fp.copyBits(stack.getStack().getProcessor(i + 1), 0, 0, Blitter.COPY);
				fp.setRoi(roi);
				fp = (FloatProcessor) fp.crop();
				ccArray[i] = new NormCrossCorrelation(reference, fp, deltaX, deltaY);
			}
			else {
				ccArray[i] = null;
			}
		}
		// this is the count of subtasks
		NormCrossCorrelation.setProgressSteps((ccArray.length - 1) * (deltaY * 2 +
			1));
		return ccArray;
	}

	/**
	 * Cancel the plugin and show a status message.
	 */
	private void canceled() {
		IJ.showStatus("Drift detection has been canceled.");
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

	/**
	 * A dialog that requests the user to set a ROI. This ROI is used to create a
	 * reference image for the cross-correlation.
	 *
	 * @param title
	 * @return the selected slice of the stack or CANCEL
	 */
	private int showRoiDialog(final String title) {
		IJ.setTool(Toolbar.RECTANGLE);
		final ExtendedWaitForUserDialog dialog = new ExtendedWaitForUserDialog(
			title + " - set ROI",
			"Set a ROI to define the reference image.\nThe ROI should contain a structure visable at all images of the stack.",
			null);
		dialog.show();
		if (dialog.escPressed() == false) return stack.getSlice();
		return CANCEL;
	}

	/**
	 * This dialog is used to setup the parameter for the automatic drift
	 * detection.
	 *
	 * @param title
	 * @return OK or CANCEL
	 */
	private int showParameterDialog(final String title) {
		final GenericDialog gd = new GenericDialog(title + " - set parameters", IJ
			.getInstance());
		// The ROI defines the maximum of delta
		final Point maxDelta = getRoiBorderDist();
		final int maxDeltaX = maxDelta.x;
		final int maxDeltaY = maxDelta.y;
		// maxValue is a multiple of 10, except when maxDelta is smaller than 10.
		gd.addSlider("Set_delta_x", 0, Math.min(Math.max(maxDeltaX / 10 * 10, 10),
			maxDeltaX), Math.min(Math.max(maxDeltaX / 10 * 10, 10), maxDeltaX) / 2);
		gd.addSlider("Set_delta_y", 0, Math.min(Math.max(maxDeltaY / 10 * 10, 10),
			maxDeltaY), Math.min(Math.max(maxDeltaY / 10 * 10, 10), maxDeltaY) / 2);
		final String[] stackLabels = new String[stack.getStackSize()];
		for (int i = 0; i < stack.getStackSize(); i++) {
			String label = stack.getStack().getShortSliceLabel(i + 1);
			if (label == null) {
				label = String.format("slice %d", i + 1);
			}
			stackLabels[i] = String.format("%s/%s (%s)", i + 1, stack.getStackSize(),
				label);
		}
		gd.addChoice("Select_reference slice", stackLabels,
			stackLabels[referenceIndex - 1]);
		// begin - CheckboxGroup
		final String[] labels = { "Perform_image_shift", "Optimise_image_shift",
			"Create_new_image" };
		final boolean[] defaults = { true, true, true };
		final String[] headings = { "Shift images" };
		gd.addCheckboxGroup(3, 1, labels, defaults, headings);
		// end - checkboxGroup
		final String[] items = new String[OptimisedStackShifter.MODES
			.values().length];
		for (int i = 0; i < items.length; i++) {
			items[i] = OptimisedStackShifter.MODES.values()[i].toString();
		}
		gd.addChoice("Border_mode:", items, items[0]);
		gd.showDialog();
		if (gd.wasCanceled() == true) {
			return CANCEL;
		}
		Scrollbar slider = (Scrollbar) gd.getSliders().get(0);
		deltaX = slider.getValue();
		slider = (Scrollbar) gd.getSliders().get(1);
		deltaY = slider.getValue();
		// Choice starts with 0; stack starts with 1
		referenceIndex = gd.getNextChoiceIndex() + 1;
		// begin - CheckboxGroup
		performShift = gd.getNextBoolean();
		optimiseShift = gd.getNextBoolean();
		createNew = gd.getNextBoolean();
		// end - checkboxGroup
		mode = OptimisedStackShifter.MODES.values()[gd.getNextChoiceIndex()];
		return OK;
	}

	/**
	 * @return the distance between the {@link Roi} and the nearest image border
	 */
	private Point getRoiBorderDist() {
		final Point dist = new Point(stack.getWidth(), stack.getHeight());
		if (stack.getRoi().getBounds().x < dist.x) dist.x = stack.getRoi()
			.getBounds().x;
		if (stack.getRoi().getBounds().y < dist.y) dist.y = stack.getRoi()
			.getBounds().y;
		final int spacingRight = stack.getWidth() - stack.getRoi().getBounds().x -
			stack.getRoi().getBounds().width;
		if (spacingRight < dist.x) dist.x = spacingRight;
		final int spacingBot = stack.getHeight() - stack.getRoi().getBounds().y -
			stack.getRoi().getBounds().height;
		if (spacingBot < dist.y) dist.y = spacingBot;
		return dist;
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

		// open the sample stack
		final ImagePlus image = IJ.openImage(
			"http://EFTEMj.entrup.com.de/Drift-Stack_max64px.tif");
		image.setRoi(64, 64, 128, 128);
		image.getStack().setSliceLabel(null, 1);
		image.show();

		// run the plugin
		final Class<?> clazz = DriftDetectionPlugin.class;
		IJ.runPlugIn(clazz.getName(), "");
	}
}
