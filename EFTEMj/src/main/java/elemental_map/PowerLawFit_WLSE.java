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
 */
public class PowerLawFit_WLSE extends PowerLawFit {

	private final static double DEFAULT_R = 4.0;

	private double rn;
	private double an;

	public PowerLawFit_WLSE(final double[] preEdgeEnergyLosses,
		final double[] counts, final double epsilon)
	{
		super(preEdgeEnergyLosses, counts, epsilon);
		r = DEFAULT_R;
		this.xValues = new double[preEdgeEnergyLosses.length];
		this.yValues = new double[counts.length];
		for (int i = 0; i < preEdgeEnergyLosses.length; i++) {
			this.xValues[i] = Math.log(preEdgeEnergyLosses[i]);
			this.yValues[i] = Math.log(counts[i]);
		}
	}

	@Override
	public void doFit() {
		rn = r;
		double rn_prev = rn + 2 * epsilon;
		an = yValues[0] + r * xValues[0];
		int convergenceCounter = 0;
		// this variable saves rn-r of the previous iteration. The initial value
		// is a random one. To trigger no
		// convergence error the value is such huge.
		double diff = 10.0;
		// Start: Iteration to calculate r
		while (Math.abs(rn_prev - rn) > epsilon) {
			rn_prev = rn;
			rn = (sum(1, 0, 1) * sum(1, 1, 0) - (sum(1, 0, 0) * sum(1, 1, 1))) / (sum(
				1, 2, 0) * sum(1, 0, 0) - Math.pow(sum(1, 1, 0), 2));
			an = (sum(1, 0, 1) * sum(1, 2, 0) - (sum(1, 1, 0) * sum(1, 1, 1))) / (sum(
				1, 2, 0) * sum(1, 0, 0) - Math.pow(sum(1, 1, 0), 2));
			// Check for a NaN error
			if (Double.isNaN(rn)) {
				r = Double.NaN;
				a = Double.NaN;
				errorCode += ERROR_R_NAN;
				return;
			}
			if (Double.isNaN(an)) {
				r = Double.NaN;
				errorCode += ERROR_A_NAN;
			}
			if (Double.isInfinite(rn)) {
				r = Double.NaN;
				a = Double.NaN;
				errorCode += ERROR_R_INFINITE;
				return;
			}
			if (Double.isInfinite(an)) {
				r = Double.NaN;
				a = Double.NaN;
				errorCode += ERROR_A_INFINITE;
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
		r = rn;
		a = Math.exp(an);
		if (Double.isNaN(an)) {
			r = Double.NaN;
			errorCode += ERROR_A_NAN;
		}
		if (Double.isInfinite(rn)) {
			r = Double.NaN;
			a = Double.NaN;
			errorCode += ERROR_R_INFINITE;
			return;
		}
		done = true;
	}

	private double sum(final int w, final int x, final int y) {
		double value = 0;
		for (int i = 0; i < xValues.length; i++) {
			value += Math.pow(w(xValues[i]), w) * Math.pow(xValues[i], x) * Math.pow(
				yValues[i], y);
		}
		return value;
	}

	private double w(final double x) {
		final double y = an - rn * x;
		return y;
	}

}
