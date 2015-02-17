/*
 * file:	SR-EELS_CreateCharacterisationOverview.ijm
 * author:	Michael Entrup b. Epping (michael.entrup@wwu.de)
 * version:	20150126
 * info:	This script generates a HTML file to show the results of the SR-EELS characterisation.
 * 			The first of the calibration images is displayed (JPEG version with overlay).
 * 			'head.html' and 'foot.html' must be present at the output directory.
 *
 * 			This scrfipt is part of the Fiji plugin EFTEMj (https://github.com/EFTEMj/EFTEMj).
 */

importClass(Packages.java.io.File);
importClass(Packages.java.io.FileReader);
importClass(Packages.java.io.FileWriter);
importClass(Packages.ij.IJ);

IJ.log(arguments.length);

var input = "Q:/Aktuell/SR-EELS Calibration measurements/";
var output = "Q:/Aktuell/SR-EELS Calibration measurements/HTML-Overview/";

var found = new Array();

var skip = false;
if (!new File(input).isDirectory()) {
	exit("Please set the input path.");
	skip = true;
}
if (!new File(output).isDirectory()) {
	exit("Please set the output path.");
	skip = true;
}
if (!new File(output + "head.html").exists()) {
	IJ.showMessage("Please place the file 'head.html' at\n" + output);
	skip = true;
}
if (!new File(output + "foot.html").exists()) {
	IJ.showMessage("Please place the file 'foot.html' at\n" + output);
	skip = true;
}

if (skip == false) {
	processFolder(input);

	IJ.log(found.length + " data sets found.");
	var headReader = new FileReader(new File(output + "head.html"));
	var footReader = new FileReader(new File(output + "foot.html"));

	var indexWriter = new FileWriter(new File(output + "index.html"));

	for (var c; ( c = headReader.read() ) != -1; ) {
		indexWriter.append(c);
	}

	for (var i = 0; i < found.length; i++) {
		var divStart = "<div class=\"col-xs-12 col-sm-6 col-md-4 col-lg-3\ outer-tile SM" + found[i].specMag + "\"><div class=\"inner-tile\">\n";
		indexWriter.append(divStart);
		var img = "<a href=\"" + found[i].img + "\"><img src=\"" + found[i].img + "\" class=\"img-responsive img-rounded\"></a>\n";
		indexWriter.append(img);
		var desc = "<div class=\"specs\"><ul><li>specMag = " + found[i].specMag + "</li><li>QSinK7 = " + found[i].qSinK7 + "%</li><li>Date: " + found[i].date + "</li><li>Parameters: " + found[i].paras + "</li></ul></div>\n";
		indexWriter.append(desc);
		var divEnd = "</div></div>\n";
		indexWriter.append(divEnd);
	}

	for (var c; ( c = footReader.read() ) != -1; ) {
		indexWriter.append(c);
	}
	headReader.close();
	footReader.close();
	indexWriter.close();
	IJ.showMessage("Finished!");
} else IJ.showMessage("Aborted!");


function processFolder(path) {
	var pattern = /(\d{8})\WSM(\d+)\W(-?\d+)%/;
	var pattern2 = /results_(\d+\w+)/;
	var folder = new File(path);
	var list = folder.list();
	for (var i = 0; i < list.length; i++) {
		if (pattern.test(list[i])) {
			var result = pattern.exec(list[i]);
			var folder2 = new File(path + list[i]);
			var list2 = folder2.list();
			for (var j = 0; j < list2.length; j++) {
				if (pattern2.test(list2[j])) {
					var result2 = pattern2.exec(list2[j]);
					var folder3 = new File(path + list[i] + "/" + list2[j]);
					if (folder3.isDirectory()) {
						var list3 = folder3.list();
						var images = new Array();
						for (var k = 0; k < list3.length; k++) {
							if (list3[k].indexOf(".jpg") >= 0) {
								images.push("../" + list[i] + "/" + list2[j] + "/" + list3[k]);
							}
						}
						found.push(new Data(result[2], result[3], result[1], result2[1], images[0]));
					}
				}
			}
		}
	}
}

function Data(specMag, qSinK7, date, paras, img) {
	this.specMag = specMag;
	this.qSinK7 = qSinK7;
	this.date = date;
	this.paras = paras;
	this.img = img;
}
