package sr_eels.testing;

import java.util.Arrays;

/**
 * This class represents a polynomial in 2D: z(x,y). It implements all necessary methods to be used in a
 * Levenberg-Marquardt algorithm.
 * 
 * @author Michael Entrup b. Epping <entrup@arcor.de>
 *
 */
public class Polynomial_2D implements LM_Function {

    private int m;
    private int n;
    private double[] params;

    /**
     * This is a static version of the value calculation.
     * 
     * @param x
     *            is the coordinate (x,y).
     * @param params
     *            is an array that contains all the necessary parameters. The order f the parameters is:<br />
     *            a<SUB>00</SUB> , a<SUB>01</SUB> , ... , a<SUB>0n</SUB> , a<SUB>10</SUB> , ... , a<SUB>m0</SUB> , ... ,
     *            a<SUB>mn</SUB>
     * @param m
     *            is the maximal order of x.
     * @param n
     *            is the maximal order of y.
     * @return the value z(x,y).
     */
    public static double val(double[] x, double[] params, int m, int n) {
	assert x.length == 2;
	assert params.length == (m + 1) * (n + 1);
	double value = 0.;
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		value += params[(n + 1) * i + j] * Math.pow(x[0], i) * Math.pow(x[1], j);
	    }
	}
	return value;
    }

    /**
     * This is a static version of the gradient calculation.
     * 
     * @param x
     *            is the coordinate (x,y).
     * @param params
     *            is an array that contains all the necessary parameters. The order f the parameters is:<br />
     *            a<SUB>00</SUB> , a<SUB>01</SUB> , ... , a<SUB>0n</SUB> , a<SUB>10</SUB> , ... , a<SUB>m0</SUB> , ... ,
     *            a<SUB>mn</SUB>
     * @param m
     *            is the maximal order of x.
     * @param n
     *            is the maximal order of y.
     * @param param
     *            is the index of the parameter.
     * @return the element of the gradient vector with the given index.
     */
    public static double grad(double[] x, double[] params, int m, int n, int param) {
	assert x.length == 2;
	assert params.length == (m + 1) * (n + 1);
	assert param < params.length;
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		if (param == (n + 1) * i + j)
		    return Math.pow(x[0], i) * Math.pow(x[1], j);
	    }
	}
	return 0.;
    }

    /**
     * This constructor creates a new 2D polynomial with given orders and all parameters = 1.<br />
     * Use the second constructor to define all parameters yourself.
     * 
     * @param m
     *            is the maximal order of x.
     * @param n
     *            is the maximal order of y.
     */
    public Polynomial_2D(int m, int n) {
	this.m = m;
	this.n = n;
	this.params = new double[(m + 1) * (n + 1)];
	Arrays.fill(params, 1.0);

    }

    /**
     * @param m
     *            is the maximal order of x.
     * @param n
     *            is the maximal order of y.
     * @param params
     * 
     *            is an array that contains all the necessary parameters. The order f the parameters is:<br />
     *            a<SUB>00</SUB> , a<SUB>01</SUB> , ... , a<SUB>0n</SUB> , a<SUB>10</SUB> , ... , a<SUB>m0</SUB> , ... ,
     *            a<SUB>mn</SUB>
     */
    public Polynomial_2D(int m, int n, double[] params) {
	assert params.length == (m + 1) * (n + 1);
	this.m = m;
	this.n = n;
	this.params = params;
    }

    /**
     * @param x
     *            is the coordinate (x,y).
     * @return the value z(x,y).
     */
    public double val(double[] x) {
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
     *            is a list of coordinates ( x<SUB>i</SUB> ,y<SUB>i</SUB> ).
     * @return a list of the values z<SUB>i</SUB> ( x<SUB>i</SUB> ,y<SUB>i</SUB> ).
     */
    public double[] val(double[][] x) {
	double[] values = new double[x.length];
	for (int i = 0; i < x.length; i++) {
	    values[i] = val(x[i]);
	}
	return values;
    }

    /**
     * @param x
     *            is the coordinate (x,y).
     * @param param
     *            is the index of the parameter.
     * @return the element of the gradient vector with the given index.
     */
    public double grad(double[] x, int param) {
	assert x.length == 2;
	assert param < (m + 1) * (n + 1);
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		if (param == (n + 1) * i + j)
		    return Math.pow(x[0], i) * Math.pow(x[1], j);
	    }
	}
	return 0.;
    }

    /**
     * @param x
     *            is the coordinate (x,y).
     * @return the gradient vector as an array.
     */
    public double[] grad(double[] x) {
	assert x.length == 2;
	double[] grads = new double[params.length];
	for (int i = 0; i < grads.length; i++) {
	    grads[i] = grad(x, i);
	}
	return grads;
    }

    private void updateParams(double[] params) {
	assert this.params.length == params.length;
	this.params = Arrays.copyOf(params, params.length);
    }

    @Override
    public double val(double[] x, double[] params) {
	updateParams(params);
	return val(x);
    }

    @Override
    public double grad(double[] x, double[] a, int ak) {
	updateParams(params);
	return grad(x, ak);
    }
}
