// levenberg-marquardt in java
//
// To use this, implement the functions in the LMfunc interface.
//
// This library uses simple matrix routines from the JAMA java matrix package,
// which is in the public domain.  Reference:
//    http://math.nist.gov/javanumerics/jama/
// (JAMA has a matrix object class.  An earlier library JNL, which is no longer
// available, represented matrices as low-level arrays.  Several years
// ago the performance of JNL matrix code was better than that of JAMA,
// though improvements in java compilers may have fixed this by now.)
//
// One further recommendation would be to use an inverse based
// on Choleski decomposition, which is easy to implement and
// suitable for the symmetric inverse required here.  There is a choleski
// routine at idiom.com/~zilla.
//
// If you make an improved version, please consider adding your
// name to it ("modified by ...") and send it back to me
// (and put it on the web).
//
// ----------------------------------------------------------------
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public
// License as published by the Free Software Foundation; either
// version 2 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Library General Public License for more details.
//
// You should have received a copy of the GNU Library General Public
// License along with this library; if not, write to the
// Free Software Foundation, Inc., 59 Temple Place - Suite 330,
// Boston, MA  02111-1307, USA.
//
// initial author contact info:
// jplewis  www.idiom.com/~zilla  zilla # computer.org,   #=at
//
// Improvements by:
// dscherba  www.ncsa.uiuc.edu/~dscherba
// Jonathan Jackson   j.jackson # ucl.ac.uk
// Michael Entrup	entrup@arcor.de

package sr_eels.testing;

import libs.lma.Polynomial_2D;
// see comment above
import Jama.Matrix;

/**
 * Levenberg-Marquardt, implemented from the general description in Numerical Recipes (NR), then tweaked slightly to
 * mostly match the results of their code. Use for nonlinear least squares assuming Gaussian errors.
 *
 * TODO this holds some parameters fixed by simply not updating them. this may be ok if the number if fixed parameters
 * is small, but if the number of varying parameters is larger it would be more efficient to make a smaller hessian
 * involving only the variables.
 *
 * The NR code assumes a statistical context, e.g. returns covariance of parameter errors; we do not do this.
 */
public final class LM {

    /**
     * calculate the current sum-squared-error (Chi-squared is the distribution of squared Gaussian errors, thus the
     * name)
     */
    static double chiSquared(final double[][] x, final double[] a, final double[] y, final double[] s,
	    final LM_Function f) {
	final int npts = y.length;
	double sum = 0.;

	for (int i = 0; i < npts; i++) {
	    double d = y[i] - f.val(x[i], a);
	    d = d / s[i];
	    sum = sum + (d * d);
	}

	return sum;
    } // chiSquared

    /**
     * Minimize E = sum {(y[k] - f(x[k],a)) / s[k]}^2 The individual errors are optionally scaled by s[k]. Note that
     * LMfunc implements the value and gradient of f(x,a), NOT the value and gradient of E with respect to a!
     *
     * @param x
     *            array of domain points, each may be multidimensional
     * @param a
     *            the parameters/state of the model
     * @param y
     *            corresponding array of values
     * @param vary
     *            false to indicate the corresponding a[k] is to be held fixed
     * @param s2
     *            sigma^2 for point i
     * @param lambda
     *            blend between steepest descent (lambda high) and jump to bottom of quadratic (lambda zero). Start with
     *            0.001.
     * @param termepsilon
     *            termination accuracy (0.01)
     * @param maxiter
     *            stop and return after this many iterations if not done
     * @param verbose
     *            set to zero (no prints), 1, 2
     *
     * @return the new lambda for future iterations. Can use this and maxiter to interleave the LM descent with some
     *         other task, setting maxiter to something small.
     */
    public static double solve(final double[][] x, final double[] a, final double[] y, final double[] s2,
	    final boolean[] vary, final LM_Function f, final double lambda, final double termepsilon,
	    final int maxiter, final int verbose) throws Exception {
	final int npts = y.length;
	final int nparm = a.length;
	assert s2.length == npts;
	assert x.length == npts;
	if (verbose > 0) {
	    System.out.print("solve x[" + x.length + "][" + x[0].length + "]");
	    System.out.print(" a[" + a.length + "]");
	    System.out.println(" y[" + y.length + "]");
	}

	double e0 = chiSquared(x, a, y, s2, f);
	// double lambda = 0.001;
	boolean done = false;

	// g = gradient, H = hessian, d = step to minimum
	// H d = -g, solve for d
	final double[][] H = new double[nparm][nparm];
	final double[] g = new double[nparm];
	// double[] d = new double[nparm];

	final double[] oos2 = new double[s2.length];
	for (int i = 0; i < npts; i++)
	    oos2[i] = 1. / (s2[i] * s2[i]);

	int iter = 0;
	int term = 0; // termination count test

	double lambdaChanged = lambda;

	do {
	    ++iter;

	    // hessian approximation
	    for (int r = 0; r < nparm; r++) {
		for (int c = 0; c < nparm; c++) {
		    for (int i = 0; i < npts; i++) {
			if (i == 0)
			    H[r][c] = 0.;
			final double[] xi = x[i];
			H[r][c] += (oos2[i] * f.grad(xi, a, r) * f.grad(xi, a, c));
		    } // npts
		} // c
	    } // r

	    // boost diagonal towards gradient descent
	    for (int r = 0; r < nparm; r++)
		H[r][r] *= (1. + lambda);

	    // gradient
	    for (int r = 0; r < nparm; r++) {
		for (int i = 0; i < npts; i++) {
		    if (i == 0)
			g[r] = 0.;
		    final double[] xi = x[i];
		    g[r] += (oos2[i] * (y[i] - f.val(xi, a)) * f.grad(xi, a, r));
		}
	    } // npts

	    // solve H d = -g, evaluate error at new location
	    // double[] d = DoubleMatrix.solve(H, g);
	    final double[] d = (new Matrix(H)).lu().solve(new Matrix(g, nparm)).getRowPackedCopy();
	    // double[] na = DoubleVector.add(a, d);
	    final double[] na = (new Matrix(a, nparm)).plus(new Matrix(d, nparm)).getRowPackedCopy();
	    final double e1 = chiSquared(x, na, y, s2, f);

	    if (verbose > 0) {
		System.out.println("\n\niteration " + iter + " lambda = " + lambda);
		System.out.print("a = ");
		(new Matrix(a, nparm)).print(10, 6);
		if (verbose > 1) {
		    System.out.print("H = ");
		    (new Matrix(H)).print(10, 6);
		    System.out.print("g = ");
		    (new Matrix(g, nparm)).print(10, 6);
		    System.out.print("d = ");
		    (new Matrix(d, nparm)).print(10, 6);
		}
		System.out.print("e0 = " + e0 + ": ");
		System.out.print("moved from ");
		(new Matrix(a, nparm)).print(10, 6);
		System.out.print("e1 = " + e1 + ": ");
		if (e1 < e0) {
		    System.out.print("to ");
		    (new Matrix(na, nparm)).print(10, 6);
		} else {
		    System.out.println("move rejected");
		}
	    }

	    // termination test (slightly different than NR)
	    if (Math.abs(e1 - e0) > termepsilon) {
		term = 0;
	    } else {
		term++;
		if (term == 4) {
		    System.out.println("terminating after " + iter + " iterations");
		    done = true;
		}
	    }
	    if (iter >= maxiter)
		done = true;

	    // in the C++ version, found that changing this to e1 >= e0
	    // was not a good idea. See comment there.
	    if (e1 > e0 || Double.isNaN(e1)) { // new location worse than before
		lambdaChanged *= 10.;
	    } else { // new location better, accept new parameters
		lambdaChanged *= 0.1;
		e0 = e1;
		// simply assigning a = na will not get results copied back to caller
		for (int i = 0; i < nparm; i++) {
		    if (vary[i])
			a[i] = na[i];
		}
	    }

	} while (!done);

	return lambdaChanged;
    } // solve

    // ----------------------------------------------------------------

    /**
     * This methods were part of LMfunc.java. I moved them to a new interface, as they are only needed for testing.
     *
     * @author Michael Entrup b. Epping <entrup@arcor.de>
     *
     */
    private interface LM_TestFunction extends LM_Function {

	/**
	 * return an array[4] of x,a,y,s for a test case; a is the desired final answer.
	 */
	public Object[] testdata();

	/**
	 * return initial guess at a[]
	 */
	double[] initial();
    }

    // ----------------------------------------------------------------

    /**
     * solve for phase, amplitude and frequency of a sinusoid
     */
    static class LMSineTest implements LM_TestFunction {
	static final int PHASE = 0;
	static final int AMP = 1;
	static final int FREQ = 2;

	@Override
	public double[] initial() {
	    final double[] a = new double[3];
	    a[PHASE] = 0.;
	    a[AMP] = 1.;
	    a[FREQ] = 1.;
	    return a;
	} // initial

	@Override
	public double val(final double[] x, final double[] a) {
	    return a[AMP] * Math.sin(a[FREQ] * x[0] + a[PHASE]);
	} // val

	@Override
	public double grad(final double[] x, final double[] a, final int a_k) {
	    if (a_k == AMP)
		return Math.sin(a[FREQ] * x[0] + a[PHASE]);

	    else if (a_k == FREQ)
		return a[AMP] * Math.cos(a[FREQ] * x[0] + a[PHASE]) * x[0];

	    else if (a_k == PHASE)
		return a[AMP] * Math.cos(a[FREQ] * x[0] + a[PHASE]);

	    else {
		assert false;
		return 0.;
	    }
	} // grad

	@Override
	public Object[] testdata() {
	    final double[] a = new double[3];
	    a[PHASE] = 0.111;
	    a[AMP] = 1.222;
	    a[FREQ] = 1.333;

	    final int npts = 10;
	    final double[][] x = new double[npts][1];
	    final double[] y = new double[npts];
	    final double[] s = new double[npts];
	    for (int i = 0; i < npts; i++) {
		x[i][0] = (double) i / npts;
		y[i] = val(x[i], a);
		s[i] = 1.;
	    }

	    final Object[] o = new Object[4];
	    o[0] = x;
	    o[1] = a;
	    o[2] = y;
	    o[3] = s;

	    return o;
	} // test

    } // SineTest

    // ----------------------------------------------------------------

    /**
     * solve for phase, amplitude and frequency of a sinusoid
     */
    static class LMPolyTest implements LM_TestFunction {

	@Override
	public double[] initial() {
	    final double[] a = { 1., 0.01, 0.0001, 0.01, 0.0001, 1e-6, 0.0001, 1e-6, 1e-8 };
	    return a;
	} // initial

	@Override
	public double val(final double[] x, final double[] a) {
	    return Polynomial_2D.val(x, a, 2, 2);
	} // val

	@Override
	public double grad(final double[] x, final double[] a, final int a_k) {
	    return Polynomial_2D.grad(x, a, 2, 2, a_k);
	} // grad

	@Override
	public Object[] testdata() {
	    return testdata(1);
	} // test

	public Object[] testdata(final int n) {
	    final double[] x_vals = { 287.859, 351.834, 415.842, 479.841, 543.846, 607.831, 671.85, 735.848, 799.859,
		    863.868, 927.84, 991.848, 1055.85, 1119.85, 1183.86, 1247.87, 1311.84, 1375.85, 1439.89, 1503.86,
		    1567.86, 1631.85, 1695.87, 1759.85, 1823.85, 1887.86, 1951.87, 2015.93, 2079.84, 2143.86, 2207.87,
		    2271.86, 2335.87, 2399.85, 2463.89, 2527.86, 2591.86, 2655.84, 2719.89, 2783.85, 2847.89, 2911.85,
		    2975.88, 3039.87, 3103.9, 3167.89, 3231.89, 3295.84, 3359.89, 3423.89, 3487.9, 3551.83, 3615.85,
		    3679.88, 3743.92, 3807.87, 287.847, 351.845, 415.843, 479.873, 543.844, 607.843, 671.847, 735.847,
		    799.858, 863.842, 927.857, 991.848, 1055.86, 1119.84, 1183.87, 1247.84, 1311.84, 1375.87, 1439.86,
		    1503.83, 1567.88, 1631.84, 1695.87, 1759.86, 1823.86, 1887.85, 1951.87, 2015.89, 2079.85, 2143.85,
		    2207.91, 2271.86, 2335.86, 2399.84, 2463.85, 2527.86, 2591.86, 2655.85, 2719.85, 2783.89, 2847.9,
		    2911.87, 2975.85, 3039.86, 3103.86, 3167.86, 3231.88, 3295.91, 3359.88, 3423.88, 3487.89, 3551.89,
		    3615.88, 3679.88, 3743.85, 3807.86, 287.846, 351.854, 415.838, 479.853, 543.85, 607.84, 671.846,
		    735.853, 799.859, 863.829, 927.871, 991.84, 1055.84, 1119.86, 1183.84, 1247.86, 1311.86, 1375.84,
		    1439.84, 1503.86, 1567.87, 1631.84, 1695.85, 1759.89, 1823.87, 1887.84, 1951.86, 2015.87, 2079.86,
		    2143.85, 2207.87, 2271.88, 2335.89, 2399.85, 2463.85, 2527.85, 2591.88, 2655.87, 2719.87, 2783.86,
		    2847.88, 2911.92, 2975.86, 3039.9, 3103.88, 3167.87, 3231.87, 3295.88, 3359.89, 3423.89, 3487.89,
		    3551.9, 3615.86, 3679.86, 3743.88, 3807.87, 287.838, 351.868, 415.82, 479.869, 543.833, 607.864,
		    671.838, 735.873, 799.864, 863.845, 927.864, 991.867, 1055.86, 1119.85, 1183.87, 1247.84, 1311.86,
		    1375.87, 1439.86, 1503.85, 1567.85, 1631.88, 1695.88, 1759.85, 1823.87, 1887.87, 1951.86, 2015.87,
		    2079.88, 2143.86, 2207.88, 2271.86, 2335.88, 2399.87, 2463.89, 2527.87, 2591.86, 2655.86, 2719.91,
		    2783.86, 2847.87, 2911.93, 2975.87, 3039.85, 3103.91, 3167.9, 3231.84, 3295.87, 3359.87, 3423.92,
		    3487.86, 3551.85, 3615.9, 3679.84, 3743.91, 3807.89, 287.848, 351.819, 415.864, 479.84, 543.863,
		    607.861, 671.86, 735.85, 799.851, 863.854, 927.848, 991.839, 1055.86, 1119.83, 1183.85, 1247.85,
		    1311.87, 1375.84, 1439.85, 1503.84, 1567.85, 1631.87, 1695.84, 1759.86, 1823.87, 1887.86, 1951.86,
		    2015.86, 2079.86, 2143.89, 2207.85, 2271.87, 2335.86, 2399.85, 2463.87, 2527.89, 2591.88, 2655.89,
		    2719.86, 2783.86, 2847.86, 2911.9, 2975.88, 3039.86, 3103.84, 3167.87, 3231.87, 3295.87, 3359.86,
		    3423.89, 3487.88, 3551.91, 3615.86, 3679.86, 3743.94, 3807.9 };

	    final double[] y_vals = { 2045.48, 2046.57, 2047.31, 2048.06, 2048.77, 2049.78, 2050.74, 2051.48, 2052.14,
		    2052.99, 2053.82, 2054.6, 2054.96, 2055.93, 2056.13, 2057.08, 2057.92, 2058.39, 2059.1, 2059.68,
		    2060.07, 2060.7, 2061.34, 2061.82, 2062.68, 2063.49, 2063.98, 2064.47, 2065.15, 2065.7, 2066.11,
		    2067.22, 2067.71, 2068.45, 2069.49, 2070.27, 2071.07, 2071.67, 2072.34, 2073.1, 2073.95, 2074.51,
		    2075.11, 2076.68, 2077.23, 2078.31, 2079.11, 2080.36, 2081.48, 2082.38, 2083.58, 2085.24, 2085.93,
		    2087.37, 2088.22, 2090.05, 485.76, 489.629, 493.954, 497.724, 501.66, 505.274, 509.077, 512.631,
		    516.104, 519.698, 523.16, 526.346, 529.858, 533.442, 536.816, 540.078, 543.215, 546.269, 549.529,
		    552.429, 555.54, 558.583, 561.378, 564.35, 567.306, 570.213, 573.35, 575.839, 578.998, 581.752,
		    584.429, 587.379, 589.852, 593.052, 595.54, 598.387, 601.189, 603.827, 606.323, 609.054, 611.846,
		    614.72, 617.578, 619.609, 622.818, 625.349, 628.016, 630.864, 633.393, 636.743, 638.802, 641.702,
		    644.651, 647.24, 649.763, 652.675, 1182.67, 1185.11, 1187.81, 1190.22, 1193.08, 1195.29, 1197.49,
		    1199.97, 1202.12, 1204.48, 1206.62, 1208.8, 1211.13, 1213.02, 1214.98, 1217.08, 1219.31, 1220.94,
		    1223.03, 1225.21, 1226.89, 1228.62, 1230.37, 1232.79, 1234.57, 1236.42, 1238.19, 1239.92, 1241.71,
		    1243.83, 1245.11, 1247.33, 1249.29, 1250.45, 1252.69, 1254.25, 1255.82, 1257.93, 1259.82, 1261.45,
		    1263.66, 1264.98, 1266.89, 1268.66, 1270.09, 1272.39, 1274.14, 1276.19, 1278.26, 1279.94, 1281.79,
		    1284.04, 1285.76, 1288.05, 1289.91, 1291.58, 3668.15, 3665.57, 3663.64, 3661.67, 3660.04, 3657.98,
		    3655.67, 3654.34, 3651.9, 3650.06, 3648.12, 3646.18, 3644.2, 3642.22, 3640.53, 3638.78, 3636.94,
		    3635.02, 3633.02, 3631.47, 3629.57, 3628.09, 3626.15, 3624.8, 3622.89, 3621.18, 3619.64, 3618.04,
		    3616.42, 3614.72, 3612.9, 3611.9, 3610.15, 3608.81, 3607.64, 3606.09, 3604.89, 3603.69, 3602.69,
		    3601.41, 3600.66, 3600.13, 3599.03, 3597.7, 3597.01, 3596.53, 3595.85, 3595.6, 3594.98, 3594.81,
		    3594.45, 3594.19, 3593.94, 3593.75, 3593.92, 3594.44, 2889.65, 2889.15, 2888.77, 2888.02, 2887.59,
		    2886.88, 2886.13, 2885.48, 2884.88, 2884.44, 2883.74, 2883.05, 2882.42, 2881.59, 2881.21, 2880.56,
		    2879.77, 2879.27, 2878.41, 2878.17, 2877.38, 2876.7, 2876.16, 2876.09, 2875.13, 2874.56, 2874.14,
		    2873.57, 2872.73, 2872.53, 2872.09, 2871.53, 2871.32, 2871.13, 2870.26, 2870.01, 2869.84, 2869.65,
		    2869.53, 2869.03, 2869.18, 2869.09, 2869.21, 2869.17, 2869.37, 2869.51, 2869.81, 2869.91, 2870.49,
		    2870.58, 2871.05, 2871.94, 2872.23, 2873.12, 2873.33, 2873.92 };

	    final double[] z_vals = { 630, 630, 628, 628, 626, 624, 622, 624, 622, 622, 620, 618, 618, 616, 614, 614,
		    616, 610, 612, 608, 608, 608, 606, 604, 606, 604, 604, 602, 602, 602, 602, 600, 600, 594, 596, 594,
		    594, 594, 594, 592, 590, 592, 592, 590, 588, 590, 588, 586, 584, 584, 586, 584, 582, 584, 578, 582,
		    580, 580, 580, 576, 576, 576, 574, 574, 572, 570, 570, 568, 568, 564, 566, 562, 564, 562, 560, 558,
		    560, 558, 556, 556, 554, 556, 552, 552, 550, 550, 550, 546, 548, 546, 544, 546, 542, 544, 542, 540,
		    540, 540, 538, 536, 538, 536, 536, 534, 534, 532, 532, 532, 530, 528, 528, 526, 614, 616, 614, 612,
		    612, 608, 610, 608, 606, 604, 602, 604, 602, 600, 600, 598, 598, 598, 594, 594, 594, 592, 588, 590,
		    588, 588, 588, 588, 584, 584, 586, 584, 582, 580, 580, 580, 578, 576, 576, 576, 576, 576, 574, 576,
		    574, 572, 572, 572, 572, 570, 568, 568, 566, 566, 568, 564, 580, 580, 576, 576, 576, 574, 574, 570,
		    572, 570, 570, 568, 568, 566, 566, 564, 562, 564, 564, 560, 560, 558, 560, 556, 556, 554, 554, 552,
		    552, 550, 550, 550, 548, 550, 546, 546, 546, 548, 546, 542, 544, 544, 542, 542, 540, 538, 538, 538,
		    536, 536, 536, 536, 536, 534, 532, 532, 620, 618, 616, 616, 614, 616, 612, 612, 610, 608, 608, 608,
		    604, 608, 606, 604, 600, 602, 600, 596, 594, 594, 596, 594, 594, 594, 592, 592, 590, 588, 588, 586,
		    588, 586, 584, 584, 584, 582, 582, 582, 580, 582, 578, 578, 578, 576, 576, 574, 576, 574, 574, 572,
		    572, 572, 574, 572 };

	    assert x_vals.length == y_vals.length;
	    assert x_vals.length == z_vals.length;
	    final int npts = (int) Math.floor(x_vals.length / n);
	    final double[][] x = new double[npts][2];
	    final double[] y = new double[npts];
	    final double[] s = new double[npts];
	    // results from Gnuplot
	    final double[] a = { 551.113, 0.0834116, -2.01285e-005, -0.0236638, 2.68602e-006, -5.03575e-010,
		    6.71201e-007, 7.10721e-010, -1.43449e-013 };

	    for (int i = 0; i < npts; i += n) {
		x[i][0] = x_vals[i];
		x[i][1] = y_vals[i];
		y[i] = z_vals[i];
		s[i] = 1.;
	    }

	    final Object[] o = new Object[4];
	    o[0] = x;
	    o[1] = a;
	    o[2] = y;
	    o[3] = s;

	    return o;
	}

    } // PolyTest

    // ----------------------------------------------------------------

    /**
     * quadratic (p-o)'S'S(p-o) solve for o, S S is a single scale factor
     */
    static class LMQuadTest implements LM_TestFunction {

	@Override
	public double val(final double[] x, final double[] a) {
	    assert a.length == 3;
	    assert x.length == 2;

	    final double ox = a[0];
	    final double oy = a[1];
	    final double s = a[2];

	    final double sdx = s * (x[0] - ox);
	    final double sdy = s * (x[1] - oy);

	    return sdx * sdx + sdy * sdy;
	} // val

	/**
	 * z = (p-o)'S'S(p-o) dz/dp = 2S'S(p-o)
	 *
	 * z = (s*(px-ox))^2 + (s*(py-oy))^2 dz/dox = -2(s*(px-ox))*s dz/ds = 2*s*[(px-ox)^2 + (py-oy)^2]
	 *
	 * z = (s*dx)^2 + (s*dy)^2 dz/ds = 2(s*dx)*dx + 2(s*dy)*dy
	 */
	@Override
	public double grad(final double[] x, final double[] a, final int a_k) {
	    assert a.length == 3;
	    assert x.length == 2;
	    assert a_k < 3 : "a_k=" + a_k;

	    final double ox = a[0];
	    final double oy = a[1];
	    final double s = a[2];

	    final double dx = (x[0] - ox);
	    final double dy = (x[1] - oy);

	    if (a_k == 0)
		return -2. * s * s * dx;

	    else if (a_k == 1)
		return -2. * s * s * dy;

	    else
		return 2. * s * (dx * dx + dy * dy);
	} // grad

	@Override
	public double[] initial() {
	    final double[] a = new double[3];
	    a[0] = 0.05;
	    a[1] = 0.1;
	    a[2] = 1.0;
	    return a;
	} // initial

	@Override
	public Object[] testdata() {
	    final Object[] o = new Object[4];
	    final int npts = 25;
	    final double[][] x = new double[npts][2];
	    final double[] y = new double[npts];
	    final double[] s = new double[npts];
	    final double[] a = new double[3];

	    a[0] = 0.;
	    a[1] = 0.;
	    a[2] = 0.9;

	    int i = 0;
	    for (int r = -2; r <= 2; r++) {
		for (int c = -2; c <= 2; c++) {
		    x[i][0] = c;
		    x[i][1] = r;
		    y[i] = val(x[i], a);
		    System.out.println("Quad " + c + "," + r + " -> " + y[i]);
		    s[i] = 1.;
		    i++;
		}
	    }
	    System.out.print("quad x= ");
	    (new Matrix(x)).print(10, 6);

	    System.out.print("quad y= ");
	    (new Matrix(y, npts)).print(10, 6);

	    o[0] = x;
	    o[1] = a;
	    o[2] = y;
	    o[3] = s;
	    return o;
	} // testdata

    } // LMQuadTest

    // ----------------------------------------------------------------

    /**
     * Replicate the example in NR, fit a sum of Gaussians to data. y(x) = \sum B_k exp(-((x - E_k) / G_k)^2) minimize
     * chisq = \sum { y[j] - \sum B_k exp(-((x_j - E_k) / G_k)^2) }^2
     *
     * B_k, E_k, G_k are stored in that order
     *
     * Works, results are close to those from the NR example code.
     */
    static class LMGaussTest implements LM_TestFunction {
	static double SPREAD = 0.001; // noise variance

	@Override
	public double val(final double[] x, final double[] a) {
	    assert x.length == 1;
	    assert (a.length % 3) == 0;

	    final int K = a.length / 3;
	    int i = 0;

	    double y = 0.;
	    for (int j = 0; j < K; j++) {
		final double arg = (x[0] - a[i + 1]) / a[i + 2];
		final double ex = Math.exp(-arg * arg);
		y += (a[i] * ex);
		i += 3;
	    }

	    return y;
	} // val

	/**
	 * <pre>
	 * y(x) = \sum B_k exp(-((x - E_k) / G_k)^2)
	 * arg  =  (x-E_k)/G_k
	 * ex   =  exp(-arg*arg)
	 * fac =   B_k * ex * 2 * arg
	 * 
	 * d/dB_k = exp(-((x - E_k) / G_k)^2)
	 * 
	 * d/dE_k = B_k exp(-((x - E_k) / G_k)^2) . -2((x - E_k) / G_k) . -1/G_k
	 *        = 2 * B_k * ex * arg / G_k
	 *   d/E_k[-((x - E_k) / G_k)^2] = -2((x - E_k) / G_k) d/dE_k[(x-E_k)/G_k]
	 *   d/dE_k[(x-E_k)/G_k] = -1/G_k
	 * 
	 * d/G_k = B_k exp(-((x - E_k) / G_k)^2) . -2((x - E_k) / G_k) . -(x-E_k)/G_k^2
	 *       = B_k ex -2 arg -arg / G_k
	 *       = fac arg / G_k
	 * d/dx[1/x] = d/dx[x^-1] = -x[x^-2]
	 */
	@Override
	public double grad(final double[] x, final double[] a, final int a_k) {
	    assert x.length == 1;

	    // i - index one of the K Gaussians
	    final int i = 3 * (a_k / 3);

	    final double arg = (x[0] - a[i + 1]) / a[i + 2];
	    final double ex = Math.exp(-arg * arg);
	    final double fac = a[i] * ex * 2. * arg;

	    if (a_k == i)
		return ex;

	    else if (a_k == (i + 1)) {
		return fac / a[i + 2];
	    }

	    else if (a_k == (i + 2)) {
		return fac * arg / a[i + 2];
	    }

	    else {
		System.err.println("bad a_k");
		return 1.;
	    }

	} // grad

	@Override
	public double[] initial() {
	    final double[] a = new double[6];
	    a[0] = 4.5;
	    a[1] = 2.2;
	    a[2] = 2.8;

	    a[3] = 2.5;
	    a[4] = 4.9;
	    a[5] = 2.8;
	    return a;
	} // initial

	@Override
	public Object[] testdata() {
	    final Object[] o = new Object[4];
	    final int npts = 100;
	    final double[][] x = new double[npts][1];
	    final double[] y = new double[npts];
	    final double[] s = new double[npts];
	    final double[] a = new double[6];

	    a[0] = 5.0; // values returned by initial
	    a[1] = 2.0; // should be fairly close to these
	    a[2] = 3.0;
	    a[3] = 2.0;
	    a[4] = 5.0;
	    a[5] = 3.0;

	    for (int i = 0; i < npts; i++) {
		x[i][0] = 0.1 * (i + 1); // NR always counts from 1
		y[i] = val(x[i], a);
		s[i] = SPREAD * y[i];
		System.out.println(i + ": x,y= " + x[i][0] + ", " + y[i]);
	    }

	    o[0] = x;
	    o[1] = a;
	    o[2] = y;
	    o[3] = s;

	    return o;
	} // testdata

    } // LMGaussTest

    // ----------------------------------------------------------------

    // test program
    public static void main(final String[] cmdline) {

	// LMfunc f = new LMQuadTest(); // works
	// LMfunc f = new LMSineTest(); // works
	// LMfunc f = new LMGaussTest(); // works
	final LM_TestFunction f = new LMPolyTest(); // works

	final double[] aguess = f.initial();
	final Object[] test = f.testdata();
	final double[][] x = (double[][]) test[0];
	final double[] areal = (double[]) test[1];
	final double[] y = (double[]) test[2];
	final double[] s = (double[]) test[3];
	final boolean[] vary = new boolean[aguess.length];
	for (int i = 0; i < aguess.length; i++)
	    vary[i] = true;
	assert aguess.length == areal.length;

	try {
	    solve(x, aguess, y, s, vary, f, 0.001, 1e-5, 1000, 2);
	} catch (final Exception ex) {
	    System.err.println("Exception caught: " + ex.getMessage());
	    System.exit(1);
	}

	System.out.print("desired solution ");
	(new Matrix(areal, areal.length)).print(10, 6);

	System.exit(0);
    } // main

} // LM

