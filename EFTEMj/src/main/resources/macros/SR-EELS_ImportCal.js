/*
 * file:	SR-EELS_ImportCal.js
 * author:	Michael Entrup b. Epping (michael.entrup@wwu.de)
 * version:	20150804
 * info:	This script is used to import calibration images into my "database".
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

var databaseKey = "EFTEMj.SR-EELS.database.path";
var databasePath;

main();

function main() {
	/*
	 * The path to the databasePath folder is stored in IJ_Prefs.txt.
	 * If the value is not yet defined, the user is asked to set a directory.
	 */
	if ((databasePath = Prefs.get(databaseKey, "")) == "") {
		if ((databasePath = getDirectory("Path to your SR-EELS database...")) != null) Prefs.set(databaseKey, databasePath);
		else return;
	}
	var path;
	/*
	 * Select the files to import.
	 */
	if ((path = getDirectory("Calibration files...")) == null) return;
	var files;
	if ((files = selectFiles(path)) == null) return;
	var parameters;
	if ((parameters = getParameters(path)) == null) return;
	if (parameters.date != null & parameters.SM != null & parameters.QSinK7 != null) {
		saveFiles(path, files, parameters);
	}
}

function getDirectory(title) {
	var dc = new DirectoryChooser(title);
	return dc.getDirectory();
}

function selectFiles(path) {
	var folder = new File(path);
	var list = folder.list();
	var gd = GenericDialog("Select files");
	for (var i = 0; i < list.length; i++) {
		if (new File(path + list[i]).isFile() & list[i].search("Cal") >= 0 & list[i].search("-exclude") < 0) {
			gd.addCheckbox(list[i], true);
		} else {
			gd.addCheckbox(list[i], false);
		}
	}
	gd.showDialog();
	if (gd.wasCanceled()) {
		return null;
	}
	var files = new Array();
	for (var i = 0; i < list.length; i++) {
		if (gd.getNextBoolean()) {
			files.push(list[i]);
		}
	}
	return files;
}

function getParameters(path) {
	var patternDate = /(\d{8})/ig;
	var patternSM = /(?:SM|SpecMag)(\d{2,3})/ig;
	var patternQSinK7 = /(QSinK7[\s|=])(-?\+?\d{1,3})%?/ig;
	var parameters = new Parameters();
	var dateArray;
	while ((dateArray = patternDate.exec(path)) !== null) {
		parameters.date = dateArray[1];
	}
	var smArray;
	while ((smArray = patternSM.exec(path)) !== null) {
		parameters.SM = smArray[1];
	}
	var qsinArray;
	while ((qsinArray = patternQSinK7.exec(path)) !== null) {
		parameters.QSinK7 = qsinArray[2];
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