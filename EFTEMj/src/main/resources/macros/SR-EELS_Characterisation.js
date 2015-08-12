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
importClass(Packages.java.io.FileWriter);
importClass(Packages.java.io.BufferedWriter);
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

var debug = true;
if (debug) {
	main();
} else {
	try {
		main();
	} catch (e) {
		IJ.showMessage(e.name, e.message);
	}
}

function main() {
	var settings,
		images,
		results = {};

	settings = {
		path: "Q:\\Aktuell\\SR-EELS_characterisation\\",
		stepSize: 64,
		filterRadius: 8,
		energyBorderLow: 256,
		energyBorderHigh: 256,
		energyPosition: 0.5,
		sigmaWeight: 3,
		polynomialOrder: 3,
		showGui: true,
		useThresholding: true,
		threshold: "Li",
		verbose: true
	}
	
	settings.images = getImages(settings);
	results.settings = settings;
	results.timeStart = new Date();
	runCharacterisation(results);
	saveResults(results);
	results.timeStop = new Date();
	
	if (settings.verbose) IJ.log("Finished in " + Math.round((results.timeStop - results.timeStart) / 1000) + " seconds!");
}

 function getImages(settings) {
 	var images = new Array(),
 		folder = new File(settings.path),
 		fileList = folder.list();
 	images = getFilteredImages(settings, fileList);
 	return images;
 }

 function getFilteredImages(settings, fileListJava) {
	var fileList = new Array(),
		filteredList = new Array(),		
		i,
		gd,
		counter;
		
 	/*
 	 * converting fileList to a JavaScript array:
 	 */
	for (i in fileListJava) {
		fileList.push(fileListJava[i]);
	}
	fileList.sort();

	if (settings.verbose) {
		IJ.log("Files offered for selection:");
		IJ.log(fileList.join("; "));
	}
	if (settings.showGui == true) {
		gd = new GenericDialog("Select files");
	 	counter = 0;
		for (i = 0; i < fileList.length; i++) {
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
		for (i = 0; i < counter; i++) {
			if (gd.getNextBoolean()) {
				filteredList.push(fileList[i]);
			}
		}
	} else {
		/*
		 * don't show the Gui and select all tif files that are not explicitly excluded
		 */
		for (i = 0; i < fileList.length; i++) {
			if (new File(settings.path + fileList[i]).isFile()) {
				if (fileList[i].search(".tif") >= 0 & fileList[i].search("-exclude") < 0) {
					filteredList.push(fileList[i]);
				}
			}
		}
	}
	if (settings.verbose) {
		IJ.log("Files selected:");
		IJ.log(filteredList.join("; "));
	}
	return filteredList
 }

function runCharacterisation(results) {
	var settings = results.settings,
		images = settings.images,
		imageName,
		i,
		j,
		p,
		image,
		imp,
		subImage,
		yPos,
		xOffset,
		roiWidth,
		mean,
		profile,
		xValues,
		yValues,
		leftValues,
		rightValues,
		widthValues,
		limit,
		plot,
		fit,
		gaussCentre,
		gaussSigma,
		gaussSigmaWeighted,
		result;
	for (i = 0; i < images.length; i++){
		imageName = images[i];
		results[imageName] = {};
		results[imageName].result = new Array();
		image = new ImageObject(images[i], settings);
		imp = image.imp;
		results[imageName].width = imp.getWidth();
		results[imageName].height = imp.getHeight();
		yPos = settings.energyBorderLow;
		xOffset = 0;
		roiWidth = image.width;
		mean = new Array();
		while (yPos < image.height - settings.energyBorderHigh) {
			imp.setRoi(new Rectangle(xOffset, yPos, roiWidth, settings.stepSize));
			subImage = new SubImageObject(new Duplicator().run(imp), xOffset, yPos);
			profile = new ProfilePlot(subImage.imp);
			xValues = new Array();
			for (p = 0; p < profile.getProfile().length; p++) {
				xValues.push(p);
			}
			fit = new CurveFitter(xValues, profile.getProfile());
			fit.doFit(CurveFitter.GAUSSIAN);
			gaussCentre = fit.getParams()[2];
			gaussSigma =  fit.getParams()[3];
			gaussSigmaWeighted = settings.sigmaWeight * gaussSigma / Math.pow(fit.getRSquared(), 2);
			xOffset = Math.max(xOffset + Math.round(gaussCentre - gaussSigmaWeighted), 0);
			roiWidth = Math.round(2 * gaussSigmaWeighted);
			imp.setRoi(new Rectangle(xOffset, yPos, roiWidth, settings.stepSize));
			subImage = new SubImageObject(new Duplicator().run(imp), xOffset, yPos);
			subImage.parent = image;
			subImage.xOffset = xOffset;
			subImage.yOffset = yPos;
			subImage.threshold = settings.threshold;
			results[imageName].result.push(runCharacterisationSub(subImage, settings.useThresholding));
			yPos += settings.stepSize;
		}
		result = results[imageName].result;
		xValues = new Array();
		yValues = new Array();
		leftValues = new Array();
		rightValues = new Array();
		widthValues = new Array();
		limit = new Array();
		for (j in result) {
			xValues.push(result[j].y);
			yValues.push(result[j].x);
			leftValues.push(result[j].left);
			rightValues.push(result[j].right);
			widthValues.push(result[j].right - result[j].left);
			limit.push(result[j].limit);
		}
		plot = new Plot("JavaScript", "Spec of " +imageName + " (" + settings.useThresholding + ")", "position x", "position y", xValues, yValues);
		plot.add("", xValues, leftValues);
		plot.add("", xValues, rightValues);
		if (!settings.useThresholding) plot.add("CROSS", xValues, limit);
		plot.setColor(java.awt.Color.RED);
		plot.add("", xValues, widthValues);
		plot.setLimits(0, imp.getHeight() - 1, 0, imp.getWidth() - 1);
		plot.show();
		result.leftFit = new CurveFitter(xValues, leftValues);
		result.leftFit.doFit(CurveFitter.POLY3);
		result.centreFit = new CurveFitter(xValues, yValues);
		result.centreFit.doFit(CurveFitter.POLY3);
		result.rightFit = new CurveFitter(xValues, rightValues);
		result.rightFit.doFit(CurveFitter.POLY3);
	}
}

function runCharacterisationSub(subImage, doThresholding) {
	var stepSize = subImage.imp.getHeight(),
		subImp = subImage.imp,
		measurements,
		statistic,
		mean,
		specWidth,
		xm,
		ym,
		roi,
		xLeft,
		xRight,
		limit,
		left,
		right,
		searchLeft,
		i;
	if (doThresholding == true) {
		IJ.setAutoThreshold(subImp, subImage.threshold + " dark");
		IJ.run(subImp, "NaN Background", "");
		measurements = Measurements.MEAN + Measurements.INTEGRATED_DENSITY + Measurements.CENTER_OF_MASS;
		statistic = ImageStatistics.getStatistics(subImp.getProcessor(), measurements, null);
		mean = statistic.mean;
		specWidth = statistic.area / subImp.getHeight();
		xm = statistic.xCenterOfMass;
		ym = statistic.yCenterOfMass;
		IJ.run(subImp, "Macro...", "code=[if(isNaN(v)) v=-10000;]");
		IJ.run(subImp, "Find Edges", "");
		subImp.setRoi(new Rectangle(Math.max(xm - specWidth, 0), 0, xm - Math.max(xm - specWidth, 0), subImp.getHeight()));
		IJ.run(subImp, "Find Maxima...", "output=[Point Selection] exclude");
		roi = subImp.getRoi().getBounds();
		xLeft = roi.x;
		subImp.setRoi(new Rectangle(xm, 0, subImp.getWidth() - Math.max(xm - specWidth, 0), subImp.getHeight()));
		IJ.run(subImp, "Find Maxima...", "output=[Point Selection] exclude");
		roi = subImp.getRoi().getBounds();
		xRight = roi.x + roi.width;
		resultSub = {
			x: xm + subImage.xOffset,
			xError: 0,
			y: ym + subImage.yOffset,
			yError: 0,
			left: xLeft + subImage.xOffset,
			leftError: 0,
			right: xRight + subImage.xOffset,
			rightError: 0,
			width: xRight - xLeft,
			widthError: 0
		}
	} else {
		IJ.run(subImp, "Bin...", "x=1 y=" + subImp.getHeight() + " bin=Average");
		statistic = ImageStatistics.getStatistics(subImp.getProcessor(), Measurements.MEAN + Measurements.STD_DEV, null);
		limit = 40; //statistic.mean; // statistic.stdDev
		left = 0;
		right = subImp.getWidth();
		searchLeft = true;
		for (i = 0; i < subImp.getWidth(); i++) {
			if (searchLeft == true) {
				if (subImp.getProcessor().getf(i, 0) > limit) {
					left = i;
					searchLeft = false;
				}
			} else {
				if (subImp.getProcessor().getf(i, 0) < limit) {
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
			width: right - left,
			widthError: 0,
			limit: limit
		}
	}
	return resultSub;
}

function saveResults(results) {
	// create different diagrams with createDiagram()
	// create differnt text files containing numeric results using createTextFile()
	var images = results.settings.images,
		settings = results.settings,
		widthFile = new TextFile(results.settings.path + "Width.txt", settings.verbose),
		bordersFile = new TextFile(results.settings.path + "Borders.txt", settings.verbose),		
		i,
		j,
		result,
		width,
		fitLeft,
		fitRight,
		fitCentre,
		widthLine,
		bordersLine;
	widthFile.appendLine("#x1-position\tx2-position\ty-values");
	bordersFile.appendLine("#x1-value\tx2-value\ty-value\tweight");
	for (i = 0; i < images.length; i++){
		result = results[images[i]].result;
		width =  results[images[i]].width;
		fitLeft = results[images[i]].result.leftFit.f(settings.energyPosition * width);
		fitCentre = results[images[i]].result.centreFit.f(settings.energyPosition * width);
		fitRight = results[images[i]].result.rightFit.f(settings.energyPosition * width);
		for (j = 0; j < result.length; j++) {
			widthLine = result[j].y + "\t" + result[j].x + "\t" + result[j].width;
			bordersLines = result[j].y + "\t" + fitLeft + "\t" + result[j].left + "\t" + 1 + "\n"
				+ result[j].y + "\t" + fitCentre + "\t" + result[j].x + "\t" + 1 + "\n"
				+ result[j].y + "\t" + fitRight + "\t" + result[j].right + "\t" + 1;
			widthFile.appendLine(widthLine);
			bordersFile.appendLine(bordersLines);
		}
	}
	widthFile.closeFile();
	bordersFile.closeFile();
}

function createDiagram(processedResult) {
	var diagram;
	// use the data in cluded in processedResult to create a diagram
	return diagram;
}

function TextFile(path, verbose) {
	this.file = new File(path);
	this.writer = new BufferedWriter(new FileWriter(this.file));
	this.verbose = verbose;
	if (this.verbose) {
		IJ.log("Writing results to " + path);
	}
	this.appendLine = function (line) {
		this.writer.write(line + "\n");
	};
	this.closeFile = function () {
		this.writer.close();
		if (this.verbose) {
			IJ.log("Finished writing results to " + path);
		}
	};
	// write all arrays from values to a new text file
}

function ImageObject(imageName, settings) {
	var filterRadius = settings.filterRadius,
		threshold;
	this.name = imageName;
	this.path = settings.path + imageName;
	this.imp = IJ.openImage(this.path);
	IJ.run(this.imp, "Rotate 90 Degrees Right", "");
	IJ.run(this.imp, "Flip Horizontally", "");
	this.width = this.imp.getWidth();
	this.height = this.imp.getHeight();
	threshold = 2 * this.imp.getStatistics.stdv;
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
