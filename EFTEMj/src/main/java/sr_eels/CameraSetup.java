
package sr_eels;

import ij.Prefs;

public class CameraSetup {

	/**
	 * @return the height of the camera stored at the ImageJ preferences.
	 */
	public static int getFullHeight() {
		return (int) Prefs.get(SR_EELS_PrefsKeys.cameraHeight.getValue(), 4096);
	}

	/**
	 * @return the width of the camera stored at the ImageJ preferences.
	 */
	public static int getFullWidth() {
		return (int) Prefs.get(SR_EELS_PrefsKeys.cameraWidth.getValue(), 4096);
	}
}
