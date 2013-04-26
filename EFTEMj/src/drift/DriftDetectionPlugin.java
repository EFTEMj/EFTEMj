/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 * 
 * Copyright (c) 2013, Michael Epping
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

import javax.naming.InitialContext;

import gui.ExtendedWaitForUserDialog;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.gui.YesNoCancelDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * This plugin can measure the drift between all images of a stack. The output is the drift of all other images compared
 * to the selected reference image. Two modes are available - an automatic and a manual one.
 * <p />
 * The automatic mode calculates the normalised cross-correlation coefficients for all possible shifts (can be limited
 * for faster processing) to determine the drift.
 * <p />
 * The manual method uses overlay techniques to help the user determining the drift manually.
 * 
 * @author Michael Epping <michael.epping@uni-muenster.de>
 * 
 */
public class DriftDetectionPlugin implements ExtendedPlugInFilter {

    /**
     * The automatic mode will be used.
     */
    private final int AUTOMATIC = 2;
    /**
     * The manual mode will be used.
     */
    private final int MANUAL = 4;
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
     * This is the {@link ImagePlus} that has been selected when starting the plugin. No changes will be done to this
     * {@link ImagePlus}.
     */
    private ImagePlus initialImp;
    /**
     * An {@link ImagePlus} that contains an {@link ImageStack}. If the initial {@link ImagePlus} is a stack then this
     * is the same {@link ImagePlus}. A new stack will be created if the {@link InitialContext} {@link ImagePlus} is no
     * stack.
     */
    private ImagePlus stack;
    /**
     * This is the index (one-based) of the slice that is used as reference image.
     */
    private int referenceIndex;
    /**
     * The maximum image shift in x-direction that will be tested by the automatic drift detection
     */
    private int deltaX;
    /**
     * The maximum image shift in y-direction that will be tested by the automatic drift detection
     */
    private int deltaY;
    /**
     * A copy of the {@link Roi}s bounding {@link Rectangle}, that has been placed during the preparation of the
     * automatic mode.
     */
    private Rectangle roi;

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
     */
    @Override
    public int setup(String arg, ImagePlus imp) {
	if (arg == "final") {
	    // TODO implement final processing
	    return NO_CHANGES | DONE;
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
    public void run(ImageProcessor ip) {
	NormCrossCorrelation[] ccArray = prepareCC();
	Point[] driftArray = new Point[ccArray.length];
	for (int i = 0; i < ccArray.length; i++) {
	    if (ccArray[i] != null) {
		ccArray[i].startCalculation();
		driftArray[i] = NormCrossCorrelation.findMax(ccArray[i].getCrossCorrelationMap());
	    } else {
		driftArray[i] = new Point(0, 0);
	    }
	}
	// TODO Add the possibility to deactivate the shift optimisation.
	ImagePlus correctedStack = OptimisedStackShifter.shiftImages(stack, driftArray, true);
	correctedStack.show();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.ExtendedPlugInFilter#showDialog(ij.ImagePlus, java.lang.String,
     * ij.plugin.filter.PlugInFilterRunner)
     */
    @Override
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
	// Check if imp is a stack.
	initialImp = imp;
	if (imp.getStackSize() <= 1) {
	    // TODO implement stack creation
	    canceled();
	    return NO_CHANGES | DONE;
	} else {
	    stack = imp;
	}
	// Select automatic or manual mode.
	switch (showModeDialog(command)) {
	case AUTOMATIC:
	    IJ.showStatus("Automatic drift detection has been selected.");
	    // Show the ROI dialog again until the user cancels it or places a ROI.
	    do {
		referenceIndex = showRoiDialog(command);
	    } while (referenceIndex != CANCEL & imp.getRoi() == null);
	    if (referenceIndex == CANCEL) {
		canceled();
		return NO_CHANGES | DONE;
	    }
	    roi = (Rectangle) imp.getRoi().getBounds().clone();
	    if (showParameterDialog(command) == CANCEL) {
		canceled();
		return NO_CHANGES | DONE;
	    }
	    break;
	case MANUAL:
	    IJ.showStatus("Manual drift detection has been selected.");
	    // TODO copy the input stack
	    // TODO implement manual drift detection
	    break;
	default:
	    canceled();
	    return NO_CHANGES | DONE;
	}
	return FLAGS;
    }

    /**
     * For each image except the reference image an instance of {@link NormCrossCorrelation} is created. The input
     * images are copied and cropped before committed to {@link NormCrossCorrelation}.
     */
    private NormCrossCorrelation[] prepareCC() {
	NormCrossCorrelation[] ccArray = new NormCrossCorrelation[stack.getStackSize()];
	roi.x = roi.x - deltaX;
	roi.y = roi.y - deltaY;
	roi.width = roi.width + 2 * deltaX;
	roi.height = roi.height + 2 * deltaY;
	FloatProcessor reference = new FloatProcessor(stack.getWidth(), stack.getHeight());
	reference.copyBits(stack.getStack().getProcessor(referenceIndex), 0, 0, Blitter.COPY);
	reference.setRoi(roi);
	reference = (FloatProcessor) reference.crop();
	for (int i = 0; i < stack.getStackSize(); i++) {
	    if (i != referenceIndex - 1) {
		FloatProcessor fp = new FloatProcessor(stack.getWidth(), stack.getHeight());
		fp.copyBits(stack.getStack().getProcessor(i + 1), 0, 0, Blitter.COPY);
		fp.setRoi(roi);
		fp = (FloatProcessor) fp.crop();
		ccArray[i] = new NormCrossCorrelation(reference, fp, deltaX, deltaY);
	    } else {
		ccArray[i] = null;
	    }
	}
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
    public void setNPasses(int nPasses) {
	// This method is not used.
    }

    /**
     * The user is asked to select the drift detection mode. A {@link YesNoCancelDialog} is used for this purpose. If
     * Yes is selected the automatic mode is used. No means the manual mode is used.
     * 
     * @param title
     * @return the selected mode for drift detection
     */
    private int showModeDialog(String title) {
	YesNoCancelDialog dialog = new YesNoCancelDialog(IJ.getInstance(), title + " - detection mode",
		"Use automatic drift detection?\nTo start manual mode select no.");
	if (dialog.yesPressed()) {
	    return AUTOMATIC;
	} else if (dialog.cancelPressed()) {
	    return CANCEL;
	}
	return MANUAL;
    }

    /**
     * A dialog that requests the user to set a ROI. This ROI is used to create a reference image for the
     * cross-correlation.
     * 
     * @param title
     * @return the selected slice of the stack or CANCEL
     */
    private int showRoiDialog(String title) {
	IJ.setTool(Toolbar.RECTANGLE);
	ExtendedWaitForUserDialog dialog = new ExtendedWaitForUserDialog(
		title + " - set ROI",
		"Set a ROI to define the reference image.\nThe ROI should contain a structure visable at all images of the stack.",
		null);
	dialog.show();
	if (!dialog.escPressed())
	    return initialImp.getSlice();
	return CANCEL;
    }

    /**
     * This dialog is used to setup the parameter for the automatic drift detection.
     * 
     * @param title
     * @return OK or CANCEL
     */
    private int showParameterDialog(String title) {
	GenericDialog gd = new GenericDialog(title + " - set parameters", IJ.getInstance());
	// The ROI defines the maximum of delta
	Point maxDelta = getRoiBorderDist();
	int maxDeltaX = maxDelta.x;
	int maxDeltaY = maxDelta.y;
	// maxValue is a multiple of 10, except when maxDelta is smaller than 10.
	gd.addSlider("delta x", 0, Math.min(Math.max(maxDeltaX / 10 * 10, 10), maxDeltaX),
		Math.min(Math.max(maxDeltaX / 10 * 10, 10), maxDeltaX) / 2);
	gd.addSlider("delta y", 0, Math.min(Math.max(maxDeltaY / 10 * 10, 10), maxDeltaY),
		Math.min(Math.max(maxDeltaY / 10 * 10, 10), maxDeltaY) / 2);
	String[] stackLabels = new String[stack.getStackSize()];
	for (int i = 0; i < stack.getStackSize(); i++) {
	    stackLabels[i] = String.format("%s/%s (%s)", i + 1, stack.getStackSize(), stack.getStack()
		    .getShortSliceLabel(i + 1));
	}
	gd.addChoice("reference image", stackLabels, stackLabels[referenceIndex - 1]);
	gd.showDialog();
	if (gd.wasCanceled()) {
	    return CANCEL;
	}
	Scrollbar slider = (Scrollbar) gd.getSliders().get(0);
	deltaX = slider.getValue();
	slider = (Scrollbar) gd.getSliders().get(1);
	deltaY = slider.getValue();
	// Choice starts with 0; stack starts with 1
	referenceIndex = gd.getNextChoiceIndex() + 1;
	return OK;

    }

    /**
     * @return the distance between the {@link Roi} and the nearest image border
     */
    private Point getRoiBorderDist() {
	Point dist = new Point(stack.getWidth(), stack.getHeight());
	Rectangle roi = stack.getRoi().getBounds();
	if (roi.x < dist.x)
	    dist.x = roi.x;
	if (roi.y < dist.y)
	    dist.y = roi.y;
	int spacingRight = stack.getWidth() - roi.x - roi.width;
	if (spacingRight < dist.x)
	    dist.x = spacingRight;
	int spacingBot = stack.getHeight() - roi.y - roi.height;
	if (spacingBot < dist.y)
	    dist.y = spacingBot;
	return dist;
    }
}