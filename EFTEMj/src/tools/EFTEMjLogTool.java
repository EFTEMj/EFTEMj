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

import ij.IJ;
import ij.gui.GenericDialog;

import java.awt.FileDialog;
import java.awt.TextArea;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

/**
 * A small tool to collect log messages. A {@link GenericDialog} is used to display all messages and the user can decide
 * to save them as a txt-file.
 * 
 * @author Michael Epping <michael.epping@uni-muenster.de>
 * 
 */
public class EFTEMjLogTool {

    /**
     * If true <code> System.out.println()</code> is used.
     */
    private static final boolean DEBUG = true;
    /**
     * Collect all messages that have been committed to all instances of {@link EFTEMjLogTool}.
     */
    private static Vector<String> globalMessages;
    /**
     * Collect all messages that have been committed to this instance of {@link EFTEMjLogTool}.
     */
    private Vector<String> localMessages;
    /**
     * A {@link String} to identify the instance of {@link EFTEMjLogTool}. This string will be added to the
     * {@link GenericDialog} and to the name of the txt-file.
     */
    private String process;

    /**
     * 
     * 
     * @param process
     *            A {@link String} to identify the instance of {@link EFTEMjLogTool}. This string will be added to the
     *            {@link GenericDialog} and to the name of the txt-file.
     */
    public EFTEMjLogTool(String process) {
	this.process = process;
	DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.ENGLISH);
	println(String.format("%s - Starting %s%n", df.format(new Date()), process));
    }

    /**
     * Adds a new message to the local and the global log.
     * 
     * @param text
     */
    public void println(String text) {
	if (DEBUG) {
	    System.out.println(text);
	}
	if (localMessages == null) {
	    localMessages = new Vector<String>();
	}
	if (globalMessages == null) {
	    globalMessages = new Vector<String>();
	}
	localMessages.add(text);
	globalMessages.add(process + ": " + text);
    }

    /**
     * Clears the local log.
     */
    public void clearMessageBuffer() {
	localMessages = new Vector<String>();
    }

    /**
     * Clears the global log.
     */
    public static void clearGlobalMessageBuffer() {
	globalMessages = new Vector<String>();
    }

    /**
     * Shows a {@link GenericDialog} that displays all local log entries at a {@link TextArea}.<br />
     * The log can be saved as a txt-file.
     */
    public void showLogDialog() {
	if (localMessages == null || localMessages.size() == 0) {
	    return;
	}
	GenericDialog gd = new GenericDialog("EFTEMj - LogViewer", IJ.getInstance());
	gd.addMessage("Log of " + process);
	gd.addMessage("The log is displayed at an editable TextArea." + "\n" + "You can edit the log befor saving.");
	int maxLengt = 0;
	String text = "";
	for (int i = 0; i < localMessages.size(); i++) {
	    if (localMessages.get(i).length() > maxLengt) {
		maxLengt = localMessages.get(i).length();
	    }
	    text += localMessages.get(i) + System.lineSeparator();
	}
	gd.addTextAreas(text, null, localMessages.size() + 1, maxLengt + 5);
	gd.setOKLabel("Save Log...");
	gd.setCancelLabel("Close");
	gd.setResizable(false);
	gd.showDialog();
	if (gd.wasOKed()) {
	    FileDialog fDialog = new FileDialog(gd, "Save log...", FileDialog.SAVE);
	    fDialog.setMultipleMode(false);
	    fDialog.setDirectory(IJ.getDirectory("image"));
	    // adds date and time to the file name
	    Calendar cal = Calendar.getInstance();
	    String fileName = String.format(Locale.ENGLISH, "%tF-%tR", cal, cal) + "_EFTEMj-LogFile";
	    // remove the ':' that is not allowed as at a file name
	    int pos = fileName.indexOf(":");
	    fileName = fileName.substring(0, pos) + fileName.substring(pos + 1);
	    fDialog.setFile(fileName + ".txt");
	    fDialog.setVisible(true);
	    if (fDialog.getFile() != null) {
		String path = fDialog.getDirectory() + System.getProperty("file.separator") + fDialog.getFile();
		IJ.saveString(text, path);
	    }
	}
    }

    // TODO Implement a Plugin that will display the global log.
}