/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 * 
 * Copyright (c) 2014, Michael Entrup b. Epping <entrup@arcor.de>
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

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ProgressBar;
import ij.process.BinaryProcessor;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This class is loaded by {@link SR_EELS_CorrectionPlugin} to perform the correction. It uses the class
 * {@link SR_EELS_CorrectionTask}, which implements {@link Runnable}, to benefit from multicore processors.
 * 
 * @author Michael Entrup b. Epping <entrup@arcor.de>
 */
public class SR_EELS_Correction {

    /**
     * <table>
     * <tr>
     * <td>64</td>
     * <td>show images with width, height and size of each distorted pixel</td>
     * </tr>
     * <tr>
     * <td>128</td>
     * <td>show an output image, that uses only the pixel size to correct the intensity</td>
     * </tr>
     * <tr>
     * <td>192</td>
     * <td>show images that show the distorted pixels at selected positions</td>
     * </tr>
     * <tr>
     * <td>256</td>
     * <td>single thread mode</td>
     * </tr>
     * </table>
     */
    private static int debug_level = 0;
    /**
     * The {@link ImagePlus} that contains the image to correct. The input image is not changed.
     */
    private ImagePlus input;
    /**
     * The {@link FloatProcessor} of the input image.
     */
    private FloatProcessor input_float;
    /**
     * The binning that was used to record the input image.
     */
    private int binning;
    /**
     * A new {@link ImagePlus} that stores the result of the correction.
     */
    private ImagePlus output;
    /**
     * This class represents the mathematical function that is used to perform the correction. It is possible to
     * implement different childs of {@link SR_EELS_CorrectionFunction} to switch between the used functions.
     */
    private SR_EELS_CorrectionFunction function;
    private int subdivision;
    /**
     * This field indicates the progress. A static method is used to increase the value by 1. It is necessary to use
     * volatile because different {@link Thread}s call the related method.
     */
    private static volatile int progress;
    /**
     * Number of steps until the correction is finished.
     */
    private static int progressSteps;

    /**
     * @param function
     * @param oversampling
     * @param subdivision
     */
    private static long startTime;
    private static long intervalTime;
    private static ImagePlus pixelSize;
    private static ImagePlus pixelWidth;
    private static ImagePlus pixelHeight;
    private ImagePlus output_distortion_only;

    public SR_EELS_Correction(ImagePlus imp, int binning, SR_EELS_CorrectionFunction function, int subdivision) {
	input = imp;
	input_float = (FloatProcessor) imp.getProcessor();
	this.binning = binning;
	this.function = function;
	this.subdivision = subdivision;
	debug_level = SR_EELS_CorrectionPlugin.debug_level;
	output = new ImagePlus(input.getShortTitle() + "_corrected", new FloatProcessor(input.getWidth(),
		input.getHeight(), new double[input.getWidth() * input.getHeight()]));
	if (debug_level >= 64) {
	    pixelSize = new ImagePlus(input.getShortTitle() + "_pixel size", new FloatProcessor(input.getWidth(),
		    input.getHeight(), new double[input.getWidth() * input.getHeight()]));
	    pixelWidth = new ImagePlus(input.getShortTitle() + "_pixel width", new FloatProcessor(input.getWidth(),
		    input.getHeight(), new double[input.getWidth() * input.getHeight()]));
	    pixelHeight = new ImagePlus(input.getShortTitle() + "_pixel height", new FloatProcessor(input.getWidth(),
		    input.getHeight(), new double[input.getWidth() * input.getHeight()]));
	}
	if (debug_level >= 128) {
	    output_distortion_only = new ImagePlus(input.getShortTitle() + "_corrected(pixel size only)",
		    new FloatProcessor(input.getWidth(), input.getHeight(), new double[input.getWidth()
			    * input.getHeight()]));
	}
	progressSteps = input.getHeight();
    }

    /**
     * Starts the calculation with parallel {@link Thread}s.
     */
    public void startCalculation() {
	IJ.showProgress(0, progressSteps);
	progress = 0;
	startTime = System.currentTimeMillis();
	intervalTime = startTime;
	if (debug_level >= 256) {
	    for (int x2 = 0; x2 < output.getHeight(); x2++) {
		SR_EELS_CorrectionTask task = new SR_EELS_CorrectionTask(x2);
		task.run();
	    }
	} else {
	    /*
	     * One SR_EELS_CorrectionTask is created for each image line. The ExecutorService will process several of
	     * them in parallel. The main Thread will wait until all tasks are finished.
	     */
	    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	    for (int x2 = 0; x2 < output.getHeight(); x2++) {
		executorService.execute(new SR_EELS_CorrectionTask(x2));
	    }
	    executorService.shutdown();
	    try {
		executorService.awaitTermination(24, TimeUnit.HOURS);
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}
    }

    /**
     * 
     */
    public void showResult() {
	output.show();
	if (debug_level >= 64) {
	    pixelSize.show();
	    pixelWidth.show();
	    pixelHeight.show();
	}
	if (debug_level >= 128) {
	    output_distortion_only.show();
	}
    }

    /**
     * {@link SR_EELS_CorrectionTask} will use this method to update the process and to estimate the remaining time.
     * <p />
     * The {@link ProgressBar} is updated each time. The time estimation is only updated every second to prevent
     * flickering when more than 1 {@link Thread} is used.
     */
    private static void updateProgress() {
	progress++;
	IJ.showProgress(progress, progressSteps);
	if ((System.currentTimeMillis() - startTime) > 5000 & (System.currentTimeMillis() - intervalTime) > 1000) {
	    IJ.showStatus(String.format(
		    "About %ds remaining...",
		    (int) Math.ceil((1. / 1000 * (progressSteps - progress) / progress)
			    * (System.currentTimeMillis() - startTime))));
	    intervalTime = System.currentTimeMillis();
	}
    }

    private class SR_EELS_CorrectionTask implements Runnable {

	private static final int WIDTH = SR_EELS_CorrectionPlugin.FULL_WIDTH;
	private static final int HEIGHT = SR_EELS_CorrectionPlugin.FULL_HEIGHT;
	/**
	 * The image row to process.
	 */
	private int x2;

	/**
	 * A default constructor that only sets one field.
	 * 
	 * @param y
	 *            The image row to process.
	 */
	public SR_EELS_CorrectionTask(int x2) {
	    super();
	    this.x2 = x2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
	    for (int x1 = 0; x1 < input.getWidth(); x1++) {
		try {
		    output.getProcessor().setf(x1, x2, (float) getCorrectetIntensity(x1, x2));
		} catch (SR_EELS_Exception e) {
		    output.getProcessor().setf(x1, x2, Float.NaN);
		}
	    }
	    SR_EELS_Correction.updateProgress();
	}

	private double getCorrectetIntensity(int x1, int x2) throws SR_EELS_Exception {
	    double intensity = 0.0;
	    double[] point00 = function.transform(x1, x2, binning);
	    if (subdivision > 1) {
		double[] point01 = function.transform(x1, x2 + 1, binning);
		double[] point10 = function.transform(x1 + 1, x2, binning);
		double[] point11 = function.transform(x1 + 1, x2 + 1, binning);
		/*
		 * As the origin is at the top, left corner of an image, the top of the rectangle has the lowest y2
		 * value and the bottom of the rectangle has the highest y2 value.
		 */
		double rectangle_l = Math.floor(subdivision
			* Math.min(point00[0], Math.min(point01[0], Math.min(point10[0], point11[0]))))
			/ subdivision;
		double rectangle_b = Math.ceil(subdivision
			* Math.max(point00[1], Math.max(point01[1], Math.max(point10[1], point11[1]))))
			/ subdivision;
		double rectangle_r = Math.ceil(subdivision
			* Math.max(point00[0], Math.max(point01[0], Math.max(point10[0], point11[0]))))
			/ subdivision;
		double rectangle_t = Math.floor(subdivision
			* Math.min(point00[1], Math.min(point01[1], Math.min(point10[1], point11[1]))))
			/ subdivision;
		if (rectangle_l < 0 | rectangle_t < 0 | rectangle_r >= WIDTH | rectangle_b >= HEIGHT) {
		    if (debug_level >= 64) {
			pixelSize.getProcessor().setf(x1, x2, Float.NaN);
			pixelWidth.getProcessor().setf(x1, x2, Float.NaN);
			pixelHeight.getProcessor().setf(x1, x2, Float.NaN);
		    }
		    return intensity = Double.NaN;
		}
		if (debug_level >= 128) {
		    output_distortion_only.getProcessor().setf(x1, x2,
			    input.getProcessor().getf((int) Math.floor(point00[0]), (int) Math.floor(point00[1])));
		}
		double border = 2.0 / subdivision;
		rectangle_l = rectangle_l - border;
		rectangle_r = rectangle_r + border;
		rectangle_t = rectangle_t - border;
		rectangle_b = rectangle_b + border;
		double rectangle_width = rectangle_r - rectangle_l;
		double rectangle_height = rectangle_b - rectangle_t;
		int temp_width = (int) Math.ceil(rectangle_width * subdivision) + 1;
		int temp_height = (int) Math.ceil(rectangle_height * subdivision) + 1;
		if (debug_level >= 63) {
		    pixelWidth.getProcessor().setf(x1, x2, temp_width);
		    pixelHeight.getProcessor().setf(x1, x2, temp_height);
		}
		if (temp_height * temp_width <= 0) {
		    System.out.println("Array Länge kleiner als 0!");
		}
		ByteProcessor byteP = new ByteProcessor(temp_width, temp_height);
		byteP.set(0);
		for (int z2 = 0; z2 < subdivision; z2++) {
		    double z2d = 1.0 * z2 / subdivision;
		    for (int z1 = 0; z1 < subdivision; z1++) {
			double z1d = 1.0 * z1 / subdivision;
			double[] point = function.transform(x1 + z1d, x2 + z2d, binning);
			int y1 = (int) Math.round(subdivision * (point[0] - rectangle_l));
			int y2 = (int) Math.round(subdivision * (point[1] - rectangle_t));
			byteP.set(y1, y2, 255);
		    }
		}
		BinaryProcessor binaryP = new BinaryProcessor(byteP);
		dilate(binaryP);
		erode(binaryP);
		if (debug_level >= 192) {
		    if (x2 == input.getHeight() / 2 & x1 % 128 == 0) {
			new ImagePlus("pixel" + x1, binaryP).show();
		    }
		}
		int pixelSizeCount = 0;
		int borderInt = (int) Math.round(border * subdivision);
		for (int y2i = borderInt; y2i < temp_height - 2 * borderInt; y2i++) {
		    for (int y1i = borderInt; y1i < temp_width - 2 * borderInt; y1i++) {
			if (binaryP.getPixel(y1i, y2i) > 0) {
			    int y1 = (int) Math.floor(1.0 * y1i / subdivision + rectangle_l);
			    int y2 = (int) Math.floor(1.0 * y2i / subdivision + rectangle_t);
			    intensity += input_float.getf(y1, y2) / Math.pow(subdivision, 2);
			    pixelSizeCount++;
			}
		    }
		}
		if (debug_level >= 128) {
		    pixelSize.getProcessor().setf(x1, x2, (float) (pixelSizeCount / Math.pow(subdivision, 2)));
		}
	    } else {
		if (point00[0] < 0 | point00[1] < 0 | point00[0] >= WIDTH | point00[1] >= HEIGHT) {
		    intensity = Double.NaN;
		} else {
		    intensity = input_float.getf((int) Math.floor(point00[0]), (int) Math.floor(point00[1]));
		}
	    }
	    return intensity;
	}
    }

    private void dilate(BinaryProcessor bp) {
	int[][] H = { { 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 } };
	int ic = (H[0].length - 1) / 2;
	int jc = (H.length - 1) / 2;

	ImageProcessor np = bp.createProcessor(bp.getWidth(), bp.getHeight());

	for (int j = 0; j < H.length; j++) {
	    for (int i = 0; i < H[j].length; i++) {
		if (H[j][i] == 1) {
		    np.copyBits(bp, i - ic, j - jc, Blitter.MAX);
		}
	    }
	}
	bp.copyBits(np, 0, 0, Blitter.COPY);
    }

    private void erode(BinaryProcessor bp) {
	bp.invert();
	dilate(bp);
	bp.invert();
    }
}
