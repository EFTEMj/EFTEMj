package sr_eels;

public abstract class CoordinateCorrector {

    protected SR_EELS_FloatProcessor inputProcessor;
    protected SR_EELS_Polynomial_2D functionWidth;
    protected SR_EELS_Polynomial_2D functionBorder;

    public CoordinateCorrector(SR_EELS_FloatProcessor inputProcessor) {
	this.inputProcessor = inputProcessor;
	this.functionWidth = inputProcessor.getWidthFunction();
	this.functionBorder = inputProcessor.getBorderFunction();
    }

    public float[] transformCoordinate(final float x1, final float x2) throws SR_EELS_Exception {
	final float[] pointIn = inputProcessor.convertToFunctionCoordinates(new float[] { x1, x2 });
	final float[] pointOut = new float[2];
	final float y2n = calcY2n(pointIn[0], pointIn[1]);
	/*
	 * To calculate the correct y1, I have to shift the polynomial. Where x1=-2048 it has to be x1=0. This is
	 * because I use the inverse of the arc length to calculate y1.
	 */
	final float y1 = calcY1(pointIn[0], y2n);
	final float y2 = calcY2(pointIn[0], y2n);
	pointOut[0] = y1;
	pointOut[1] = y2;
	return inputProcessor.convertToImageCoordinates(pointOut);
    }

    abstract float calcY2n(float x1, float x2) throws SR_EELS_Exception;

    abstract float calcY1(float x1, float y2n);

    abstract float calcY2(float y1, float y2n);

}
