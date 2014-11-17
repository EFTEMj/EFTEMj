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

import ij.IJ;
import ij.ImageJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 * This plugin will setup the energy dispersion values used by {@link SR_EELS_DispersionCalibrationPlugin}.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 */
public class SR_EELS_DispersionConfigurationPlugin extends SR_EELS implements PlugIn {

    /**
     * A prefix used to create key for accessing IJ_Prefs.txt by the class {@link Prefs}.
     */
    protected static final String PREFIX = PREFS_PREFIX + KEYS.dispersion + ".";
    /**
     * This {@link Hashtable} is used to manage the SpecMag-dispersion pairs.
     */
    private Hashtable<Double, Double> dispersionStorage;
    private boolean somethingChanged = false;

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.PlugIn#run(java.lang.String)
     */
    @Override
    public void run(final String arg) {
	boolean showAgain;
	dispersionStorage = new Hashtable<Double, Double>();
	final String empty = "";
	/*
	 * The Keys to access the dispersion are stored as a string like "125;163;200;250;315".
	 */
	String specMags = Prefs.get(PREFIX + KEYS.specMagValues, empty);
	String[] keys;
	if (!specMags.equals(empty)) {
	    keys = specMags.split(";");
	    for (final String key : keys) {
		final String value = Prefs.get(PREFIX + key, empty);
		if (!value.equals(empty)) {
		    dispersionStorage.put(new Double(key), new Double(value));
		}
	    }
	} else {
	    /*
	     * Show the dialog again, until the user presses cancel.
	     */
	    showAgain = true;
	    while (showAgain == true) {
		showAgain = showAddNewDialog();
	    }
	}
	/*
	 * If the user adds new values, the dialog will be shown again, to check them.
	 */
	showAgain = true;
	while (showAgain == true) {
	    showAgain = showEditDialog();
	}
	/*
	 * We have to write the dispersion to the preferences only if the user changed a value.
	 */
	if (somethingChanged == true) {
	    specMags = "";
	    for (final Enumeration<Double> e = dispersionStorage.keys(); e.hasMoreElements();) {
		final double key = e.nextElement();
		if (specMags.length() > 0) {
		    specMags += ";";
		}
		specMags += key;
		final double val = dispersionStorage.get(key);
		Prefs.set(PREFIX + Double.toString(key), val);
	    }
	    Prefs.set(PREFIX + KEYS.specMagValues, specMags);
	    Prefs.savePreferences();
	}
    }

    /**
     * A dialog that is used to add a single new energy dispersion value.
     *
     * @return <code>true</code> if a value was added <br />
     *         <code>false</code> if cancel was pressed.
     */
    private boolean showAddNewDialog() {
	final GenericDialog gd = new GenericDialog("Add dispersion value", IJ.getInstance());
	gd.addNumericField("Spec._Mag:", 0, 0);
	gd.addNumericField("energy dispersion:", 1, 6, 10, "eV/px");
	gd.setOKLabel("Add");
	final String help = "<html><h3>Add energy dispersion values</h3>" + "<p>description</p></html>";
	gd.addHelp(help);
	gd.showDialog();
	if (gd.wasOKed() == true) {
	    final double key = gd.getNextNumber();
	    if (key == 0)
		return true;
	    final double dispersion = gd.getNextNumber();
	    if (dispersion == 0)
		return true;
	    dispersionStorage.put(key, dispersion);
	    somethingChanged = true;
	    return true;
	}
	return false;
    }

    /**
     * A dialog that is used to edit the energy dispersion values. It is possible to switch to the add dialog.
     *
     * @return <code>true</code> if a new value has been added. <br />
     *         <code>false</code> otherwise.
     */
    private boolean showEditDialog() {
	final GenericDialog gd = new GenericDialog("Edit dispersion values", IJ.getInstance());
	gd.addMessage("Set the dispersion to 0 to remove an entry.");
	final Enumeration<Double> e = dispersionStorage.keys();
	final List<Double> list = Collections.list(e);
	Collections.sort(list);
	final Object[] keys = list.toArray();
	for (final Object key : keys) {
	    final double val = dispersionStorage.get(key);
	    gd.addNumericField("Spec. Mag: " + key, val, 6, 10, "eV/px");
	}
	gd.addCheckbox("Add_new", false);
	final String help = "<html><h3>Edit energy dispersion values</h3>" + "<p>description</p></html>";
	gd.addHelp(help);
	gd.showDialog();
	if (gd.wasOKed() == true) {
	    for (final Object key : keys) {
		final double val = gd.getNextNumber();
		if (val == 0) {
		    dispersionStorage.remove(key);
		    somethingChanged = true;
		} else {
		    if (val != dispersionStorage.get(key)) {
			dispersionStorage.put((Double) key, val);
			somethingChanged = true;
		    }
		}
	    }
	    /*
	     * If the user want to add new energy dispersion values editing is not performed.
	     */
	    if (gd.getNextBoolean() == true) {
		boolean showAgain = true;
		while (showAgain == true)
		    showAgain = showAddNewDialog();
		return true;
	    }
	}
	return false;
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

	// run the plugin
	final Class<?> clazz = SR_EELS_DispersionConfigurationPlugin.class;
	IJ.runPlugIn(clazz.getName(), "");
    }
}