package sr_eels;

import ij.Prefs;
import sr_eels.SR_EELS.KEYS;

public class CameraSetup {

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
