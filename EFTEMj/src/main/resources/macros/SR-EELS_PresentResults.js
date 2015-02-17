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

importClass(Packages.ij.IJ);
importClass(Packages.ij.ImageStack);
importClass(Packages.ij.ImagePlus);
importClass(Packages.java.io.File);
importClass(Packages.java.io.FileReader);
importClass(Packages.java.io.FileWriter);
importClass(Packages.ij.plugin.MontageMaker);
importClass(Packages.ij.gui.GenericDialog);

/*
 * input - The folder where the data sets can be found.
 * output - The folder where to create the html overview.
 */
var input = "Q:/Aktuell/SR-EELS Calibration measurements/";
var output = "Q:/Aktuell/SR-EELS Calibration measurements/HTML-Overview/";

main();

function main() {
	if (!initialTests()) {
		IJ.showMessage("Aborted!");
		return;
	}
	/*
	 * Select the overviews that will be created.
	 * If both are selected the characterisation overview is created first.
	 */
	var dialog = new GenericDialog("Select Type");
	var characterisation = "Summarise characterisation results";
	var correction = "Summarise correction results";
	dialog.addCheckbox(characterisation, true);
	dialog.addCheckbox(correction, true);
	dialog.showDialog();
	if (dialog.wasCanceled()) return;
	var createCharacterisation = dialog.getNextBoolean();
	var createCorrection = dialog.getNextBoolean();

	if (!createCharacterisation & !createCorrection) {
		IJ.showMessage("Nothing selected.\nAborting.");
		return;
	}

	var foundDataSets = findDatasets(input, createCharacterisation, createCorrection);

	if (createCharacterisation) {
		IJ.log("Creating an overview of the characterisation results.");
		var characterisationWriter = new FileWriter(new File(output + "characterisation.html"));
		writeHead(characterisationWriter);
		var count = 0;
		for (var i = 0; i < foundDataSets.length; i++) {
			if (foundDataSets[i].type == 1) {
				count++;
				var divStart = "<div class=\"col-xs-12 col-sm-6 col-md-4 col-lg-3\ outer-tile SM" + foundDataSets[i].specMag + "\"><div class=\"inner-tile\">\n";
				characterisationWriter.append(divStart);
				var img = "<a href=\"" + foundDataSets[i].img + "\"><img src=\"" + foundDataSets[i].img + "\" class=\"img-responsive img-rounded\"></a>\n";
				characterisationWriter.append(img);
				var desc = "<div class=\"specs\"><ul><li>specMag = " + foundDataSets[i].specMag + "</li><li>QSinK7 = " + foundDataSets[i].qSinK7 + "</li><li>Date: " + foundDataSets[i].date + "</li><li>Comment: " + foundDataSets[i].comment + "</li><li>Parameters: " + foundDataSets[i].paras + "</li></ul></div>\n";
				characterisationWriter.append(desc);
				var divEnd = "</div></div>\n";
				characterisationWriter.append(divEnd);
			}
		}
		writeFoot(characterisationWriter);
		characterisationWriter.close();
		IJ.log(count + " data sets processed.");
	}	

	if (createCorrection) {
		IJ.log("Creating an overview of the correction results.");
		var correctionWriter = new FileWriter(new File(output + "correction.html"));
		writeHead(correctionWriter);
		var count = 0;
		for (var i = 0; i < foundDataSets.length; i++) {
			if (foundDataSets[i].type == 2) count++;
		}
		var fullCount = count;
		count = 0;
		for (var i = 0; i < foundDataSets.length; i++) {
			if (foundDataSets[i].type == 2) {
				count++;
				var montage = createMontage(foundDataSets[i].img);
				montage = montage.replace(input, "../");
				var divStart = "<div class=\"col-md-12 col-lg-6\ outer-tile SM" + foundDataSets[i].specMag + "\"><div class=\"inner-tile\">\n";
				correctionWriter.append(divStart);
				var img = "<a href=\"" + montage + "\"><img src=\"" + montage + "\" class=\"img-responsive img-rounded\"></a>\n";
				correctionWriter.append(img);
				var desc = "<div class=\"specs\"><ul><li>specMag = " + foundDataSets[i].specMag + "</li><li>QSinK7 = " + foundDataSets[i].qSinK7 + "</li><li>Date: " + foundDataSets[i].date + "</li><li>Comment: " + foundDataSets[i].comment + "</li><li>Parameters: " + foundDataSets[i].paras + "</li></ul></div>\n";
				correctionWriter.append(desc);
				var divEnd = "</div></div>\n";
				correctionWriter.append(divEnd);
				IJ.log("Processed " + count + "/" + fullCount);
			}
		}
		writeFoot(correctionWriter);
		correctionWriter.close();
		IJ.log(count + " data sets processed.");
	}
	IJ.showMessage("Finished!");
}

/*
 * Some tests to guaranty that all necessary folders and files are available.
 */
function initialTests() {
	if (!new File(input).isDirectory()) {
		exit("Please set the input path.");
		return false;
	}
	if (!new File(output).isDirectory()) {
		exit("Please set the output path.");
		return false;
	}
	if (!new File(output + "head.html").exists()) {
		IJ.showMessage("Please place the file 'head.html' at\n" + output);
		return false;
	}
	if (!new File(output + "foot.html").exists()) {
		IJ.showMessage("Please place the file 'foot.html' at\n" + output);
		return false;
	}
	return true;
}


function findDatasets(path, createCharacterisation, createCorrection) {
	var array = new Array();
	/*
	 * An example that will match the following RegEx pattern:
	 * 		"20150205 SM125 -30% B8_0,0625s"
	 * The first group is a date
	 * The second group is the number that follows "SM"
	 * The third group is the value of QSinK7 including the % and an optional sign
	 * The fourth group is optional (?:...)?. This is a non-grouping version of regular parentheses.
	 * It includes a group that contains the comment.
	 */
	var patternDataSet = /(\d{8})\sSM(\d+)\s([+|-]?\d+%)(?:\s([\w|,]+))?/;
	var patternCharacterisation = /results_(\d+\w+)/;
	var patternCorrection = /result_(\d+\w+).*/;
	var folder = new File(path);
	var list = folder.list();
	for (var i = 0; i < list.length; i++) {
		if (patternDataSet.test(list[i])) {
			var resultDataSet = patternDataSet.exec(list[i]);
			var folder2 = new File(path + list[i]);
			var list2 = folder2.list();
			for (var j = 0; j < list2.length; j++) {
				if (createCharacterisation & patternCharacterisation.test(list2[j])) {
					var resultCharacterisation = patternCharacterisation.exec(list2[j]);
					var folder3 = new File(path + list[i] + "/" + list2[j]);
					if (folder3.isDirectory()) {
						var list3 = folder3.list();
						var images = new Array();
						for (var k = 0; k < list3.length; k++) {
							if (list3[k].indexOf(".jpg") >= 0) {
								images.push("../" + list[i] + "/" + list2[j] + "/" + list3[k]);
							}
						}
						array.push(new Data(1, resultDataSet[2], resultDataSet[3], resultDataSet[1], resultDataSet[4], resultCharacterisation[1], images[0]));
					}
				}
				else if (createCorrection & patternCorrection.test(list2[j])) {
					var resultCorrection = patternCorrection.exec(list2[j]);
					var image = path + list[i] + "/" + list2[j];
					array.push(new Data(2, resultDataSet[2], resultDataSet[3], resultDataSet[1], resultDataSet[4], resultCorrection[1], image));
				}
			}
		}
	}
	return array;
}

function writeHead(writer, headReader) {	
	var headReader = new FileReader(new File(output + "head.html"));
	for (var c; ( c = headReader.read() ) != -1; ) {
		writer.append(c);
	}	
	headReader.close();
}

function writeFoot(writer, footReader) {	
	var footReader = new FileReader(new File(output + "foot.html"));
	for (var c; ( c = footReader.read() ) != -1; ) {
		writer.append(c);
	}
	footReader.close();
}

function createMontage(correctedImage) {
	var pattern1 = /(.*\/)[^\/]+/;
	var pattern2 = /result_(\d+\w+).*/;
	var projectionPath = pattern1.exec(correctedImage)[1] + "projection.tif";
	var filePath = pattern1.exec(correctedImage)[1] + "montage_" + pattern2.exec(correctedImage)[1] + ".png";
	var file = new File(filePath);
	if (!file.exists()) {
		var imp1 = IJ.openImage(projectionPath);
		var imp2 = IJ.openImage(correctedImage);
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

function Data(type, specMag, qSinK7, date, comment, paras, img) {
	this.type = type;
	this.specMag = specMag;
	this.qSinK7 = qSinK7;
	this.date = date;
	if (comment != null) this.comment = comment;
	else this.comment = "-";
	this.paras = paras;
	this.img = img;
}
