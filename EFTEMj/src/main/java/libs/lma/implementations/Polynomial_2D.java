package libs.lma.implementations;

import java.util.Arrays;

import libs.lma.LMAMultiDimFunction;

/**
 * This class represents a polynomial in 2D: y(x1,x2). It implements all necessary methods to be used in a
 * Levenberg-Marquardt algorithm.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 *
 */
public class Polynomial_2D extends LMAMultiDimFunction {

    /**
     * The order of the polynomial in x1.
     */
    protected final int m;
    /**
     * The order of the polynomial in x2.
     */
    protected final int n;
    /**
     * The (m+1)x(n+1) parameters of the polynomial.
     * <p>
     * <code>a<sub>00</sub>, a<sub>01</sub>, ... a<sub>0n</sub>, ... a<sub>10</sub>, ... a<sub>mn</sub></code>
     */
    protected double[] params;

    /**
     * This constructor creates a new 2D polynomial with given orders and all parameters = 1.<br />
     * Use the second constructor to define all parameters yourself.
     *
     * @param m
     *            is the maximal order of x1.
     * @param n
     *            is the maximal order of x2.
     */
    public Polynomial_2D(final int m, final int n) {
	this.m = m;
	this.n = n;
	this.params = new double[(m + 1) * (n + 1)];
	Arrays.fill(params, 1.0);

    }

    /**
     * @param m
     *            is the maximal order of x1.
     * @param n
     *            is the maximal order of x2.
     * @param params
     *            is an array that contains all the necessary parameters. The order f the parameters is:
     *            <code>a<sub>00</sub>, a<sub>01</sub>, ... a<sub>0n</sub>, ... a<sub>10</sub>, ... a<sub>mn</sub></code>
     */
    public Polynomial_2D(final int m, final int n, final double[] params) {
	assert params.length == (m + 1) * (n + 1);
	this.m = m;
	this.n = n;
	this.params = params;
    }

    /**
     * @param x
     *            is the coordinate (x1,x2).
     * @return the value y(x1,x2).
     */
    public double val(final double[] x) {
	assert x.length == 2;
	double value = 0.;
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		value += params[(n + 1) * i + j] * Math.pow(x[0], i) * Math.pow(x[1], j);
	    }
	}
	return value;
    }

    /**
     * @param x
     *            is a list of coordinates ( x1<SUB>i</SUB> ,x2<SUB>i</SUB> ).
     * @return a list of the values y<SUB>i</SUB> ( x1<SUB>i</SUB> ,x2<SUB>i</SUB> ).
     */
    public double[] val(final double[][] x) {
	final double[] values = new double[x.length];
	for (int i = 0; i < x.length; i++) {
	    values[i] = val(x[i]);
	}
	return values;
    }

    /**
     * @param x
     *            is the coordinate (x1,x2).
     * @param paramIndex
     *            is the index of the parameter.
     * @return the element of the gradient vector with the given index.
     */
    public double grad(final double[] x, final int paramIndex) {
	assert x.length == 2;
	assert paramIndex < (m + 1) * (n + 1);
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		if (paramIndex == (n + 1) * i + j)
		    return Math.pow(x[0], i) * Math.pow(x[1], j);
	    }
	}
	return 0.;
    }

    /**
     * @param x
     *            is the coordinate (x1,x2).
     * @return the gradient vector as an array.
     */
    public double[] grad(final double[] x) {
	assert x.length == 2;
	final double[] grads = new double[params.length];
	for (int i = 0; i < grads.length; i++) {
	    grads[i] = grad(x, i);
	}
	return grads;
    }

    private void updateParams(final double[] paramsNew) {
	assert params.length == paramsNew.length;
	params = Arrays.copyOf(paramsNew, paramsNew.length);
    }

    @Override
    public double getY(final double[] x, final double[] a) {
	updateParams(a);
	return val(x);
    }

    @Override
    public double getPartialDerivate(final double[] x, final double[] a, final int parameterIndex) {
	updateParams(a);
	return grad(x, parameterIndex);
    }
}
