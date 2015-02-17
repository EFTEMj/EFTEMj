/*
 * file:	SR-EELS_CreateCorrectionOverview.ijm
 * author:	Michael Entrup b. Epping (michael.entrup@wwu.de)
 * version:	20150126
 * info:	This script generates a HTML file to show the results of the SR-EELS correction (of projection.tif).
 * 			It shows the uncorrected image side by side with the corrected image..
 * 			'head.html' and 'foot.html' must be present at the output directory.
 *
 * 			This scrfipt is part of the Fiji plugin EFTEMj (https://github.com/EFTEMj/EFTEMj).
 */

importClass(Packages.ij.IJ);
importClass(Packages.ij.ImageStack);
importClass(Packages.ij.ImagePlus);
importClass(Packages.java.io.File);
importClass(Packages.java.io.FileReader);
importClass(Packages.java.io.FileWriter);
importClass(Packages.ij.plugin.MontageMaker);

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

	IJ.log(found.length + " results found.");
	var headReader = new FileReader(new File(output + "head.html"));
	var footReader = new FileReader(new File(output + "foot.html"));

	var indexWriter = new FileWriter(new File(output + "index2.html"));

	for (var c; ( c = headReader.read() ) != -1; ) {
		indexWriter.append(c);
	}

	for (var i = 0; i < found.length; i++) {
		var montage = createMontage(found[i].result);
		montage = montage.replace(input, "../");
		var divStart = "<div class=\"col-md-12 col-lg-6\ outer-tile SM" + found[i].specMag + "\"><div class=\"inner-tile\">\n";
		indexWriter.append(divStart);
		var img = "<a href=\"" + montage + "\"><img src=\"" + montage + "\" class=\"img-responsive img-rounded\"></a>\n";
		indexWriter.append(img);
		var desc = "<div class=\"specs\"><ul><li>specMag = " + found[i].specMag + "</li><li>QSinK7 = " + found[i].qSinK7 + "%</li><li>Date: " + found[i].date + "</li><li>Parameters: " + found[i].paras + "</li></ul></div>\n";
		indexWriter.append(desc);
		var divEnd = "</div></div>\n";
		indexWriter.append(divEnd);
		IJ.log("Processed " + (i + 1) + "/" + found.length);
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
	var pattern2 = /result_(\d+\w+).*/;
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
					var image = path + list[i] + "/" + list2[j];
					found.push(new Data(result[2], result[3], result[1], result2[1], image));
				}
			}
		}
	}
}

function Data(specMag, qSinK7, date, paras, result) {
	this.specMag = specMag;
	this.qSinK7 = qSinK7;
	this.date = date;
	this.paras = paras;
	this.result = result;
}

function createMontage(resultPath) {
	var pattern1 = /(.*\/)[^\/]+/;
	var pattern2 = /result_(\d+\w+).*/;
	var projectionPath = pattern1.exec(resultPath)[1] + "projection.tif";
	var filePath = pattern1.exec(resultPath)[1] + "montage_" + pattern2.exec(resultPath)[1] + ".png";
	var file = new File(filePath);
	if (!file.exists()) {
		var imp1 = IJ.openImage(projectionPath);
		var imp2 = IJ.openImage(resultPath);
		var stack = new ImageStack(imp1.getWidth(), imp1.getHeight());
		stack.addSlice(imp1.getProcessor());
		stack.setSliceLabel("Projection", 1);
		stack.addSlice(imp2.getProcessor());
		stack.setSliceLabel("Correction", 2);
		var imp = new ImagePlus("Stack", stack);
		IJ.run(imp, "Log", "stack");
		//IJ.run("Brightness/Contrast...");
		IJ.run(imp, "Enhance Contrast", "saturated=0.35");
		var border = 16;
		var font = 32;
		var height = imp.getHeight();
		var scale = 1;
		if (height > 1024 & height <= 2048) scale = 0.5;
		else if (height > 2048) scale = 0.25;
		var mm = new MontageMaker();
		var montage = mm.makeMontage2(imp, 2, 1, scale, 1, 2, 1, border, true);
		//IJ.run(imp, "Make Montage...", "columns=2 rows=1 scale=" + scale + " first=1 last=2 increment=1 border=" + border + " font=" + font + " label");
		imp.changes = false; // verhinder, das IJ 'Save changes to "Stack"?' fragt.
		imp.close();
		//var montage = IJ.getImage();
		IJ.saveAs(montage, "png", filePath);
		montage.close();
	}
	return filePath;
}