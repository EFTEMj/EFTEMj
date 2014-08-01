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
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Michael Entrup b. Epping <entrup@arcor.de>
 *
 */
public class SR_EELS_Correction {

    private ImagePlus input;
    private int binning;
    private ImagePlus output;
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

    public SR_EELS_Correction(ImagePlus imp, int binning, SR_EELS_CorrectionFunction function, int subdivision) {
	input = imp;
	this.binning = binning;
	this.function = function;
	this.subdivision = subdivision;
	output = new ImagePlus(input.getTitle() + "_corrected", new FloatProcessor(input.getWidth(), input.getHeight(),
		new double[input.getWidth() * input.getHeight()]));
	pixelSize = new ImagePlus(input.getTitle() + "pixel size", new FloatProcessor(input.getWidth(),
		input.getHeight(), new double[input.getWidth() * input.getHeight()]));
	progressSteps = input.getHeight();
    }

    /**
     * Starts the calculation with parallel {@link Thread}s.
     */
    public void startCalculation() {
	boolean debug = false;
	if (debug) {
	    for (int x2 = 0; x2 < output.getHeight(); x2++) {
		SR_EELS_CorrectionTask task = new SR_EELS_CorrectionTask(x2);
		task.run();
	    }
	} else {
	    IJ.showProgress(0, progressSteps);
	    progress = 0;
	    startTime = System.currentTimeMillis();
	    intervalTime = startTime;
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
		executorService.awaitTermination(60, TimeUnit.MINUTES);
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
	pixelSize.show();

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
	private int bin;

	/**
	 * A default constructor that only sets one field.
	 * 
	 * @param y
	 *            The image row to process.
	 */
	public SR_EELS_CorrectionTask(int x2) {
	    super();
	    this.x2 = x2;
	    bin = binning;
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
		    output.getProcessor().setf(x1, x2, (float) getCorrectetIntensity(x1 * bin, x2 * bin));
		} catch (SR_EELS_Exception e) {
		    output.getProcessor().setf(x1, x2, Float.NaN);
		}
	    }
	    SR_EELS_Correction.updateProgress();
	}

	private double getCorrectetIntensity(int x1, int x2) throws SR_EELS_Exception {
	    double intensity = 0.0;
	    double[] point00 = function.transform(x1, x2);
	    double[] point01 = function.transform(x1, x2 + bin);
	    double[] point10 = function.transform(x1 + bin, x2);
	    double[] point11 = function.transform(x1 + bin, x2 + bin);
	    /*
	     * As the origin is at the top, left corner of an image, the top of the rectangle has the lowest y2 value
	     * and the bottom of the rectangle has the highest y2 value.
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
		pixelSize.getProcessor().setf(x1 / bin, x2 / bin, Float.NaN);
		return intensity = Double.NaN;
	    }
	    double rectangle_width = rectangle_r - rectangle_l;
	    double rectangle_height = rectangle_b - rectangle_t;
	    int temp_width = (int) Math.ceil(rectangle_width / bin * subdivision) + 1;
	    int temp_height = (int) Math.ceil(rectangle_height / bin * subdivision) + 1;
	    if (temp_height * temp_width <= 0) {
		System.out.println("Array Länge kleiner als 0!");
	    }
	    ByteProcessor ip = new ByteProcessor(temp_width, temp_height);
	    BinaryProcessor bp = new BinaryProcessor(ip);
	    bp.set(0);
	    for (int z2 = 0; z2 < subdivision; z2++) {
		double z2d = 1.0 * z2 / subdivision;
		for (int z1 = 0; z1 < subdivision; z1++) {
		    double z1d = 1.0 * z1 / subdivision;
		    double[] point = function.transform(x1 + bin * z1d, x2 + bin * z2d);
		    int y1 = (int) Math.round(subdivision * (point[0] - rectangle_l) / bin);
		    int y2 = (int) Math.round(subdivision * (point[1] - rectangle_t) / bin);
		    bp.putPixel(y1, y2, 255);
		}
	    }
	    bp.dilate();
	    bp.erode();
	    int count = 0;
	    for (int y2i = 0; y2i < temp_height; y2i++) {
		double y2 = y2i / subdivision + rectangle_t / bin;
		for (int y1i = 0; y1i < temp_width; y1i++) {
		    double y1 = y1i / subdivision + rectangle_l / bin;
		    if (bp.getPixel(y1i, y2i) > 0) {
			int index = (int) (Math.floor(y1) + Math.floor(y2) * input.getWidth());
			intensity += input.getProcessor().getf(index) / Math.pow(subdivision, 2);
			count++;
		    }
		}
	    }
	    pixelSize.getProcessor().setf(x1 / bin, x2 / bin, count);
	    return intensity;
	}
    }
}
