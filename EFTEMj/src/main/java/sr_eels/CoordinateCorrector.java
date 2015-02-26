package sr_eels;

public abstract class CoordinateCorrector {

    protected SR_EELS_FloatProcessor inputProcessor;
    protected SR_EELS_FloatProcessor outputProcessor;
    protected SR_EELS_Polynomial_2D functionWidth;
    protected SR_EELS_Polynomial_2D functionBorder;

    public CoordinateCorrector(final SR_EELS_FloatProcessor inputProcessor, final SR_EELS_FloatProcessor outputProcessor) {
	this.inputProcessor = inputProcessor;
	this.outputProcessor = outputProcessor;
	this.functionWidth = inputProcessor.getWidthFunction();
	this.functionBorder = inputProcessor.getBorderFunction();
    }

    public float[] transformCoordinate(final float x1, final float x2) throws SR_EELS_Exception {
	final float[] pointIn = outputProcessor.convertToFunctionCoordinates(new float[] { x1, x2 });
	final float[] pointOut = new float[2];
	final float y2n = calcY2n(pointIn[0], pointIn[1]);
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
