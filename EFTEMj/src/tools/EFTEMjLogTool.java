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

import ij.gui.GenericDialog;

import java.util.Vector;

/**
 * @author Michael Epping <michael.epping@uni-muenster.de>
 * 
 */
public class EFTEMjLogTool {

    private static final boolean DEBUG = true;
    private static Vector<String> messages;

    public static void println(String text) {
	if (DEBUG) {
	    System.out.println(text);
	}
	if (messages == null) {
	    messages = new Vector<String>();
	}
	messages.add(text);
    }

    public static void clearMessageBuffer() {
	messages = new Vector<String>();
    }

    public static void showDialog() {
	if (messages == null || messages.size() == 0) {
	    return;
	}
	GenericDialog gd = new GenericDialog("");
	int maxLengt = 0;
	String text = "";
	for (int i = 0; i < messages.size(); i++) {
	    if (messages.get(i).length() > maxLengt) {
		maxLengt = messages.get(i).length();
	    }
	    text += messages.get(i) + System.lineSeparator();
	}
	gd.addTextAreas(text, null, messages.size() + 1, maxLengt + 5);
	gd.setResizable(false);
	gd.setModal(false);
	gd.showDialog();
	if (gd.wasCanceled()) {
	    return;
	}
	return;
    }
}