/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 * 
 * Copyright (c) 2013, Michael Epping
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package sr_eels;

import java.util.Arrays;
import tools.EFTEMjLogTool;
import gui.ExtendedWaitForUserDialog;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.filter.RankFilters;
import ij.plugin.filter.Transformer;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

/**
 * This plugin is used to correct SR-EELS data that shows a geometric aberration. This aberration is visible in SR-EELS
 * data recorded with a Zeiss in-column Omega filter. Using a Gatan post-column filter the aberration may be corrected
 * by the filter itself.
 * 
 * The correction consists of two steps. The first is to identify the inclined border of the spectrum. An automatic and
 * a manual method are available. The second step is to process the SR-EELS data to correct the aberration. The plugin
 * {@link SR_EELS_CorrectionOnlyPlugin} will perform this step.
 * 
 * For faster processing the energy loss direction has to be the y-axis. Images with the energy loss direction at the
 * x-axis will be rotated during processing.
 * 
 * @author Michael Epping <michael.epping@uni-muenster.de>
 * 
 */
public class SR_EELS_CorrectionPlugin implements ExtendedPlugInFilter {

    /**
     * The automatic mode will be used.
     */
    private final int AUTOMATIC = 2;
    /**
     * The manual mode will be used.
     */
    private final int MANUAL = 4;
    /**
     * The plugin will be aborted.
     */
    private final int CANCEL = 0;
    /**
     * The plugin will continue with the next step.
     */
    private final int OK = 1;
    /**
     * <code>DOES_32 | NO_CHANGES | FINAL_PROCESSING</code>
     */
    private final int FLAGS = DOES_32 | NO_CHANGES | FINAL_PROCESSING;
    /**
     * The commend that was used t run the plugin. This is used as a prefix for all dialog titles.
     */
    private String command;
    /**
     * An instance of {@link EFTEMjLogTool}.
     */
    private EFTEMjLogTool logTool;
    /**
     * The uncorrected spectrum image. No changes will be done to this {@link ImagePlus}, except a temporary rotation by
     * 90° that will be undone at the end.
     */
    private ImagePlus input;
    /**
     * An {@link ImagePlus} (this is a composite image) containing a copy of the uncorrected SR-EELS data and 1(2)
     * channels that show(s) the spectrum border (unprocessed and/or optional a linear fit).
     */
    private ImagePlus result;
    /**
     * The radius of the kernel used for the median filter. Select "Process > Filters > Show Circular Masks..." at
     * ImageJ to see all possible kernels.
     */
    private float kernelRadius = 2;
    /**
     * This is the range, in pixel, that is used to identify a local maximum.
     */
    private int localMaxRadius;
    /**
     * The number of local maximums (borders) that are regarded to find the left and right border of the Spectrum.
     */
    private int maxCount = 10;
    /**
     * This {@link Boolean} turns the linear fit of the left and right border on or off.
     */
    private boolean useLinearFit = false;
    /**
     * Each border is split into this number of intervals. At each interval a separate linear fit is done.
     */
    private int linearFitIntervals = 1;
    /**
     * If a linear fit is used, the unprocessed border can be added to the composite image.
     */
    private boolean showUnprocessedBorder = true;
    /**
     * When this value is true, an iterative process is used to automatically optimize the parameters
     * <code>kernelRadius</code> and <code>maxCount</code>.
     */
    private boolean optimizeMaxCount;
    /**
     * Determines if the image has to be rotated before and after processing.
     */
    private boolean rotate;
    private Line leftLine;
    private Line rightLine;
    private int[] leftBorder;
    private int[] rightBorder;
    private int mode;

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
     */
    @Override
    public int setup(String arg, ImagePlus imp) {
	if (arg == "final" & result != null) {
	    // TODO Edit SR_EELS_CorrectionOnlyPlugin to only match this type of call.
	    SR_EELS_CorrectionOnlyPlugin correction = new SR_EELS_CorrectionOnlyPlugin();
	    correction.showDialog(result, command, null);
	    correction.run(result.getProcessor());
	    correction.setup("final", result);
	}
	return FLAGS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.ExtendedPlugInFilter#showDialog(ij.ImagePlus, java.lang.String,
     * ij.plugin.filter.PlugInFilterRunner)
     */
    @Override
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
	this.command = command;
	logTool = new EFTEMjLogTool(command);
	input = imp;
	switch (showModeDialog(command)) {
	case AUTOMATIC:
	    IJ.showStatus("Automatic SR-EELS correction has been selected.");
	    if (showParameterDialog(command) == CANCEL) {
		canceled();
		return NO_CHANGES | DONE;
	    }
	    mode = AUTOMATIC;
	    break;
	case MANUAL:
	    IJ.showStatus("Manual SR-EELS correction has been selected.");
	    mode = MANUAL;
	    break;
	default:
	    canceled();
	    return NO_CHANGES | DONE;
	}
	return FLAGS;
    }

    /**
     * The user is asked to select the SR-EELS correction mode. A {@link GenericDialog} is used for this purpose. The
     * Buttons <code>Ok</code> and <code>Cancel</code> are labelled <code>Automatic</code> and <code>Manual</code>.
     * 
     * @param title
     * @return the selected mode for the SR-EELS correction
     */
    private int showModeDialog(String title) {
	GenericDialog gd = new GenericDialog(title + " - detection mode", IJ.getInstance());
	gd.addMessage("Select the mode of the SR-EELS correction.");
	gd.setOKLabel("Automatic");
	gd.setCancelLabel("Manual");
	// TODO write the description
	String help = "<html><h3>Automatic mode</h3>" + "<p>description</p>" + "<h3>Manual mode</h3>"
		+ "<p>description</p></html>";
	gd.addHelp(help);
	gd.showDialog();
	if (gd.wasOKed()) {
	    return AUTOMATIC;
	} else if (gd.wasCanceled()) {
	    return MANUAL;
	}
	return CANCEL;
    }

    /**
     * This dialog is used to setup the parameter for the automatic drift detection.
     * 
     * @param title
     * @return OK or CANCEL
     */
    private int showParameterDialog(String title) {
	GenericDialog gd = new GenericDialog(title + " - set detection parameters", IJ.getInstance());
	String[] items = { "x-axis", "y-axis" };
	// Try to make a good default selection.
	String selectedItem = ((input.getWidth() >= input.getHeight()) ? items[0] : items[1]);
	gd.addChoice("Energy loss on...", items, selectedItem);
	gd.addNumericField("Median filter radius:", kernelRadius, 1, 3, "pixel");
	gd.addNumericField("Maximums per row:", maxCount, 0, 3, null);
	gd.addNumericField("Radius of a local maximum:", localMaxRadius, 0, 2, "pixel");
	gd.addCheckbox("Use a linear fit:", false);
	gd.addNumericField("Linear fit steps:", linearFitIntervals, 0, 2, null);
	gd.addCheckbox("Show unprocessed border", true);
	gd.addCheckbox("Optimise maxCount:", false);
	// TODO write the description
	String help = "<html><h3>Detection parameters</h3><p>description</p></html>";
	gd.addHelp(help);
	gd.showDialog();
	if (gd.wasCanceled() == true) {
	    return CANCEL;
	}
	// for faster processing the energy loss axis has to be the y-axis.
	switch (gd.getNextChoice()) {
	case "x-axis":
	    rotate = true;
	    break;
	case "y-axis":
	    rotate = false;
	    break;
	default:
	    break;
	}
	kernelRadius = (float) gd.getNextNumber();
	maxCount = (int) gd.getNextNumber();
	localMaxRadius = (int) gd.getNextNumber();
	useLinearFit = gd.getNextBoolean();
	linearFitIntervals = (int) gd.getNextNumber();
	if ((int) Math.floor(1.0 * input.getHeight() / linearFitIntervals) < 3) {
	    linearFitIntervals = (int) Math.floor(1.0 * input.getHeight() / 3);
	    IJ.showMessage("Linear fit steps", "The number of fit steps has been reduced to " + linearFitIntervals
		    + ".\n " + "You need an intervall of at least 3 pixel tobenefit from a linear fit.");
	}
	showUnprocessedBorder = gd.getNextBoolean();
	optimizeMaxCount = gd.getNextBoolean();
	return OK;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
     */
    @Override
    public void run(ImageProcessor ip) {
	if (rotate == true) {
	    Transformer transformer = new Transformer();
	    transformer.setup("right", input);
	    transformer.run(input.getProcessor());
	}
	switch (mode) {
	case AUTOMATIC:
	    runAutomaticDetection();
	    break;
	case MANUAL:
	    runManualDetection();
	    break;
	default:
	    break;
	}
	// Select the grayscale channel.
	if (result.getStackSize() == 2) {
	    result.setC(2);
	} else if (result.getStackSize() == 3) {
	    result.setC(3);
	}
	if (rotate == true) {
	    Transformer transformer = new Transformer();
	    transformer.setup("left", input);
	    transformer.run(input.getProcessor());
	    transformer.setup("left", result);
	    transformer.run(result.getProcessor());
	}
    }

    /**
     * Code of the automatic spectrum border detection.
     */
    private void runAutomaticDetection() {
	// The input image is not changed. The program only changes values at a duplicate or at new images.
	FloatProcessor fp_filtered = (FloatProcessor) input.getProcessor().duplicate();
	RankFilters rf = new RankFilters();
	rf.rank(fp_filtered, kernelRadius, RankFilters.MEDIAN);
	FloatProcessor fp_border = new FloatProcessor(input.getHeight(), input.getHeight());
	// The filter matrix (-1, 0, 1) is applied.
	for (int y = 0; y < fp_filtered.getHeight(); y++) {
	    // left border: (-1;y) = (0;y)
	    float value = Math.abs(fp_filtered.getf(1, y) - fp_filtered.getf(0, y));
	    fp_border.setf(0, y, value);
	    for (int x = 1; x < fp_filtered.getWidth() - 1; x++) {
		value = Math.abs(fp_filtered.getf(x + 1, y) - fp_filtered.getf(x - 1, y));
		fp_border.setf(x, y, value);
	    }
	    // right border: (width;y) = (width-1;y)
	    value = Math.abs(fp_filtered.getf(input.getWidth() - 1, y) - fp_filtered.getf(input.getWidth() - 2, y));
	    fp_border.setf(fp_filtered.getWidth() - 1, y, value);
	}
	int[] leftBorderPosition = new int[input.getHeight()];
	int[] rightBorderPosition = new int[input.getHeight()];
	int[] linearFitLeft = new int[input.getHeight()];
	int[] linearFitRight = new int[input.getHeight()];
	int[] leftBorderPositionTemp = new int[input.getHeight()];
	int[] rightBorderPositionTemp = new int[input.getHeight()];
	int[] linearFitLeftTemp = new int[input.getHeight()];
	int[] linearFitRightTemp = new int[input.getHeight()];
	long[] variances = (optimizeMaxCount) ? new long[2 * maxCount] : new long[1];
	variances[0] = Long.MAX_VALUE;
	// This index is 0 based.
	int minVarianceIndex = 0;
	// This index is 1 based.
	int currentMaxCount = (optimizeMaxCount) ? 1 : maxCount;
	// ProcessBar
	int finalIndex = (variances.length - 1);
	int progress = 0;
	IJ.showProgress(0, finalIndex);
	// I use a do-while-loop to optimise maxCount. If the optimisation is not selected, the loop will stop after
	// the first execution.
	do {
	    // This variable is used to count the empty places at the arrays "maxPos" and "maxValues".
	    int count;
	    // This is the smallest value at the array "maxValues".
	    float limit;
	    // The next 2 arrays will be accessed by the same index.
	    int[] maxPos = new int[currentMaxCount];
	    float[] maxValues = new float[currentMaxCount];
	    for (int y = 0; y < input.getHeight(); y++) {
		// The following code is processed at each line of the image.
		count = currentMaxCount;
		Arrays.fill(maxPos, 0);
		Arrays.fill(maxValues, 0);
		maxValues[0] = fp_border.getf(0, y);
		limit = fp_border.getf(0, y);
		count--;
		for (int x = 1; x < input.getWidth(); x++) {
		    // 1. Check if this pixel is a local maximum.
		    if (isLocalMaximum(fp_border, x, y)) {
			// 2. Check if there are empty places at maxPos.
			if (count > 0) {
			    maxPos[currentMaxCount - count] = x;
			    maxValues[currentMaxCount - count] = fp_border.getf(x, y);
			    count--;
			    // Skip all pixels that can't be a local maximum.
			    x += localMaxRadius + 1;
			    // When the arrays are filled you have to find the smallest maximum.
			    if (count == 0) {
				// A temporary array used for sorting the content of "maxValues".
				float[] temp = Arrays.copyOf(maxValues, maxValues.length);
				Arrays.sort(temp);
				limit = temp[0];
			    }
			}
			// else part of 2.
			else {
			    // 3. Check if the local maximum is larger than the current limit.
			    if (fp_border.getf(x, y) > limit) {
				// Find out the position of the current limit.
				int index = searchArray(maxValues, limit);
				// Replace the old limit by the new local maximum.
				maxPos[index] = x;
				maxValues[index] = fp_border.getf(x, y);
				// The new local maximum might not be the smallest.
				// temporary array used for sorting the content of "maxValues".
				float[] temp = Arrays.copyOf(maxValues, maxValues.length);
				Arrays.sort(temp);
				limit = temp[0];
				// Skip all pixels that can't be a local maximum.
				x += localMaxRadius + 1;
			    }
			}
		    }
		}
		// Get the most left and the most right position of the largest local maximums.
		Arrays.sort(maxPos);
		// If count is not zero you have to skip the first entries of the array.
		leftBorderPosition[y] = maxPos[0 + count];
		rightBorderPosition[y] = maxPos[maxPos.length - 1];
		// Continue at next image line
	    }
	    linearFitLeft = applyLinearFit(leftBorderPosition);
	    linearFitRight = applyLinearFit(rightBorderPosition);
	    variances[(optimizeMaxCount ? currentMaxCount - 1 : 0)] = calculateVariance(leftBorderPosition,
		    linearFitLeft) + calculateVariance(rightBorderPosition, linearFitRight);
	    logTool.println(String.format("Variance (maxCount = %d): %d", currentMaxCount,
		    variances[(optimizeMaxCount ? currentMaxCount - 1 : 0)]));
	    if (optimizeMaxCount && variances[currentMaxCount - 1] < variances[minVarianceIndex]) {
		minVarianceIndex = currentMaxCount - 1;
		leftBorderPositionTemp = Arrays.copyOf(leftBorderPosition, leftBorderPosition.length);
		rightBorderPositionTemp = Arrays.copyOf(rightBorderPosition, rightBorderPosition.length);
		linearFitLeftTemp = Arrays.copyOf(linearFitLeft, linearFitLeft.length);
		linearFitRightTemp = Arrays.copyOf(linearFitRight, linearFitRight.length);
	    }
	    IJ.showProgress(++progress, finalIndex);
	    currentMaxCount++;
	    optimizeMaxCount = (optimizeMaxCount & currentMaxCount <= 2 * maxCount) ? true : false;
	} while (optimizeMaxCount);
	if (variances.length > 1) {
	    logTool.println(String.format("The optimal maxCount is %d.", minVarianceIndex + 1));
	    leftBorderPosition = leftBorderPositionTemp;
	    rightBorderPosition = rightBorderPositionTemp;
	    linearFitLeft = linearFitLeftTemp;
	    linearFitRight = linearFitRightTemp;
	}
	// ImageJ can combine 32-bit greyscale images to an RGB image. A stack
	// with the size 7 is used to handle the 32-bit images and ImageJ shows
	// a RGB image. If you open the saved file with Digital Micrograph it's
	// a regular 32-bit stack.
	ImagePlus[] images = new ImagePlus[7];
	// index 0 = red
	images[0] = (useLinearFit) ? new ImagePlus("Borders", paintBorders(linearFitLeft, linearFitRight))
		: new ImagePlus("borders", paintBorders(leftBorderPosition, rightBorderPosition));
	// index 1 = green
	images[1] = (useLinearFit & showUnprocessedBorder) ? new ImagePlus("Unprocessed Borders", paintBorders(
		leftBorderPosition, rightBorderPosition)) : null;
	// index 2 = blue
	// index 3 = white
	images[3] = input;
	// This class creates the RGB image.
	RGBStackMerge rgbMerge = new RGBStackMerge();
	ImagePlus composite = rgbMerge.mergeHyperstacks(images, true);
	composite.setTitle(input.getTitle());
	composite.show();
	result = composite;
	logTool.showLogDialog();
    }

    /**
     * Searches the array for the given value and returns the index. In contrast to <code>Arrays.binarySearch()</code>
     * the array has not to be sorted.
     * 
     * @param array
     *            An array that can be unsorted.
     * @param value
     *            The value to search for.
     * @return The index of the first occurrence.
     */
    private int searchArray(float[] array, float value) {
	int i;
	for (i = 0; i < array.length; i++) {
	    if (array[i] == value) {
		break;
	    }
	}
	return i;
    }

    /**
     * Creates a {@link FloatProcessor} that shows the left and the right borders as white (255) on black (0).
     * 
     * @param leftBorder
     * @param rightBorder
     * @return A new {@link FloatProcessor}
     */
    private FloatProcessor paintBorders(int[] leftBorder, int[] rightBorder) {
	FloatProcessor result = new FloatProcessor(input.getWidth(), input.getHeight());
	result.setValue(0);
	result.fill();
	for (int y = 0; y < input.getHeight(); y++) {
	    // TODO Check for values outside the image boundaries.
	    result.setf(leftBorder[y], y, 255);
	    result.setf(rightBorder[y], y, 255);
	}
	return result;
    }

    /**
     * Applies a linear fit to the given array of int values. This method considers the configured number of fit
     * intervals.
     * 
     * @param input
     *            An array of int values.
     * @return A new array of {@link Integer}s that represent a linear fit to the input data.
     */
    private int[] applyLinearFit(int[] input) {
	int[] result = new int[input.length];
	int intervalWidth = (int) Math.floor(1.0 * this.input.getHeight() / linearFitIntervals);
	int remainder = this.input.getHeight() % linearFitIntervals;
	int start = 0;
	int end;
	for (int index = 1; index <= linearFitIntervals; index++) {
	    end = start + intervalWidth - 1;
	    // the reminder is distributed to the first intervals
	    if (remainder > 0) {
		end++;
	    }
	    float meanX;
	    float meanY;
	    int count = 0;
	    float sumX = 0;
	    float sumY = 0;
	    for (int i = start; i <= end; i++) {
		if (input[i] != 0 & input[i] != this.input.getWidth()) {
		    sumX += i;
		    sumY += input[i];
		    count++;
		}
	    }
	    meanX = sumX / count;
	    meanY = sumY / count;
	    float sumCovar = 0;
	    float sumX2 = 0;
	    for (int i = start; i <= end; i++) {
		if (input[i] != 0) {
		    sumCovar += (i - meanX) * (input[i] - meanY);
		    sumX2 += Math.pow(i - meanX, 2);
		}
	    }
	    float m = sumCovar / sumX2;
	    float b = meanY - m * meanX;
	    for (int i = start; i <= end; i++) {
		result[i] = Math.round(m * i + b);
	    }

	    remainder--;
	    start = end + 1;
	}

	return result;
    }

    /**
     * Calculates the variance of the given arrays. Both must have the same length.
     * 
     * @param values
     *            1st array with length N.
     * @param fit
     *            2nd array with length N.
     * @return The variance.
     */
    private long calculateVariance(int[] values, int[] fit) {
	long variance = 0;
	for (int i = 0; i < values.length; i++) {
	    variance += Math.pow(values[i] - fit[i], 2);
	}
	return variance;
    }

    /**
     * Checks if the pixel is a local maximum. Only the x-direction is considered.
     * 
     * @param input
     *            A {@link FloatProcessor}
     * @param x
     *            The x-axis coordinate.
     * @param y
     *            The y-axis coordinate.
     * @return True if the Point (x,y) is a local maximum.
     */
    private boolean isLocalMaximum(FloatProcessor input, int x, int y) {
	boolean isMax = true;
	float testValue = input.getf(x, y);
	// all neighbouring pixels are stored at an array
	float[] neighbor = new float[2 * localMaxRadius + 1];
	for (int i = -localMaxRadius; i <= localMaxRadius; i++) {
	    if (x + i >= 0 & x + i < input.getWidth()) {
		neighbor[i + localMaxRadius] = input.getf(x + i, y);
	    } else {
		if (x + 1 < 0) {
		    neighbor[i + localMaxRadius] = input.getf(0, y);
		} else {
		    if (x + 1 >= input.getWidth()) {
			neighbor[i + localMaxRadius] = input.getf(input.getWidth() - 1, y);
		    }
		}
	    }
	}
	// check if no neighbour is larger than the given pixel (x,y)
	for (int i = 0; i < neighbor.length; i++) {
	    if (neighbor[i] > testValue) {
		isMax = false;
		break;
	    }
	}
	return isMax;
    }

    /**
     * Code of the manual spectrum border detection.
     */
    private void runManualDetection() {
	// Store some settings for resetting them later on.
	double oldMax = input.getDisplayRangeMax();
	double oldMin = input.getDisplayRangeMin();
	String oldTool = IJ.getToolName();
	// Set a display limit that enhances the visibility of the spectrum border.
	int[] histogram = input.getStatistics(ImageStatistics.MIN_MAX, 256, 0.0, Math.pow(2, 16)).histogram;
	int limit = 0;
	int sum = 0;
	while (sum < 0.01 * input.getWidth() * input.getHeight()) {
	    sum += histogram[limit];
	    limit++;
	}
	input.setDisplayRange(0, Math.pow(2, 16) / 256 * limit);
	input.updateAndDraw();
	// Wait for the user to place two lines as ROIs.
	IJ.setTool(Toolbar.LINE);
	RoiManager roiManager = new RoiManager();
	roiManager.runCommand("show all with labels");
	roiManager.setVisible(true);
	// TODO Add some controls (e.g. Zoom) to the ExtendedWaitForUserDialog.
	ExtendedWaitForUserDialog waitDLG = new ExtendedWaitForUserDialog(command + " - manual mode",
		"Place two lines at the border of the spectrum\n" + "1. line at the left side\n"
			+ "2. line at the right side\n" + "Press OK when you have added both lines to the RoiManager",
		null);
	waitDLG.show();
	// The user has pressed Ok.
	Roi[] rois = roiManager.getRoisAsArray();
	roiManager.runCommand("show none");
	roiManager.close();
	if (rois.length == 2) {
	    if (rois[0].isLine() & rois[1].isLine()) {
		leftLine = (Line) rois[0];
		rightLine = (Line) rois[1];
		System.out.println(leftLine.toString());
		System.out.println(leftLine.x1 + "; " + leftLine.x2 + "; " + leftLine.y1 + "; " + leftLine.y2);
		System.out.println(rightLine.toString());
		System.out.println(rightLine.x1 + "; " + rightLine.x2 + "; " + rightLine.y1 + "; " + rightLine.y2);
		leftBorder = lineToArray(leftLine);
		rightBorder = lineToArray(rightLine);
		FloatProcessor input_borders;
		input_borders = paintBorders(leftBorder, rightBorder);
		ImagePlus ipBorders = new ImagePlus("Borders", input_borders);
		// A composite image can contain up to 7 channels.
		ImagePlus[] images = new ImagePlus[7];
		// index 0 = red
		images[0] = ipBorders;
		// index 1 = green
		// index 2 = blue
		// index 3 = grey
		images[3] = input;
		// It is easier to reset the display limits before creating the composite images.
		input.setDisplayRange(oldMin, oldMax);
		// This class creates the RGB image.
		RGBStackMerge rgbMerge = new RGBStackMerge();
		ImagePlus composite = rgbMerge.mergeHyperstacks(images, true);
		composite.setTitle(input.getTitle());
		composite.show();
		result = composite;
	    }
	}
	// Reset some settings.
	input.setDisplayRange(oldMin, oldMax);
	input.updateAndDraw();
	IJ.setTool(oldTool);
    }

    /**
     * This method calculates slope and intercept of the given line. Both values are used to fill the array. The index
     * of the array is the y-axis of the input image. The array is filled by evaluating the equation
     * <code>f(y) = Math.round((y - intercept) / slope)</code>.
     * 
     * @param line
     *            A line object.
     * @return An array representing the line.
     */
    private int[] lineToArray(Line line) {
	int[] border = new int[input.getHeight()];
	double slope = (line.y2d - line.y1d) / (line.x2d - line.x1d);
	System.out.println(String.format("slope: %s", slope));
	double intercept = -line.x1d * slope + line.y1d;
	System.out.println(String.format("intercept: %s", intercept));
	for (int y = 0; y < border.length; y++) {
	    border[y] = (int) Math.round((y - intercept) / slope);
	}
	return border;
    }

    /**
     * Cancel the plugin and show a status message.
     */
    private void canceled() {
	IJ.showStatus("Drift detection has been canceled.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.ExtendedPlugInFilter#setNPasses(int)
     */
    @Override
    public void setNPasses(int nPasses) {
	// This method is not used.
    }
}