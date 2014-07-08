/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 * 
 * Copyright (c) 2013, Michael Entrup b. Epping <entrup@arcor.de>
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
 * This is a blueprint for a power law fit method ( y(x) = a&sdot;x<sup>-r</sup> ). It is used by EFTEMj, but it can be
 * used for other tasks as well. That is why no classes from ImageJ are used.
 * 
 * @author Michael Entrup b. Epping <entrup@arcor.de>
 * 
 */
public abstract class PowerLawFit {

    protected float[] xValues;
    protected float[] yValues;
    /**
     * The break condition for iterative methods.
     */
    protected float epsilon;
    protected double r;
    protected double a;
    protected int errorCode;
    protected boolean done;
    public final static int ERROR_NONE = 0;
    public final static int ERROR_R_NAN = 1;
    public final static int ERROR_R_INFINITE = 2;
    public final static int ERROR_CONVERGE = 4;
    public final static int ERROR_A_NAN = 8;
    public final static int ERROR_A_INFINITE = 16;

    /**
     * @return The calculated value of <strong>a</strong>. NaN if an error occurred during calculation.
     */
    public double getA() {
	if (done == false) {
	    doFit();
	}
	return a;
    }

    /**
     * @return The calculated value of <strong>r</strong>. NaN if an error occurred during calculation.
     */
    public double getR() {
	if (done == false) {
	    doFit();
	}
	return r;
    }

    /**
     * @return An int value that represents a type of error. All error codes are constants of this class.
     */
    public int getErrorCode() {
	return errorCode;
    }

    /**
     * Calculate <strong>r</strong> and <strong>a</strong>.
     */
    public abstract void doFit();

    /**
     * This will create a power law fit method. It will try to fit a power law function to the given data points.
     * 
     * @param xValues
     *            x-values of the data points.
     * @param yValues
     *            y-values of the data points.
     * @param epsilon
     *            The break condition for iterative methods.
     */
    public PowerLawFit(float[] xValues, float[] yValues, float epsilon) {
	errorCode = ERROR_NONE;
	this.xValues = xValues;
	this.yValues = yValues;
	this.epsilon = epsilon;
    }
}