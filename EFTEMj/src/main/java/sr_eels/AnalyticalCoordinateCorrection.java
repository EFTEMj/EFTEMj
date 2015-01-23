package sr_eels;

import sr_eels.testing.SR_EELS_Polynomial_2D;

public class AnalyticalCoordinateCorrection extends CoordinateCorrector {

    public AnalyticalCoordinateCorrection(final SR_EELS_Polynomial_2D functionWidth,
	    final SR_EELS_Polynomial_2D functionBorder, CameraSetup camSetup) {
	super(functionWidth, functionBorder, camSetup);
	// TODO Auto-generated constructor stub
    }

    @Override
    float calcY2n(final float x1, final float x2) throws SR_EELS_Exception {
	return (float) functionWidth.normAreaToMax(new double[] { 0, x2 });
    }

    @Override
    float calcY1(final float x1, final float y2n) {
	return (float) functionBorder.getY1(x1, y2n);
    }

    @Override
    float calcY2(final float y1, final float y2n) {
	return (float) functionBorder.val(new double[] { y1, y2n });
    }

}