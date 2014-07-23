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

import ij.ImagePlus;
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
    private int oversampling;

    /**
     * @param function
     * @param oversampling
     * @param subdivision
     */
    public SR_EELS_Correction(ImagePlus imp, int binning, SR_EELS_CorrectionFunction function, int subdivision,
	    int oversampling) {
	input = imp;
	this.binning = binning;
	this.function = function;
	this.subdivision = subdivision;
	this.oversampling = oversampling;
	output = new ImagePlus(input.getTitle() + "_corrected", new FloatProcessor(input.getWidth(), input.getHeight(),
		new double[input.getWidth() * input.getHeight()]));
    }

    /**
     * Starts the calculation with parallel {@link Thread}s.
     */
    public void startCalculation() {
	ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	for (int x2 = 0; x2 < output.getHeight(); x2++) {
	    executorService.execute(new SR_EELS_CorrectionTask(x2));
	}
	executorService.shutdown();
	try {
	    executorService.awaitTermination(5, TimeUnit.MINUTES);
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}
    }

    /**
     * 
     */
    public void showResult() {
	output.show();

    }

    private class SR_EELS_CorrectionTask implements Runnable {

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
	    // TODO Extend this method to correct the intensity.
	    for (int x1 = 0; x1 < input.getWidth(); x1++) {
		double[] point = function.transform(bin * x1, bin * x2);
		int y1 = (int) Math.floor(point[0] / bin);
		int y2 = (int) Math.floor(point[1] / bin);
		if (y1 >= 0 & y2 >= 0 & y1 < bin * input.getWidth() & y2 < bin * input.getHeight()) {
		    output.getProcessor().setf(x1, x2, input.getProcessor().getf(y1, y2));
		} else {
		    output.getProcessor().setf(x1, x2, 0f);
		}
	    }
	}

    }
}
