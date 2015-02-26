package sr_eels;

public class SimpleCoordinateCorrection extends CoordinateCorrector {

    public SimpleCoordinateCorrection(final SR_EELS_FloatProcessor inputProcessor,
	    final SR_EELS_FloatProcessor outputProcessor) {
	super(inputProcessor, outputProcessor);
    }

    @Override
    float calcY2n(final float x1, final float x2) throws SR_EELS_Exception {
	final float y2n = functionWidth.getY2n(new float[] { x1, x2 });
	return y2n;
    }

    @Override
    float calcY1(final float x1, final float y2n) {
	return x1;
    }

    @Override
    float calcY2(final float y1, final float y2n) {
	final float y2 = functionBorder.getY2(new float[] { y1, y2n });
	return y2;
    }
}
