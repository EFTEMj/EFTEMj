/*
 * file:	SR-EELS_characterisation.ijm
 * author:	Michael Entrup (entrup@arcor.de)
 * version:	20140409
 * date:	09.04.2014
 * info:	This macro is used to characterise the  aberrations of an Zeiss in-column energy filter when using SR-EELS. A series of calibration datasets is necessary  to run the caracterisation. Place all datasets (images) at a folder that contains no other images.
 * You can find an example SR-EELS series at https://github.com/EFTEMj/EFTEMj/tree/master/Scripts+Macros/examples/SR-EELS_characterisation. There you will find a instruction on how to record such a series, too. 
 */

/*
 * Parameters:
 * Only this values should be changed by the user.
 * 
 * ToDo: create a dialog for parameter input.
 */
 /*
  *  step_size determines the number of energy channels that will result in one datapoint of the resulting dataset.
  */ 
step_size = -1;		// choose '-1' for automatic mode 'abs(height / 64)'
/*
 * border_top and border_bot are used to limit the energy range that is used. This is usefull to cut off the ZLP or to ignore low counts at high energy losses.
 */
border_top = -1;	// choose '-1' for automatic mode 'abs(height / 16)'
border_bot = -1;	// choose '-1' for automatic mode 'abs(height / 16)'
/*
 * This value is used when applying differnt filters, like remove outliers or median.
 */
filter_radius = -1;	// choose '-1' for automatic mode 'sqrt(step_size)'
/*
 * The macro will create a diagram that plots the spectrum width against the spectrum position. This value determines the energy channel that is used to calculate the spectrum width.
 * 
 * ToDo: implement a function to calibrate the energy scale. Then it is possible to select an energy loss instead of a relative value.
 */
energy_pos = 0.5;	// choose a value between 0 and 1; 0.5 is the centre of the SR-EELS dataset

/*
 * Load images:
 * This macro requires at least two images (SR-EELS datasets) to run properly. You have to select a folder and ImageJ will load all tif and dm3 images that are stored at the selected folder. Subfolders are not considered.
 */
dir = getDirectory("Choose a Directory ");
if (dir == "") exit();	// if cancelwas selected, the script will stop
list = getFileList(dir);
list = filter_images(list);	// only select tif and dm3 files; ignore subfolders

open(list[0]);
id = getImageID();	// id will be used to close the image
draw_axes_as_overlay(); 	// create an overlay to simplify the next user choice
doRotate = getBoolean("The macro requires the following configuration:\nx: lateral axis\ny: energy axis\n\nRotate the images?");
run("Remove Overlay");
updateResults(); // show the results table

// setup
start = getTime();
// Batch mode will speed up the macro
setBatchMode(true);
selectImage(id);
if (!doRotate) {
	width = getWidth;
	height = getHeight;
} else {
	height = getWidth;
	width = getHeight;
}
close();
if (step_size == -1) {
	step_size = abs(height / 64);
}
if (border_top == -1) {
	border_top = abs(height / 16);
}
if (border_bot == -1) {
	border_bot = abs(height / 16);
}
if (filter_radius == -1) {
	filter_radius = sqrt(step_size);
}
datapoints = floor(height - border_top - border_bot) / step_size;
array_index = newArray(datapoints);
array_pos_y = newArray(datapoints);
array_pos_x = newArray(datapoints);
array_left = newArray(datapoints);
array_right = newArray(datapoints);
array_width = newArray(datapoints);
array_width_calc = newArray(datapoints);
// global variables for use in 'save_pos_and_width()'
var array_pos_all; var array_width_all; var array_pos_all_calc; var array_width_all_calc;
// create a folder that will be used to save the results
sub_dir = dir + "results" + File.separator;
File.makeDirectory(sub_dir);
if (!File.exists(sub_dir)) {
	exit("Unable to create directory");
}

for (i=0; i<list.length; i++) {    
	open(list[i]);
	img_name = File.nameWithoutExtension;
	// id will be used to close the image
	id = getImageID();
	if (doRotate) {
		run("Rotate 90 Degrees Right");
	}
	// use pixel as coordinates
	run("Properties...", "unit=px pixel_width=1 pixel_height=1 origin=0,0");
	// we need an empty results table
	run("Clear Results");
	// remove the outliners and smooth
	run("Remove Outliers...", "radius=" + filter_radius + " threshold=32 which=Bright");
	run("Remove Outliers...", "radius=" + filter_radius + " threshold=32 which=Dark");
	run("Median...", "radius=" + filter_radius);
	// measure the mean value, the centre of mass und the integrated intensity
	run("Set Measurements...", "  mean center integrated redirect=None decimal=3");
	y_pos = 0;
	while (y_pos < getHeight - border_top - border_bot) {
		makeRectangle(0, y_pos + border_top, getWidth, step_size);
		// create a temporary image: only the rectange selection gets duplicated
		run("Duplicate...", "temp");
		// threshold with set background to NaN
		setAutoThreshold("Li dark");
		run("NaN Background");
		run("Measure");
		// line number at the result table
		result_pos = y_pos / step_size;
		array_index[result_pos] = result_pos;
		// the mean width of all energy channels
		spec_width = getResult("IntDen", result_pos) / getResult("Mean", result_pos) / step_size;
		array_width[result_pos] = spec_width;
		// the centre of mass in x-direction
		XM = getResult("XM", result_pos);
		array_pos_x[result_pos] = XM;
		// the centre of mass in y-direction
		// we need to add 'y_pos', because the measurement is done on a croped image with the height 'step_size'
		YM = getResult("YM", result_pos) + y_pos + border_top;
		array_pos_y[result_pos] = YM;
		// detect left and right border
		// replace NaN by '-1000'; this will result in the highest slope at the spectrum border
		run("Macro...", "code=[if(isNaN(v)) v=-1000;]");
		run("Find Edges");
		run("Bin...", "x=1 y=" + step_size + " bin=Average");
		// left border
		makeRectangle(maxOf(XM - spec_width, 1), 0, XM - maxOf(XM - spec_width, 1), step_size);
		getMinAndMax(min, max);
		run("Find Maxima...", "output=[Point Selection]");
		getSelectionBounds(x, y, w, h);
		array_left[result_pos] = x;
		run("Select None");
		// right border
		makeRectangle(XM, 0, width - maxOf(XM - spec_width, 1), step_size);			
		getMinAndMax(min, max);
		run("Find Maxima...", "output=[Point Selection]");
		getSelectionBounds(x, y, w, h);
		array_right[result_pos] = x + w;
		// calculate the width by using the borders positions
		array_width_calc[result_pos] = array_right[result_pos] - array_left[result_pos];
		// close the temporary image
		close();
		if (abs(YM - energy_pos * height) <= step_size  / 2) {
			save_pos_and_width(i, array_pos_x[result_pos], array_width[result_pos], array_left[result_pos], array_right[result_pos]);
		}
		y_pos += step_size;
	}
	// select and close the image
	selectImage(id);
	close();		
	// plot and save the results
	func = "y = a*pow(x,2) + b*x + c";
	// spectrum width
	Fit.doFit(func, array_pos_y, array_width);
	Fit.plot;
	saveAs("PNG", sub_dir + "width_" + img_name + ".png");
	close();
	// position of spectrum center
	Fit.doFit(func, array_pos_y, array_pos_x);
	Fit.plot;
	saveAs("PNG", sub_dir + "center_" + img_name + ".png");
	close();
	// position of left border
	Fit.doFit(func, array_pos_y, array_left);	
	Fit.plot;
	saveAs("PNG", sub_dir + "left_" + img_name + ".png");
	close();
	// position of right border
	Fit.doFit(func, array_pos_y, array_right);	
	Fit.plot;
	saveAs("PNG", sub_dir + "right_" + img_name + ".png");
	close();
	Array.show("Values", array_index, array_pos_y, array_pos_x, array_left, array_right, array_width, array_width_calc);
	if (isOpen("Values")) {
		selectWindow("Values");
		saveAs("Results", sub_dir + "values_" + img_name + ".txt");
		run("Close");
	}
}
// select and close the result window
if (isOpen("Results")) {
	selectWindow("Results");
	run("Close");
}
showMessage("<html><p>The evaluation finished.</p><p>Elapsed time: " + (getTime() - start) / 1000 + "s</p>");

/*
 * function: filter_images
 * description: keep tif and dm3 files only; ignore subdirectories
 */
function filter_images(array_str) {
	temp = -1;
	for (i=0; i<list.length; i++) {
		path = dir + list[i];
		if (!endsWith(path,"/") && (endsWith(path,".tif") || endsWith(path,".dm3"))) {
			 if (temp == -1) {
			 	temp = newArray(1);
			 	temp[0] = path;
			 } else {
			 	temp = Array.concat(temp, path); 
			 }
		}	
	}
	return temp;
}

function save_pos_and_width(index, pos, width, left, right) {
	if (index == 0) {		
		array_pos_all = newArray(list.length);
		array_width_all = newArray(list.length);
		array_pos_all_calc = newArray(list.length);
		array_width_all_calc = newArray(list.length);
	}
	array_pos_all[index] =  pos;
	array_width_all[index] =  width;
	array_pos_all_calc[index] = left + (right - left) / 2;
	array_width_all_calc[index] = right - left;
	
	if (index == list.length - 1) {
		func = "y = a*pow(x,2) + b*x + c";
		Fit.doFit(func, array_pos_all, array_width_all);
		Fit.plot;
		saveAs("PNG", sub_dir + "width_vs_pos.png");
		close();
		Fit.doFit(func, array_pos_all_calc, array_width_all_calc);
		Fit.plot;
		saveAs("PNG", sub_dir + "width_vs_pos_calc.png");
		close();
		Array.show("Values", array_pos_all, array_width_all, array_pos_all_calc, array_width_all_calc);
		if (isOpen("Values")) {
			selectWindow("Values");
			saveAs("Results", sub_dir + "width_vs_pos.txt");
			run("Close");
		}		
	}
}

function draw_axes_as_overlay() {
	setFont("SansSerif", getHeight/32, " antialiased");
	setColor("white");
	Overlay.drawString("lateral axis", getWidth*0.7, getHeight*0.2, 0.0);
	Overlay.drawString("energy axis", getWidth*0.12, getHeight*0.7, 0.0);
	Overlay.show();
	makeLine(getWidth*0.7, getHeight*0.1, getWidth*0.9, getHeight*0.1);
	run("Add Selection...");
	makeLine(getWidth*0.9, getHeight*0.1, getWidth*0.85, getHeight*0.05);
	run("Add Selection...");
	makeLine(getWidth*0.9, getHeight*0.1, getWidth*0.85, getHeight*0.15);
	run("Add Selection...");
	makeLine(getWidth*0.1, getHeight*0.7, getWidth*0.1, getHeight*0.9);
	run("Add Selection...");
	makeLine(getWidth*0.1, getHeight*0.9, getWidth*0.05, getHeight*0.85);
	run("Add Selection...");
	makeLine(getWidth*0.1, getHeight*0.9, getWidth*0.15, getHeight*0.85);
	run("Add Selection...");
	run("Select None");
}
