package elemental_map;

public class PowerLawFit_WLSE extends PowerLawFit {

    private final static double DEFAULT_R = 4.0;

    private double[] xValues;
    private double[] yValues;
    private double rn;
    private double an;

    public PowerLawFit_WLSE(float[] xValues, float[] yValues, float epsilon) {
	super(xValues, yValues, epsilon);
	r = DEFAULT_R;
	this.xValues = new double[xValues.length];
	this.yValues = new double[yValues.length];
	for (int i = 0; i < xValues.length; i++) {
	    this.xValues[i] = Math.log(xValues[i]);
	    this.yValues[i] = Math.log(yValues[i]);
	}
    }

    @Override
    public void doFit() {
	rn = r;
	double rn_prev = rn + 2 * epsilon;
	an = yValues[0] + r * xValues[0];
	int convergenceCounter = 0;
	// this variable saves rn-r of the previous iteration. The initial value
	// is a random one. To trigger no
	// convergence error the value is such huge.
	double diff = 10.0;
	// Start: Iteration to calculate r
	while (Math.abs(rn_prev - rn) > epsilon) {
	    rn_prev = rn;
	    rn = (sum(1, 0, 1) * sum(1, 1, 0) - (sum(1, 0, 0) * sum(1, 1, 1)))
		    / (sum(1, 2, 0) * sum(1, 0, 0) - Math.pow(sum(1, 1, 0), 2));
	    an = (sum(1, 0, 1) * sum(1, 2, 0) - (sum(1, 1, 0) * sum(1, 1, 1)))
		    / (sum(1, 2, 0) * sum(1, 0, 0) - Math.pow(sum(1, 1, 0), 2));
	    // Check for a NaN error
	    if (Double.isNaN(rn)) {
		r = Double.NaN;
		a = Double.NaN;
		errorCode += ERROR_R_NAN;
		return;
	    }
	    if (Double.isNaN(an)) {
		r = Double.NaN;
		errorCode += ERROR_A_NAN;
	    }
	    if (Double.isInfinite(rn)) {
		r = Double.NaN;
		a = Double.NaN;
		errorCode += ERROR_R_INFINITE;
		return;
	    }
	    if (Double.isInfinite(an)) {
		r = Double.NaN;
		a = Double.NaN;
		errorCode += ERROR_A_INFINITE;
		return;
	    }
	    // Checks for a convergence error. The combination of the 2. and 3.
	    // if statement prevents an infinite number
	    // of iterations.
	    if (Math.abs(rn_prev - rn) == diff) {
		r = Double.NaN;
		a = Double.NaN;
		errorCode += ERROR_CONVERGE;
		return;
	    }
	    if (Math.abs(rn_prev - rn) > diff) {
		convergenceCounter++;
	    }
	    if (convergenceCounter >= 25) {
		r = Double.NaN;
		a = Double.NaN;
		errorCode += ERROR_CONVERGE;
		return;
	    }
	    diff = Math.abs(rn_prev - rn);
	}
	r = rn;
	a = Math.exp(an);
	if (Double.isNaN(an)) {
	    r = Double.NaN;
	    errorCode += ERROR_A_NAN;
	}
	if (Double.isInfinite(rn)) {
	    r = Double.NaN;
	    a = Double.NaN;
	    errorCode += ERROR_R_INFINITE;
	    return;
	}
	done = true;
    }

    private double sum(int w, int x, int y) {
	double value = 0;
	for (int i = 0; i < xValues.length; i++) {
	    value += Math.pow(w(xValues[i]), w) * Math.pow(xValues[i], x) * Math.pow(yValues[i], y);
	}
	return value;
    }

    private double w(double x) {
	double y = an - rn * x;
	return y;
    }

}