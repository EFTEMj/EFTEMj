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
import ij.plugin.Duplicator;

import java.awt.Point;

/**
 * This class is used to shift all images of an {@link ImagePlus} that contains a stack.
 * 
 * @author Michael Epping <michael.epping@uni-muenster.de>
 * 
 */
public class OptimisedStackShifter {

    /**
     * A prefix added to the stack and slice titles.
     */
    private static String prefix = "DK-";

    /**
     * All images of an {@link ImagePlus} stack are shifted by the given shift values.
     * 
     * @param initialStack
     *            {@link ImagePlus} containing a stack to be shifted
     * @param shift
     *            array of {@link Point}s that represent the shift of each image. This array will be modified if
     *            optimise is set <code>true</code>
     * @param optimise
     *            true to optimise the given shift values
     * @param createNew
     *            true to create a new {@link ImagePlus} and keep the initial one untouched
     * @return a new {@link ImagePlus} that contains the shifted images
     */
    public static ImagePlus shiftImages(ImagePlus initialStack, Point[] shift, boolean optimise, boolean createNew) {
	ImagePlus correctedStack;
	if (createNew == true) {
	    initialStack.deleteRoi();
	    correctedStack = new Duplicator().run(initialStack);
	    initialStack.restoreRoi();
	    // TODO Extend Duplicator to perform this task.
	} else {
	    correctedStack = initialStack;
	}
	correctedStack.setTitle(prefix.concat(correctedStack.getTitle()));
	if (optimise == true) {
	    shift = optimizedImageShift(shift);
	}
	for (int i = 0; i < shift.length; i++) {
	    correctedStack.getStack().getProcessor(i + 1).translate(shift[i].x, shift[i].y);
	    correctedStack.getStack().setSliceLabel(prefix.concat(correctedStack.getStack().getSliceLabel(i + 1)),
		    i + 1);
	}
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
     * @param invert
     *            true to invert all shift values
     * @param optimise
     *            true to optimise the given shift values
     * @param createNew
     *            true to create a new {@link ImagePlus} and keep the initial one untouched
     * @return a new {@link ImagePlus} that contains the shifted images
     */
    public static ImagePlus shiftImages(ImagePlus initialStack, Point[] shift, boolean invert, boolean optimise,
	    boolean createNew) {
	if (invert == true) {
	    for (int i = 0; i < shift.length; i++) {
		shift[i] = new Point(-shift[i].x, -shift[i].y);
	    }
	}
	return shiftImages(initialStack, shift, optimise, createNew);
    }

    /**
     * Before translating the images the centroid of the shift values is calculated. The centroid is used to reduce to
     * maximum drift to one direction.
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
}
