package sr_eels;

import sr_eels.SR_EELS.KEYS;
import ij.ImagePlus;
import ij.Prefs;

public class CameraSetup {

    private final int binningX1;
    private final int binningX2;
    private final int camWidth;
    private final int camHeight;

    /**
     * @param inputImage
     * @param camWidth
     * @param camHeight
     */
    public CameraSetup(final ImagePlus inputImage, final int camWidth, final int camHeight) {
	super();
	this.camWidth = camWidth;
	this.camHeight = camHeight;
	this.binningX1 = camWidth / inputImage.getWidth();
	this.binningX2 = camHeight / inputImage.getHeight();
    }

    /**
     * @param inputImage
     */
    public CameraSetup(final ImagePlus inputImage) {
	super();
	/*
	 * load width and height with ij.Prefs this.camWidth = (int) Prefs.get(PREFS_PREFIX + KEYS.cameraWidth, 4096);
	 */
	this.camWidth = 4096;
	this.camHeight = 4096;
	this.binningX1 = camWidth / inputImage.getWidth();
	this.binningX2 = camHeight / inputImage.getHeight();
    }

    public int getBinningX1() {
	return binningX1;
    }

    public int getBinningX2() {
	return binningX2;
    }

    public int getCameraOffsetX1() {
	return camWidth / 2;
    }

    public int getCameraOffsetX2() {
	return camHeight / 2;
    }

    public static int getFullHeight() {
	return (int) Prefs.get(SR_EELS.PREFS_PREFIX + KEYS.cameraHeight, 4096);
    }

    public static int getFullWidth() {
	return (int) Prefs.get(SR_EELS.PREFS_PREFIX + KEYS.cameraWidth, 4096);
    }
}
