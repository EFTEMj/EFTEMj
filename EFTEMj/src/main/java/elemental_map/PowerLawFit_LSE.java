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
package elemental_map;

/**
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 *
 */
public class PowerLawFit_LSE extends PowerLawFit {

    double[] xValues;
    double[] yValues;

    public PowerLawFit_LSE(float[] xValues, float[] yValues, float epsilon) {
	super(xValues, yValues, epsilon);
	this.xValues = new double[xValues.length];
	this.yValues = new double[yValues.length];
	for (int i = 0; i < xValues.length; i++) {
	    this.xValues[i] = Math.log(xValues[i]);
	    this.yValues[i] = Math.log(yValues[i]);
	}
    }

    @Override
    public void doFit() {
	float xMean = 0;
	float yMean = 0;
	for (int i = 0; i < xValues.length; i++) {
	    xMean += xValues[i] / xValues.length;
	    yMean += yValues[i] / yValues.length;
	}
	float sum = 0;
	float sum2 = 0;
	for (int i = 0; i < xValues.length; i++) {
	    sum += (xValues[i] - xMean) * (yValues[i] - yMean);
	    sum2 += Math.pow(xValues[i] - xMean, 2);
	}
	r = -sum / sum2;
	if (Double.isNaN(r)) {
	    a = Double.NaN;
	    errorCode += ERROR_R_NAN;
	    return;
	}
	if (Double.isInfinite(r)) {
	    r = Double.NaN;
	    a = Double.NaN;
	    errorCode += ERROR_R_INFINITE;
	    return;
	}
	a = Math.exp(yMean + r * xMean);
	if (Double.isNaN(a)) {
	    r = Double.NaN;
	    errorCode += ERROR_A_NAN;
	    return;
	}
	done = true;
    }
}