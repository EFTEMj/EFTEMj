package sr_eels;

import sr_eels.testing.SR_EELS_Polynomial_2D;

public abstract class CoordinateCorrector {

    SR_EELS_Polynomial_2D functionWidth;
    SR_EELS_Polynomial_2D functionBorder;
    CameraSetup camSetup;

    public CoordinateCorrector(final SR_EELS_Polynomial_2D functionWidth, final SR_EELS_Polynomial_2D functionBorder,
	    CameraSetup camSetup) {
	this.functionWidth = functionWidth;
	this.functionBorder = functionBorder;
	this.camSetup = camSetup;
    }

    public float[] transformCoordinate(final float x1, final float x2) throws SR_EELS_Exception {
	final float[] pointIn = new float[] { x1 - camSetup.getCameraOffsetX1(), x2 - camSetup.getCameraOffsetX2() };
	final float[] pointOut = new float[2];
	// if (x2 == 2048) {
	// System.out.println("Break");
	// }
	final float y2n = calcY2n(pointIn[0], pointIn[1]);
	/*
	 * To calculate the correct y1, I have to shift the polynomial.
	 * 
	 * Where x1=-2048 it has to be x1=0. This is
	 * 
	 * because I use the inverse of the arc length to calculate y1.
	 */
	pointOut[0] = calcY1(pointIn[0], y2n) + camSetup.getCameraOffsetX1();
	pointOut[1] = calcY2(pointOut[0], y2n) + camSetup.getCameraOffsetX2();
	return pointOut;
    }

    abstract float calcY2n(float x1, float x2) throws SR_EELS_Exception;

    abstract float calcY1(float x1, float y2n);

    abstract float calcY2(float y1, float y2n);

}
