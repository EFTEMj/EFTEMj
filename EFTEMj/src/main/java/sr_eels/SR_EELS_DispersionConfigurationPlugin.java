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

import eftemj.EFTEMj_Prefs;
import ij.IJ;
import ij.ImageJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * This plugin will setup the energy dispersion values used by {@link SR_EELS_DispersionCalibrationPlugin}.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 */
public class SR_EELS_DispersionConfigurationPlugin extends SR_EELS implements PlugIn {

    /**
     * A prefix used to create key for accessing IJ_Prefs.txt by the class {@link Prefs}.
     */
    protected static final String PREFIX = PREFS_PREFIX + KEYS.dispersionEloss + ".";
    /**
     * This {@link Hashtable} is used to manage the SpecMag-dispersion pairs.
     */
    private Hashtable<Integer, Double> dispersionStorage;
    private boolean somethingChanged = false;

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.PlugIn#run(java.lang.String)
     */
    @Override
    public void run(final String arg) {
	boolean showAgain;
	dispersionStorage = buildDispersionStorage();
	if (dispersionStorage.size() == 0) {
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
	    for (final Enumeration<Integer> e = dispersionStorage.keys(); e.hasMoreElements();) {
		final int key = e.nextElement();
		final double val = dispersionStorage.get(key);
		Prefs.set(PREFIX + Integer.toString(key), val);
	    }
	    Prefs.savePreferences();
	}
    }

    public static Hashtable<Integer, Double> buildDispersionStorage() {
	final Hashtable<Integer, Double> dispersionStorage = new Hashtable<Integer, Double>();
	final Vector<String> keys = EFTEMj_Prefs.getAllKeys(KEYS.dispersionEloss.toString());
	for (final String key : keys) {
	    final String empty = "0.0";
	    final String valueStr = Prefs.get(key, empty);
	    if (!valueStr.equals(empty)) {
		final double value = new Double(valueStr);
		dispersionStorage.put(new Integer(key.substring(key.lastIndexOf('.') + 1, key.length())), value);
	    }
	}
	return dispersionStorage;
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
	    final int key = (int) gd.getNextNumber();
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
	final Enumeration<Integer> e = dispersionStorage.keys();
	final List<Integer> list = Collections.list(e);
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
		if (val != dispersionStorage.get(key)) {
		    dispersionStorage.put((Integer) key, val);
		    somethingChanged = true;
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