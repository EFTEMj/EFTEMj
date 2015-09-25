/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 *
 * Copyright (c) 2015, Michael Entrup b. Epping <michael.entrup@wwu.de>
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

package drift;

import java.awt.Point;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * This class is used to calculate the normalised cross-correlation coefficients
 * of two images. It uses multiple {@link Thread}s to speed up the calculation.
 * Each line of the resulting normalised cross-correlation (coefficient) map is
 * calculated in a separate task. You can switch to the calculation of the
 * normalised cross-correlation by using <code>useCoefficient(false)</code>.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 */
public class NormCrossCorrelation {

	/**
	 * This static field is used to switch on the debug mode.
	 */
	public static boolean debug = false;
	/**
	 * This field is used to switch between the calculation of the normalised
	 * cross-correlation or the normalised cross-correlation coefficient.
	 */
	private boolean useCoefficient = true;
	/**
	 * This is the target image.
	 */
	private final ImageProcessor image;
	/**
	 * This reference image will be compared with the target image.
	 */
	private final ImageProcessor reference;
	/**
	 * A map of normalised cross correlation coefficients. The x- and
	 * y-coordinates represent all checked image shifts. The width is
	 * (2*max-x-shift+1) and the height is (2*max-y-shift+1).
	 */
	private final FloatProcessor normCrossCorrelationMap;
	/**
	 * The width of the normCrossCorrelationMap.
	 */
	private final int mapWidth;
	/**
	 * The height of the normCrossCorrelationMap.
	 */
	private final int mapHeight;
	/**
	 * Necessary for the coefficient calculation.
	 */
	private double meanT;
	/**
	 * Necessary for the coefficient calculation.
	 */
	private double sigmaT;
	/**
	 * Necessary if the coefficient is not used.
	 */
	private double squareSumT;
	/**
	 * This field indicates the progress. A static method is used to increase the
	 * value by 1. It is necessary to use volatile because different
	 * {@link Thread}s call the related method.
	 */
	private static volatile int progress;
	/**
	 * Number of steps until the drift detection is finished.
	 */
	private static int progressSteps;

	/**
	 * Creates an instance of {@link NormCrossCorrelation} and prepares the
	 * calculation of the normalised cross-correlation (coefficient) values.
	 *
	 * @param ip1 The reference image
	 * @param ip2 The target image
	 * @param shiftX The maximum value that is tested as shift in x-direction
	 * @param shiftY The maximum value that is tested as shift in y-direction
	 */
	public NormCrossCorrelation(final ImageProcessor ip1,
		final ImageProcessor ip2, final int shiftX, final int shiftY)
	{
		image = ip2;
		ip1.setRoi(shiftX, shiftY, ip1.getWidth() - 2 * shiftX, ip1.getHeight() -
			2 * shiftY);
		reference = ip1.crop();
		mapWidth = 2 * shiftX + 1;
		mapHeight = 2 * shiftY + 1;
		normCrossCorrelationMap = new FloatProcessor(mapWidth, mapHeight,
			new double[mapWidth * mapHeight]);
		progressSteps = mapHeight;
	}

	/**
	 * Switch between the calculation of the normalised cross-correlation (
	 * <code>false</code>) and the normalised cross-correlation coefficient (
	 * <code>true</code>).
	 *
	 * @param bool
	 */
	public void useCoefficient(final boolean bool) {
		useCoefficient = bool;
	}

	/**
	 * New {@link Thread}s are created to calculate the normalised
	 * cross-correlation (coefficient) values as a background task.
	 */
	public void startCalculation() {
		final ExecutorService executorService = Executors.newFixedThreadPool(Runtime
			.getRuntime().availableProcessors());
		if (useCoefficient == true) {
			calculateMeanAndSigma();
		}
		else {
			calculateSquareSumT();
		}
		for (int s = 0; s < mapHeight; s++) {
			executorService.execute(new NormCrossCorrelationTask(s));
		}
		executorService.shutdown();
		try {
			executorService.awaitTermination(5, TimeUnit.MINUTES);
		}
		catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Calculates the square-sum of the reference image.
	 */
	private void calculateSquareSumT() {
		squareSumT = 0;
		double value = 0;
		for (int j = 0; j < reference.getHeight(); j++) {
			for (int i = 0; i < reference.getWidth(); i++) {
				value = reference.getf(i, j);
				squareSumT += Math.pow(value, 2);
			}
		}
	}

	/**
	 * Calculates mean and sigma of the reference image.
	 */
	private void calculateMeanAndSigma() {
		double value;
		double sum = 0;
		for (int j = 0; j < reference.getHeight(); j++) {
			for (int i = 0; i < reference.getWidth(); i++) {
				value = reference.getf(i, j);
				sum += value;
			}
		}
		final int pixels = reference.getWidth() * reference.getHeight();
		meanT = sum / pixels;
		float sigmaSquare = 0;
		for (int j = 0; j < reference.getHeight(); j++) {
			for (int i = 0; i < reference.getWidth(); i++) {
				value = reference.getf(i, j);
				sigmaSquare += Math.pow(value - meanT, 2);
			}
		}
		sigmaT = Math.sqrt(sigmaSquare);
	}

	/**
	 * @return The {@link FloatProcessor} containing the map of calculated
	 *         normalised cross-correlation (coefficient) values
	 */
	public ImagePlus getCrossCorrelationMap() {
		ImagePlus ip;
		if (useCoefficient == true) {
			ip = new ImagePlus("normalized cross-correlation coefficient",
				normCrossCorrelationMap);
		}
		else {
			ip = new ImagePlus("normalized cross-correlation",
				normCrossCorrelationMap);
		}
		ip.getCalibration().xOrigin = (normCrossCorrelationMap.getWidth() - 1) / 2;
		ip.getCalibration().yOrigin = (normCrossCorrelationMap.getHeight() - 1) / 2;
		return ip;
	}

	/**
	 * @param map A map of normalised cross-correlation (coefficient) values.
	 * @return The position of the maximum with consideration of the calibration
	 *         (origin)
	 */
	public static Point findMax(final ImagePlus map) {
		final FloatProcessor fp = (FloatProcessor) map.getProcessor();
		Point p = new Point(0, 0);
		float max = fp.getf(0, 0);
		for (int j = 0; j < fp.getHeight(); j++) {
			for (int i = 1; i < fp.getWidth(); i++) {
				if (fp.getf(i, j) > max) {
					max = fp.getf(i, j);
					p = new Point(i, j);
				}
			}
		}
		p.x = (int) (p.x - map.getCalibration().xOrigin);
		p.y = (int) (p.y - map.getCalibration().yOrigin);
		return p;
	}

	/**
	 * {@link NormCrossCorrelationTask} will use this method to update the
	 * process.
	 */
	private static void updateProgress() {
		progress++;
		IJ.showProgress(progress, progressSteps);
	}

	/**
	 * You only need this method if more than one instance of
	 * {@link NormCrossCorrelation} is used. The constructor will set the right
	 * number of steps if only one instance is used. Call this method after
	 * creating all instances.
	 *
	 * @param steps for N instances of {@link NormCrossCorrelation} this is
	 *          <code>N * (2 * shiftY + 1)</code>
	 */
	public static void setProgressSteps(final int steps) {
		progressSteps = steps;
		progress = 0;
	}

	/**
	 * Each {@link NormCrossCorrelationTask} calculates a row of the resulting
	 * normalized cross-correlation (coefficient) map. It implements
	 * {@link Runnable} to allow a parallel execution.
	 */
	private class NormCrossCorrelationTask implements Runnable {

		/**
		 * The column of the normalised cross-correlation (coefficient) map that is
		 * processed by this task.
		 */
		private final int s;
		/**
		 * An array that is used for temporarily storing the calculated normalised
		 * cross-correlation (coefficient) values.
		 */
		private final float[] result;

		/**
		 * @param column that is processed by this new instance of
		 *          {@link NormCrossCorrelationTask}
		 */
		public NormCrossCorrelationTask(final int column) {
			super();
			s = column;
			result = new float[normCrossCorrelationMap.getWidth()];
		}

		@Override
		public void run() {
			if (useCoefficient == true) {
				calculateCoefficient();
			}
			else {
				calculateCorrelation();
			}
			NormCrossCorrelation.updateProgress();
		}

		private void calculateCorrelation() {
			for (int r = 0; r < result.length; r++) {
				double squareSumI = 0;
				double covariance = 0;
				for (int j = 0; j < reference.getHeight(); j++) {
					for (int i = 0; i < reference.getWidth(); i++) {
						final double valueI = image.getf(r + i, s + j);
						final double valueT = reference.getf(i, j);
						covariance += valueI * valueT;
						squareSumI += Math.pow(valueI, 2);
					}
				}
				result[r] = (float) (covariance / Math.sqrt(squareSumI * squareSumT));
				if (Float.isInfinite(result[r])) {
					result[r] = 0;
				}
			}
			final float[] coefficientMapArray = (float[]) normCrossCorrelationMap
				.getPixels();
			for (int r = 0; r < result.length; r++) {
				coefficientMapArray[r + s * normCrossCorrelationMap.getWidth()] =
					result[r];
			}
		}

		private void calculateCoefficient() {
			final int pixels = reference.getWidth() * reference.getHeight();
			for (int r = 0; r < result.length; r++) {
				double sum = 0;
				double sumSquare = 0;
				double covariance = 0;
				for (int j = 0; j < reference.getHeight(); j++) {
					for (int i = 0; i < reference.getWidth(); i++) {
						final double valueI = image.getf(r + i, s + j);
						final double valueT = reference.getf(i, j);
						sum += valueI;
						sumSquare += valueI * valueI;
						covariance += valueI * valueT;
					}
				}
				final double meanI = sum / pixels;
				result[r] = (float) ((covariance - pixels * meanI * meanT) / (Math.sqrt(
					sumSquare - (pixels * meanI * meanI)) * sigmaT));
			}
			final float[] coefficientMapArray = (float[]) normCrossCorrelationMap
				.getPixels();
			for (int r = 0; r < result.length; r++) {
				coefficientMapArray[r + s * normCrossCorrelationMap.getWidth()] =
					result[r];
			}
		}
	}
}
