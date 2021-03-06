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

import libs.lma.LMA;
import libs.lma.LMAFunction;

/**
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 */
public class PowerLawFit_LMA extends PowerLawFit {

	private final LMA lma;

	public PowerLawFit_LMA(final double[] xValues, final double[] yValues,
		final double epsilon)
	{
		super(xValues, yValues, epsilon);
		lma = new LMA(new PowerLawFunction(), new double[] { Math.exp(18), 2 },
			new double[][] { xValues, yValues });
	}

	public static class PowerLawFunction extends LMAFunction {

		@Override
		public double getY(final double x, final double[] a) {
			return a[0] * Math.pow(x, -a[1]);
		}

		@Override
		public double getPartialDerivate(final double x, final double[] a,
			final int parameterIndex)
		{
			switch (parameterIndex) {
				case 0:
					return Math.pow(x, -a[1]);
				case 1:
					return -1 * a[0] * Math.log(a[1]) * Math.pow(x, -a[1]);
			}
			throw new RuntimeException("No such parameter index: " + parameterIndex);
		}
	}

	@Override
	public void doFit() {
		try {
			lma.fit();
			if (Double.isNaN(lma.chi2)) {
				errorCode = ERROR_CONVERGE;
				a = Double.NaN;
				r = Double.NaN;
			}
			errorCode = ERROR_NONE;
			a = lma.parameters[0];
			r = lma.parameters[1];
		}
		catch (final Exception e) {
			errorCode = ERROR_CONVERGE;
			a = Double.NaN;
			r = Double.NaN;
		}
		done = true;
	}

}
