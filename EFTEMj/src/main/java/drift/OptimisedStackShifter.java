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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;

import java.awt.Point;

/**
 * This class is used to shift all images of an {@link ImagePlus} that contains a stack.
 * 
 * @author Michael Entrup b. Epping <entrup@arcor.de>
 * 
 */
public class OptimisedStackShifter {

    /**
     * A prefix added to the stack and slice titles.
     */
    private static String prefix = "DK-";

    public static enum MODES {
	CROP, NAN, NAN_EQ, ZERO, ZERO_EQ, SMALL_NEGATIV, SMALL_NEGATIV_EQ
    }

    /**
     * All images of an {@link ImagePlus} stack are shifted by the given shift values.
     * 
     * @param initialStack
     *            {@link ImagePlus} containing a stack to be shifted
     * @param shift
     *            array of {@link Point}s that represent the shift of each image. This array will be modified if
     *            optimise is set <code>true</code>
     * @param mode
     *            The mode used for handling the borders that are created by translating the image
     * @param optimise
     *            true to optimise the given shift values
     * @param createNew
     *            true to create a new {@link ImagePlus} and keep the initial one untouched
     * @return a new {@link ImagePlus} that contains the shifted images
     */
    public static ImagePlus shiftImages(ImagePlus initialStack, Point[] shift, MODES mode, boolean optimise,
	    boolean createNew) {
	ImagePlus correctedStack;
	if (createNew == true) {
	    // you have to delete the roi otherwise only the roi is duplicated
	    initialStack.deleteRoi();
	    correctedStack = new Duplicator().run(initialStack);
	    initialStack.restoreRoi();
	} else {
	    correctedStack = initialStack;
	}
	correctedStack.setTitle(prefix.concat(initialStack.getTitle()));
	if (optimise == true) {
	    shift = optimizedImageShift(shift);
	}
	for (int i = 0; i < shift.length; i++) {
	    correctedStack.getStack().getProcessor(i + 1).translate(shift[i].x, shift[i].y);
	    correctedStack.getStack().setSliceLabel(prefix.concat(correctedStack.getStack().getSliceLabel(i + 1)),
		    i + 1);
	}
	processBorder(correctedStack, shift, mode);
	return correctedStack;
    }

    /**
     * All images of an {@link ImagePlus} stack are shifted by the given shift values. This method should be used if
     * drift values instead of shift values are committed.
     * 
     * @param initialStack
     *            {@link ImagePlus} containing a stack to be shifted
     * @param shift
     *            array of {@link Point}s that represent the shift of each image. This array will be modified if
     *            optimise or invert are set <code>true</code>
     * @param mode
     *            The mode used for handling the borders that are created by translating the image
     * @param invert
     *            true to invert all shift values
     * @param optimise
     *            true to optimise the given shift values
     * @param createNew
     *            true to create a new {@link ImagePlus} and keep the initial one untouched
     * @return a new {@link ImagePlus} that contains the shifted images
     */
    public static ImagePlus shiftImages(ImagePlus initialStack, Point[] shift, MODES mode, boolean invert,
	    boolean optimise, boolean createNew) {
	if (invert == true) {
	    for (int i = 0; i < shift.length; i++) {
		shift[i] = new Point(-shift[i].x, -shift[i].y);
	    }
	}
	return shiftImages(initialStack, shift, mode, optimise, createNew);
    }

    /**
     * The centroid of the shift values is calculated. The centroid is used to reduce to maximum drift to one direction.
     * For example (0/0), (10/10) and (20/20) will result in (-10/-10), (0/0) and (10/10).
     */
    private static Point[] optimizedImageShift(Point[] shift) {
	IJ.showStatus("Using optimized image sift.");
	int minX = 0;
	int maxX = 0;
	int minY = 0;
	int maxY = 0;
	for (int i = 0; i < shift.length; i++) {
	    if (shift[i].x < minX) {
		minX = shift[i].x;
	    } else {
		if (shift[i].x > maxX) {
		    maxX = shift[i].x;
		}
	    }
	    if (shift[i].y < minY) {
		minY = shift[i].y;
	    } else {
		if (shift[i].y > maxY) {
		    maxY = shift[i].y;
		}
	    }
	}
	int optimalX = Math.round((maxX - minX) / 2) + minX;
	int optimalY = Math.round((maxY - minY) / 2) + minY;
	for (int i = 0; i < shift.length; i++) {
	    shift[i] = new Point(shift[i].x - optimalX, shift[i].y - optimalY);
	}
	return shift;
    }

    /**
     * This method will set the same border to all slices of a stack. Additionally it is possible to crop the stack.
     * 
     * @param stack
     *            An {@link ImagePlus} containing an {@link ImageStack} to work with
     * @param shift
     *            The border of each image that was created by translating it
     * @param mode
     *            This defines the method that is used
     */
    private static void processBorder(ImagePlus stack, Point[] shift, MODES mode) {
	/*
	 * To crop the images or to set the same border for all slices one has to find the largest border for each side.
	 */
	int lBorder = 0;
	int rBorder = 0;
	int tBorder = 0;
	int bBorder = 0;
	for (int i = 0; i < shift.length; i++) {
	    lBorder = Math.max(lBorder, shift[i].x);
	    rBorder = Math.min(rBorder, shift[i].x);
	    tBorder = Math.max(tBorder, shift[i].y);
	    bBorder = Math.min(bBorder, shift[i].y);
	}
	switch (mode) {
	case NAN:
	case ZERO:
	case SMALL_NEGATIV:
	    setBorder(stack, shift, mode);
	    break;
	case CROP:
	    stack.setStack(stack.getStack().crop(lBorder, tBorder, 0, stack.getWidth() - lBorder + rBorder,
		    stack.getHeight() - tBorder + bBorder, stack.getStackSize()));
	    break;
	case NAN_EQ:
	case ZERO_EQ:
	case SMALL_NEGATIV_EQ:
	    Point[] border = new Point[shift.length];
	    Point borderValues = new Point(lBorder, tBorder);
	    for (int i = 0; i < shift.length; i++) {
		border[i] = borderValues;
	    }
	    setBorder(stack, border, mode);
	    borderValues = new Point(rBorder, bBorder);
	    for (int i = 0; i < shift.length; i++) {
		border[i] = borderValues;
	    }
	    setBorder(stack, border, mode);
	    break;
	default:
	    break;
	}
    }

    /**
     * This method sets the pixel values of each border, created by translating the image, to a user defined value.
     * 
     * @param stack
     *            An {@link ImagePlus} containing an {@link ImageStack} to work with
     * @param border
     *            The border of each image that was created by translating it
     * @param mode
     *            This defines the pixel value used to fill the border
     */
    private static void setBorder(ImagePlus stack, Point[] border, MODES mode) {
	Float value;
	switch (mode) {
	case NAN:
	case NAN_EQ:
	    value = Float.NaN;
	    break;
	case SMALL_NEGATIV:
	case SMALL_NEGATIV_EQ:
	    value = -Float.MIN_VALUE;
	    break;
	case ZERO:
	case ZERO_EQ:
	default:
	    value = 0f;
	    break;
	}
	for (int i = 0; i < border.length; i++) {
	    int startX = 0;
	    int startY = 0;
	    int stopX = border[i].x;
	    int stopY = border[i].y;
	    if (border[i].x < 0) {
		startX = stack.getWidth() - 1 + border[i].x;
		stopX = stack.getWidth() - 1;
	    }
	    if (border[i].y < 0) {
		startY = stack.getHeight() - 1 + border[i].y;
		stopY = stack.getHeight() - 1;
	    }
	    for (int y = 0; y < stack.getHeight(); y++) {
		for (int x = startX; x <= stopX; x++) {
		    stack.getStack().getProcessor(i + 1).setf(x, y, value);
		}
	    }
	    for (int y = startY; y <= stopY; y++) {
		for (int x = 0; x < stack.getWidth(); x++) {
		    stack.getStack().getProcessor(i + 1).setf(x, y, value);
		}
	    }
	}

    }
}
