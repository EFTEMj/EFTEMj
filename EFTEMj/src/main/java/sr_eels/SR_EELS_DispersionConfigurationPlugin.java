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

import java.util.Arrays;
import java.util.Vector;

import sr_eels.SR_EELS.KEYS;

/**
 * This plugin will setup the energy dispersion values used by {@link SR_EELS_DispersionCalibrationPlugin}.
 * 
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 */
public class SR_EELS_DispersionConfigurationPlugin implements PlugIn {

    /**
     * A prefix used to create key for accessing IJ_Prefs.txt by the class {@link Prefs}.
     */
    protected static final String PREFIX = SR_EELS.PREFS_PREFIX + "dispersion.";
    /**
     * This {@link Vector} is used to manage the keys.
     */
    private Vector<String> keyStorage;

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.PlugIn#run(java.lang.String)
     */
    @Override
    public void run(String arg) {
	keyStorage = new Vector<String>();
	String empty = "";
	String values = Prefs.get(PREFIX + KEYS.specMagValues, empty);
	if (values.equals(empty)) {
	    /*
	     * Show the dialog again until the user selects cancel.
	     */
	    while (showAddNewDialog() == true)
		;
	    values = "";
	    for (String key : keyStorage) {
		values += ";" + key;
	    }
	    values = values.substring(1);
	    Prefs.set(PREFIX + KEYS.specMagValues, values);
	    Prefs.savePreferences();
	    return;
	} else {
	    String[] keys;
	    double[] dispersionValues;
	    /*
	     * Show the dialog again if the user has decided to add a new value. The arrays and the Vector has to be
	     * initialised again to show the newly added value at the edit dialog.
	     */
	    do {
		values = Prefs.get(PREFIX + KEYS.specMagValues, empty);
		// The Keys to access the dispersion are stored as a string like "125;163;200;250;315".
		keys = values.split(";");
		dispersionValues = new double[keys.length];
		for (int i = 0; i < keys.length; i++) {
		    keyStorage.add(keys[i]);
		    dispersionValues[i] = Prefs.get(PREFIX + keys[i], 0);
		}
	    } while (showEditDialog(keys, dispersionValues) == true);
	}
    }

    /**
     * A dialog that is used to add a single new energy dispersion value.
     * 
     * @return <code>true</code> if a value was added <br />
     *         <code>false</code> if cancel was pressed.
     */
    private boolean showAddNewDialog() {
	GenericDialog gd = new GenericDialog("Add dispersion value", IJ.getInstance());
	gd.addNumericField("Spec._Mag:", 0, 0);
	gd.addNumericField("energy dispersion:", 1, 6, 10, "eV/px");
	gd.setOKLabel("Add");
	String help = "<html><h3>Add energy dispersion values</h3>" + "<p>description</p></html>";
	gd.addHelp(help);
	gd.showDialog();
	if (gd.wasOKed() == true) {
	    int key = (int) gd.getNextNumber();
	    if (key == 0)
		return true;
	    double dispersion = gd.getNextNumber();
	    if (dispersion == 0)
		return true;
	    Prefs.set(PREFIX + key, dispersion);
	    keyStorage.add("" + key);
	    return true;
	}
	return false;
    }

    /**
     * A dialog that is used to edit the energy dispersion values. It is possible to switch to the add dialog.
     * 
     * @param keys
     *            Spec. Mag values read from IJ_Prefs.txt.
     * @param dispersionValues
     *            Energy dispersion values associated with the Spec. Mag values.
     * @return <code>true</code> if a new value has been added. <br />
     *         <code>false</code> otherwise.
     */
    private boolean showEditDialog(String[] keys, double[] dispersionValues) {
	GenericDialog gd = new GenericDialog("Edit dispersion values", IJ.getInstance());
	gd.addMessage("Set the dispersion to 0" + "\n" + "to remove an entry.");
	for (int i = 0; i < keys.length; i++) {
	    gd.addNumericField("Spec. Mag: " + keys[i], dispersionValues[i], 6, 10, "eV/px");
	}
	gd.addCheckbox("Add_new", false);
	String help = "<html><h3>Edit energy dispersion values</h3>" + "<p>description</p></html>";
	gd.addHelp(help);
	gd.showDialog();
	if (gd.wasOKed() == true) {
	    double[] newDispersionValues = Arrays.copyOf(dispersionValues, dispersionValues.length);
	    for (int i = 0; i < dispersionValues.length; i++) {
		newDispersionValues[i] = gd.getNextNumber();
	    }
	    /*
	     * If the user want to add new energy dispersion values editing is not performed.
	     */
	    if (gd.getNextBoolean() == true) {
		while (showAddNewDialog() == true)
		    ;
		String values = "";
		for (String key : keyStorage) {
		    values += ";" + key;
		}
		values = values.substring(1);
		Prefs.set(PREFIX + KEYS.specMagValues, values);
		Prefs.savePreferences();
		return true;
	    } else {
		boolean removedOne = false;
		/*
		 * Check all values for changes and edit or remove them.
		 */
		for (int i = 0; i < dispersionValues.length; i++) {
		    if (newDispersionValues[i] == 0) {
			/*
			 * If 0 has been entered the value will be removed.
			 */
			Prefs.set(PREFIX + keys[i], 0.0);
			keyStorage.remove(keys[i]);
			removedOne = true;
		    } else {
			if (newDispersionValues[i] != dispersionValues[i]) {
			    /*
			     * If a value has changed it will be updated atIJ_Prefs.txt.
			     */
			    Prefs.set(PREFIX + keys[i], newDispersionValues[i]);
			}
		    }
		}
		if (removedOne == true) {
		    String values = "";
		    for (String key : keyStorage) {
			values += ";" + key;
		    }
		    if (values.equals("") == false) {
			values = values.substring(1);
		    }
		    Prefs.set(PREFIX + KEYS.specMagValues, values);
		    Prefs.savePreferences();
		}
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
    public static void main(String[] args) {
	// start ImageJ
	new ImageJ();

	// run the plugin
	Class<?> clazz = SR_EELS_DispersionConfigurationPlugin.class;
	IJ.runPlugIn(clazz.getName(), "");
    }
}