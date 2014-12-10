package sr_eels;

import sr_eels.testing.SR_EELS_Polynomial_2D;

public abstract class CoordinateCorrector {

    SR_EELS_Polynomial_2D functionWidth;
    SR_EELS_Polynomial_2D functionBorder;
    float offset = 2048;

    public CoordinateCorrector(final SR_EELS_Polynomial_2D functionWidth, final SR_EELS_Polynomial_2D functionBorder) {
	this.functionWidth = functionWidth;
	this.functionBorder = functionBorder;
    }

    public float[] transformCoordinate(final float x1, final float x2) throws SR_EELS_Exception {
	final float[] pointIn = new float[] { x1 - offset, x2 - offset };
	final float[] pointOut = new float[2];
	final float y2n = calcY2n(pointIn[0], pointIn[1]);
	pointOut[0] = calcY1(pointIn[0], y2n) + offset;
	pointOut[1] = calcY2(pointOut[0], y2n) + offset;
	return pointOut;
    }

    abstract float calcY2n(float x1, float x2) throws SR_EELS_Exception;

    abstract float calcY1(float x1, float y2n);

    abstract float calcY2(float y1, float y2n);

}
