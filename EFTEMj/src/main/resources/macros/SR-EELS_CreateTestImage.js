/*
 * file:	SR-EELS_CreateTestImage.js
 * author:	Michael Entrup b. Epping (michael.entrup@wwu.de)
 * version:	20150211
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
	for (var k = 1; k <= 5; k++) {
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

function calcPoly(num, x) {
	var y = 0;
	if (num == 1) {
		var a = 2041;
		var b = 0.01711;
		var c = -4.405e-6
		var d = 8.638e-10;
		y = a + b * x + c * x*x + d * x*x*x;
	} else if (num == 2) {
		var a = 467.2;
		var b = 0.06752;
		var c = -8.871e-6;
		var d = 9.837e-10;
		y = a + b * x + c * x*x + d * x*x*x;
	} else if (num == 3) {
		var a = 1171;
		var b = 0.04418;
		var c = -6.624e-6
		var d = 8.909e-10;
		y = a + b * x + c * x*x + d * x*x*x;
	} else if (num == 4) {
		var a = 3676;
		var b = -0.02785;
		var c = -2.854e-6
		var d = 1.191e-9;
		y = a + b * x + c * x*x + d * x*x*x;
	} else if (num == 5) {
		var a = 2892;
		var b = 0.005692;
		var c = -3.815e-6
		var d = 1.081e-9;
		y = a + b * x + c * x*x + d * x*x*x;
	}
	return y;
}