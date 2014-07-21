/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 * 
 * Copyright (c) 2013, Michael Entrup b. Epping <entrup@arcor.de>
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
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextField;

/**
 * This plugin is used to shift the images of an {@link ImagePlus} containing a stack. The user is asked to enter the
 * enter the shift values (x- and y-direction) for each image of the stack. Then he can select to apply the given values
 * or let the plugin optimise the shift values and afterwards apply them.
 * 
 * @author Michael Entrup b. Epping <entrup@arcor.de>
 * 
 */
public class StackShifterPlugin implements ExtendedPlugInFilter {

    /**
     * The plugin will be aborted.
     */
    private final int CANCEL = 0;
    /**
     * The plugin will continue with the next step.
     */
    private final int OK = 1;
    /**
     * <code>DOES_32 | NO_CHANGES | FINAL_PROCESSING</code>
     */
    private final int FLAGS = DOES_ALL;
    /**
     * This is the {@link ImagePlus} that has been selected when starting the plugin.
     */
    private ImagePlus initialImp;
    /**
     * If true a new {@link ImagePlus} is created and the input {@link ImagePlus} is not changed.
     */
    private boolean createNew;
    /**
     * If true the shift values are optimised.
     */
    private boolean optimise;
    /**
     * An array containing the shift values (as {@link Point}s) for each image of the initial stack.
     */
    private Point[] shift;
    /**
     * The methods that can be used to fill the border.
     */
    private OptimisedStackShifter.MODES mode;
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
	return FLAGS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
     */
    @Override
    public void run(ImageProcessor ip) {
	ImagePlus correctedImp = OptimisedStackShifter.shiftImages(initialImp, shift, mode, optimise, createNew);
	if (createNew == true) {
	    correctedImp.setCalibration(calibration);
	    correctedImp.show();
	} else {
	    correctedImp.changes = true;
	    correctedImp.updateAndRepaintWindow();
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.ExtendedPlugInFilter#showDialog(ij.ImagePlus, java.lang.String,
     * ij.plugin.filter.PlugInFilterRunner)
     */
    @Override
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
	initialImp = imp;
	calibration = imp.getCalibration();
	if (showParameterDialog(command) == CANCEL) {
	    canceled();
	    return NO_CHANGES | DONE;
	}
	if (createNew == true) {
	    return FLAGS | NO_CHANGES;
	}
	return FLAGS;
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

    /***
     * This dialog is used to setup the parameter for the stack shift.
     * 
     * @param title
     * @return OK or CANCEL
     */
    private int showParameterDialog(String title) {
	GenericDialog gd = new GenericDialog(title, IJ.getInstance());
	// create 2 numeric fields for each slice of the stack.
	int defaultValue = 0;
	int digits = 0;
	// TODO read shift values from a CSV-file
	TextField[] xFields = new TextField[initialImp.getStackSize()];
	TextField[] yFields = new TextField[initialImp.getStackSize()];
	for (int i = 0; i < initialImp.getStackSize(); i++) {
	    String label = initialImp.getStack().getShortSliceLabel(i + 1);
	    if (label == null) {
		label = String.format("slice %d", i+1);
	    }
	    gd.addMessage(label);
	    Panel cont = new Panel(new FlowLayout());
	    cont.add(new Label("x:"));
	    TextField tf1 = new TextField(IJ.d2s(defaultValue, digits));
	    tf1.addActionListener(gd);
	    tf1.addTextListener(gd);
	    tf1.addFocusListener(gd);
	    tf1.addKeyListener(gd);
	    xFields[i] = tf1;
	    cont.add(tf1);
	    cont.add(new Label("y:"));
	    TextField tf2 = new TextField(IJ.d2s(defaultValue, digits));
	    tf2.addActionListener(gd);
	    tf2.addTextListener(gd);
	    tf2.addFocusListener(gd);
	    tf2.addKeyListener(gd);
	    cont.add(tf2);
	    yFields[i] = tf2;
	    gd.addPanel(cont);
	}
	String[] labels = { "Optimise image shift", "Create a new image" };
	boolean[] defaults = { true, true };
	gd.addCheckboxGroup(2, 1, labels, defaults);
	String[] items = new String[OptimisedStackShifter.MODES.values().length];
	for (int i = 0; i < items.length; i++) {
	    items[i] = OptimisedStackShifter.MODES.values()[i].toString();
	}
	gd.addChoice("Border mode:", items, items[0]);
	// TODO write the description
	String help = "<html><h3>Stack Shifter</h3><p>description</p></html>";
	gd.addHelp(help);
	gd.showDialog();
	if (gd.wasCanceled() == true) {
	    return CANCEL;
	}
	shift = new Point[initialImp.getStackSize()];
	for (int i = 0; i < initialImp.getStackSize(); i++) {
	    shift[i] = new Point(new Integer(xFields[i].getText()), new Integer(yFields[i].getText()));
	}
	optimise = gd.getNextBoolean();
	createNew = gd.getNextBoolean();
	mode = OptimisedStackShifter.MODES.values()[gd.getNextChoiceIndex()];
	return OK;
    }

    /**
     * Cancel the plugin and show a status message.
     */
    private void canceled() {
	IJ.showStatus("Stack shift has been canceled.");
    }

    /**
     * Main method for debugging.
     *
     * For debugging, it is convenient to have a method that starts ImageJ, loads an image and calls the plugin, e.g.
     * after setting breakpoints.
     *
     * @param args
     *            unused
     */
    public static void main(String[] args) {
	// start ImageJ
	new ImageJ();

	// open the sample stack
	ImagePlus image = IJ.openImage("http://EFTEMj.entrup.com.de/Drift-Stack_max64px.tif");
	image.setRoi(64, 64, 128, 128);
	image.getStack().setSliceLabel(null, 1);
	image.show();

	// run the plugin
	Class<?> clazz = StackShifterPlugin.class;
	IJ.runPlugIn(clazz.getName(), "");
    }
}
