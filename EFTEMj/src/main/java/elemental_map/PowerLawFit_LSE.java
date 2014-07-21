package elemental_map;

public class PowerLawFit_LSE extends PowerLawFit {

    double[] xValues;
    double[] yValues;

    public PowerLawFit_LSE(float[] xValues, float[] yValues, float epsilon) {
	super(xValues, yValues, epsilon);
	this.xValues = new double[xValues.length];
	this.yValues = new double[yValues.length];
	for (int i = 0; i < xValues.length; i++) {
	    this.xValues[i] = Math.log(xValues[i]);
	    this.yValues[i] = Math.log(yValues[i]);
	}
    }

    @Override
    public void doFit() {
	float xMean = 0;
	float yMean = 0;
	for (int i = 0; i < xValues.length; i++) {
	    xMean += xValues[i] / xValues.length;
	    yMean += yValues[i] / yValues.length;
	}
	float sum = 0;
	float sum2 = 0;
	for (int i = 0; i < xValues.length; i++) {
	    sum += (xValues[i] - xMean) * (yValues[i] - yMean);
	    sum2 += Math.pow(xValues[i] - xMean, 2);
	}
	r = -sum / sum2;
	if (Double.isNaN(r)) {
	    a = Double.NaN;
	    errorCode += ERROR_R_NAN;
	    return;
	}
	if (Double.isInfinite(r)) {
	    r = Double.NaN;
	    a = Double.NaN;
	    errorCode += ERROR_R_INFINITE;
	    return;
	}
	a = Math.exp(yMean + r * xMean);
	if (Double.isNaN(a)) {
	    r = Double.NaN;
	    errorCode += ERROR_A_NAN;
	    return;
	}
	done = true;
    }
}