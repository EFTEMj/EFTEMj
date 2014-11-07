package libs.lma.implementations;

import java.util.Arrays;

import libs.lma.LMA;
import libs.lma.LMAFunction;

public class TestFunctions {

    public static LMAFunction sin = new LMAFunction() {
	@Override
	public double getY(final double x, final double[] a) {
	    return a[0] * Math.sin(x / a[1]);
	}

	@Override
	public double getPartialDerivate(final double x, final double[] a, final int parameterIndex) {
	    switch (parameterIndex) {
	    case 0:
		return Math.sin(x / a[1]);
	    case 1:
		return a[0] * Math.cos(x / a[1]) * (-x / (a[1] * a[1]));
	    }
	    throw new RuntimeException("No such fit parameter: " + parameterIndex);
	}
    };

    public static void main(final String[] args) {
	final double[] x = { 0.0, 0.1, 0.2, 0.3, 0.5, 0.7 };// , 1.1, 1.4, 2.5, 6.4, 7.9, 10.4, 12.6};
	final double[] a = { 2.2, 0.4 };
	final double[][] data = { x, sin.generateData(x, a) };
	final LMA lma = new LMA(sin, new double[] { 0.1, 10 }, data, null);
	lma.fit();
	System.out.println("RESULT PARAMETERS: " + Arrays.toString(lma.parameters));

	/*
	 * ArrayTool.writeToFileByColumns("fittest.dat", data); GnuPlotTool2.plot(ArrayTool.toFloatArray(data));
	 * double[] af = {2.370453217483242, 0.43162827642649365}; for (int i = 0; i < x.length; i++) { double y =
	 * sin.getY(x[i], a); double y_f = sin.getY(x[i], af); System.out.println("y = "+ y + ", y_f = " + y_f +
	 * ", dy = " + (y - y_f)); }
	 */
    }
}
