package sr_eels;

import ij.process.FloatProcessor;

public abstract class IntensityCorrector {

    FloatProcessor inputImage;
    CoordinateCorrector coordinateCorrector;
    CameraSetup camSetup;

    public IntensityCorrector(final FloatProcessor inputImage, final CoordinateCorrector coordinateCorrector,
	    final CameraSetup camSetup) {
	this.inputImage = inputImage;
	this.coordinateCorrector = coordinateCorrector;
	this.camSetup = camSetup;
    }

    public abstract float getIntensity(int x1, int x2);

}
