
package libs.lma.implementations;

import libs.lma.LMA;
import libs.lma.LMAMultiDimFunction;

/**
 * An example fit which fits a plane to some data points and prints out the
 * resulting fit parameters.
 */
public class MultiDimExampleFit {

	/** An example function with a form of y = a0 * x0 + a1 * x1 + a2 */
	public static class MultiDimExampleFunction extends LMAMultiDimFunction {

		@Override
		public double getY(final double x[], final double[] a) {
			return a[0] * x[0] + a[1] * x[1] + a[2];
		}

		@Override
		public double getPartialDerivate(final double x[], final double[] a,
			final int parameterIndex)
		{
			switch (parameterIndex) {
				case 0:
					return x[0];
				case 1:
					return x[1];
				case 2:
					return 1;
			}
			throw new RuntimeException("No such parameter index: " + parameterIndex);
		}
	}

	/**
	 * Does the actual fitting by using the above MultiDimExampleFunction (a
	 * plane)
	 */
	public static void main(final String[] args) {
		final LMA lma = new LMA(new MultiDimExampleFunction(), new double[] { 1, 1,
			1 }, new double[][] {
				// y x0 x1
				{ 0, 2, 6 }, { 5, 10, 2 }, { 7, 20, 4 }, { 9, 30, 7 }, { 12, 40, 6 } });
		lma.fit();
		System.out.println("iterations: " + lma.iterationCount);
		System.out.println("chi2: " + lma.chi2 + ",\n" + "param0: " +
			lma.parameters[0] + ",\n" + "param1: " + lma.parameters[1] + ",\n" +
			"param2: " + lma.parameters[2]);
	}
}
