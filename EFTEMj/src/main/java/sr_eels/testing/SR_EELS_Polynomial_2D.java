package sr_eels.testing;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.CurveFitter;
import ij.process.FloatProcessor;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

import libs.lma.implementations.Polynomial_2D;
import sr_eels.CameraSetup;

public class SR_EELS_Polynomial_2D extends Polynomial_2D {

    public static final int BORDERS = 1;
    public static final int WIDTH_VS_POS = 2;

    private FloatProcessor transformWidth;

    public SR_EELS_Polynomial_2D(final int m, final int n) {
	super(m, n);
    }

    public SR_EELS_Polynomial_2D(final int m, final int n, final double[] params) {
	super(m, n, params);
    }

    public SR_EELS_Polynomial_2D(final int m, final int n, final double[] params, final boolean isWidth) {
	super(m, n, params);
	if (isWidth) {
	    /*
	     * The correction of the width needs some steps of preparation. The result of this preparation is an image,
	     * that contains the uncorrected coordinate for each corrected coordinate.
	     * 
	     * First we have to calculate the roots of the polynomial along the x2-direction. If the roots are outside
	     * the image boundaries, they are replaced by the image boundaries.
	     * 
	     * Read on at the next comment.
	     */
	    double rootL = -Math.sqrt(Math.pow(getParam(0, 1), 2) / (4 * Math.pow(getParam(0, 2), 2)) - getParam(0, 0)
		    / getParam(0, 2))
		    - getParam(0, 1) / (2 * getParam(0, 2));
	    double rootH = Math.sqrt(Math.pow(getParam(0, 1), 2) / (4 * Math.pow(getParam(0, 2), 2)) - getParam(0, 0)
		    / getParam(0, 2))
		    - getParam(0, 1) / (2 * getParam(0, 2));
	    final double maxPos = -getParam(0, 1) / (2 * getParam(0, 2));
	    final double[] maxPoint = { 0, maxPos };
	    final double maxValue = val(maxPoint);
	    if (rootL < -CameraSetup.getFullHeight() / 2)
		rootL = -CameraSetup.getFullHeight() / 2;
	    if (rootH > CameraSetup.getFullHeight() / 2 - 1)
		rootH = CameraSetup.getFullHeight() / 2 - 1;
	    IJ.showMessage(rootL + ", " + maxPos + ", " + rootH);
	    /*
	     * The second step is to map uncorrected and corrected coordinates. For each uncorrected coordinate the
	     * corrected coordinate is calculated. The inverse function is hard to determine Instead we switch the axes
	     * and fit a polynomial of 3rd order that fits very well.
	     */
	    final LinkedHashMap<Integer, Double> map = new LinkedHashMap<Integer, Double>();
	    final double a00 = getParam(0, 0) / maxValue;
	    final double a01 = getParam(0, 1) / maxValue;
	    final double a02 = getParam(0, 2) / maxValue;
	    int index = 0;
	    for (int x = (int) Math.ceil(rootL); x <= rootH; x++) {
		final double num = 3 * Math.pow(2 * x + a01 / a02, 2);
		final double denum = (4 * a02 * Math.pow(x, 3) + 6 * a01 * Math.pow(x, 2) + 12 * a00 * x
			- Math.pow(a01, 3) / Math.pow(a02, 2) + 6 * a00 * a01 / a02);
		final double sum1 = num / denum;
		final double sum2 = -a01 / (2 * a02);
		map.put(x, sum1 + sum2);
	    }
	    final double[] x = new double[map.size()];
	    final double[] xc = new double[map.size()];
	    final Collection<Integer> set = map.keySet();
	    final Iterator<Integer> iterator = set.iterator();
	    index = 0;
	    while (iterator.hasNext()) {
		final int key = iterator.next();
		x[index] = key;
		xc[index] = map.get(key);
		index++;
	    }
	    final CurveFitter fit = new CurveFitter(xc, x);
	    fit.doFit(CurveFitter.POLY3);
	    final double[] fitParams = fit.getParams();
	    IJ.showMessage(map.get((int) rootL) + ", " + map.get((int) rootH));
	    transformWidth = new FloatProcessor(CameraSetup.getFullWidth(), (int) (2 * Math.max(-rootL, rootH)));
	    for (int x2 = 0; x2 < transformWidth.getHeight(); x2++) {
		final float value = (float) fit.f(fitParams, x2 - transformWidth.getHeight() / 2);
		for (int x1 = 0; x1 < transformWidth.getWidth(); x1++) {
		    transformWidth.setf(x1, x2, value);
		}
	    }
	    final ImagePlus imp = new ImagePlus("transform width", transformWidth);
	    imp.show();
	    // Fitter.plot(fit);
	}
    }

    public String compareWithGnuplot(final int functionType, final CameraSetup camSetup) {
	String filename = "";
	String using = "";
	final String offsetX1 = "offsetX1 = " + camSetup.getCameraOffsetX1();
	final String offsetX2 = "offsetX2 = " + camSetup.getCameraOffsetX2();
	switch (functionType) {
	case BORDERS:
	    filename = "Borders.txt";
	    using = "using ($1-offsetX1):($2-offsetX2):($3-offsetX2):4";
	    break;
	case WIDTH_VS_POS:
	    filename = "Width.txt";
	    using = "using ($1-offsetX1):($2-offsetX2):3:(1)";
	    break;
	default:
	    break;
	}
	String functionGnu = "f(x,y) = ";
	String fit = "fit f(x,y) '" + filename + "' " + using + " zerror via ";
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		functionGnu += String.format("a%d%d*x**%d*y**%d", i, j, i, j);
		fit += String.format("a%d%d", i, j);
		if (i != m | j != n) {
		    functionGnu += " + ";
		    fit += ",";
		}
	    }
	}
	final String title = String.format("%n#####%n# Fit of '%s':%n#####%n", filename);
	fit = title + "\n" + offsetX1 + "\n" + offsetX2 + "\n" + functionGnu + "\n" + fit;
	String residuals = "";
	switch (functionType) {
	case BORDERS:
	    filename = "Borders.txt";
	    using = "using ($1-offsetX1):($2-offsetX2):($3-offsetX2)";
	    residuals = "using ($1-offsetX1):($2-offsetX2):(abs( $3 - offsetX2 - fJ($1-offsetX1,$2-offsetX2))**2)";
	    break;
	case WIDTH_VS_POS:
	    filename = "Width.txt";
	    using = "using ($1-offsetX1):($2-offsetX2):3";
	    residuals = "using ($1-offsetX1):($2-offsetX2):(abs( $3 - fJ($1-offsetX1,$2-offsetX2))**2)";
	    break;
	default:
	    break;
	}
	final String splot = String
		.format("splot '%s' %s notitle,\\%nf(x,y) title 'Gnuplot', fJ(x,y) title 'Java LMA',\\%n'%1$s' %s title 'residuals'",
			filename, using, residuals);
	String functionJava = "fJ(x,y) = ";
	String compare = "#Java LMA";
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		compare += String.format("\naJ%d%d = %+6e", i, j, params[(n + 1) * i + j]);
		functionJava += String.format("aJ%d%d*x**%d*y**%d", i, j, i, j);
		if (i != m | j != n) {
		    functionJava += " + ";
		}
	    }
	}
	return fit + "\n\n" + functionJava + "\n\n" + compare + "\n\n" + splot;
    }

    public double intDX2(final double[] x, final double[] shiftedParams) {
	double value = 0;
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		value += shiftedParams[(n + 1) * i + j] * Math.pow(x[0], i) * Math.pow(x[1], j + 1) / (j + 1);
	    }
	}
	return value;
    }

    public double normAreaToMax(final double[] x) {
	return transformWidth.getf((int) x[0] + 2048, (int) x[1] + 2048);
    }

    public double normAreaToMax(final double x2) {
	final double[] x = new double[] { 0, x2 };
	return normAreaToMax(x);
    }

    private double[] getMaxPos(final double[] x) {
	// double b0 = 0;
	double b1 = 0;
	double b2 = 0;
	for (int i = 0; i < m; i++) {
	    // b0 += params[i * m] * Math.pow(x[0], i);
	    b1 += params[i * m + 1] * Math.pow(x[0], i);
	    b2 += params[i * m + 2] * Math.pow(x[0], i);
	}
	final double yMaxPos = b1 / (2 * b2);
	return new double[] { x[0], yMaxPos };

    }

    public double getY1(final double arcLength, final double x2) {
	final double a = getA(x2);
	final double b = getB(x2);
	return 1
		/ Math.sqrt(getA(x2))
		* Math.log(2 * Math.sqrt(a * (a * arcLength * arcLength + b * arcLength + getC(x2))) + 2 * a
			* arcLength + b);
    }

    private double getA(final double x2) {
	final double a2 = params[6] + params[7] * x2 * params[8] * x2 * x2;
	final double a = 4 * a2;
	return a;
    }

    private double getB(final double x2) {
	final double a1 = params[3] + params[4] * x2 * params[5] * x2 * x2;
	final double a2 = params[6] + params[7] * x2 * params[8] * x2 * x2;
	final double b = 4 * a1 * a2;
	return b;
    }

    private double getC(final double x2) {
	final double a1 = params[3] + params[4] * x2 * params[5] * x2 * x2;
	return a1 * a1 + 1;
    }

}
