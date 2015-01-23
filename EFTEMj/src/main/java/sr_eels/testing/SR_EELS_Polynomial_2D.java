package sr_eels.testing;

import java.util.HashMap;

import libs.lma.implementations.Polynomial_2D;
import sr_eels.CameraSetup;
import sr_eels.SR_EELS_Exception;

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

    public double intDX2(final double[] x) {
	double value = 0;
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		value += params[(n + 1) * i + j] * Math.pow(x[0], i) * Math.pow(x[1], j + 1) / (j + 1);
	    }
	}
	return value;
    }

    HashMap<Double, Double> calculatedIntMax = new HashMap<Double, Double>();

    public double normAreaToMax(final double[] x) throws SR_EELS_Exception {
	if (val(x) <= 0) {
	    throw new SR_EELS_Exception();
	}
	double value;
	if (calculatedIntMax.containsKey(x[0])) {
	    value = calculatedIntMax.get(x[0]) - intDX2(x);
	} else {
	    final double intMax = intDX2(getMaxPos(x));
	    value = intMax - intDX2(x);
	    calculatedIntMax.put(x[0], intMax);
	}
	return value / calculatedMax.get(x[0]);
    }

    public double normAreaToMax(final double x2) throws SR_EELS_Exception {
	final double[] x = new double[] { 0, x2 };
	return normAreaToMax(x);
    }

    HashMap<Double, Double> calculatedMaxPos = new HashMap<Double, Double>();
    HashMap<Double, Double> calculatedMax = new HashMap<Double, Double>();

    private double[] getMaxPos(final double[] x) {
	if (calculatedMaxPos.containsKey(x[0])) {
	    return new double[] { x[0], calculatedMaxPos.get(x[0]) };
	}
	// double b0 = 0;
	double b1 = 0;
	double b2 = 0;
	for (int i = 0; i < m; i++) {
	    // b0 += params[i * m] * Math.pow(x[0], i);
	    b1 += params[i * m + 1] * Math.pow(x[0], i);
	    b2 += params[i * m + 2] * Math.pow(x[0], i);
	}
	final double yMaxPos = b1 / (2 * b2);
	calculatedMaxPos.put(x[0], yMaxPos);
	final double yMax = val(new double[] { x[0], yMaxPos });
	calculatedMax.put(x[0], yMax);
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
