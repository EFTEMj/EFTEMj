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
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.gui.WaitForUserDialog;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.filter.RankFilters;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
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
     * The uncorrected spectrum image. No changes will be done to this {@link ImagePlus}.
     */
    private ImagePlus input;
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
    private boolean optimizeParameters;
    /**
     * Determines if the image has to be rotated before and after processing.
     */
    private boolean rotate;
    private Line leftLine;
    private Line rightLine;
    private int[] leftBorder;
    private int[] rightBorder;
    private double oldMax;
    private double oldMin;
    private int mode;
    private EFTEMjLogTool logTool;

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
     */
    @Override
    public int setup(String arg, ImagePlus imp) {
	if (arg == "final") {
	    // TODO Implement final processing. Remove steps from run() and place them here.
	}
	return FLAGS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
     */
    @Override
    public void run(ImageProcessor ip) {
	// TODO Implement the rotation.
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
    }

    private void runAutomaticDetection() {
	if (optimizeParameters == true) {
	    optimizeMaxCount();
	}
	// The input image is not changed. The program only changes values at a duplicate or at new images.
	FloatProcessor input_filtered = (FloatProcessor) input.getProcessor().duplicate();
	applyMedianFilter(input_filtered);

	FloatProcessor input_borderDetection = new FloatProcessor(input.getHeight(), input.getHeight());
	applyBorderDetectionFilter(input_filtered, input_borderDetection);

	int[] leftBorderPosition = new int[input.getHeight()];
	int[] rightBorderPosition = new int[input.getHeight()];
	detectBorder(input_borderDetection, leftBorderPosition, rightBorderPosition);

	int[] linearFitLeft = new int[input.getHeight()];
	int[] linearFitRight = new int[input.getHeight()];
	linearFitLeft = applyLinearFit(leftBorderPosition);
	linearFitRight = applyLinearFit(rightBorderPosition);

	FloatProcessor input_borders;
	if (useLinearFit) {
	    input_borders = paintBorders(linearFitLeft, linearFitRight);
	} else {
	    input_borders = paintBorders(leftBorderPosition, rightBorderPosition);
	}
	ImagePlus ipBorders = new ImagePlus("Borders", input_borders);

	// ImageJ can combine 32-bit greyscale images to an RGB image. A stack
	// with the size 7 is used to handle the 32-bit images and ImageJ shows
	// a RGB image. If you open the saved file with Digital Micrograph it's
	// a regular 32-bit stack.
	ImagePlus[] images = new ImagePlus[7];
	// index 0 = red
	images[0] = ipBorders;
	if (useLinearFit & showUnprocessedBorder) {
	    FloatProcessor unprocessedBorders = paintBorders(leftBorderPosition, rightBorderPosition);
	    ImagePlus ipUnprocessedBorders = new ImagePlus("unprocessed Borders", unprocessedBorders);
	    // index 1 = green
	    images[1] = ipUnprocessedBorders;
	}
	// index 3 = white
	images[3] = input;
	// This class creates the RGB image.
	RGBStackMerge rgbMerge = new RGBStackMerge();
	ImagePlus composite = rgbMerge.mergeHyperstacks(images, true);
	composite.setTitle(input.getTitle());
	composite.show();
	logTool.showLogDialog();
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
	    result.setf(leftBorder[y], y, 255);
	    result.setf(rightBorder[y], y, 255);
	}
	return result;
    }

    /**
     * Applies a linear fit to the given array of {@link Integer}s. This method considers the configured number of fit
     * intervals.
     * 
     * @param input
     * @return A new array of {@link Integer}s that represent a linear fit to the input data.
     */
    private int[] applyLinearFit(int[] input) {
	int[] result = new int[input.length];
	if (linearFitIntervals == 1) {
	    linearFit(input, result, 0, this.input.getHeight() - 1);
	} else {
	    int intervalWidth = (int) Math.floor(1.0 * this.input.getHeight() / linearFitIntervals);
	    int remainder = this.input.getHeight() % linearFitIntervals;
	    int start = 0;
	    int end;
	    for (int i = 1; i <= linearFitIntervals; i++) {
		end = start + intervalWidth - 1;
		// the reminder is distributed to the first intervals
		if (remainder > 0) {
		    end++;
		}
		linearFit(input, result, start, end);
		remainder--;
		start = end + 1;
	    }

	}
	return result;
    }

    /**
     * This method calculates a linear fit. This is done at an interval defined by "start" and "end". Both indices are
     * included.
     * 
     * @param input
     *            is not modified
     * @param result
     *            is modified at the given interval
     * @param start
     *            (included to the interval)
     * @param end
     *            (included to the interval)
     */
    private void linearFit(int[] input, int[] result, int start, int end) {
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
    }

    /**
     * This method calculates the optimal value for <code>maxCount</code>. It checks all values between 1 and twice of
     * the current value of <code>maxCount</code>. The optimisation uses a linear fit of the detected border as a
     * reference. The value of <code>maxCount</code> that results in the smallest variance between the detected border
     * and the fit is chosen at the optimised value.
     */
    private void optimizeMaxCount() {
	int maxCountOld = maxCount;
	logTool.println(String.format("maxCount Startwert: %d", maxCountOld));
	long[] variances = new long[2 * maxCount];
	variances[0] = Long.MAX_VALUE;
	// Process
	int finalIndex = (variances.length - 1);
	int progress = 0;
	IJ.showProgress(0, finalIndex);

	// Median filter
	FloatProcessor input_filtered = (FloatProcessor) input.getProcessor().duplicate();
	applyMedianFilter(input_filtered);

	for (maxCount = 1; maxCount < variances.length; maxCount++) {
	    FloatProcessor input_borderDetection = new FloatProcessor(input.getWidth(), input.getHeight());
	    applyBorderDetectionFilter(input_filtered, input_borderDetection);
	    int[] leftBorderPosition = new int[input.getHeight()];
	    int[] rightBorderPosition = new int[input.getHeight()];
	    detectBorder(input_borderDetection, leftBorderPosition, rightBorderPosition);
	    int[] leftBorderFit = applyLinearFit(leftBorderPosition);
	    int[] rightBorderFit = applyLinearFit(rightBorderPosition);
	    variances[maxCount] = 0;
	    variances[maxCount] += calculateVariance(leftBorderPosition, leftBorderFit);
	    variances[maxCount] += calculateVariance(rightBorderPosition, rightBorderFit);
	    IJ.showProgress(++progress, finalIndex);
	}
	for (int i = 1; i < variances.length; i++) {
	    logTool.println(String.format("maxCount = %d: %d", i, variances[i]));
	}

	// Update maxCount
	maxCount = findMinimumPosition(variances);
	if (maxCount != maxCountOld) {
	    logTool.println(String.format("Setze maxCount auf %d.", maxCount));
	} else {
	    logTool.println("maxCount wurde nicht verändert.");
	}
    }

    /**
     * Finds the index of first occurrence of the smallest value.
     * 
     * @param variances
     *            An array.
     * @return The index of the smallest value (1st occurrence).
     */
    private int findMinimumPosition(long[] variances) {
	long min = variances[0];
	int minPos = 0;
	for (int i = 0; i < variances.length; i++) {
	    if (min > variances[i]) {
		min = variances[i];
		minPos = i;

	    }
	}
	return minPos;
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
     * Creates an instance of {@link RankFilters} and applies a median filter on the given {@link FloatProcessor}. The
     * radius is defined by the field "kernelRadius".
     * 
     * @param ip
     *            has to be a {@link FloatProcessor}
     */
    private void applyMedianFilter(FloatProcessor ip) {
	RankFilters rf = new RankFilters();
	rf.rank(ip, kernelRadius, RankFilters.MEDIAN);
    }

    /**
     * An edge detection filter that calculates the derivation in x-direction.
     * 
     * @param input
     *            {@link FloatProcessor}
     * @param result
     *            {@link FloatProcessor}
     */
    private void applyBorderDetectionFilter(FloatProcessor input, FloatProcessor result) {
	float value;
	for (int y = 0; y < input.getHeight(); y++) {
	    // left border: (-1;y) = (0;y)
	    value = Math.abs(input.getf(1, y) - input.getf(0, y));
	    result.setf(0, y, value);
	    for (int x = 1; x < input.getWidth() - 1; x++) {
		value = Math.abs(input.getf(x + 1, y) - input.getf(x - 1, y));
		result.setf(x, y, value);
	    }
	    // right border: (width;y) = (width-1;y)
	    value = Math.abs(input.getf(input.getWidth() - 1, y) - input.getf(input.getWidth() - 2, y));
	    result.setf(input.getWidth() - 1, y, value);
	}
    }

    /**
     * This filter detects the most outer local maximums at each image line. A side condition is that they contain to
     * the <code>n</code> highest local maximums. <code>n</code> is defined by the filed "maxCount".
     * 
     * @param input
     *            A {@link FloatProcessor} with previously applied edge detection.
     * @param leftBorderPosition
     *            An array of {@link Integer}s to save the position of the left border.
     * @param rightBorderPosition
     *            An array of {@link Integer}s to save the position of the right border.
     */
    private void detectBorder(FloatProcessor input, int[] leftBorderPosition, int[] rightBorderPosition) {
	int countStart = maxCount;
	// this variable is used to count the empty places at the arrays
	// "maxPos" and "maxValues".
	int count;
	// this is the smallest value at the array "maxValues"
	float limit;
	// the next 2 arrays will be accessed by the same index
	int[] maxPos = new int[countStart];
	float[] maxValues = new float[countStart];
	// a temporary array used for sorting the content of "maxValues"
	float[] temp = new float[countStart];

	for (int y = 0; y < input.getHeight(); y++) {
	    // the following code is processed at each line of the image
	    count = countStart;
	    limit = input.getf(0, y);
	    Arrays.fill(maxPos, 0);
	    Arrays.fill(maxValues, 0);
	    maxValues[0] = input.getf(0, y);
	    count--;
	    for (int x = 1; x < input.getWidth(); x++) {
		// 1. check if this pixel is a local maximum
		if (isMaximum(input, x, y)) {
		    // 2. check if there are empty places at maxPos
		    if (count > 0) {
			maxPos[countStart - count] = x;
			maxValues[countStart - count] = input.getf(x, y);
			count--;
			x += localMaxRadius + 1;
			// when the arrays are filled you have to find the
			// smallest maximum
			if (count == 0) {
			    temp = Arrays.copyOf(maxValues, maxValues.length);
			    Arrays.sort(temp);
			    limit = temp[0];
			}
		    }
		    // else part of 2.
		    else {
			// 3. check if the local maximum is larger than the
			// current limit
			if (input.getf(x, y) > limit) {
			    // find out the position of the current limit
			    int index = searchArray(maxValues, limit);
			    // replace the old limit by the new local maximum
			    maxPos[index] = x;
			    maxValues[index] = input.getf(x, y);
			    // The new local maximum might not be the smallest
			    temp = Arrays.copyOf(maxValues, maxValues.length);
			    Arrays.sort(temp);
			    limit = temp[0];
			    // skip all pixels that can't be a local maximum
			    x += localMaxRadius + 1;
			}
		    }
		}
	    }
	    // get the most left and the most right position of the largest
	    // local maximums
	    Arrays.sort(maxPos);
	    leftBorderPosition[y] = maxPos[0 + count];
	    rightBorderPosition[y] = maxPos[maxPos.length - 1];
	    // continue at next image line
	}
    }

    /**
     * Searches the array for the given value and returns the index.
     * 
     * @param array
     * @param value
     * @return
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
     * Checks if the pixel is a local maximum. Only the x-direction is considered.
     * 
     * @param input
     * @param x
     * @param y
     * @return
     */
    private boolean isMaximum(FloatProcessor input, int x, int y) {
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

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.ExtendedPlugInFilter#showDialog(ij.ImagePlus, java.lang.String,
     * ij.plugin.filter.PlugInFilterRunner)
     */
    @Override
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
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
	gd.addMessage("Select mode of the SR-EELS correction.");
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
	GenericDialog gd = new GenericDialog(title + " - set parameters", IJ.getInstance());
	String[] items = { "y-axis", "x-axis" };
	gd.addChoice("energy loss on...", items, items[0]);
	gd.addNumericField("Median filter radius:", kernelRadius, 1, 3, "pixel");
	gd.addNumericField("Maximums per row:", maxCount, 0, 3, null);
	gd.addNumericField("Radius of a local maximum:", localMaxRadius, 0, 2, "pixel");
	gd.addCheckbox("Use a linear fit", false);
	gd.addNumericField("Linear fit steps:", linearFitIntervals, 0, 2, null);
	gd.addCheckbox("Show unprocessed border", true);
	gd.addCheckbox("Use maxCount optimization", false);
	// TODO write the description
	String help = "<html><h3>Parameter</h3><p>description</p></html>";
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
	optimizeParameters = gd.getNextBoolean();
	return OK;
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

    /**
     * Cancel the plugin and show a status message.
     */
    private void canceled() {
	IJ.showStatus("Drift detection has been canceled.");
    }

    private void runManualDetection() {
	oldMax = input.getDisplayRangeMax();
	oldMin = input.getDisplayRangeMin();
	String oldTool = IJ.getToolName();

	input.setDisplayRange(0, 10);
	input.updateAndDraw();
	IJ.setTool(Toolbar.LINE);
	new RoiManager().setVisible(true);
	WaitForUserDialog waitDLG = new WaitForUserDialog("1. line at the left" + System.getProperty("line.separator")
		+ "2. line at the right" + System.getProperty("line.separator")
		+ "Press OK when you have added both lines to the RoiManager");
	waitDLG.show();
	Roi[] rois = RoiManager.getInstance().getRoisAsArray();
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
		// ImageJ can combine 32-bit greyscale images to an RGB image. A stack with the size 7 is used to handle
		// the 32-bit images and ImageJ shows a RGB image. If you open the saved file with Digital Micrograph
		// it's a regular 32-bit stack.
		ImagePlus[] images = new ImagePlus[7];
		// index 0 = red
		images[0] = ipBorders;
		resetDisplayLimits();
		images[3] = input;
		// This class creates the RGB image.
		RGBStackMerge rgbMerge = new RGBStackMerge();
		ImagePlus composite = rgbMerge.mergeHyperstacks(images, true);
		composite.setTitle(input.getTitle());
		composite.show();
		if (RoiManager.getInstance() != null)
		    RoiManager.getInstance().close();
	    }
	}
	resetDisplayLimits();
	IJ.setTool(oldTool);
    }

    private void resetDisplayLimits() {
	input.setDisplayRange(oldMin, oldMax);
	input.updateAndDraw();
    }

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
}