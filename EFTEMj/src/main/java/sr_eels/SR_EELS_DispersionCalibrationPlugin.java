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
package sr_eels;

import gui.ExtendedWaitForUserDialog;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

import java.awt.Choice;
import java.awt.Rectangle;
import java.awt.TextField;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

import tools.EFTEMjLogTool;

/**
 * This class is used to set the energy dispersion of SR-EELS images or stacks. There is a second class (
 * {@link SR_EELS_DispersionConfigurationPlugin}) that is used to setup predefined values. The offset (origin) can be
 * set by different methods.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 *
 */
public class SR_EELS_DispersionCalibrationPlugin extends SR_EELS implements ExtendedPlugInFilter {

    /**
     * Items of a {@link Choice} element.<br />
     * This items are hard coded.
     */
    private static enum BINNING {
	B1("1"), B2("2"), B4("4"), B8("8"), BINNING_OTHER("Other binning");

	/**
	 * The {@link String} that will be returned when using the method <code>toString()</code>.
	 */
	private final String string;

	/**
	 * @param string
	 *            The {@link String} that will be returned when using the method <code>toString()</code>.
	 */
	private BINNING(final String string) {
	    this.string = string;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() {
	    return string;
	}

	/**
	 * @return A {@link String} array that can be used to create a choice at a {@link GenericDialog}.
	 */
	public static String[] toStringArray() {
	    final String[] array = new String[values().length];
	    for (int i = 0; i < array.length; i++) {
		array[i] = values()[i].toString();
	    }
	    return array;
	}
    }

    /**
     * <code>DOES_ALL | FINAL_PROCESSING</code>
     */
    private static final int FLAGS = DOES_ALL | FINAL_PROCESSING;
    /**
     * An instance of {@link EFTEMjLogTool}.
     */
    private EFTEMjLogTool logTool;
    /**
     * Items of a {@link Choice} element.
     */
    private String[] specMagValues;
    /**
     * Items of a {@link Choice} element.<br />
     * This items are hard coded.
     */
    private final String[] offsetMethods = { "absolute offset", "peak selection", "center loss", "lowest loss",
	    "highest loss" };
    /**
     * The image that was selected when starting the plugin.
     */
    private ImagePlus input;
    /**
     * The orientation of the energy loss axis.<br />
     * <ul>
     * <li>0 = x-axis</li>
     * <li>1 = y-axis</li>
     * </ul>
     */
    private int orientation;
    /**
     * The selection of the Spec Mag {@link Choice} element.<br />
     * At first this field is used to store the default value obtained by using the {@link Prefs} class.
     */
    private int specMagIndex;
    /**
     * The selection of the binning {@link Choice} element.<br />
     * At first this field is used to store the default value obtained by using the {@link Prefs} class.
     */
    private int binningIndex;
    /**
     * The selection of the offset {@link Choice} element.<br />
     * At first this field is used to store the default value obtained by using the {@link Prefs} class.
     */
    private int offsetIndex;
    /**
     * The value entered to the binning {@link TextField}.<br />
     * At first this field is used to store the default value obtained by using the {@link Prefs} class.
     */
    private int binningUser;
    /**
     * The value entered to the offset energy loss {@link TextField}.<br />
     * At first this field is used to store the default value obtained by using the {@link Prefs} class.
     */
    private double offsetLoss;
    /**
     * The value entered to the absolute offset {@link TextField}.<br />
     * At first this field is used to store the default value obtained by using the {@link Prefs} class.
     */
    private int offsetAbsolute;
    /**
     * This filed is only used when the peak selection offset method is selected.<br />
     * The point selection is represented by a {@link Rectangle} with width = height = 0.
     */
    private Rectangle point;

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
     */
    @Override
    public int setup(final String arg, final ImagePlus imp) {
	if (arg == "final") {
	    // Update the ImagePlus to make the new calibration visible. RepaintWindow is necessary to update the
	    // information displayed above the image (dimension, type, size).
	    input.updateAndRepaintWindow();
	    return DONE;
	}
	return init();
    }

    /**
     * Read values from IJ_Prefs.txt by using the class {@link Prefs}.
     *
     * @return FLAGS if the initialisation was successful.
     */
    private int init() {
	/*
	 * We use a local variable as dispersionStorage is only used to build a String array.
	 */
	final Hashtable<Integer, Double> dispersionStorage = SR_EELS_DispersionConfigurationPlugin
		.buildDispersionStorage();
	if (dispersionStorage.size() == 0) {
	    IJ.showMessage("No Spec. Mag values found", "The IJ_Prefs.txt contains no Spec. Mag values." + "\n"
		    + "You can ente some values by using the dispersion configuration.");
	    return DONE;
	}
	specMagValues = new String[dispersionStorage.size()];
	int index = 0;
	for (final Enumeration<Integer> e = dispersionStorage.keys(); e.hasMoreElements();) {
	    final int key = e.nextElement();
	    specMagValues[index] = "" + key;
	    index++;
	}
	Arrays.sort(specMagValues);
	/*
	 * Load the values that were saved at the last usage of this plugin.
	 */
	specMagIndex = (int) Prefs.get(SR_EELS_PrefsKeys.specMagIndex.getValue(), 0);
	binningIndex = (int) Prefs.get(SR_EELS_PrefsKeys.binningIndex.getValue(), 0);
	binningUser = (int) Prefs.get(SR_EELS_PrefsKeys.binningUser.getValue(), 1);
	offsetIndex = (int) Prefs.get(SR_EELS_PrefsKeys.offsetIndex.getValue(), 0);
	offsetLoss = Prefs.get(SR_EELS_PrefsKeys.offsetLoss.getValue(), 0);
	offsetAbsolute = (int) Prefs.get(SR_EELS_PrefsKeys.offsetAbsolute.getValue(), 0);
	return FLAGS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
     */
    @Override
    public void run(final ImageProcessor ip) {
	final double dispersion = Prefs.get(SR_EELS_PrefsKeys.dispersionEloss.getValue() + specMagValues[specMagIndex],
		1);
	int offsetValue = 0;
	int binning;
	if (BINNING.toStringArray()[binningIndex].equals(BINNING.BINNING_OTHER.toString())) {
	    binning = binningUser;
	} else {
	    binning = new Integer(BINNING.toStringArray()[binningIndex]);
	}
	switch (offsetIndex) {
	case 0:
	    /*
	     * absolute value
	     */
	    offsetValue = offsetAbsolute;
	    break;
	case 1:
	    /*
	     * peak selection
	     */
	    if (orientation == 0) {
		offsetValue = -((int) Math.round(offsetLoss / dispersion / binning - point.x));
	    } else {
		offsetValue = -((int) Math.round(offsetLoss / dispersion / binning - point.y));
	    }
	    input.setRoi(null, false);
	    break;
	case 2:
	    /*
	     * center loss
	     */
	    double offsetCenter;
	    if (orientation == 0) {
		offsetCenter = 1.0 * input.getWidth() / 2;
	    } else {
		offsetCenter = 1.0 * input.getHeight() / 2;
	    }
	    offsetValue = -((int) Math.round(offsetLoss / dispersion / binning - offsetCenter));
	    break;
	case 3:
	    /*
	     * lowest loss
	     */
	    offsetValue = -((int) Math.round(offsetLoss / dispersion / binning));
	    break;
	case 4:
	    /*
	     * highest loss
	     */
	    if (orientation == 0) {
		offsetValue = -((int) Math.round(offsetLoss / dispersion / binning - input.getWidth()));
	    } else {
		offsetValue = -((int) Math.round(offsetLoss / dispersion / binning - input.getHeight()));
	    }
	    break;
	default:
	    break;
	}
	final Calibration cal = new Calibration(input);
	if (orientation == 0) {
	    cal.pixelWidth = dispersion * binning;
	    /*
	     * If you use setXUnit() it's the same like using setUnit().
	     */
	    cal.setXUnit("eV");
	    cal.xOrigin = offsetValue;
	} else {
	    cal.pixelHeight = dispersion * binning;
	    cal.setYUnit("eV");
	    /*
	     * there is a filed called yunit but it is not yet used by the GUI.
	     */
	    cal.setUnit("eV");
	    cal.yOrigin = offsetValue;
	}
	input.setCalibration(cal);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.ExtendedPlugInFilter#showDialog(ij.ImagePlus, java.lang.String,
     * ij.plugin.filter.PlugInFilterRunner)
     */
    @Override
    public int showDialog(final ImagePlus imp, final String command, final PlugInFilterRunner pfr) {
	input = imp;
	logTool = new EFTEMjLogTool(command);
	final GenericDialog gd = new GenericDialog(command, IJ.getInstance());
	final String[] items = { "x-axis", "y-axis" };
	/*
	 * Try to make a good default selection for the orientation.
	 */
	final String selectedItem = ((input.getWidth() >= input.getHeight()) ? items[0] : items[1]);
	gd.addChoice("Energy_axis:", items, selectedItem);
	gd.addChoice("Spec_Mag:", specMagValues, specMagValues[specMagIndex]);
	gd.addChoice("Binning:", BINNING.toStringArray(), BINNING.toStringArray()[binningIndex]);
	gd.addNumericField("Other_binning:", binningUser, 1);
	gd.addChoice("Offset_method:", offsetMethods, offsetMethods[offsetIndex]);
	gd.addNumericField("Energy_loss:", offsetLoss, 0, 6, "eV");
	gd.addNumericField("Absolute_offset:", offsetAbsolute, 0, 6, "px");
	gd.showDialog();
	if (gd.wasOKed()) {
	    orientation = gd.getNextChoiceIndex();
	    specMagIndex = gd.getNextChoiceIndex();
	    Prefs.set(SR_EELS_PrefsKeys.specMagIndex.getValue(), specMagIndex);
	    binningIndex = gd.getNextChoiceIndex();
	    Prefs.set(SR_EELS_PrefsKeys.binningIndex.getValue(), binningIndex);
	    binningUser = (int) gd.getNextNumber();
	    Prefs.set(SR_EELS_PrefsKeys.binningUser.getValue(), binningUser);
	    offsetIndex = gd.getNextChoiceIndex();
	    Prefs.set(SR_EELS_PrefsKeys.offsetIndex.getValue(), offsetIndex);
	    offsetLoss = gd.getNextNumber();
	    Prefs.set(SR_EELS_PrefsKeys.offsetLoss.getValue(), offsetLoss);
	    offsetAbsolute = (int) gd.getNextNumber();
	    Prefs.set(SR_EELS_PrefsKeys.offsetAbsolute.getValue(), offsetAbsolute);
	    Prefs.savePreferences();
	    if (offsetIndex == 1) { // peak selection
		boolean ok;
		do {
		    ok = showPointSelectionDialog();
		} while (ok & input.getRoi() == null);
		if (!ok) {
		    cancel();
		    return DONE | NO_CHANGES;
		}
		/*
		 * A point is a rectangle with width = height = 0.
		 */
		point = input.getRoi().getBounds();
	    }
	    return FLAGS;
	}
	cancel();
	return DONE | NO_CHANGES;
    }

    /**
     * A dialog that requests the user to set a point selection. This point is used to calibrate the origin of the
     * energy axis.
     *
     * @return <code>true</code> if Ok has been pressed.
     */
    private boolean showPointSelectionDialog() {
	final String oldTool = IJ.getToolName();
	IJ.setTool(Toolbar.POINT);
	final ExtendedWaitForUserDialog dialog = new ExtendedWaitForUserDialog("Offset calibration",
		"Select the feature with an energy loss of " + offsetLoss + "eV." + "\n"
			+ "Press Ok to use the selected point for the offset calibration.", null);
	dialog.show();
	IJ.setTool(oldTool);
	if (dialog.escPressed())
	    return false;
	return true;
    }

    /**
     * Cancel the plugin and show a status message.
     */
    private void cancel() {
	final String message = "Dispersion calibration has been canceled.";
	logTool.println(message);
	IJ.showStatus(message);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.ExtendedPlugInFilter#setNPasses(int)
     */
    @Override
    public void setNPasses(final int nPasses) {
	// This method is not used.
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
    public static void main(final String[] args) {
	// start ImageJ
	new ImageJ();

	// open the sample
	final ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
	image.show();

	// run the plugin
	final Class<?> clazz = SR_EELS_DispersionCalibrationPlugin.class;
	IJ.runPlugIn(clazz.getName(), "");
    }
}