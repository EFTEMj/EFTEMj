
package sr_eels;

import eftemj.EFTEMj;
import ij.Prefs;

/**
 * This enum generates all {@link Prefs} keys that are used by
 * <code>SR_EELS_</code> classes.<br />
 * If you select an item of this enum, use <code>getValue()</code> to get the
 * corresponding {@link Prefs} key as a {@link String}.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 */
public enum SR_EELS_PrefsKeys {
		specMagValues("specMagValues"), specMagIndex("specMagIndex"), binningIndex(
			"binningIndex"), binningUser("binningUser"), offsetIndex("offsetIndex"),
		offsetLoss("offsetLoss"), offsetAbsolute("offsetAbsolute"), cameraWidth(
			"cameraWidth"), cameraHeight("cameraHeight"), dispersionEloss(
				"dispersionEloss."), dispersionSettings("dispersionSettings."), none(
					""), characterisationDatabasePath("characterisationDatabasePath");

	/**
	 * <code>EFTEMj.PREFS_PREFIX + "SR-EELS.".<code>
	 */
	protected static final String PREFS_PREFIX = EFTEMj.PREFS_PREFIX + "SR-EELS.";

	private String value;

	SR_EELS_PrefsKeys(final String value) {
		this.value = value;
	}

	/**
	 * @return the full key that is used to access the property with {@link Prefs}
	 *         .
	 */
	public String getValue() {
		switch (this) {
			case specMagIndex:
			case binningIndex:
			case binningUser:
			case offsetIndex:
			case offsetLoss:
			case offsetAbsolute:
				return dispersionSettings.getValue() + value;
			case dispersionSettings:
			case dispersionEloss:
			case cameraHeight:
			case cameraWidth:
			case characterisationDatabasePath:
			case none:
			default:
				return PREFS_PREFIX + value;
		}
	}
}
