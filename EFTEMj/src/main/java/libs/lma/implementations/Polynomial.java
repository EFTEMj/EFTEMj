
package libs.lma.implementations;

import libs.lma.LMAFunction;

/**
 * LMA polynomial y = a_n * x^n + ... + a_2 * x^2 + a_1 * x + a_0
 */
public class Polynomial extends LMAFunction {

	/**
	 * @return The partial derivate of the polynomial which is x to the power of
	 *         the parameter index.
	 */
	@Override
	public double getPartialDerivate(final double x, final double[] a,
		final int parameterIndex)
	{
		return pow(x, parameterIndex);
	}

	/**
	 * Polynomial y = a_n * x^n + ... + a_2 * x^2 + a_1 * x + a_0
	 *
	 * @param a 0: a_0, 1: a_1, 2: a_2, ..., a_n
	 */
	@Override
	public double getY(final double x, final double[] a) {
		double result = 0;
		for (int i = 0; i < a.length; i++) {
			result += pow(x, i) * a[i];
		}
		return result;
	}

	/** fast power */
	private static double pow(final double x, final int exp) {
		double result = 1;
		for (int i = 0; i < exp; i++) {
			result *= x;
		}
		return result;
	}

	public static void main(final String[] args) {
		System.out.println(pow(2, 1));
	}

}
