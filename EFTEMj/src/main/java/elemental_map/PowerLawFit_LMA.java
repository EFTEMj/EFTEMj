package elemental_map;

import lma.LMA;
import lma.LMAFunction;

public class PowerLawFit_LMA extends PowerLawFit {

    private LMA lma;

    public PowerLawFit_LMA(float[] xValues, float[] yValues, float epsilon) {
	super(xValues, yValues, epsilon);
	lma = new LMA(new PowerLawFunction(), new float[] { 1f, 1f },
		new float[][] { xValues, yValues });
    }

    public static class PowerLawFunction extends LMAFunction {
	@Override
	public double getY(double x, double[] a) {
	    return a[0] * Math.pow(x, -a[1]);
	}

	@Override
	public double getPartialDerivate(double x, double[] a,
		int parameterIndex) {
	    switch (parameterIndex) {
	    case 0:
		return Math.pow(x, -a[1]);
	    case 1:
		return -1 * a[0] * Math.log(x) * Math.pow(x, -a[1]);
	    }
	    throw new RuntimeException("No such parameter index: "
		    + parameterIndex);
	}
    }

    @Override
    public void doFit() {
	lma.fit();
	a = lma.parameters[0];
	r = lma.parameters[1];
	done = true;
    }

}
