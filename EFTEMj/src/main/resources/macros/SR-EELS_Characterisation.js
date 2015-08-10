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
importClass(Packages.ij.process.ImageStatistics);
importClass(Packages.java.lang.Runnable);
importClass(Packages.java.lang.Runtime);
importClass(Packages.java.lang.System);
importClass(Packages.java.lang.Thread);
importClass(Packages.java.util.concurrent.atomic.AtomicInteger);
importClass(Packages.java.io.File);
importClass(Packages.java.awt.Rectangle);
importClass(Packages.ij.io.DirectoryChooser);
importClass(Packages.ij.gui.GenericDialog);
importClass(Packages.ij.gui.Plot);
importClass(Packages.ij.gui.ProfilePlot);
importClass(Packages.ij.plugin.Duplicator);
importClass(Packages.ij.measure.CurveFitter);
importClass(Packages.ij.measure.Measurements);
importClass(Packages.ij.Prefs);
importClass(Packages.sr_eels.SR_EELS_PrefsKeys);

main();

function main() {
	var settings = {
		path: "Q:\\Aktuell\\SR-EELS_characterisation\\",
		stepSize: 128,
		energyBorderLow: 128,
		energyBorderHigh: 256,
		energyPosition: 0.5,
		threshold: "Li",
		sigmaWeight: 3,
		polynomialOrder: 3,
		useThresholding: true,
		debug: true
	}
	settings.filterRadius = Math.sqrt(settings.stepSize);

	try {
		var images = getImages(settings);
	} catch (e) {
		IJ.showMessage(e.name, e.message);
	}
	IJ.log(images.join("\n"));
	var results = runCharacterisation(settings, images);
	IJ.log(results);
	IJ.log("Finished!");
}

 function getImages(settings) {
 	var images = new Array();
 	var folder = new File(settings.path);
 	var fileList = folder.list();
 	images = getFilteredImages(settings, fileList); // getFilteredImages(settings, fileList, true) for GUI
 	return images;
 }

 function getFilteredImages(settings, fileList, gui) {
	var filtered = new Array();
	if (gui == true) {
		var gd = GenericDialog("Select files");
	 	var counter = 0;
		for (var i = 0; i < fileList.length; i++) {
			if (new File(settings.path + fileList[i]).isFile()) {
				counter++;
				if (fileList[i].search(".tif") >= 0 & fileList[i].search("-exclude") < 0) {
					gd.addCheckbox(fileList[i], true);
				} else {
					gd.addCheckbox(fileList[i], false);
				}
			}
		}
		if (counter < 1) {
			throw new Error("No files...", "There are no files to load in\n" + path);
		}
		gd.showDialog();
		if (gd.wasCanceled()) {
			throw new Error("Canceled by user...", "The script has been canceld.");
		}
		for (var i = 0; i < counter; i++) {
			if (gd.getNextBoolean()) {
				filtered.push(fileList[i]);
			}
		}
	} else {
		for (var i = 0; i < fileList.length; i++) {
			if (new File(settings.path + fileList[i]).isFile()) {
				if (fileList[i].search(".tif") >= 0 & fileList[i].search("-exclude") < 0) {
					filtered.push(fileList[i]);
				}
			}
		}
	}
	return filtered
 }

function runCharacterisation(settings, images) {
	var results = {};
	for (var i = 0; i < images.length; i++){
		results[images[i]] = {};
		results[images[i]].result = new Array();
		var image = new ImageObject(settings.path + images[i], settings.filterRadius);
		yPos = settings.energyBorderLow;
		xOffset = 0;
		roiWidth = image.width;
		var mean = new Array();
		while (yPos < image.height - settings.energyBorderHigh) {
			image.imp.setRoi(new Rectangle(xOffset, yPos, roiWidth, settings.stepSize));
			var subImage = new SubImageObject(new Duplicator().run(image.imp), xOffset, yPos);
			var profile = new ProfilePlot(subImage.imp);
			var xValues = new Array();
			for (var p = 0; p < profile.getProfile().length; p++) {
				xValues.push(p);
			}
			var fit = new CurveFitter(xValues, profile.getProfile());
			fit.doFit(CurveFitter.GAUSSIAN);
			var gaussCentre = fit.getParams()[2];
			var gaussSigma =  fit.getParams()[3];
			var gaussSigmaWeighted = settings.sigmaWeight * gaussSigma / Math.pow(fit.getRSquared(), 2);
			xOffset = Math.max(xOffset + Math.round(gaussCentre - gaussSigmaWeighted), 0);
			roiWidth = Math.round(2 * gaussSigmaWeighted);
			image.imp.setRoi(new Rectangle(xOffset, yPos, roiWidth, settings.stepSize));
			subImage = new SubImageObject(new Duplicator().run(image.imp), xOffset, yPos);
			subImage.parent = image;
			subImage.xOffset = xOffset;
			subImage.yOffset = yPos;
			subImage.threshold = settings.threshold;
			results[images[i]].result.push(runCharacterisationSub(subImage, settings.useThresholding));
			yPos += settings.stepSize;
		}
		var res = results[images[i]].result;
		var xValues = new Array();
		var yValues = new Array();
		var leftValues = new Array();
		var rightValues = new Array();
		var widthValues = new Array();
		var limit = new Array();
		for (var j in res) {
			xValues.push(res[j].y);
			yValues.push(res[j].x);
			leftValues.push(res[j].left);
			rightValues.push(res[j].right);
			widthValues.push(res[j].right - res[j].left);
			limit.push(res[j].limit);
		}
		var plot = new Plot("JavaScript", "Spec of " + images[i] + " (" + settings.useThresholding + ")", "position x", "position y", xValues, yValues);
		plot.add("", xValues, leftValues);
		plot.add("", xValues, rightValues);
		if (!settings.useThresholding) plot.add("CROSS", xValues, limit);
		plot.setColor(java.awt.Color.RED);
		plot.add("", xValues, widthValues);
		plot.setLimits(0, image.imp.getHeight() - 1, 0, image.imp.getWidth() - 1);
		plot.show();
	}
	return results;
}

function runCharacterisationSub(subImage, doThresholding) {
	var stepSize = subImage.imp.getHeight();
	if (doThresholding == true) {
		IJ.setAutoThreshold(subImage.imp, subImage.threshold + " dark");
		IJ.run(subImage.imp, "NaN Background", "");
		var measurements = Measurements.MEAN + Measurements.INTEGRATED_DENSITY + Measurements.CENTER_OF_MASS;
		var statistic = ImageStatistics.getStatistics(subImage.imp.getProcessor(), measurements, null);
		var mean = statistic.mean;
		var specWidth = statistic.area / subImage.imp.getHeight();
		var xm = statistic.xCenterOfMass;
		var ym = statistic.yCenterOfMass;
		IJ.run(subImage.imp, "Macro...", "code=[if(isNaN(v)) v=-10000;]");
		IJ.run(subImage.imp, "Find Edges", "");
		subImage.imp.setRoi(new Rectangle(Math.max(xm - specWidth, 0), 0, xm - Math.max(xm - specWidth, 0), subImage.imp.getHeight()));
		IJ.run(subImage.imp, "Find Maxima...", "output=[Point Selection] exclude");
		var roi = subImage.imp.getRoi().getBounds();
		var xLeft = roi.x;
		subImage.imp.setRoi(new Rectangle(xm, 0, subImage.imp.getWidth() - Math.max(xm - specWidth, 0), subImage.imp.getHeight()));
		IJ.run(subImage.imp, "Find Maxima...", "output=[Point Selection] exclude");
		var roi = subImage.imp.getRoi().getBounds();
		var xRight = roi.x + roi.width;
		resultSub = {
			x: xm + subImage.xOffset,
			xError: 0,
			y: ym + subImage.yOffset,
			yError: 0,
			left: xLeft + subImage.xOffset,
			leftError: 0,
			right: xRight + subImage.xOffset,
			rightError: 0
		}
	} else {
		IJ.run(subImage.imp, "Bin...", "x=1 y=" + subImage.imp.getHeight() + " bin=Average");
		var statistic = ImageStatistics.getStatistics(subImage.imp.getProcessor(), Measurements.MEAN + Measurements.STD_DEV, null);
		var limit = statistic.mean; // statistic.stdDev
		var left = 0;
		var right = subImage.imp.getWidth();
		var searchLeft = true;
		for (var i = 0; i < subImage.imp.getWidth(); i++) {
			if (searchLeft == true) {
				if (subImage.imp.getProcessor().getf(i, 0) > limit) {
					left = i;
					searchLeft = false;
				}
			} else {
				if (subImage.imp.getProcessor().getf(i, 0) < limit) {
					right = i;
					break;
				}
			}
		}
		resultSub = {
			x: (left + right) / 2 + subImage.xOffset,
			xError: 0,
			y: stepSize / 2 + subImage.yOffset,
			yError: 0,
			left: left + subImage.xOffset,
			leftError: 0,
			right: right + subImage.xOffset,
			rightError: 0,
			limit: limit
		}
		IJ.log(resultSub.y + "; " + (resultSub.right - resultSub.left));
	}
	return resultSub;
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

function ImageObject(path, filterRadius) {
	this.path = path;
	this.imp = IJ.openImage(path);
	IJ.run(this.imp, "Rotate 90 Degrees Right", "");
	IJ.run(this.imp, "Flip Horizontally", "");
	this.width = this.imp.getWidth();
	this.height = this.imp.getHeight();
	var threshold = 2 * this.imp.getStatistics.stdv;
	IJ.run(this.imp, "Remove Outliers...", "radius=" + filterRadius + " threshold=" + threshold + " which=Bright");
	IJ.run(this.imp, "Remove Outliers...", "radius=" + filterRadius + " threshold=" + threshold + " which=Dark");
	IJ.run(this.imp, "Median...", "radius=" + filterRadius);
}

function SubImageObject(imp, xPos, yPos) {
	this.imp = imp;
	this.imp.setRoi(new Rectangle(0, 0, this.imp.getWidth(), this.imp.getHeight()));
	this.xPos = xPos;
	this.yPos = yPos;
}
