package sr_eels.testing;

import java.util.HashMap;

import sr_eels.SR_EELS_Exception;
import libs.lma.implementations.Polynomial_2D;

public class SR_EELS_Polynomial_2D extends Polynomial_2D {

    public static final int BORDERS = 1;
    public static final int WIDTH_VS_POS = 2;

    public SR_EELS_Polynomial_2D(int m, int n) {
	super(m, n);
	// TODO Auto-generated constructor stub
    }

    public SR_EELS_Polynomial_2D(int m, int n, double[] params) {
	super(m, n, params);
    }

    public String getGnuplotCommands(int functionType) {
	String filename = "";
	String using = "";
	switch (functionType) {
	case BORDERS:
	    filename = "Borders.txt";
	    using = "using ($1-2048):($2-2048):($3-2048):4";
	    break;
	case WIDTH_VS_POS:
	    filename = "Width.txt";
	    using = "using ($1-2048):($2-2048):3:(1)";
	    break;
	default:
	    break;
	}
	String function = "f(x,y) = ";
	String fit = "fit f(x,y) '" + filename + "' " + using + " via ";
	String splot = "splot '" + filename + "' " + using + " notitle, f(x,y) notitle";
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
	return function + "\n" + fit + "\n" + splot;
    }

    public String compareParameters(int functionType, double[] compareParams) {
	String filename = "";
	String using = "";
	switch (functionType) {
	case BORDERS:
	    filename = "Borders.txt";
	    using = "using ($1-2048):($2-2048):($3-2048):4";
	    break;
	case WIDTH_VS_POS:
	    filename = "Width.txt";
	    using = "using ($1-2048):($2-2048):3:(1)";
	    break;
	default:
	    break;
	}
	String splot = "splot '" + filename + "' " + using
		+ " notitle, f(x,y) title 'Gnuplot', fJ(x,y) title 'Java LMA'";
	String function = "fJ(x,y) = ";
	String compare = "#Java LMA\t\t\tGnuplot";
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		compare += String.format("\naJ%d%d = %+6e\t\t# %+6e", i, j, params[(n + 1) * i + j],
			compareParams[(n + 1) * i + j]);
		function += String.format("a%d%d*x**%d*y**%d", i, j, i, j);
		if (i != m | j != n) {
		    function += " + ";
		}
	    }
	}
	return compare + "\n\n" + function + "\n" + splot;
    }

    public double intDX2(double[] x) {
	double value = 0;
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		value += params[(n + 1) * i + j] * Math.pow(x[0], i) * Math.pow(x[1], j + 1) / (j + 1);
	    }
	}
	return value;
    }

    HashMap<Double, Double> calculatedIntMax = new HashMap<Double, Double>();

    public double normAreaToMax(double[] x) throws SR_EELS_Exception {
	if (val(x) <= 0) {
	    throw new SR_EELS_Exception();
	}
	double value;
	if (calculatedIntMax.containsKey(x[0])) {
	    value = calculatedIntMax.get(x[0]) - intDX2(x);
	} else {
	    double intMax = intDX2(getMaxPos(x));
	    value = intMax - intDX2(x);
	    calculatedIntMax.put(x[0], intMax);
	}
	return value / calculatedMax.get(x[0]);
    }

    public double normAreaToMax(double x2) throws SR_EELS_Exception {
	double[] x = new double[] { 0, x2 };
	return normAreaToMax(x);
    }

    HashMap<Double, Double> calculatedMaxPos = new HashMap<Double, Double>();
    HashMap<Double, Double> calculatedMax = new HashMap<Double, Double>();

    private double[] getMaxPos(double[] x) {
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
	double yMaxPos = b1 / (2 * b2);
	calculatedMaxPos.put(x[0], yMaxPos);
	double yMax = val(new double[] { x[0], yMaxPos });
	calculatedMax.put(x[0], yMax);
	return new double[] { x[0], yMaxPos };

    }

}
