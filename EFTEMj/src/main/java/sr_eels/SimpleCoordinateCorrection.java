package sr_eels;

import sr_eels.testing.SR_EELS_Polynomial_2D;

public class SimpleCoordinateCorrection extends CoordinateCorrector {

    public SimpleCoordinateCorrection(final SR_EELS_Polynomial_2D functionWidth,
	    final SR_EELS_Polynomial_2D functionBorder, CameraSetup camSetup) {
	super(functionWidth, functionBorder, camSetup);
    }

    @Override
    float calcY2n(final float x1, final float x2) throws SR_EELS_Exception {
	return (float) functionWidth.normAreaToMax(new double[] { 0, x2 });
    }

    @Override
    float calcY1(final float x1, final float y2n) {
	return x1;
    }

    @Override
    float calcY2(final float y1, final float y2n) {
	return (float) functionBorder.val(new double[] { y1, y2n });
    }

}
