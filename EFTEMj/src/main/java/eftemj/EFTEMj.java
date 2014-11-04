/**
 * EFTEMj - Processing of Energy Filtering TEM images with ImageJ
 *
 * Copyright (c) 2014, Michael Entrup b. Epping <michael.entrup@wwu.de>
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
package eftemj;

/**
 * This class is used to store constants of EFTEMj.
 *
 * Maybe there will be some other things this class is used for.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 *
 */
public class EFTEMj {

    /**
     * <code>EFTEMj.<code>
     */
    public static final String PREFS_PREFIX = "EFTEMj.";

    /**
     * The path inside the JAR file where the macro files are located.
     */
    public static final String PATH_MACROS = "/macros/";

    /**
     * @param classToParse
     *            can be any {@link Object}.
     * @return the name of the passed {@link Object} without the package prefix.
     */
    public static String getNameWithoutPackage(final Object classToParse) {
	final String className = classToParse.getClass().getName()
		.substring(classToParse.getClass().getName().indexOf(".") + 1);
	return className;
    }

}
