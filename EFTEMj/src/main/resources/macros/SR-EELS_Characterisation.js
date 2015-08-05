/*
 * file:	SR-EELS_characterisation.js
 * author:	Michael Entrup b. Epping (michael.entrup@wwu.de)
 * version:	20150805
 * info:	This script is used to characterise the distortions of an Zeiss in-column energy filter when using SR-EELS.
 * 			A series of special SR-EELS data sets is necessary to run the characterisation.
 * 			Place all data sets (images) at a single folder and run this script.
 * 			You can find an example SR-EELS series at:
 * 				https://github.com/EFTEMj/EFTEMj/tree/master/Scripts+Macros/examples/SR-EELS_characterisation
 * 			There you will find a instruction on how to record such a series, too.
 * 			
 * 			The ijm version (SR-EELS_characterisation.ijm) is the template for this script.
 *
 * 			This script is part of the Fiji plugin EFTEMj (https://github.com/EFTEMj/EFTEMj).
 *
 * help:	* You can exclude files from the characterisation if '-exclude' is added to the file name.
 */

 /*
  * DRAFT - INFO
  * The first version of this script will not contain any GUI. One has to set all parameters directly.
  * Not before the characterisation is finished a GUI will be added.
  */

importClass(Packages.ij.IJ);
importClass(Packages.ij.ImagePlus);
importClass(Packages.ij.process.FloatProcessor);
importClass(Packages.java.lang.Runnable);
importClass(Packages.java.lang.Runtime);
importClass(Packages.java.lang.System);
importClass(Packages.java.lang.Thread);
importClass(Packages.java.util.concurrent.atomic.AtomicInteger);
importClass(Packages.java.io.File);
importClass(Packages.ij.io.DirectoryChooser);
importClass(Packages.ij.gui.GenericDialog);
importClass(Packages.ij.Prefs);
importClass(Packages.sr_eels.SR_EELS_PrefsKeys);

 function settings() {
 	this.path = "Q:\\Aktuell\\SR-EELS Calibration measurements\\20150803 SM125 -15%";
 }

function runCharacterisation(image) {
	var result;
	// here goes the loop over all images in settings.path
	// run runCharacterisation_sub() multithreaded
	return result;
}

function runCharacterisation_sub(subimage) {
	var result_sub;
	// do something
	return result_sub;
}

function saveResult(result) {
	// create different diagrams with createDiagram()
	// create differnt text files containing numeric results using createTextFile()
}

function createDiagram(processedResult) {
	var diagram;
	// use the data in cluded in processedResult to create a diagram
	return diagram;
}

function createTextFile(path, values) {
	var file;
	// write all arrays from values to a new text file
}
