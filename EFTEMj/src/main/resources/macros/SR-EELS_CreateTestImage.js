/*
 * file:	SR-EELS_CreateTestImage.js
 * author:	Michael Entrup b. Epping (michael.entrup@wwu.de)
 * version:	20150217
 * info:	This is a draft script to create a test image for the SR-EELS correction.
 * 			It would be much better to ask the user to select a SR-EELS characterisation data set,
 * 			that is used for creating th test image.
 *
 * 			This script is part of the Fiji plugin EFTEMj (https://github.com/EFTEMj/EFTEMj).
 */

importClass(Packages.ij.IJ);
importClass(Packages.ij.ImagePlus);
importClass(Packages.ij.process.FloatProcessor);

var testImp = createImage("SR-EELS_TestImage");
drawCurves(testImp, 255);
testImp.show();

function createImage(title) {
	var width = 4096;
	var height = 4096;
	var imp = new ImagePlus(title, new FloatProcessor(width, height));
	return imp;
}

function drawCurves(imp, value) {
	var ps = 16;
	var processor = imp.getProcessor();
	for (var k = 1; k <= 6; k++) {
		for (var x = ps; x < imp.getWidth() - ps; x++) {
			var y = calcPoly(k, x);
			for (var i = -ps; i <= ps; i++) {
				for (var j = -ps; j <= ps; j++) {
					processor.setf(x + i, y + j, value);
				}
			}
			IJ.showProgress((k  - 1) * imp.getWidth() + (x + 1), 5 * imp.getWidth());
		}
	}
	IJ.showProgress(1.0);
}

/*
 * The data set '20140106 SM125 -20%' is used to create the test image.
 */
function calcPoly(num, x) {
	var y = 0;
	if (num == 1) {
		var a = 1924.65856;
		var b = 0.020126;
		var c = -5.79527e-6
		var d = 1.06154e-9;
		y = a + b * x + c * x*x + d * x*x*x;
	} else if (num == 2) {
		var a = 2934.15694;
		var b = -0.0348442;
		var c = -4.12822e-7;
		var d = 1.32445e-9;
		y = a + b * x + c * x*x + d * x*x*x;
	} else if (num == 3) {
		var a = 715.42725;
		var b = 0.094510;
		var c = -1.53099e-5
		var d = 1.20209e-9;
		y = a + b * x + c * x*x + d * x*x*x;
	} else if (num == 4) {
		var a = 2355.54082;
		var b = -0.0025618;
		var c = -3.84297e-6
		var d = 1.18869e-9;
		y = a + b * x + c * x*x + d * x*x*x;
	} else if (num == 5) {
		var a = 3332.97634;
		var b = -0.057912;
		var c = 1.66855e-6
		var d = 1.40348e-9;
		y = a + b * x + c * x*x + d * x*x*x;
	}
 else if (num == 6) {
		var a = 1099.98571;
		var b = 0.071886;
		var c = -1.38888e-5
		var d = 1.39438e-9;
		y = a + b * x + c * x*x + d * x*x*x;
	}
	return y;
}