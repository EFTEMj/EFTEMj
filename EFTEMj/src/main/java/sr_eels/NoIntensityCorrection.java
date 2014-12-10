package sr_eels;

import ij.process.FloatProcessor;

public class NoIntensityCorrection extends IntensityCorrector {

    public NoIntensityCorrection(final FloatProcessor inputImage, final CoordinateCorrector coordinateCorrector) {
	super(inputImage, coordinateCorrector);
    }

    @Override
    public float getIntensity(final int x1, final int x2) {
	final float[] point;
	try {
	    point = coordinateCorrector.transformCoordinate(x1, x2);
	} catch (SR_EELS_Exception exc1) {
	    return 0f;
	}
	final int y1 = Math.round(point[0]);
	final int y2 = Math.round(point[1]);
	try {
	    return inputImage.getf(y1, y2);
	} catch (ArrayIndexOutOfBoundsException exc) {
	    return 0f;
	}
    }

}
