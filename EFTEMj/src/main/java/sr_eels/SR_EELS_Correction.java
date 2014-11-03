/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 *
 * Copyright (c) 2014, Michael Entrup b. Epping <michael.entrup@wwu.de>
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
import ij.process.FloatProcessor;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 *
 */
public class SR_EELS_Correction {

    private final ImagePlus input;
    private final int binning;
    private final ImagePlus output;
    private final SR_EELS_CorrectionFunction function;
    private final int subdivision;
    private final int oversampling;
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
    public SR_EELS_Correction(final ImagePlus imp, final int binning, final SR_EELS_CorrectionFunction function,
	    final int subdivision, final int oversampling) {
	input = imp;
	this.binning = binning;
	this.function = function;
	this.subdivision = subdivision;
	this.oversampling = oversampling;
	output = new ImagePlus(input.getTitle() + "_corrected", new FloatProcessor(input.getWidth(), input.getHeight(),
		new double[input.getWidth() * input.getHeight()]));
	progressSteps = input.getHeight();
    }

    /**
     * Starts the calculation with parallel {@link Thread}s.
     */
    public void startCalculation() {
	final boolean debug = false;
	if (debug) {
	    for (int x2 = 0; x2 < output.getHeight(); x2++) {
		final SR_EELS_CorrectionTask task = new SR_EELS_CorrectionTask(x2);
		task.run();
	    }
	} else {
	    final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
		    .availableProcessors());
	    for (int x2 = 0; x2 < output.getHeight(); x2++) {
		executorService.execute(new SR_EELS_CorrectionTask(x2));
	    }
	    executorService.shutdown();
	    try {
		executorService.awaitTermination(60, TimeUnit.MINUTES);
	    } catch (final InterruptedException e) {
		e.printStackTrace();
	    }
	}
    }

    /**
     *
     */
    public void showResult() {
	output.show();

    }

    /**
     * {@link SR_EELS_CorrectionTask} will use this method to update the process.
     */
    private static void updateProgress() {
	progress++;
	IJ.showProgress(progress, progressSteps);
    }

    private class SR_EELS_CorrectionTask implements Runnable {

	/**
	 * The image row to process.
	 */
	private final int x2;
	private final int bin;
	private final int width = 4096;
	private final int height = 4096;

	/**
	 * A default constructor that only sets one field.
	 *
	 * @param x2
	 *            The image row to process.
	 */
	public SR_EELS_CorrectionTask(final int x2) {
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
		output.getProcessor().setf(x1, x2, (float) getCorrectetIntensity(x1 * bin, x2 * bin));
	    }
	    SR_EELS_Correction.updateProgress();
	}

	private double getCorrectetIntensity(final int x1Binned, final int x2Binned) {
	    double intensity = 0.0;
	    final double[] point00 = function.transform(x1Binned, x2Binned);
	    final double[] point01 = function.transform(x1Binned, x2Binned + bin);
	    final double[] point10 = function.transform(x1Binned + bin, x2Binned);
	    final double[] point11 = function.transform(x1Binned + bin, x2Binned + bin);
	    final double rectangle_l = Math.floor(subdivision * Math.min(point00[0], point01[0])) / subdivision;
	    final double rectangle_b = Math.ceil(subdivision * Math.max(point01[1], point11[1])) / subdivision;
	    final double rectangle_r = Math.ceil(subdivision * Math.max(point10[0], point11[0])) / subdivision;
	    final double rectangle_t = Math.floor(subdivision * Math.min(point00[1], point10[1])) / subdivision;
	    if (rectangle_l < 0 | rectangle_t < 0 | rectangle_r >= width | rectangle_b >= height)
		return intensity;
	    final double rectangle_width = rectangle_r - rectangle_l;
	    final double rectangle_height = rectangle_b - rectangle_t;
	    final int temp_width = (int) Math.ceil(rectangle_width / bin * subdivision) + 1;
	    final int temp_height = (int) Math.ceil(rectangle_height / bin * subdivision) + 1;
	    final int[] pixels_temp = new int[temp_width * temp_height];
	    Arrays.fill(pixels_temp, 0);
	    for (int z2 = 0; z2 < oversampling * subdivision; z2++) {
		final double z2d = 1.0 * z2 / (oversampling * subdivision);
		for (int z1 = 0; z1 < oversampling * subdivision; z1++) {
		    final double z1d = 1.0 * z1 / (oversampling * subdivision);
		    final double[] point = function.transform(x1Binned + bin * z1d, x2Binned + bin * z2d);
		    final double y1 = Math.round(subdivision * (point[0] - rectangle_l) / bin);
		    final double y2 = Math.round(subdivision * (point[1] - rectangle_t) / bin);
		    final int index = (int) (y1 + y2 * temp_width);
		    pixels_temp[index] = 1;
		}
	    }
	    for (int y2i = 0; y2i < temp_height; y2i++) {
		final double y2 = y2i / subdivision + rectangle_t / bin;
		for (int y1i = 0; y1i < temp_width; y1i++) {
		    final double y1 = y1i / subdivision + rectangle_l / bin;
		    if (pixels_temp[y1i + y2i * temp_width] == 1) {
			final int index = (int) (Math.floor(y1) + Math.floor(y2) * input.getWidth());
			intensity += input.getProcessor().getf(index) / Math.pow(subdivision, 2);
		    }
		}
	    }
	    return intensity;
	}
    }
}
