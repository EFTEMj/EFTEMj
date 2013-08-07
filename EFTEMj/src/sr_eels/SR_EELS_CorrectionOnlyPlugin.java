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
package sr_eels;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.Arrays;

/**
 * This plugin is used to correct SR-EELS data that shows a geometric aberration. This aberration is visible in SR-EELS
 * data recorded with a Zeiss in-column Omega filter. Using a Gatan post-column filter the aberration may be corrected
 * by the filter itself.
 * 
 * The correction consists of two steps. The first is to identify the inclined border of the spectrum.
 * {@link SR_EELS_CorrectionPlugin} performs the first step. The second step is to process the SR-EELS data to correct
 * the aberration. {@link SR_EELS_CorrectionPlugin} will run {@link SR_EELS_CorrectionOnlyPlugin} subsequently, but it
 * is possible to use {@link SR_EELS_CorrectionOnlyPlugin} to run the correction only. It is necessary to run
 * {@link SR_EELS_CorrectionOnlyPlugin} on a composite image that contains the spectrum border as a separate channel.
 * 
 * For faster processing the energy loss direction has to be the y-axis. Images with the energy loss direction at the
 * x-axis will be rotated during processing.
 * 
 * @author Michael Epping <michael.epping@uni-muenster.de>
 * 
 */
public class SR_EELS_CorrectionOnlyPlugin implements ExtendedPlugInFilter {

    /**
     * <code>DOES_32 | NO_CHANGES | FINAL_PROCESSING</code>
     */
    private final int FLAGS = STACK_REQUIRED | DOES_32 | NO_CHANGES | FINAL_PROCESSING;
    /**
     * The plugin will be aborted.
     */
    private final int CANCEL = 0;
    /**
     * The plugin will continue with the next step.
     */
    private final int OK = 1;
    /**
     * The stack created by {@link Border_Detection}. No changes will be done to this {@link ImagePlus}.
     */
    private ImagePlus input;
    /**
     * An {@link ImagePlus} containing the corrected data.
     */
    private ImagePlus result;
    /**
     * A user defined value for the new spectrum width that disables the auto detection if not 0.
     */
    private int optionalSpecWidth = 0;
    /**
     * If the input contains processed borders (linear fit) and unprocessed borders this {@link Boolean} is used to
     * select one.
     */
    private boolean useUnprocessedBorders = false;
    /**
     * Determines if the image has to be rotated before and after processing.
     */
    private boolean rotate;
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
    public int setup(String arg, ImagePlus imp) {
	if (arg.equals("final")) {
	    if (rotate == true) {
		input = null;
		IJ.run(result, "Rotate 90 Degrees Left", "");
	    }
	    result.setCalibration(calibration);
	    result.show();
	    // TODO Show a log dialog and write some log messages.
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
    public void run(ImageProcessor ip) {
	correctData();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.ExtendedPlugInFilter#showDialog(ij.ImagePlus, java.lang.String,
     * ij.plugin.filter.PlugInFilterRunner)
     */
    @Override
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
	if (imp.getStackSize() != 2 & imp.getStackSize() != 3) {
	    IJ.error("Wrong Image", "The stack has not a size of 2 or 3.\n" + "Image 1: The detected borders.\n"
		    + "(Image 2: The unprocessed borders.)\n" + "Image 2(3): The original SR-EELS data.");
	    return DONE;
	}
	input = imp;
	calibration = imp.getCalibration();
	if (showParameterDialog(command) == OK) {
	    if (rotate) {
		ImagePlus temp = input;
		input = new Duplicator().run(input, 1, input.getStackSize(), 1, 1, 1, 1);
		temp.unlock();
		IJ.run(input, "Rotate 90 Degrees Right", "");
	    }
	    return FLAGS;
	} else {
	    return DONE;
	}
    }

    /**
     * A {@link GenericDialog} is displayed to setup the correction.
     * 
     * @param title
     *            Prefix of the window title.
     * @return <code>true</code> for Ok and <code>false</code> for Cancel.
     */
    private int showParameterDialog(String title) {
	GenericDialog gd = new GenericDialog(title + " - set correction parameters");
	String[] items = { "x-axis", "y-axis" };
	// Try to make a good default selection.
	String selectedItem = ((input.getWidth() >= input.getHeight()) ? items[0] : items[1]);
	gd.addChoice("Energy loss on...", items, selectedItem);
	gd.addNumericField("Predefined spectrum width:", optionalSpecWidth, 0, 4, "pixel");
	gd.addMessage("(If you choose 0, the width will be set by the automatically.)");
	if (input.getStackSize() == 3) {
	    gd.addCheckbox("Use unprocessed borders", false);
	}
	gd.showDialog();
	if (gd.wasOKed()) {
	    // for faster processing the energy loss axis has to be the y-axis.
	    switch (gd.getNextChoice()) {
	    case "x-axis":
		rotate = true;
		break;
	    case "y-axis":
		rotate = false;
		break;
	    default:
		break;
	    }
	    optionalSpecWidth = (int) gd.getNextNumber();
	    if (input.getStackSize() == 3) {
		useUnprocessedBorders = gd.getNextBoolean();
	    }
	    return OK;
	}
	return CANCEL;
    }

    /**
     * This is the correction of the SR-EELS data.
     */
    private void correctData() {
	FloatProcessor fp_borders;
	// By default the first image of the stack shows the borders.
	// If the first image of the stack shows a processed border, the second image shows the unprocessed border.
	if (useUnprocessedBorders) {
	    fp_borders = (FloatProcessor) input.getStack().getProcessor(2);
	} else {
	    fp_borders = (FloatProcessor) input.getStack().getProcessor(1);
	}
	FloatProcessor fp_data;
	// You have to check if the stack contains 2 or 3 images to get the input image.
	if (input.getStackSize() == 2) {
	    fp_data = (FloatProcessor) input.getStack().getProcessor(2);
	} else {
	    if (input.getStackSize() == 3) {
		fp_data = (FloatProcessor) input.getStack().getProcessor(3);
	    } else {
		IJ.showMessage("<html><p>The selected image/stack is not suitable for the correction.</p></html>");
		return;
	    }
	}
	// read position of borders from the image
	int[] leftBorderPositions = new int[input.getHeight()];
	int[] rightBorderPositions = new int[input.getHeight()];
	// the default positions of the borders are the left and the right border of the image
	Arrays.fill(leftBorderPositions, 0);
	Arrays.fill(rightBorderPositions, input.getWidth() - 1);
	int minimumWidth = input.getWidth();
	for (int y = 0; y < input.getHeight(); y++) {
	    int count = 2;
	    int x = 0;
	    while (count > 0 & x < input.getWidth()) {
		if (fp_borders.getf(x, y) != 0) {
		    switch (count) {
		    case 2:
			leftBorderPositions[y] = x;
			count--;
			break;
		    case 1:
			rightBorderPositions[y] = x;
			count--;
			break;
		    default:
			break;
		    }
		}
		x++;
	    }
	    // Find the optimal spectrum width but ignore the first and last 1% of the image.
	    if (y > 0.01 * input.getHeight() && y < 0.99 * input.getHeight()
		    && rightBorderPositions[y] - leftBorderPositions[y] < minimumWidth) {
		minimumWidth = rightBorderPositions[y] - leftBorderPositions[y];
	    }
	}
	int newSpecWidth;
	if (optionalSpecWidth == 0) {

	    newSpecWidth = minimumWidth;
	} else {
	    newSpecWidth = optionalSpecWidth;
	}
	FloatProcessor fp_result = new FloatProcessor(newSpecWidth, fp_data.getHeight());
	for (int y = 0; y < fp_data.getHeight(); y++) {
	    int leftIndex = leftBorderPositions[y];
	    int rightIndex = rightBorderPositions[y];
	    int oldWidth = rightIndex - leftIndex;
	    // TODO Add an intensity correction for round borders.
	    double sumNewIntensity = 0;
	    for (int x = 0; x < newSpecWidth; x++) {
		// a linear scaling is used to calculate the new values
		float x0 = 1.0f * oldWidth / newSpecWidth * x;
		float deltaX = (float) (x0 - Math.floor(x0));
		int xLeft = (int) Math.floor(x0);
		int xRight = (int) Math.ceil(x0);
		float valueLeft = fp_data.getf(xLeft + leftIndex, y);
		float valueRight = fp_data.getf(xRight + leftIndex, y);
		float newValue = deltaX * (valueRight - valueLeft) + valueLeft;
		sumNewIntensity += newValue;
		fp_result.setf(x, y, newValue);
	    }
	    // the sum of all intensities in an image line stays the same
	    double sumIntensity = 0;
	    for (int x = leftIndex; x < rightIndex; x++) {
		sumIntensity += fp_data.getf(x, y);
	    }
	    // each pixel is normalised to maintain the sum of all intensities in an image line.
	    for (int x = 0; x < newSpecWidth; x++) {
		fp_result.setf(x, y, (float) (sumIntensity / sumNewIntensity * fp_result.getf(x, y)));
	    }
	}
	result = new ImagePlus("Corrected Spectrum", fp_result);
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
}