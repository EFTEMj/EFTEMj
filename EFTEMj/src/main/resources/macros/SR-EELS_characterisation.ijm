/*
 * file:	SR-EELS_characterisation.ijm
 * author:	Michael Entrup (entrup@arcor.de)
 * version:	20140812
 * date:	12.08.2014
 * info:	This macro is used to characterise the  aberrations of an Zeiss in-column energy filter when using SR-EELS. A series of calibration datasets is necessary  to run the characterisation. Place all datasets (images) at a folder that contains no other images.
 * You can find an example SR-EELS series at https://github.com/EFTEMj/EFTEMj/tree/master/Scripts+Macros/examples/SR-EELS_characterisation. There you will find a instruction on how to record such a series, too.
 */

/*
 * Parameters:
 * Only this values should be changed by the user.
 */
 /*
  *  step_size determines the number of energy channels that will result in one data point of the resulting dataset.
  */
var step_size = -1;	// choose '-1' for automatic mode 'abs(height / 64)'
/*
 * border_left and border_right are used to limit the energy range that is used. This is useful to cut off the ZLP or to ignore low counts at high energy losses.
 */
var border_left = -1;	// choose '-1' for automatic mode 'abs(height / 16)'
var border_right = -1;	// choose '-1' for automatic mode 'abs(height / 16)'
/*
 * This value is used when applying different filters, like remove outliers or median.
 */
var filter_radius = -1;	// choose '-1' for automatic mode 'round(sqrt(step_size))'
/*
 * The macro will create a diagram that plots the spectrum width against the spectrum position. This value determines the energy channel that is used to calculate the spectrum width.
 *
 * ToDo: implement a function to calibrate the energy scale. Then it is possible to select an energy loss instead of a relative value.
 */
var energy_pos = 0.5;	// choose a value between 0 and 1; 0.5 is the centre of the SR-EELS dataset
/*
 * Select some methods used for threshold. The following options are available:
 * Default, Huang, Intermodes, IsoData, Li, MaxEntropy, Mean, MinError, Minimum, Moments, Otsu, Percentile, RenyiEntropy, Shanbhag, Triangle and Yen
 *
 * With 'Li' the borders match best.
 * Other methods perform better at low counts, but they underestimate the width of the spectrum at higher counts.
 * If you want to test other threshold methods, just add them to the array 'thresholds'. A seperate folder with results is created for each method.
 *
 * Li
 * Implements Li's Minimum Cross Entropy thresholding method based on the iterative version (2nd reference below) of the algorithm.
 * 	Li, CH & Lee, CK (1993), "Minimum Cross Entropy Thresholding", Pattern Recognition 26(4): 617-625
 * 	Li, CH & Tam, PKS (1998), "An Iterative Algorithm for Minimum Cross Entropy Thresholding", Pattern Recognition Letters 18(8): 771-776
 * 	Sezgin, M & Sankur, B (2004), "Survey over Image Thresholding Techniques and Quantitative Performance Evaluation", Journal of Electronic Imaging 13(1): 146-165 [1]
 * Ported from ME Celebi's fourier_0.8 routines [2].
 *
 * [1]:	http://citeseer.ist.psu.edu/sezgin04survey.html
 * [2]:	http://sourceforge.net/projects/fourier-ipal
 */
// var thresholds = newArray("Default", "Huang", "Intermodes", "IsoData", "Li", "MaxEntropy", "Mean", "MinError", "Minimum", "Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag", "Triangle and Yen");	// this are all available methods
// var thresholds = newArray("IsoData", "Huang", "RenyiEntropy", "Minimum", "Otsu", "Li", "Default");	// these 7 methods performed best with my test datasets
var thresholds = newArray("Li");
 /*
  * This is the multiplicator of the standard deviation sigma:
  * This is only a minimal value. The macro will automaticaly increas it if r² of the gaussian fit decreases.
  */
sigma_weighting = 3;	// 1 = 68.27%, 2 = 95.45%, 3 = 99.73%

/*
 * Global variables
 */
var doRotate; var input_dir; var result_dirs; var skip_threshold; var datapoints; var list; var width; var height;	// global variables for use in 'load_images()'
var array_pos_all; var array_width_all; var array_pos_all_calc; var array_width_all_calc;	// global variables for use in 'save_pos_and_width()'
var threshold;	// global variables for use in 'analyse_dataset()'

load_images();

/*
 *  Set-up:
 *  Start a timer and switch to Batch mode.
 */
start = getTime();
/*
 * I have to remove the option 'save_column'. Otherwise gnuplot would try to process the header, because there is no # in front of it.
 */
run("Input/Output...", "jpeg=85 gif=-1 file=.txt use_file copy_row save_row");
setBatchMode(true);	// Batch mode will speed up the macro

/*
 * Set different parameters and start 'analyse_dataset()'.
 * This version only allows to iterate through different threshold methods. Future versions will be able to to iterate through other parameters, too.
 */
for(m=0; m < thresholds.length; m++) {
	if (skip_threshold[m] == false) {
		threshold = thresholds[m];
		analyse_dataset();	// this is the main function of this macro
		run("Collect Garbage");	// The macro needs a large amount of memory. After each analysis most of the used memory can be freed.
	}
}
/*
 *  Select and close the result window:
 */
if (isOpen("Results")) {
	selectWindow("Results");
	run("Close");
}
showMessage("<html><p>The evaluation finished.</p><p>Elapsed time: " + (getTime() - start) / 1000 + "s</p>");

/*
 * function: analyse_dataset
 * description: This is the main part of the macro. For every set of parameters (global variables) this function is called. It contains image filters, threshold, curve fitting and saving the results.
 */
function analyse_dataset() {
	/*
	 * Create all necessary arrays:
	 */
	array_index = newArray(datapoints);
	array_pos_y = newArray(datapoints);
	array_pos_x = newArray(datapoints);
	array_left = newArray(datapoints);
	array_right = newArray(datapoints);
	array_width = newArray(datapoints);
	array_width_calc = newArray(datapoints);
	/*
	 * This for-loop will process every image separately. When the last image has been processed, 'width_vs_pos' will be created (for more details see 'save_pos_and_width()').
	 */
	for (i=0; i<list.length; i++) {
		open(list[i]);
		id = getImageID();	// id will be used to close the image
		img_name = File.nameWithoutExtension;
		/*
		 * since 20140620: The default configuration is energy loss on the x-axis, but the fastes processing (of this macro) is achieved when the lateral axis is on the x-axis. That is why we rotate if doRotate == false. If the input images contain the lateral axis on the x-axis, we save an image that shows the energy loss on the x-axis (see else branch).
		 */
		if (!doRotate) {
			run("Rotate 90 Degrees Right");
		} else {
			run("Duplicate...", "title=temp.tif");
			run("Rotate 90 Degrees Left");
			if (!File.isDirectory(input_dir + "rotated")) {
				File.makeDirectory(input_dir + "rotated");
				if (!File.exists(input_dir + "rotated")) {
					exit("Unable to create the directory:\n" + result_dirs[m]);
				}
			}
			saveAs("tiff", input_dir + "rotated" + File.separator + img_name + "_rot.tif");
			close();
			selectImage(id);
		}
		run("Flip Horizontally");	// I need the top right corner to have the coordinates 0,0. This is achieved by flipping the image horizontally.
		run("Properties...", "unit=[] pixel_width=1 pixel_height=1 origin=0,0");	// use pixel as coordinates
		run("Clear Results");	// we need an empty results table
		/*
		 *  remove the outliers and smooth
		 */
		run("Remove Outliers...", "radius=" + filter_radius + " threshold=32 which=Bright");
		run("Remove Outliers...", "radius=" + filter_radius + " threshold=32 which=Dark");
		run("Median...", "radius=" + filter_radius);
		run("Set Measurements...", "  mean center integrated redirect=None decimal=9");	// measure the mean value, the centre of mass and the integrated intensity
		y_pos = 0;
		x_offset = 0;
		roi_width = width;
		while (y_pos < getHeight - border_left - border_right) {
			makeRectangle(x_offset, y_pos + border_left, roi_width, step_size);
			/*
			 * A gaussian fit is used to limit the region used for thresholding:
			 * The profile is no gaussian distribution, but the gaussian fit estimates centre and width well. For more presice results thresholding is needed.
			 */
			profile = getProfile();
			/*
			 * We need an array to define the x-axis values (0,1,2,...,roi_width-1) when fitting a gaussian:
			 */
			array_x = newArray(roi_width);
			for (x=0; x<roi_width; x++) {
				array_x[x] = x;
			}
			Fit.doFit("Gaussian", array_x, profile);
			gauss_centre = Fit.p(2);
			gauss_sigma =  Fit.p(3);	// sigma is used to estimate the region that is used for thresholding
			sigma_weighed = sigma_weighting * gauss_sigma / pow(Fit.rSquared(), 2);	// if the fit has a low r², sigma will be increased
			x_offset = maxOf(x_offset + round(gauss_centre - sigma_weighed), 0);	// all further measurements use the coordinates of the duplicated selection and this value is used to transform them to coordinates of the full image
			roi_width = round(2 * sigma_weighed);
			makeRectangle(x_offset, y_pos + border_left, roi_width, step_size);
			run("Duplicate...", "temp");	// create a temporary image: only the rectangle selection gets duplicated
			setAutoThreshold(threshold + " dark");	// threshold with...
			run("NaN Background");	// ..set background to NaN
			run("Measure");
			result_pos = y_pos / step_size;	// line number at the result table
			array_index[result_pos] = result_pos;
			spec_width = getResult("IntDen", result_pos) / getResult("Mean", result_pos) / step_size;	// the mean width of all 'step_size' energy channels
			array_width[result_pos] = spec_width;
			XM = getResult("XM", result_pos);	// the centre of mass in x-direction
			array_pos_x[result_pos] = XM + x_offset;
			YM = getResult("YM", result_pos) + y_pos + border_left; // the centre of mass in y-direction; we need to add 'y_pos', because the measurement is done on a cropped image with the height 'step_size'
			array_pos_y[result_pos] = YM;
			/*
			 * Detect left and right border:
			 */
			run("Macro...", "code=[if(isNaN(v)) v=-1000;]");	// replace NaN by '-1000'; this will result in the highest slope at the spectrum border
			run("Find Edges");
			run("Bin...", "x=1 y=" + step_size + " bin=Average");
			/*
			 * Left border:
			 */
			makeRectangle(maxOf(XM - spec_width, 0), 0, XM - maxOf(XM - spec_width, 0), step_size);
			getMinAndMax(min, max);
			run("Find Maxima...", "output=[Point Selection]");
			getSelectionBounds(x, y, w, h);
			array_left[result_pos] = x + x_offset;
			run("Select None");
			/*
			 *  Right border:
			 */
			makeRectangle(XM, 0, width - maxOf(XM - spec_width, 0), step_size);
			getMinAndMax(min, max);
			run("Find Maxima...", "output=[Point Selection]");
			getSelectionBounds(x, y, w, h);
			array_right[result_pos] = x + w + x_offset;
			array_width_calc[result_pos] = array_right[result_pos] - array_left[result_pos];	// calculate the width by using the borders positions
			close();	// close the temporary image
			if (abs(round(YM) - energy_pos * height) <= (step_size  / 2)) {	// check if the given value of 'energy_pos' is inside the current energy interval
				save_pos_and_width(i, array_pos_x[result_pos], array_width[result_pos], array_left[result_pos], array_right[result_pos]);
			}
			y_pos += step_size;
		}
		/*
		 * The following code will create a jpg-version of the input image that shows the detected borders and the detected centre of the spectrum.
		 * The image will be converted to logarithmic scale to enhance the visibility at regions with low signals.
		 */
		selectImage(id);
		run("Select None");
		run("Log");
		run("Enhance Contrast", "saturated=0.35");
		addPointsToOverlay(array_left, array_pos_y, 0);
		addPointsToOverlay(array_pos_x, array_pos_y, 1);
		addPointsToOverlay(array_right, array_pos_y, 2);
		run("Flatten");
		run("Flip Horizontally");
		run("Rotate 90 Degrees Left");
		saveAs("Jpeg", result_dirs[m] + img_name + ".jpg");
		close();	// close the image that contains the overlay
		selectImage(id);	// select and...
		close();	// ...close the image
		/*
		 * Plot and save the results:
		 * The created diagrams are to estimate the quality of the used datasets.
		 * For final fitting, Gnuplot (http://gnuplot.info/) should be used, which creates for superior results.
		 */
		/*
		 * Spectrum width:
		 */
		Fit.doFit("3rd Degree Polynomial", array_pos_y, array_width);
		Fit.plot;
		saveAs("PNG", result_dirs[m] + "width_" + img_name + ".png");
		close();
		/*
		 * Position of spectrum centre:
		 */
		Fit.doFit("3rd Degree Polynomial", array_pos_y, array_pos_x);
		Fit.plot;
		saveAs("PNG", result_dirs[m] + "center_" + img_name + ".png");
		close();
		/*
		 * Position of left border:
		 */
		Fit.doFit("3rd Degree Polynomial", array_pos_y, array_left);
		Fit.plot;
		saveAs("PNG", result_dirs[m] + "bottom_" + img_name + ".png");
		close();
		/*
		 * Position of right border:
		 */
		Fit.doFit("3rd Degree Polynomial", array_pos_y, array_right);
		Fit.plot;
		saveAs("PNG", result_dirs[m] + "top_" + img_name + ".png");
		close();
		/*
		 * Create a table containing the results:
		 * The table can be saved as a text file with  tab-separated values.
		 * Gnuplot can access this files without any changes to the file.
		 */
		Array.show("Values", array_index, array_pos_y, array_pos_x, array_left, array_right, array_width, array_width_calc);
		if (isOpen("Values")) {
			selectWindow("Values");
			saveAs("Results", result_dirs[m] + "values_" + img_name + ".txt");
			run("Close");
		}
	}
}

/*
 * function: Load images:
 * This macro requires at least two images (SR-EELS datasets) to run properly. You have to select a folder and ImageJ will load all tif and dm3 images that are stored at the selected folder. Sub-folders are not considered.
 */
function load_images() {
	input_dir = getDirectory("Choose a Directory ");
	if (input_dir == "") exit();	// if cancel was selected, the script will stop
	list = getFileList(input_dir);
	list = filter_images(list);	// only select tif and dm3 files; ignore sub-folders
	open(list[0]);
	id = getImageID();	// id will be used to close the image
	draw_axes_as_overlay(); 	// create an overlay to simplify the next user choice
	doRotate = getBoolean("The macro requires the following configuration:\nx: energy axis\ny: lateral axis\n\nRotate the images?");	// This name is a bit stange. We will rotate the image  if doRotate == false. This is because the energy axis on the x-axis is best for further processing and theoretical description, but this macro runs fastes with the lateral axis on the x-axis.
	run("Remove Overlay");
	if (!doRotate) {
		width = getWidth;
		height = getHeight;
	} else {	// Each image will be rotated at the function 'analyse_dataset'. We have to interchange width and height if doRotate is true.
		height = getWidth;
		width = getHeight;
	}
	close();	// We will open the image again, when entering the function 'analyse_dataset'
	/*
	 * Automatic assignment of parameters:
	 */
	if (step_size == -1) {
		step_size = abs(height / 64);
	}
	if (border_left == -1) {
		border_left = abs(height / 16);
	}
	if (border_right == -1) {
		border_right = abs(height / 16);
	}
	if (filter_radius == -1) {
		filter_radius = round(sqrt(step_size));
	}
	/*
	 * A dialogue will be created to modify the previous determined values.
	 */
	Dialog.create("SR-EELS Characterisation - Setup");
	Dialog.addNumber("Step size:", step_size, 0, 4, "px");
	Dialog.addNumber("Left border:", border_left, 0, 4, "px");
	Dialog.addNumber("Right border:", border_right, 0, 4, "px");
	Dialog.addNumber("Filter radius:", filter_radius, 0, 4, "px");
	Dialog.addSlider("Energy Position:", 0, 1, 0.5);
	Dialog.show();
	step_size = Dialog.getNumber();
	border_left = Dialog.getNumber();
	border_right = Dialog.getNumber();
	filter_radius = Dialog.getNumber();
	energy_pos = Dialog.getNumber();
	datapoints = ceil((height - border_left - border_right) / step_size);
	/*
	 * For each set of parameters (currently only threshold can include different values) it is checked, if there are already some result. The user is asked, if he wants to overwrite these previous results.
	 */
	result_dirs = newArray(thresholds.length);
	skip_threshold = newArray(thresholds.length);
	for(m=0; m<thresholds.length; m++) {
		result_dirs[m] = input_dir + "results_" +  toString(step_size) + toString(border_left) + toString(border_right) + toString(filter_radius) + thresholds[m] + File.separator;	// the folder name contains the parameters
		if (File.isDirectory(result_dirs[m])) {
			if (!getBoolean("There are previous results for the selected parameters.\nstep size: "+ step_size +"\ntop border: " + border_left + "\nbottom border: " + border_right + "\nfilter radius: " + filter_radius + "\nthreshold method: " + thresholds[m] + "\nDo you want to overwrite these results?")) {
				skip_threshold[m] = true;
			}
		} else {
			File.makeDirectory(result_dirs[m]);
			if (!File.exists(result_dirs[m])) {
				exit("Unable to create the directory:\n" + result_dirs[m]);
			}
			skip_threshold[m] = false;
		}
	}
}

/*
 * function: filter_images
 * description: Keep tif and dm3 files only. Ignore subdirectories.
 * since 20140620: A dialog is presented to select the files for the characterization. By default all files are selected and you have to deselect all files that are not necassary.
 */
function filter_images(array_str) {
	temp = -1;
	for (i=0; i<list.length; i++) {
		path = input_dir + list[i];
		if (!endsWith(path,"/") && (endsWith(path,".tif") || endsWith(path,".dm3"))) {
			 if (temp == -1) {
			 	temp = newArray(1);
			 	temp[0] = path;
			 } else {
			 	temp = Array.concat(temp, path);
			 }
		}
	}
	Dialog.create("Select files");
	for (i=0; i<temp.length; i++) {
		Dialog.addCheckbox(File.getName(temp[i]), true);
	}
	var selected_files;
	init = true;
	Dialog.show();
	for (i=0; i<temp.length; i++) {
		if (Dialog.getCheckbox()) {
			if (init) {
				selected_files = newArray(1);
				selected_files[0] = temp[i];
				init = false;
			} else {
				selected_files = Array.concat(selected_files, temp[i]);
			}
		}
	}
	return selected_files;
}

/*
 * function: save_pos_and_width
 * description: This function is used to collect values from all datasets. Finally a fit will be performed and the result will be saved as PNG-file.
 */
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
		Fit.doFit("3rd Degree Polynomial", array_pos_all, array_width_all);
		Fit.plot;
		saveAs("PNG", result_dirs[m] + "width_vs_pos.png");
		close();
		Fit.doFit("3rd Degree Polynomial", array_pos_all_calc, array_width_all_calc);
		Fit.plot;
		saveAs("PNG", result_dirs[m] + "width_vs_pos_calc.png");
		close();
		Array.show("Values", array_pos_all, array_width_all, array_pos_all_calc, array_width_all_calc);
		if (isOpen("Values")) {
			selectWindow("Values");
			saveAs("Results", result_dirs[m] + "width_vs_pos.txt");
			run("Close");
		}
	}
}

/*
 * function: draw_axes_as_overlay
 * description: Text and arrows are drawn to help the user decide if the dataset has to be rotated.
 */
function draw_axes_as_overlay() {
	setFont("SansSerif", getHeight/32, " antialiased");
	setColor("white");
	Overlay.drawString("energy axis", getWidth*0.7, getHeight*0.2, 0.0);
	Overlay.drawString("lateral axis", getWidth*0.12, getHeight*0.7, 0.0);
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

/*
 * function: ceil
 * description: There is no function ceil available, that is why had to implement it.
 */
function ceil(value) {
	if (value != floor(value)) {
		return floor(value) + 1;
	} else {
		return value;
	}
}

/*
 * function: addPointsToOverlay:
 * description: This function is used to draw data points at the current image. The datapoints are added to the overlay.
 */
function addPointsToOverlay(xPos, yPos, overlayColorIndex) {
	color = newArray("Green", "Red", "Blue");
	markerSize = "Tiny";
	if (maxOf(getHeight, getWidth) > 4000) {
		markerSize = "Large";
	} else {
		if (maxOf(getHeight, getWidth) > 2000) {
			markerSize = "Mediam";
		} else {
			if (maxOf(getHeight, getWidth) > 1000) {
				markerSize = "Small";
			}
			}
		}
	run("Point Tool...", "selection=" + color[overlayColorIndex] + " cross=White marker=" + markerSize + " mark=0");
	for (i=0; i<xPos.length; i++) {
		makePoint(xPos[i], yPos[i]);
		run("Add Selection...");
	}
}