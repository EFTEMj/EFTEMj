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
package elemental_map;

/**
 * This is an implementation of {@link PowerLawFit} that uses the
 * <strong>Maximum Likelihood Estimation (MLE)</strong>. The MLE is an iterative
 * method to do a parameter fit.
 * 
 * @author Michael Entrup b. Epping <entrup@arcor.de>
 * 
 */
public class PowerLawFit_MLE extends PowerLawFit {

    private final static double DEFAULT_R = 4.0;
    private final static float DEFAULT_EPSILON = 1.0E-6f;

    /**
     * A constructor that uses a default value for epsilon.<br />
     * The constructor of {@link PowerLawFit} is called.
     * 
     * @param xValues
     * @param yValues
     */
    public PowerLawFit_MLE(float[] xValues, float[] yValues) {
	super(xValues, yValues, DEFAULT_EPSILON);
	r = DEFAULT_R;
    }

    /**
     * A constructor that uses the same parameters as the constructor of
     * {@link PowerLawFit}.
     * 
     * @param xValues
     * @param yValues
     * @param epsilon
     */
    public PowerLawFit_MLE(float[] xValues, float[] yValues, float epsilon) {
	super(xValues, yValues, epsilon);
	r = DEFAULT_R;
    }

    /**
     * The constructor of {@link PowerLawFit} is called. Additionally you can
     * define a starting value for the iterative calculation of
     * <strong>r</strong>.
     * 
     * @param xValues
     * @param yValues
     * @param epsilon
     * @param rStart
     */
    public PowerLawFit_MLE(float[] xValues, float[] yValues, float epsilon,
	    float rStart) {
	super(xValues, yValues, epsilon);
	r = rStart;
    }

    @Override
    public void doFit() {
	double rn = r;
	double rn_prev = rn + 2 * epsilon;
	int convergenceCounter = 0;
	// this variable saves rn-r of the previous iteration. The initial value
	// is a random one. To trigger no
	// convergence error the value is such huge.
	double diff = 10.0;
	double num;
	double denum;
	// Start: Iteration to calculate r
	while (Math.abs(rn_prev - rn) > epsilon) {
	    rn_prev = rn;
	    num = numerator(rn);
	    denum = denominator(rn);
	    rn = rn_prev - num / denum;
	    // Check for a NaN error
	    if (Double.isNaN(rn)) {
		r = Double.NaN;
		a = Double.NaN;
		errorCode += ERROR_R_NAN;
		return;
	    }
	    if (Double.isInfinite(rn)) {
		r = Double.NaN;
		a = Double.NaN;
		errorCode += ERROR_R_INFINITE;
		return;
	    }
	    // Checks for a convergence error. The combination of the 2. and 3.
	    // if statement prevents an infinite number
	    // of iterations.
	    if (Math.abs(rn_prev - rn) == diff) {
		r = Double.NaN;
		a = Double.NaN;
		errorCode += ERROR_CONVERGE;
		return;
	    }
	    if (Math.abs(rn_prev - rn) > diff) {
		convergenceCounter++;
	    }
	    if (convergenceCounter >= 25) {
		r = Double.NaN;
		a = Double.NaN;
		errorCode += ERROR_CONVERGE;
		return;
	    }
	    diff = Math.abs(rn_prev - rn);
	}
	a = sumCounts() / sumExp(rn, 0);
	if (Double.isNaN(a)) {
	    r = Double.NaN;
	    errorCode += ERROR_A_NAN;
	}
	r = rn;
	done = true;
    }

    /**
     * The denominator of the equation to calculate <strong>r</strong>.
     * 
     * @param rn
     *            The value of <strong>r</strong> at the current iteration.
     * @return The result of the equation at the denominator.
     */
    private double denominator(double rn) {
	double s0 = sumExp(rn, 0);
	double s2 = sumExp(rn, 2);
	return Math.pow((sumExp(rn, 1) / s0), 2) - s2 / s0;
    }

    /**
     * The numerator of the equation to calculate <strong>r</strong>.
     * 
     * @param rn
     *            The value of <strong>r</strong> at the current iteration.
     * @return The result of the equation at the numerator.
     */
    private double numerator(double rn) {
	return sumExp(rn, 1) / sumExp(rn, 0) - weight();
    }

    /**
     * Sums the counts of all pre-edge images at the currently processed pixel
     * position.
     * 
     * @return Sum of the counts.
     */
    private double sumCounts() {
	double value = 0;
	for (int i = 0; i < xValues.length; i++) {
	    value += yValues[i];
	}
	return value;
    }

    /**
     * Calculates the sum of E<sub>i</sub><sup>exponent</sup>.
     * 
     * @param rn
     *            The value of <strong>r</strong> at the current iteration.
     * @param exponent
     *            Can be 0, 1, or 2.
     * @return The sum of power functions.
     */
    private double sumExp(double rn, int exponent) {
	double value = 0;
	for (int i = 0; i < xValues.length; i++) {
	    value += (Math.pow(Math.log(xValues[i]), exponent))
		    * Math.exp(-1 * rn * Math.log(xValues[i]));
	}
	return value;
    }

    /**
     * The MLE uses a weight that is calculated at this method.
     * 
     * @return A weighted mean energy loss.
     */
    private double weight() {
	double value1 = 0;
	double value2 = 0;
	for (int i = 0; i < xValues.length; i++) {
	    value1 += Math.log(xValues[i]) * yValues[i];
	    value2 += yValues[i];
	}
	// If true this will result in 0/1
	if (value2 == 0)
	    value2 = 1;
	return value1 / value2;
    }

}
