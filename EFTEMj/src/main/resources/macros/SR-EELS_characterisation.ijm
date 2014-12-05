/*
 * file:	SR-EELS_characterisation.ijm
 * author:	Michael Entrup b. Epping (michael.entrup@wwu.de)
 * version:	20141128
 * date:	28.11.2014
 * info:	This macro is used to characterise the distortions of an Zeiss in-column energy filter when using SR-EELS.
 * 			A series of calibration data sets is necessary to run the characterisation.
 * 			Place all data sets (images) at a single folder and run this macro.
 * 			You can find an example SR-EELS series at:
 * 				https://github.com/EFTEMj/EFTEMj/tree/master/Scripts+Macros/examples/SR-EELS_characterisation
 * 			There you will find a instruction on how to record such a series, too.
 * 			
 * 			This macro is part of the Fiji plugin EFTEMj (https://github.com/EFTEMj/EFTEMj).
 */

/*
 * Parameters:
 * Only this values should be changed by the user.
 */
/*
 * Set this to false if you don't want to use any GUI.
 * Make sure that all parameters are set.
 */
var skip_gui = false;
/*
 * The directory to read the files from.
 * Make sure there are only the calibration images at this folder, if you use 'skip_gui = true'.
 * Example: 'var input_dir = "C:\\Temp\\folder with cal images\\"'
 * Don't forget the '\\' at the end of the string.
 */
var input_dir = "C:\\Temp\\folder with cal images\\";
/*
 * This parameter is only used for 'var skip_gui = true;'. 
 * A dialogue is presented when working with GUI.
 * Overwrite the results if there are already results with the given parameters.
 */
var overwrite_results = true;
/*
 * The macro requires the energy loss at the x-axis and the lateral information on the y-axis.
 * Set 'doRotate = true' if your image axes are swapped.
 */
var doRotate = true; 
/*
 *  step_size determines the number of energy channels that will result in one data point of the resulting data set.
 *  Choose '-1' for automatic mode 'abs(height / 64)'
 */
var step_size = -1;
/*
 * energy_border_lower and energy_border_higher are used to limit the energy range that is used.
 * This is useful to cut off the ZLP or to ignore low counts at high energy losses.
 * Choose '-1' for automatic mode 'abs(height / 16)'
 */
var energy_border_lower = -1;
var energy_border_higher = -1;
/*
 * This value is used when applying different filters, like remove outliers or median.
 * Choose '-1' for automatic mode 'round(sqrt(step_size))'
 */
var filter_radius = -1;
/*
 * The macro will create a diagram that plots the spectrum width against the spectrum position.
 * This value determines the energy channel that is used to calculate the spectrum width.
 * Choose a value between 0 and 1; 0.5 is the centre of the SR-EELS data set
 *
 * ToDo: implement a function to calibrate the energy scale. Then it is possible to select an energy loss instead of a relative value.
 */
var energy_pos = 0.5;
/*
 * Select some methods used for threshold. The following options are available:
 * Default, Huang, Intermodes, IsoData, Li, MaxEntropy, Mean, MinError, Minimum, Moments, Otsu, Percentile, RenyiEntropy, Shanbhag, Triangle and Yen
 *
 * With 'Li' the borders match best.
 * Other methods perform better at low counts, but they underestimate the width of the spectrum at higher counts.
 * If you want to test other threshold methods, just add them to the array 'thresholds'. A separate folder with results is created for each method.
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
 *
 * This are all available methods:
 * var thresholds = newArray("Default", "Huang", "Intermodes", "IsoData", "Li", "MaxEntropy", "Mean", "MinError", "Minimum", "Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag", "Triangle and Yen");
 *
 * These 7 methods performed best with my test data sets:
 * var thresholds = newArray("IsoData", "Huang", "RenyiEntropy", "Minimum", "Otsu", "Li", "Default");
 */
var thresholds = newArray("Li");
 /*
  * This is the multiplier of the standard deviation sigma:
  * This is only a minimal value. The macro will automatically increases it if r² of the Gaussian fit decreases.
  * A Gaussian fit is used before thresholding, to optimize the results.
  * 	1 = 68.27%
  * 	2 = 95.45%
  * 	3 = 99.73%
  */
var sigma_weighting = 3;
/*
 * Set options for plotting:
 * Width and height are set for the coordinate system. The size of the resulting image is larger.
 */
var plot_width = 1200;
var plot_height = 900;
run("Profile Plot Options...", "width=" + plot_width + " height=" + plot_height + " minimum=0 maximum=0 interpolate draw");
/*
 * Set the size of the jpeg image. 
 * The given height is only applied for images with a height that is a multiple of the given height.
 * 'jpeg_bin = round(height / jpeg_height)' is used.
 * Reduce the jpeg quality to reduce the size of the jpeg images.
 */
var jpeg_height = 1024;
var jpeg_quality = 50;
run("Input/Output...", "jpeg=" + jpeg_quality + " gif=-1 file=.txt use_file copy_row save_row");
/*
 * This is some kind of debug level:
 * A check box at the GUI allows to switch between the configured value and 0.
 * 0	create text/data files only
 * 1	create plots/images to check the quality of the characterisation
 */
var detailed_results = 1;
/*
 * This is the actual size of the used camera in pixels.
 * These Values are used to determine the binning of the characterisation images.
 */
var camera_width = 4096; var camera_height = 4096;
/*
 * By default the distortion is described in coordinates of camera pixels.
 * Binning reduces the number of pixels in an image and we will scale them up.
 * Set this value to 'false' if you don't want to correct for binning.
 */
var correct_binning = true;
/*
 * End of Parameters
 */

/*
 * The macro will change some settings.
 * The current settings will be restored when the macro has finished.
 */
saveSettings()
/*
 * Close all open images to avoid problems.
 * Batch mode will speed up the macro.
 */
close("*");
setBatchMode(true);

/*
 * Global variables
 */

/*
 *  global variables for use in 'load_images()'
 */
var result_dirs; var skip_threshold; var datapoints; var list; var width; var height;
/*
 * global variables for use in 'save_pos_and_width()'
 */
var array_pos_all; var array_width_all; var array_pos_all_calc; var array_width_all_calc;
/*
 * global arrays to store all values needed for fitting a 2D polynomial
 * x1	energy loss
 * x2	lateral position (centre of the spectrum)
 * y	width of the spectrum
 */
var array_x1; var array_x2 = newArray(); var array_y = newArray();

/*
 * global arrays to store all values needed for fitting all borders with a function series
 * x1		energy loss
 * x2		lateral position of the border/centre at x1=0
 * y		lateral position of the border/centre
 * weight	1.0 for borders and 0.5 for the centre
 */
var array_borders_x1; var array_borders_x2; var array_borders_y; var array_borders_weight;
/*
 * global variables for use in 'analyse_dataset()'
 */
var threshold;
/*
 * The camera binning that has been used to record the calibration images.
 * By default this is 1, as no assignment is done, if 'correct_binning = false'.
 */
var binning = 1;
/*
 * This value is used to create smaller jpeg images.
 */
var jpeg_bin;

/*
 * This function shows some dialogues to set up the macro.
 */
setup_macro();

/*
 * Start a timer.
 */
start = getTime();

/*
 * Set different parameters and start 'analyse_dataset()'.
 * This version only allows to iterate through different threshold methods.
 * Future versions will be able to to iterate through other parameters, too.
 */
for(m=0; m < thresholds.length; m++) {
	if (skip_threshold[m] == false) {
		threshold = thresholds[m];
		/*
		 * This is the main function of this macro.
		 */
		analyse_dataset();
		
		if (detailed_results >= 1) {
			/*
			 * Create the file that contains the values for plotting/fitting a 2D polynomial.
			 */
			prepareFileForWidthFit();
			/*
			 * Create the file that contains the values for plotting/fitting a all borders with a function series.
			 */
			prepareFileForBordersFit();
		}
	}
}

/*
 * Exit the batch mode and display all hidden images.
 * We use display for debugging.
 * Normally all images get closed by the macro and there is nothing to display.
 */
setBatchMode("exit and display");
showMessage("<html><p>The evaluation finished.</p><p>Elapsed time: " + (getTime() - start) / 1000 + "s</p>");
restoreSettings();
/*
 * End of macro:
 * The following code contains function definitions only.
 */
 

/*
 * function: analyse_dataset
 * description: This is the main part of the macro.
 * 				For every set of parameters (global variables) this function is called.
 * 				It contains image filters, threshold, curve fitting and saving the results.
 */
function analyse_dataset() {
	/*
	 * Create all necessary arrays:
	 */
	/*
	 * The following arrays are reused for every image that is analysed.
	 */
	array_index = newArray(datapoints);
	array_pos_y = newArray(datapoints);
	array_pos_x = newArray(datapoints);
	array_left = newArray(datapoints);
	array_right = newArray(datapoints);
	array_width = newArray(datapoints);
	array_width_calc = newArray(datapoints);
	if (detailed_results >= 1) {
		/*
		 * The following arrays store data from all analysed images.
		 * 'Array.concat()' is used to extend these arrays.
		 */
		array_x1 = newArray();
		array_x2 = newArray();
		array_y = newArray();
		array_borders_x1 = newArray();
		array_borders_x2 = newArray();
		array_borders_y = newArray();
		array_borders_weight = newArray();
	}
	/*
	 * This for-loop will process every image separately.
	 * When the last image has been processed, 'width_vs_pos' will be created (for more details see 'save_pos_and_width()').
	 */
	for (i=0; i<list.length; i++) {
		open(list[i]);
		/*
		 * id will be used to close the image
		 */
		id = getImageID();
		img_name = File.nameWithoutExtension;
		/*
		 * since 20140620: The default configuration is energy loss on the x-axis.
		 * But the fastest processing (of this macro) is achieved when the lateral axis is on the x-axis.
		 * That is why we rotate 'if (doRotate == false)'.
		 */
		if (doRotate == false) {
			run("Rotate 90 Degrees Right");
		}
		/*
		 * We need the top right corner to have the coordinates 0,0.
		 * This is achieved by flipping the image horizontally.
		 */
		run("Flip Horizontally");
		/*
		 * Use pixel as coordinates instead of calibrated values.
		 */
		run("Properties...", "unit=[] pixel_width=1 pixel_height=1 origin=0,0");
		/*
		 *  Remove the outliers and smooth the image.
		 */
		run("Remove Outliers...", "radius=" + filter_radius + " threshold=32 which=Bright");
		run("Remove Outliers...", "radius=" + filter_radius + " threshold=32 which=Dark");
		run("Median...", "radius=" + filter_radius);
		/*
		 * Reset some values that changed during a previous iteration of the surrounding for loop.
		 */
		y_pos = 0;
		x_offset = 0;
		roi_width = width;
		/*
		 * This loop moves a rectangle selection from low energy loss to high energy loss.
		 * For each position some measurements are performed.
		 * The axes are swapped (see a few lines above): x is lateral and y is energy loss.
		 * The height (in energy channels) of this rectangle is defined by 'step_size'.
		 */
		while (y_pos < getHeight - energy_border_lower - energy_border_higher) {
			makeRectangle(x_offset, y_pos + energy_border_lower, roi_width, step_size);
			/*
			 * A Gaussian fit is used to limit the region used for thresholding:
			 * The profile is no Gaussian distribution, but the Gaussian fit estimates centre and width well. For more precise results thresholding is used.
			 */
			profile = getProfile();
			/*
			 * We need an array to define the x-axis values (0,1,2,...,roi_width-1) when fitting a Gaussian:
			 */
			array_x = newArray(roi_width);
			for (x=0; x<roi_width; x++) {
				array_x[x] = x;
			}
			/*
			 * Formula: y = a + (b-a)*exp(-(x-c)*(x-c)/(2*d*d))
			 */
			Fit.doFit("Gaussian", array_x, profile);
			/*
			 * Fit.p(2) = c
			 */
			gauss_centre = Fit.p(2);
			/*
			 * Sigma is used to estimate the region that is used for thresholding.
			 * Fit.p(3) = d
			 */
			gauss_sigma =  Fit.p(3);
			/*
			 * If the fit has a low r², sigma will be increased.
			 */
			sigma_weighed = sigma_weighting * gauss_sigma / pow(Fit.rSquared(), 2);
			/*
			 * All further measurements use the coordinates of a duplicated selection.
			 */
			x_offset = maxOf(x_offset + round(gauss_centre - sigma_weighed), 0);
			roi_width = round(2 * sigma_weighed);
			makeRectangle(x_offset, y_pos + energy_border_lower, roi_width, step_size);
			/*
			 * Create a temporary image:
			 * 		Only the rectangle selection gets duplicated.
			 */
			run("Duplicate...", "temp");
			/*
			 * Threshold with...
			 */
			setAutoThreshold(threshold + " dark");
			/*
			 * ..set background to NaN.
			 * All pixels with NaN are not regarded for measurements.
			 */
			run("NaN Background");
			/*
			 * List functions are described at:
			 * 		http://rsbweb.nih.gov/ij/developer/macro/functions.html#L
			 * List.setMeasurements works like 'run("Measure")' but without the annoying results table.
			 * Accessing values by key/value pairs is much more easy than extracting them from the results table.
			 */
			List.setMeasurements;
			/*
			 * Create an index to address the fields of the arrays.
			 */
			index = y_pos / step_size;
			array_index[index] = index;
			/*
			 * The mean width of all 'step_size' energy channels:
			 */
			spec_width = List.getValue("IntDen") / List.getValue("Mean") / step_size;
			array_width[index] = binning * spec_width;
			/*
			 * The centre of mass in x-direction:
			 * We need to add 'x_offset', because the measurement is done on a cropped image.

			 */
			XM = List.getValue("XM");
			array_pos_x[index] = binning * (XM + x_offset);
			/*
			 * The centre of mass in y-direction:
			 * We need to add 'y_pos', because the measurement is done on a cropped image.
			 */
			YM = List.getValue("YM") + y_pos + energy_border_lower;
			array_pos_y[index] = binning * YM;
			/*
			 * Detect left and right border:
			 * Replace NaN by '-1000'.
			 * This will result in the highest slope at the spectrum border.
			 */
			run("Macro...", "code=[if(isNaN(v)) v=-1000;]");
			run("Find Edges");
			/*
			 * Using 'Bin' is like averaging. The result is like a line scan with a width of 'step_size'.
			 */
			run("Bin...", "x=1 y=" + step_size + " bin=Average");
			/*
			 * The line scan is divided into two parts: left and right of the spectrum centre.
			 * For each part the highest value is determined, which corresponds to the steepest edge.
			 */
			/*
			 * Left border:
			 */
			makeRectangle(maxOf(XM - spec_width, 0), 0, XM - maxOf(XM - spec_width, 0), step_size);
			getMinAndMax(min, max);
			run("Find Maxima...", "output=[Point Selection]");
			getSelectionBounds(x, y, w, h);
			array_left[index] = binning * (x + x_offset);
			run("Select None");
			/*
			 *  Right border:
			 */
			makeRectangle(XM, 0, width - maxOf(XM - spec_width, 0), step_size);
			getMinAndMax(min, max);
			run("Find Maxima...", "output=[Point Selection]");
			getSelectionBounds(x, y, w, h);
			array_right[index] = binning * (x + w + x_offset);
			/*
			 * Calculate the width by using the borders positions:
			 */
			array_width_calc[index] = array_right[index] - array_left[index];
			/*
			 * Close the temporary image:
			 */
			close();
			/*
			 * Check if the given value of 'energy_pos' is inside the current energy interval.
			 */
			if (abs(round(YM) - energy_pos * height) <= (step_size  / 2)) {
				save_pos_and_width(i, array_pos_x[index], array_width[index], array_left[index], array_right[index]);
			}
			y_pos += step_size;
			/*
			 * Start next iteration.
			 */
		}
		if (detailed_results >= 1) {
			/*
			 * The following code will create a jpg-version of the input image that shows the detected borders and the detected centre of the spectrum.
			 * The image will be converted to logarithmic scale to enhance the visibility at regions with low signals.
			 */
			selectImage(id);
			run("Select None");
			jpeg_bin = round(height/jpeg_height);
			run("Bin...", "x="+ jpeg_bin + " y=" + jpeg_bin + " bin=Average");
			run("Log");
			run("Enhance Contrast", "saturated=0.35");
			addPointsToOverlay(array_left, array_pos_y, 0);
			addPointsToOverlay(array_pos_x, array_pos_y, 1);
			addPointsToOverlay(array_right, array_pos_y, 2);
			/*
			 * 'Flatten' creates a new image.
			 */
			run("Flatten");
			/*
			 * 'Flip' and 'Rotate' are used to get the default configuration:
			 * energy loss on the x-axis.
			 */
			run("Flip Horizontally");
			run("Rotate 90 Degrees Left");
			saveAs("Jpeg", result_dirs[m] + img_name + ".jpg");
			/*
			 * Close the image that contains the overlay,...
			 */
			close();
		}
		/*
		 * ..select and...
		 */
		selectImage(id);
		/*
		 * ...close the full image.
		 */
		close();
		if (detailed_results >= 1) {
			/*
			 * Plot and save the results:
			 * The created diagrams are used to estimate the quality of the characterisation.
			 * For final fitting, Gnuplot (http://gnuplot.info/) should be used, which creates far superior results.
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
			temp = newArray(array_pos_x.length);
			Array.fill(temp, Fit.f(0));
			array_borders_x2 = Array.concat(array_borders_x2, temp);
			Array.fill(temp, 0.5);
			array_borders_weight = Array.concat(array_borders_weight, temp);
			saveAs("PNG", result_dirs[m] + "center_" + img_name + ".png");
			close();
			/*
			 * Position of left border:
			 */
			Fit.doFit("3rd Degree Polynomial", array_pos_y, array_left);
			Fit.plot;
			temp = newArray(array_left.length);
			Array.fill(temp, Fit.f(0));
			array_borders_x2 = Array.concat(array_borders_x2, temp);
			Array.fill(temp, 1.0);
			array_borders_weight = Array.concat(array_borders_weight, temp);
			saveAs("PNG", result_dirs[m] + "bottom_" + img_name + ".png");
			close();
			/*
			 * Position of right border:
			 */
			Fit.doFit("3rd Degree Polynomial", array_pos_y, array_right);
			Fit.plot;
			temp = newArray(array_right.length);
			Array.fill(temp, Fit.f(0));
			array_borders_x2 = Array.concat(array_borders_x2, temp);
			Array.fill(temp, 1.0);
			array_borders_weight = Array.concat(array_borders_weight, temp);
			saveAs("PNG", result_dirs[m] + "top_" + img_name + ".png");
			close();
		}
		/*
		 * Save all results as a text file.
		 * A CSV like formatting is used with tab-separated values.
		 * Gnuplot can access this files without any changes to the file.
		 */
		f = File.open(result_dirs[m] + "values_" + img_name + ".txt");
		print(f, "#index\tx1-position\tx2-position\tbottom_pos\ttop_position\twidth\twidth_calc");
		for (p=0; p<array_index.length; p++) {
			print(f, array_index[p] + "\t" + array_pos_y[p] + "\t" + array_pos_x[p] + "\t" + array_left[p] + "\t" + array_right[p] + "\t" + array_width[p] + "\t" + array_width_calc[p]);
		}
		File.close(f);
		if (detailed_results >= 1) {
			/*
			 * Put all determined values into a single array. These values are necessary to plot a 2D polynomial.
			 */
			 array_x1 = Array.concat(array_x1, array_pos_y);
			 array_x2 = Array.concat(array_x2, array_pos_x);
			 array_y = Array.concat(array_y, array_width);
			/*
			 * Put all determined values into a single array. These values are necessary to fit a single function series to all borders.
			 */
			array_borders_x1 = Array.concat(array_borders_x1, array_pos_y);
			array_borders_x1 = Array.concat(array_borders_x1, array_pos_y);
			array_borders_x1 = Array.concat(array_borders_x1, array_pos_y);
			/*
			 * We need y-values for each border. That is why we add them 3 times.
			 */
			array_borders_y = Array.concat(array_borders_y, array_pos_x);
			array_borders_y = Array.concat(array_borders_y, array_left);
			array_borders_y = Array.concat(array_borders_y, array_right);
		}
	}
	/*
	 * Characterise the next image.
	 */
}

/*
 * function: setup_macro:
 * description:	This macro requires at least two images (SR-EELS datasets) to run properly.
 * 				You have to select a folder and ImageJ will load all tif and dm3 images that are stored at the selected folder.
 * 				'filter_images()' (see below) is used to exclude images.
 * 				Sub-folders are not considered.
 */
function setup_macro() {
	if (skip_gui == false) {
		input_dir = getDirectory("Choose a Directory ");
		/*
		 * If cancel was selected, the script will stop.
		 */
		if (input_dir == "") stopMacro("");
	}
	/*
	 *  Only select tif and dm3 files. Ignore sub-folders. 
	 *  See below this function.
	 */
	list = filter_images();
	open(list[0]);
	/*
	 * 'id' will be used to close the image.
	 */
	id = getImageID();
	if (skip_gui == false) {
		/*
		 * Create an overlay to simplify the next user choice.
		 */
		draw_axes_as_overlay();
		/*
		 * Normally no images are shown in batch mode.
		 */		
		setBatchMode("show");
		/*
		 * This name is a bit strange.
		 * We will rotate the image if doRotate == false.
		 * This is because the energy axis on the x-axis is best for further processing and theoretical description,
		 * but this macro runs fastest with the lateral axis on the x-axis.
		 */
		doRotate = getBoolean("The macro requires the following configuration:\nx: energy axis\ny: lateral axis\n\nRotate the images?");
		run("Remove Overlay");
	}
	if (doRotate == false) {
		width = getWidth;
		height = getHeight;
	} else {
		/*
		 * Each image will be rotated at the function 'analyse_dataset'.
		 * We have to interchange width and height if doRotate is true.
		 */
		height = getWidth;
		width = getHeight;
	}
	if (correct_binning == true) {
		bin_x = camera_width / getWidth;		
		bin_y = camera_height / getHeight;
		if (bin_x == bin_y && bin_x == round(bin_x)) {
			binning = bin_x;
		} else {
			print("The following values have been determined as binning:\nx binning: " + bin_x + "\ny binning: " + bin_y + "\nThe binning has to be an integer and equal for both axes.");
			stopMacro("");
		}
	}
	/*
	 * We will open the image again, when entering the function 'analyse_dataset'.
	 */
	close();
	/*
	 * Automatic assignment of parameters:
	 */
	if (step_size == -1) {
		step_size = abs(height / 64);
	}
	if (energy_border_lower == -1) {
		energy_border_lower = abs(height / 16);
	}
	if (energy_border_higher == -1) {
		energy_border_higher = abs(height / 16);
	}
	if (filter_radius == -1) {
		filter_radius = round(sqrt(step_size));
	}
	
if (skip_gui == false) {
		/*
		 * A dialogue will be created to modify the previous determined values.
		 */
		Dialog.create("SR-EELS Characterisation - Setup");
		Dialog.addNumber("Step size:", step_size, 0, 4, "px");
		Dialog.addNumber("Left border:", energy_border_lower, 0, 4, "px");
		Dialog.addNumber("Right border:", energy_border_higher, 0, 4, "px");
		Dialog.addNumber("Filter radius:", filter_radius, 0, 4, "px");
		Dialog.addCheckbox("No detailed results", false);
		Dialog.addSlider("Energy Position:", 0, 1, 0.5);
		Dialog.show();
		step_size = Dialog.getNumber();
		energy_border_lower = Dialog.getNumber();
		energy_border_higher = Dialog.getNumber();
		filter_radius = Dialog.getNumber();
		if (Dialog.getCheckbox() == true) {
			detailed_results = 0;
		}
		energy_pos = Dialog.getNumber();
	}
	datapoints = ceil((height - energy_border_lower - energy_border_higher) / step_size);
	/*
	 * For each set of parameters it is checked if there are already some result.
	 * The user is asked, if he wants to overwrite these previous results.
	 */
	result_dirs = newArray(thresholds.length);
	skip_threshold = newArray(thresholds.length);
	for(m=0; m<thresholds.length; m++) {
		/*
		 * The folder name contains the parameters.
		 */
		result_dirs[m] = input_dir + "results_" +  toString(step_size) + toString(energy_border_lower) + toString(energy_border_higher) + toString(filter_radius) + thresholds[m] + File.separator;
		if (File.isDirectory(result_dirs[m])) {
			/*
			 * The directory already exists:
			 */
			if (skip_gui == true) {
				if (overwrite_results == false) {
					skip_threshold[m] = true;
				}
			} else {
				if (!getBoolean("There are previous results for the selected parameters.\nstep size: "+ step_size +"\ntop border: " + energy_border_lower + "\nbottom border: " + energy_border_higher + "\nfilter radius: " + filter_radius + "\nthreshold method: " + thresholds[m] + "\nDo you want to overwrite these results?")) {
					skip_threshold[m] = true;
				}
			}
		} else {
			/*
			 * There is no directory with the given name:
			 */
			File.makeDirectory(result_dirs[m]);
			if (!File.exists(result_dirs[m])) {
				stopMacro("Unable to create the directory:\n" + result_dirs[m]);
			}
			skip_threshold[m] = false;
		}
	}
}

/*
 * function: filter_images
 * description: Keep tif and dm3 files only. Ignore subdirectories.
 * since 20140620:	A dialogue is presented to select the files for the characterization.
 * 					By default all files are selected and you have to deselect all files that are not necessary.
 */
function filter_images() {
	list = getFileList(input_dir);
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
	if (skip_gui == false) {
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
	} else {
		var selected_files = temp;
	}
	return selected_files;
}

/*
 * function: save_pos_and_width
 * description: This function is used to collect values from all datasets.
 * 				Finally a fit will be performed and the result may be saved as PNG-file.
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
		if (detailed_results >= 1){
			Fit.doFit("3rd Degree Polynomial", array_pos_all, array_width_all);
			Fit.plot;
			saveAs("PNG", result_dirs[m] + "width_vs_pos.png");
			close();
			Fit.doFit("3rd Degree Polynomial", array_pos_all_calc, array_width_all_calc);
			Fit.plot;
			saveAs("PNG", result_dirs[m] + "width_vs_pos_calc.png");
			close();
		}
		f = File.open(result_dirs[m] + "width_vs_pos.txt");
		print(f, "#x2-position\twidth\tx2-pos_calc\twidth_calc");
		for (p=0; p<array_pos_all.length; p++) {
			print(f, array_pos_all[p] + "\t" + array_width_all[p] + "\t" + array_pos_all_calc[p] + "\t" + array_width_all_calc[p]);
		}
		File.close(f);
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
	/*
	 * These lines will create two arrows.
	 * "Add Selection..." adds the line to the overlay manager.
	 */
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
	/*
	 * The line will be visible, but this will remove the selection markers.
	 */
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
 * description: This function is used to draw data points at the current image.
 * 				The datapoints are added to the overlay.
 */
function addPointsToOverlay(xPos, yPos, overlayColorIndex) {
	color = newArray("Green", "Red", "Blue");
	markerSize = "Tiny";
	if (maxOf(getHeight, getWidth) > 4000) {
		markerSize = "[Extra Large]";
	} else {
		if (maxOf(getHeight, getWidth) > 2000) {
			markerSize = "Large";
		} else {
			if (maxOf(getHeight, getWidth) > 1000) {
				markerSize = "Small";
			}
			}
		}
	run("Point Tool...", "type=Hybrid color=" + color[overlayColorIndex] + " size=" + markerSize);
	for (i=0; i<xPos.length; i++) {
		makePoint(xPos[i] / (binning * jpeg_bin), yPos[i] / (binning * jpeg_bin));
		run("Add Selection...");
	}
}

/*
 * function: prepareFileForWidthFit:
 * description: We need to combine the results of all analysed images to fit the 2D polynomial width (y) versus position (x1, x2).
 * 				This function writes the data stored in 3 arrays to a file.
 */
function prepareFileForWidthFit() {
	f = File.open(result_dirs[m] + "Width.txt");
	print(f, "#x1-position\tx2-position\ty-value");
	for (p=0; p<array_x1.length; p++) {
		print(f, array_x1[p] + "\t" + array_x2[p] + "\t" + array_y[p]);
	}
	File.close(f);
}

/*
 * function: prepareFileForFitBorders
 * description: We need to combine the results of all analysed images to fit all borders with one function series.
 * 				This function writes the data stored in 4 arrays to a file.
 */
function prepareFileForBordersFit() {
	f = File.open(result_dirs[m] + "Borders.txt");
	print(f, "#x1-value\tx2-value\ty-value\tweight");
	for (p=0; p<array_borders_y.length; p++) {
		print(f, array_borders_x1[p] + "\t" + array_borders_x2[p] + "\t" + array_borders_y[p] + "\t" + array_borders_weight[p]);
	}
	File.close(f);
}

/*
 * function: stopMacro
 * description: When the macro is canceled, this function is used to restore the previous settings.
 */
function stopMacro(message) {
	restoreSettings();
	if (message != "") {
		exit(message);
	}
}