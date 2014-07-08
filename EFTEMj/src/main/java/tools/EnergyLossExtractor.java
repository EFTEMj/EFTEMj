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
package tools;

import ij.ImagePlus;
import ij.ImageStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Michael Entrup b. Epping <entrup@arcor.de>
 * 
 */
public class EnergyLossExtractor {

    private static final String PATTERN_ELOSS_LONG = "\\[\\d*[.]?[,]?\\d+eV\\]";
    private static final String PATTERN_ELOSS_SHORT = "\\d*[.]?[,]?\\d+eV";

    /**
     * By the use of regular expressions the energy loss is extracted from the title of an image.
     * 
     * @param imp
     *            The {@link ImagePlus} contains the images.
     * @param index
     *            The index of the image where you want to extract the energy loss. <code>index</code> starts at 0.
     * @return The energy loss in eV that has been found. If the label does not contain an readable energy loss 0 is
     *         returned.
     */
    public static float eLossFromTitle(ImagePlus imp, int index) {
	ImageStack stack = imp.getStack();
	String label;
	if (index == 0 & stack.getSize() == 1) {
	    label = imp.getShortTitle();
	} else {
	    label = stack.getShortSliceLabel(index + 1);
	}
	return findELoss(label);
    }

    /**
     * By the use of regular expressions the energy loss is extracted from the title of an image.
     * 
     * @param imageStack
     *            The {@link ImageStack} that contains the image at <code>(index+1)</code>.
     * @param index
     *            The index of the image where you want to extract the energy loss. <code>index</code> starts at 0.
     * @return The energy loss in eV that has been found. If the label does not contain an readable energy loss 0 is
     *         returned.
     */
    public static float eLossFromTitle(ImageStack imageStack, int index) {
	String label = imageStack.getShortSliceLabel(index + 1);
	return findELoss(label);
    }

    /**
     * Tries to find the eLoss at the given String.
     * 
     * @param label
     *            A String that may contain an eLoss.
     * @return The eLoss fount at the String, 0 if no eLoss was found.
     */
    private static float findELoss(String label) {
	Matcher matcher1 = Pattern.compile(PATTERN_ELOSS_LONG).matcher(label);
	if (matcher1.find()) {
	    String eLossStr = label.substring(matcher1.start() + 1, matcher1.end() - 3);
	    eLossStr = eLossStr.replace(",", ".");
	    return stringToFloat(eLossStr);
	} else {
	    Matcher matcher2 = Pattern.compile(PATTERN_ELOSS_SHORT).matcher(label);
	    if (matcher2.find()) {
		String eLossStr = label.substring(matcher2.start(), matcher2.end() - 2);
		eLossStr = eLossStr.replace(",", ".");
		return stringToFloat(eLossStr);
	    } else {
		return 0;
	    }
	}
    }

    /**
     * This method is used to convert the energy loss string to a float value. If there is a ',' it is replaced by a
     * '.'.
     * 
     * @param eLossStr
     *            A {@link String} that contains only the energy loss.
     * @return A float value, 0 if converting fails.
     */
    private static float stringToFloat(String eLossStr) {
	eLossStr.replace(',', '.');
	try {
	    return new Float(eLossStr);
	} catch (Exception e) {
	    return 0;
	}
    }

}
