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
package drift;

import java.awt.Point;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

/**
 * This plugin is used to shift the images of an {@link ImagePlus} containing a stack. The user is asked to enter the
 * enter the shift values (x- and y-direction) for each image of the stack. Then he can select to apply the given values
 * or let the plugin optimise the shift values and afterwards apply them.
 * 
 * @author Michael Epping <michael.epping@uni-muenster.de>
 * 
 */
public class StackShifterPlugin implements ExtendedPlugInFilter {

    /**
     * The plugin will be aborted.
     */
    private final int CANCEL = 0;
    /**
     * The plugin will continue with the next step.
     */
    private final int OK = 1;
    /**
     * <code>DOES_32 | NO_CHANGES | FINAL_PROCESSING</code>
     */
    private final int FLAGS = DOES_ALL;
    /**
     * This is the {@link ImagePlus} that has been selected when starting the plugin.
     */
    private ImagePlus initialImp;
    /**
     * If true a new {@link ImagePlus} is created and the input {@link ImagePlus} is not changed.
     */
    private boolean createNew;
    /**
     * If true the shift values are optimised.
     */
    private boolean optimise;
    /**
     * An array containing the shift values (as {@link Point}s) for each image of the initial stack.
     */
    private Point[] shift;

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
     */
    @Override
    public int setup(String arg, ImagePlus imp) {
	return FLAGS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
     */
    @Override
    public void run(ImageProcessor ip) {
	if (createNew == true) {
	    ImagePlus newImp = (ImagePlus) initialImp.clone();
	    OptimisedStackShifter.shiftImages(newImp, shift, optimise);
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.ExtendedPlugInFilter#showDialog(ij.ImagePlus, java.lang.String,
     * ij.plugin.filter.PlugInFilterRunner)
     */
    @Override
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
	initialImp = imp;
	showParameterDialog(command);
	if (createNew == true) {
	    return FLAGS | NO_CHANGES;
	}
	return FLAGS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.ExtendedPlugInFilter#setNPasses(int)
     */
    @Override
    public void setNPasses(int nPasses) {
	// This method is not used.
    }

    private int showParameterDialog(String title) {
	GenericDialog gd = new GenericDialog(title, IJ.getInstance());

	gd.showDialog();
	if (gd.wasCanceled() == true) {
	    return CANCEL;
	}
	return OK;
    }

}
