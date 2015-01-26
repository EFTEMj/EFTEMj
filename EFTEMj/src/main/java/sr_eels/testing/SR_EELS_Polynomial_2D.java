package sr_eels.testing;

import libs.lma.implementations.Polynomial_2D;
import sr_eels.CameraSetup;

public class SR_EELS_Polynomial_2D extends Polynomial_2D {

    public static final int BORDERS = 1;
    public static final int WIDTH_VS_POS = 2;

    public SR_EELS_Polynomial_2D(final int m, final int n) {
	super(m, n);
    }

    public SR_EELS_Polynomial_2D(final int m, final int n, final double[] params) {
	super(m, n, params);
    }

    public String getGnuplotCommands(final int functionType) {
	String filename = "";
	String using = "";
	assert (CameraSetup.getFullWidth() == CameraSetup.getFullHeight());
	final String offset = "offset = " + CameraSetup.getFullWidth() / 2;
	switch (functionType) {
	case BORDERS:
	    filename = "Borders.txt";
	    using = "using ($1-offset):($2-offset):($3-offset):4";
	    break;
	case WIDTH_VS_POS:
	    filename = "Width.txt";
	    using = "using ($1-offset):($2-offset):3:(1)";
	    break;
	default:
	    break;
	}
	String function = "f(x,y) = ";
	String fit = "fit f(x,y) '" + filename + "' " + using + " zerror via ";
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		function += String.format("a%d%d*x**%d*y**%d", i, j, i, j);
		fit += String.format("a%d%d", i, j);
		if (i != m | j != n) {
		    function += " + ";
		    fit += ",";
		}
	    }
	}
	return offset + "\n" + function + "\n" + fit;
    }

    public String compareWithGnuplot(final int functionType) {
	final String fit = getGnuplotCommands(functionType);
	String filename = "";
	String using = "";
	String residuals = "";
	switch (functionType) {
	case BORDERS:
	    filename = "Borders.txt";
	    using = "using ($1-offset):($2-offset):($3-offset)";
	    residuals = "using ($1-offset):($2-offset):(abs( $3 - offset - fJ($1-offset,$2-offset))**2)";
	    break;
	case WIDTH_VS_POS:
	    filename = "Width.txt";
	    using = "using ($1-offset):($2-offset):3";
	    residuals = "using ($1-offset):($2-offset):(abs( $3 - fJ($1-offset,$2-offset))**2)";
	    break;
	default:
	    break;
	}
	final String splot = String
		.format("splot '%s' %s notitle,\\%nf(x,y) title 'Gnuplot', fJ(x,y) title 'Java LMA',\\%n'%1$s' %s title 'residuals'",
			filename, using, residuals);
	String function = "fJ(x,y) = ";
	String compare = "#Java LMA";
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		compare += String.format("\naJ%d%d = %+6e", i, j, params[(n + 1) * i + j]);
		function += String.format("a%d%d*x**%d*y**%d", i, j, i, j);
		if (i != m | j != n) {
		    function += " + ";
		}
	    }
	}
	return fit + "\n\n" + function + "\n\n" + compare + "\n\n" + splot;
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
	final double[] shiftedParams = params.clone();
	if (val(x) < 0) {
	    shiftedParams[0] = params[0] - val(x);
	}
	final double intMax = intDX2(getMaxPos(x), shiftedParams);
	final double value = intDX2(x, shiftedParams) - intMax;
	final double yMax = val(getMaxPos(x));
	return getMaxPos(x)[1] + value / yMax;
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
