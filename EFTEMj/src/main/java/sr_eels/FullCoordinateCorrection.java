package sr_eels;

public class FullCoordinateCorrection extends CoordinateCorrector {

    public FullCoordinateCorrection(SR_EELS_FloatProcessor inputProcessor) {
	super(inputProcessor);
    }

    @Override
    float calcY2n(float x1, float x2) throws SR_EELS_Exception {
	final float y2n = functionWidth.getY2n(new double[] { 0, x2 });
	return y2n;
    }

    @Override
    float calcY1(float x1, float y2n) {
	final float y1 = (float) functionBorder.getY1(new double[] { x1, y2n });
	return y1;
    }

    @Override
    float calcY2(float y1, float y2n) {
	final float y2 = (float) functionBorder.val(new double[] { y1, y2n });
	return y2;
    }

}
