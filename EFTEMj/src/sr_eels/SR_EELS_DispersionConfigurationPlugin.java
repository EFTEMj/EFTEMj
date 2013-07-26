/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 * 
 * Copyright (c) 2013, Michael Epping
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
import ij.plugin.PlugIn;

/**
 * @author Michael Epping <michael.epping@uni-muenster.de>
 * 
 */
public class SR_EELS_DispersionConfigurationPlugin implements PlugIn {

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.PlugIn#run(java.lang.String)
     */
    @Override
    public void run(String arg) {
	Prefs.set(EFTEMj.PREFS_PREFIX + "SR-EELS.dispersion.specMagValues", "125;163;200;250;315");
	Prefs.set(EFTEMj.PREFS_PREFIX + "SR-EELS.dispersion.dispersion.125", 0.051724149);
	Prefs.set(EFTEMj.PREFS_PREFIX + "SR-EELS.dispersion.dispersion.163", 0.03966051);
	Prefs.set(EFTEMj.PREFS_PREFIX + "SR-EELS.dispersion.dispersion.200", 0.03166561);
	Prefs.set(EFTEMj.PREFS_PREFIX + "SR-EELS.dispersion.dispersion.250", 0.02569021);
	Prefs.set(EFTEMj.PREFS_PREFIX + "SR-EELS.dispersion.dispersion.315", 0.02021019);
    }
}