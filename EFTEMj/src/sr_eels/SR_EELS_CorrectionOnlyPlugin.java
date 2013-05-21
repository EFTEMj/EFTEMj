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

import java.awt.Rectangle;
import java.util.Arrays;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * @author Michael Epping <michael.epping@uni-muenster.de>
 * 
 */
public class SR_EELS_CorrectionOnlyPlugin implements ExtendedPlugInFilter {

    /**
     * The stack created by {@link Border_Detection}. No changes will be done to this {@link ImagePlus}.
     */
    private ImagePlus ipInput;
    /**
     * The width of the spectrum image.
     */
    private int width;
    /**
     * The height of the spectrum image.
     */
    private int height;
    /**
     * A user defined value for the new spectrum width that disables the auto detection if not 0.
     */
    private int optionalSpecWidth = 0;

    /**
     * If the input contains processed borders (linear fit) and unprocessed borders this {@link Boolean} is used to
     * select one.
     */
    private boolean useUnprocessedBorders = false;

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
     */
    @Override
    public int setup(String arg, ImagePlus imp) {
	return DOES_32;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
     */
    @Override
    public void run(ImageProcessor ip) {
	width = ipInput.getWidth();
	height = ipInput.getHeight();
	// By default the first image of the stack shows the borders
	FloatProcessor input_borders = (FloatProcessor) ipInput.getStack().getProcessor(1);
	// If the first image of the stack shows a processed border, the second
	// image shows the unprocessed border
	if (useUnprocessedBorders) {
	    input_borders = (FloatProcessor) ipInput.getStack().getProcessor(2);
	}
	FloatProcessor input = null;
	// You have to check if the stack contains 2 or 3 images to get the
	// input image
	if (ipInput.getStackSize() == 2) {
	    input = (FloatProcessor) ipInput.getStack().getProcessor(2);
	} else {
	    if (ipInput.getStackSize() == 3) {
		input = (FloatProcessor) ipInput.getStack().getProcessor(3);
	    } else {
		IJ.showMessage("<html><p>the selected image/stack is not suitable for the correction.</p></html>");
		return;
	    }
	}
	// If a ROI is placed the image i cropped to the selection
	if (ipInput.getRoi() != null) {
	    Rectangle roi = ipInput.getRoi().getBounds();
	    input_borders.setRoi(roi.x, roi.y, roi.width, roi.height);
	    input_borders = (FloatProcessor) input_borders.crop();
	    input.setRoi(roi.x, roi.y, roi.width, roi.height);
	    input = (FloatProcessor) input.crop();
	    width = input.getWidth();
	    height = input.getHeight();
	}
	// read position of borders from the image
	int[] leftBorderPosition = new int[height];
	int[] rightBorderPosition = new int[height];
	readBorderPositions(input_borders, leftBorderPosition, rightBorderPosition);
	// find the optimal spectrum width
	int specWidth;
	if (optionalSpecWidth == 0) {
	    int minimumWidth = findMinimumWidth(leftBorderPosition, rightBorderPosition);
	    specWidth = minimumWidth;
	} else {
	    specWidth = optionalSpecWidth;
	}
	FloatProcessor result = correctSpec(specWidth, input, leftBorderPosition, rightBorderPosition);
	ImagePlus ipResult = new ImagePlus("Corrected Spectrum", result);
	ipResult.show();
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
		    + "(Image 2: The unprocessed borders.\n" + "Image 2(3): The original spectrum.");
	    return DONE;
	}
	ipInput = imp;
	GenericDialog gd = new GenericDialog("Spectrum correction");
	gd.addNumericField("Predefined spectrum width:", optionalSpecWidth, 0, 4, "pixel");
	gd.addMessage("(If you choose 0, the width will be set by the programm.)");
	if (ipInput.getStackSize() == 3) {
	    gd.addCheckbox("Use unprocessed borders", false);
	}
	gd.showDialog();
	if (gd.wasCanceled())
	    return DONE;
	optionalSpecWidth = (int) gd.getNextNumber();
	if (ipInput.getStackSize() == 3) {
	    useUnprocessedBorders = gd.getNextBoolean();
	}
	return STACK_REQUIRED | DOES_32 | NO_CHANGES;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.ExtendedPlugInFilter#setNPasses(int)
     */
    @Override
    public void setNPasses(int nPasses) {
	// TODO Auto-generated method stub

    }

    /**
     * The correction creates a new {@link FloatProcessor} with the given width. Each line of the original spectrum is
     * shrank in width to fit the new value.
     * 
     * @param width
     * @param input
     * @param left
     * @param right
     * @return
     */
    private static FloatProcessor correctSpec(int width, FloatProcessor input, int[] left, int[] right) {
	FloatProcessor result = new FloatProcessor(width, input.getHeight());
	for (int y = 0; y < input.getHeight(); y++) {
	    int leftIndex = left[y];
	    int rightIndex = right[y];
	    int oldWidth = rightIndex - leftIndex;
	    double sumNewIntensity = 0;
	    for (int x = 0; x < width; x++) {
		// a linear scaling is used to calculate the new values
		float x0 = 1.0f * oldWidth / width * x;
		float deltaX = (float) (x0 - Math.floor(x0));
		int xLeft = (int) Math.floor(x0);
		int xRight = (int) Math.ceil(x0);
		float valueLeft = input.getf(xLeft + leftIndex, y);
		float valueRight = input.getf(xRight + leftIndex, y);
		float newValue = deltaX * (valueRight - valueLeft) + valueLeft;
		sumNewIntensity += newValue;
		result.setf(x, y, newValue);
	    }
	    // the sum of all intensities in an image line stays the same
	    double sumIntensity = 0;
	    for (int x = leftIndex; x < rightIndex; x++) {
		sumIntensity += input.getf(x, y);
	    }
	    // each pixel is normalized to maintain the sum of all intensities
	    // in an image line.
	    for (int x = 0; x < width; x++) {
		result.setf(x, y, (float) (sumIntensity / sumNewIntensity * result.getf(x, y)));
	    }
	}
	return result;
    }

    /**
     * Finds the minimum distance between the left border and the right border.
     * 
     * @param leftBorderPosition
     * @param rightBorderPosition
     * @return
     */
    private int findMinimumWidth(int[] leftBorderPosition, int[] rightBorderPosition) {
	int min = width;
	for (int y = 0; y < height; y++) {
	    if (rightBorderPosition[y] - leftBorderPosition[y] < min) {
		min = rightBorderPosition[y] - leftBorderPosition[y];
	    }
	}
	return min;
    }

    /**
     * Fills the 2 arrays with the position of the left and the right border.
     * 
     * @param input_borders
     *            A black (0) image with 2 values >0 in each line.
     * @param leftBorderPosition
     * @param rightBorderPosition
     */
    private void readBorderPositions(FloatProcessor input_borders, int[] leftBorderPosition, int[] rightBorderPosition) {
	// the default positions of the borders are the left and the right
	// border of the image
	Arrays.fill(leftBorderPosition, 0);
	Arrays.fill(rightBorderPosition, width - 1);
	int count;
	int x;
	for (int y = 0; y < height; y++) {
	    count = 2;
	    x = 0;
	    while (count > 0 & x < width) {
		if (input_borders.getf(x, y) != 0) {
		    switch (count) {
		    case 2:
			leftBorderPosition[y] = x;
			count--;
			break;
		    case 1:
			rightBorderPosition[y] = x;
			count--;
			break;
		    default:
			break;
		    }
		}
		x++;
	    }
	}
    }
}