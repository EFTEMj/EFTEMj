/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 *
 * Copyright (c) 2015, Michael Entrup b. Epping <michael.entrup@wwu.de>
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

import eftemj.EFTEMj;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.JavaScriptEvaluator;
import ij.plugin.PlugIn;
import tools.JARTool;

/**
 * <p>
 * This plugin is used to create a test image for the SR-EELS correction.
 * </p>
 * <p>
 * The code is loaded from an imagej script that is written in JavaScript. This
 * class is only used to load this code. For more details on the implementation,
 * have a look at the script file.
 * </p>
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 */
public class SR_EELS_DEBUG_CreateTestImage implements PlugIn {

	private final String filename_sreels_testimage = "SR-EELS_CreateTestImage.js";

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run(final String arg) {
		final String javaScriptCode = new JARTool().getText(
			EFTEMj.PATH_SCRIPTS_AND_MACROS + filename_sreels_testimage);
		final JavaScriptEvaluator jse = new JavaScriptEvaluator();
		jse.run(javaScriptCode);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		// start ImageJ
		new ImageJ();

		// run the plugin
		final Class<?> clazz = SR_EELS_DEBUG_CreateTestImage.class;
		IJ.runPlugIn(clazz.getName(), "");
	}

}
