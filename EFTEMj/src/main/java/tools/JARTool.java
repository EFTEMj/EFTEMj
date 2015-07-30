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

package tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import eftemj.EFTEMj;
import ij.IJ;

/**
 * This class is used to load data that is stored at the JAR file of this plugin
 * package.
 *
 * @author Michael Entrup b. Epping <entrup@arcor.de>
 */
public class JARTool {

	public JARTool() {
		super();
	}

	/**
	 * Loads a text file from within a JAR file using getResourceAsStream().<br />
	 * Taken from
	 * <url>http://imagej.nih.gov/ij/plugins/download/JAR_Resources_Demo.java
	 * </url>.
	 *
	 * @return Content of the text file.
	 */
	public String getText(final String path) {
		String text = "";
		try {
			// get the text resource as a stream
			final InputStream is = getClass().getResourceAsStream(path);
			if (is == null) {
				IJ.showMessage("Load macro from JAR", "File not found in JAR at " +
					path);
				return "";
			}
			final InputStreamReader isr = new InputStreamReader(is);
			final StringBuffer sb = new StringBuffer();
			final char[] b = new char[8192];
			int n;
			// read a block and append any characters
			while ((n = isr.read(b)) > 0)
				sb.append(b, 0, n);
			text = sb.toString();
		}
		catch (final IOException e) {
			String msg = e.getMessage();
			if (msg == null || msg.equals("")) msg = "" + e;
			IJ.showMessage("Load macro from JAR", msg);
		}
		return text;
	}

	public String getHelpText(final Object plugin) {
		return new JARTool().getText(EFTEMj.PATH_HELP + plugin.getClass()
			.getSimpleName() + ".html");
	}

}
