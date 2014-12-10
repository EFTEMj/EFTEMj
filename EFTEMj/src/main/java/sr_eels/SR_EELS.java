/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 *
 * Copyright (c) 2014, Michael Entrup b. Epping
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package sr_eels;

import eftemj.EFTEMj;
import ij.Prefs;

/**
 * This class contains constants and utility methods for the sr_eels package.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 *
 */
public class SR_EELS {

    /**
     * This {@link Enum} holds all {@link Prefs} keys that are used by this package.<br />
     * When using <code>dispersion</code> you have to add a additional number that indicates the Spec Mag.<br />
     * <code>PREFIX + KEYS.dispersion + "." + number</code>
     */
    public static enum KEYS {
	specMagValues, specMagIndex, dispersion, binningIndex, binningUser, offsetIndex, offsetLoss, offsetAbsolute
    }

    /**
     * <code>EFTEMj.PREFS_PREFIX + "SR-EELS.".<code>
     */
    protected static final String PREFS_PREFIX = EFTEMj.PREFS_PREFIX + "SR-EELS.";

    /**
     * This is the name of the file that stores the data points used to describe the change of the spectrum width.
     */
    protected static final String FILENAME_WIDTH = "Width.txt";

    /**
     * This is the name of the file that stores the data points used to describe the spectrum borders.
     */
    public static final String FILENAME_BORDERS = "Borders.txt";

}
