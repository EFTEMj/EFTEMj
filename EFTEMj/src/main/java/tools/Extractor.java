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

package tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ij.ImagePlus;
import ij.ImageStack;

public abstract class Extractor {

	/**
	 * This array has to be defined at the constructor of the inheriting class.
	 */
	protected String[] pattern;
	protected String replace;

	/**
	 * This method is used to convert a string to a float value. If there is a ','
	 * it is replaced by a '.'.
	 *
	 * @param eLossStr A {@link String} that contains only the float.
	 * @return A float value, 0 if converting fails.
	 */
	public float stringToFloat(final String eLossStr) {
		eLossStr.replace(',', '.');
		try {
			return new Float(eLossStr);
		}
		catch (final Exception e) {
			return 0;
		}
	}

	/**
	 * By the use of regular expressions a float is extracted from the title of an
	 * image.
	 *
	 * @param imp is the {@link ImagePlus} contains the images.
	 * @param index The index of the image where you want to extract the float.
	 *          <code>index</code> starts at 0.
	 * @return The float that has been found. 0 is returned if the label does not
	 *         contain a float.
	 */
	public float extractFloatFromTitle(final ImagePlus imp, final int index) {
		final ImageStack stack = imp.getStack();
		String label;
		if (index == 0 & stack.getSize() == 1) {
			label = imp.getShortTitle();
		}
		else {
			label = stack.getShortSliceLabel(index + 1);
		}
		if (label == null) label = "";
		return findFloat(label);
	}

	/**
	 * By the use of regular expressions a float is extracted from the title of an
	 * image.
	 *
	 * @param imageStack The {@link ImageStack} that contains the image at
	 *          <code>(index+1)</code>.
	 * @param index The index of the image where you want to extract the float.
	 *          <code>index</code> starts at 0.
	 * @return The float that has been found. 0 is returned if the label does not
	 *         contain a float.
	 */
	public float extractFloatFromTitle(final ImageStack imageStack,
		final int index)
	{
		final String label = imageStack.getShortSliceLabel(index + 1);
		return findFloat(label);
	}

	/**
	 * This method searches for the pattern defined at the inheriting classes.
	 *
	 * @param label is the {@link String} to search in.
	 * @return the {@link String} as a {@link Float}.
	 */
	protected float findFloat(final String label) {
		for (int i = 0; i < pattern.length; i++) {
			final Matcher matcher = Pattern.compile(pattern[i]).matcher(label);
			if (matcher.find()) {
				final String result = matcher.replaceAll(replace);
				return stringToFloat(result);
			}
		}
		return 0;
	}

}
