/*
 * file:	Custom_FFT_filter.ijm
 * author:	Michael Entrup b. Epping (michael.entrup@wwu.de)
 * version:	20150609
 * info:	This macro can apply a custom FFT filter to an image.
 * 			The user selects diffraction spots that are include in a mask.
 * 			The inverse FFT only uses the signal defined by the mask.
 */

input = getImageID();
run("FFT");
fft = getImageID();
run("Enhance Contrast", "saturated=0.35");
roiManager("Show All");
setTool("point");
waitForUser("Please select diffration spots\n" +
			"and add them by using [Ctrl] + [T].\n" +
			"Click OK to continue.");

setBatchMode(true);
count = roiManager("count");
diameter = getNumber("Diameter of the selection:", 100);

for(i=0;i<count;i++) {
	roiManager("select", i);
	Roi.getBounds(x, y, width, height);
	setKeyDown("shift");
	makeOval(x - diameter/2, y - diameter/2, diameter, diameter);
	roiManager("Add");
}

roiManager("Deselect");
for(i=0;i<count;i++) {
	roiManager("select", 0);
	roiManager("Delete");
}

selection = newArray(roiManager("count"));
for(i=0;i<roiManager("count");i++){
	selection[i] = i;
}
roiManager("Select", selection);
roiManager("Combine");
run("Create Mask");
maskName = getTitle();
mask = getImageID()
selectImage(fft);
close();
selectImage(input);
run("Custom Filter...", "filter=" + maskName);
selectImage(mask);
close();
selectWindow("ROI Manager");
run("Close");
setBatchMode(false);