package sr_eels;

import sr_eels.SR_EELS.KEYS;
import ij.ImagePlus;
import ij.Prefs;

/**
 * <p>
 * This class will handle the properties of the used camera.
 * </p>
 * 
 * <p>
 * Currently the following values are part of this class:
 * <ul>
 * <li>camera width</li>
 * <li>camera height</li>
 * <li>binning in x1 direction (horizontal)</li>
 * <li>binning in x2 direction (vertical)</li>
 * </ul>
 * width and height can be loaded from the ImageJ preferences by using the class {@link Prefs}.
 * </p>
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 *
 */
public class CameraSetup {

    private final int binningX1;
    private final int binningX2;
    private final int camWidth;
    private final int camHeight;

    /**
     * Create a new instance of {@link CameraSetup} that can be used by any class for easier handling of camera
     * properties.
     * 
     * @param inputImage
     *            is the {@link ImagePlus} that contains the processed image. We need this to get width and height of
     *            the image.
     * @param camWidth
     *            is the width of the camera.
     * @param camHeight
     *            is the height of the camera.
     */
    public CameraSetup(final ImagePlus inputImage, final int camWidth, final int camHeight) {
	super();
	this.camWidth = camWidth;
	this.camHeight = camHeight;
	this.binningX1 = camWidth / inputImage.getWidth();
	this.binningX2 = camHeight / inputImage.getHeight();
    }

    /**
     * Create a new instance of {@link CameraSetup} that can be used by any class for easier handling of camera
     * properties.
     * 
     * @param inputImage
     *            is the {@link ImagePlus} that contains the processed image. We need this to get width and height of
     *            the image.
     */
    public CameraSetup(final ImagePlus inputImage) {
	super();
	/*
	 * load width and height with ij.Prefs this.camWidth = (int) Prefs.get(PREFS_PREFIX + KEYS.cameraWidth, 4096);
	 */
	this.camWidth = (int) Prefs.get(SR_EELS.PREFS_PREFIX + KEYS.cameraWidth, 4096);
	this.camHeight = (int) Prefs.get(SR_EELS.PREFS_PREFIX + KEYS.cameraHeight, 4096);
	this.binningX1 = camWidth / inputImage.getWidth();
	this.binningX2 = camHeight / inputImage.getHeight();
    }

    public int getBinningX1() {
	return binningX1;
    }

    public int getBinningX2() {
	return binningX2;
    }

    /**
     * @return Camera width divided by 2.
     */
    public int getCameraOffsetX1() {
	return camWidth / 2;
    }

    /**
     * @return Camera height divided by 2.
     */
    public int getCameraOffsetX2() {
	return camHeight / 2;
    }

    /**
     * @return the height of the camera stored at the ImageJ preferences.
     */
    public static int getFullHeight() {
	return (int) Prefs.get(SR_EELS.PREFS_PREFIX + KEYS.cameraHeight, 4096);
    }

    /**
     * @return the width of the camera stored at the ImageJ preferences.
     */
    public static int getFullWidth() {
	return (int) Prefs.get(SR_EELS.PREFS_PREFIX + KEYS.cameraWidth, 4096);
    }
}
