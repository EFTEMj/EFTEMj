package sr_eels;

import ij.process.FloatProcessor;

public abstract class IntensityCorrector {

    FloatProcessor input;
    CoordinateCorrector coordinateCorrector;

    public IntensityCorrector(final FloatProcessor inputImage, final CoordinateCorrector coordinateCorrector) {
	this.input = inputImage;
	this.coordinateCorrector = coordinateCorrector;
    }

    public abstract float getIntensity(int x1, int x2);

}
