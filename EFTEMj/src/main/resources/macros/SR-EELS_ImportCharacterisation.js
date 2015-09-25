/*
 * file:	SR-EELS_ImportCharacterisation.js
 * author:	Michael Entrup b. Epping (michael.entrup@wwu.de)
 * version:	20150925
 * info:	This script is used to import characterisation images into my "database".
 * 			The "databse" is a folder structure that contains all my calibration images.
 * 			This script trises to gues the acquisition data and all parameters by processing the file path.
 *
 * 			This script is part of the Fiji plugin EFTEMj (https://github.com/EFTEMj/EFTEMj).
 */

importClass(Packages.ij.IJ);
importClass(Packages.ij.Prefs);
importClass(Packages.java.io.File);
importClass(Packages.ij.io.DirectoryChooser);
importClass(Packages.ij.gui.GenericDialog);
importClass(Packages.ij.Prefs);
importClass(Packages.sr_eels.SR_EELS_PrefsKeys);


var databasePath
main();

function main() {
	/*
	 * Read path to database from IJ_Prefs.txt or ask the user to set the path.
	 */
	IJ.showStatus("Loading path of database from IJ_Prefs.txt.");
	databasePath = Prefs.get(SR_EELS_PrefsKeys.characterisationDatabasePath.getValue(), null);
	if (!databasePath) {
		databasePath = getDirectory("Set the path to the database...");
		if (!databasePath) return;
		Prefs.set(SR_EELS_PrefsKeys.characterisationDatabasePath.getValue(), databasePath);
		Prefs.savePreferences();
	}
	/* 
	 *  Step 1
	 *  Select the folder to import from.
	 */
	var path;
	IJ.showStatus("Loading files for import.");
	if ((path = getDirectory("Characterisation files...")) == null) return;
	/* 
	 *  Step 2
	 *  Show list of files to select from.
	 */
	var files;
	if ((files = selectFiles(path)) == null) return;
	/* 
	 *  Step 3
	 *  Show a dialog to amend the parameters
	 */
	var parameters;
	if ((parameters = getParameters(path)) == null) return;
	/* 
	 *  Step 4
	 *  Import files to the database.
	 */
	if (parameters.date != null & parameters.SM != null & parameters.QSinK7 != null) {
		saveFiles(path, files, parameters);
	}	
	IJ.showStatus("Finished importing from " + path);
}

function getDirectory(title) {
	var dc = new DirectoryChooser(title);
	return dc.getDirectory();
}

function selectFiles(path) {
	var folder = new File(path);
	var list = folder.list();
	var gd = GenericDialog("Select files");
	var counter = 0;
	for (var i = 0; i < list.length; i++) {
		if (new File(path + list[i]).isFile()) {
			counter++;
			if (list[i].search(".dm3") >= 0 & list[i].search("-exclude") < 0) {
				gd.addCheckbox(list[i], true);
			} else {
				gd.addCheckbox(list[i], false);
			}
		}
	}
	if (counter < 1) {
		IJ.showMessage("Script aborted", "There are no files to import in\n" + path);
		return null;
	}
	gd.showDialog();
	if (gd.wasCanceled()) {
		return null;
	}
	var files = new Array();
	for (var i = 0; i < counter; i++) {
		if (gd.getNextBoolean()) {
			files.push(list[i]);
		}
	}
	return files;
}

function getParameters(path) {
	/*
	 * For details on JavaScript RegExp see
	 * http://www.w3schools.com/jsref/jsref_obj_regexp.asp
	 */
	var patternDate = /(\d{8})/ig;
	var patternSM = /(?:SM|SpecMag)(\d{2,3})/ig;
	var patternQSinK7 = /QSinK7\s?[\s|=]\s?(-?\+?\d{1,3})%?/ig;
	var parameters = new Parameters();
	var dateArray;
	/*
	 * The while loop is used to find the last match of the given RegExp.
	 * Index 0 of the array is the complete match. All following indices reference the groups.
	 */
	while ((dateArray = patternDate.exec(path)) !== null) {
		parameters.date = dateArray[1];
	}
	var smArray;
	while ((smArray = patternSM.exec(path)) !== null) {
		parameters.SM = smArray[1];
	}
	var qsinArray;
	while ((qsinArray = patternQSinK7.exec(path)) !== null) {
		parameters.QSinK7 = qsinArray[1];
	}
	var gd = GenericDialog("Set parameters");
	gd.addStringField("date:", parameters.date);
	gd.addStringField("SpecMag:", parameters.SM);
	gd.addStringField("QSinK7:", parameters.QSinK7);
	gd.addStringField("comment:", parameters.comment);
	gd.showDialog();
	if (gd.wasCanceled()) {
		return null;
	} else {
		parameters.date = gd.getNextString();
		parameters.SM = gd.getNextString();
		parameters.QSinK7 = gd.getNextString();	
		parameters.comment = gd.getNextString();
		/*
		 * We replace space by underscore to easily recordnice the complete comment.
		 * This is usefull wehn further processing is done.
		 */
		parameters.comment = parameters.comment.replace(" ", "_");
	}
	return parameters;
}

function Parameters() { 
	this.date = null;
	this.SM = null;
	this.QSinK7 = null;
	this.comment = null;
}

function saveFiles(path, files, parameters) {
	output = databasePath + parameters.date + " SM" + parameters.SM + " " + parameters.QSinK7 + "%";
	if (parameters.comment != "") {
		output += " " + parameters.comment;
	}
	output += "/";
	folder = new File(output);
	if (folder.exists()) {
		IJ.showMessage("Script aborted", "This data set already exists\n"
			+ folder.toString().replace(database, ""));
		return;
	}
	folder.mkdir();
	for (index in files) {
		imp = IJ.openImage(path + files[index]);
		IJ.run(imp, "Rotate 90 Degrees Left", "");
		IJ.save(imp, output + "Cal_" + pad(++index, 2) + ".tif");
		imp.close();
	}
}

function pad(num, size) {
    var s = num+"";
    while (s.length < size) s = "0" + s;
    return s;
}