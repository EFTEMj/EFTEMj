package sr_eels;

import ij.process.FloatProcessor;

public abstract class IntensityCorrector {

    FloatProcessor inputImage;
    CoordinateCorrector coordinateCorrector;

    public IntensityCorrector(final FloatProcessor inputImage, final CoordinateCorrector coordinateCorrector) {
	this.inputImage = inputImage;
	this.coordinateCorrector = coordinateCorrector;
    }

    public abstract float getIntensity(int x1, int x2);

}
