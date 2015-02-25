package sr_eels;

public class SimpleCoordinateCorrection extends CoordinateCorrector {

    public SimpleCoordinateCorrection(SR_EELS_FloatProcessor inputProcessor) {
	super(inputProcessor);
    }

    @Override
    float calcY2n(final float x1, final float x2) throws SR_EELS_Exception {
	final float y2n = functionWidth.getY2n(new double[] { x1, x2 });
	return y2n;
    }

    @Override
    float calcY1(final float x1, final float y2n) {
	return x1;
    }

    @Override
    float calcY2(final float y1, final float y2n) {
	final float y2 = (float) functionBorder.getY2(new double[] { y1, y2n });
	return y2;
    }
}
