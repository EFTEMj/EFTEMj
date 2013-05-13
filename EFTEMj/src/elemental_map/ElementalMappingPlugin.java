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
package elemental_map;

import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.util.Arrays;
import java.util.LinkedHashMap;
import javax.swing.JLabel;
import tools.EnergyLossExtractor;
import tools.ExtendedStackToImage;
import tools.IonisationEdges;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

/**
 * 
 * 
 * @author Michael Epping <michael.epping@uni-muenster.de>
 * 
 */
public class ElementalMappingPlugin implements ExtendedPlugInFilter {

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
    private final int FLAGS = DOES_32 | NO_CHANGES | FINAL_PROCESSING;
    private ImagePlus stack;
    private float[] eLossArray;
    private float edgeELoss;

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
     */
    @Override
    public int setup(String arg, ImagePlus imp) {
	if (arg == "final") {
	    // implement final processing
	    return NO_CHANGES | DONE;
	}
	// No setup is done here. See showDialog() for the setup procedure.
	return FLAGS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
     */
    @Override
    public void run(ImageProcessor ip) {
	// TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see ij.plugin.filter.ExtendedPlugInFilter#showDialog(ij.ImagePlus, java.lang.String,
     * ij.plugin.filter.PlugInFilterRunner)
     */
    @Override
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
	// Check if imp is a stack.
	if (imp.getStackSize() <= 1) {
	    // ExtendedStackToImage is a plugin
	    new ExtendedStackToImage().convertImagesToStack();
	    if (IJ.getImage().getStackSize() <= 1) {
		canceled();
		return NO_CHANGES | DONE;
	    } else {
		imp = IJ.getImage();
	    }
	}
	stack = imp;
	if (showParameterDialog(command) == CANCEL) {
	    canceled();
	    return NO_CHANGES | DONE;
	}
	return FLAGS;
    }

    private int showParameterDialog(String title) {
	GenericDialog gd = new GenericDialog(title + " - set parameters", IJ.getInstance());
	gd.addSlider("Edge energy loss:", getMinELoss(), getMaxELoss(), getPredictedEdgeELoss());
	Panel panel = new Panel(new FlowLayout());
	panel.add(new Label("Predicted edge:"));
	panel.add(new JLabel("<html>" + getPredictedEdgeLabel(Math.round(edgeELoss)) + "</html>"));
	gd.addPanel(panel);
	// TODO add Choice to select epsilon
	gd.showDialog();
	if (gd.wasCanceled()) {
	    return CANCEL;
	}
	Scrollbar scrollbar = (Scrollbar) gd.getSliders().get(0);
	edgeELoss = scrollbar.getValue();
	IJ.showMessage("Energy loss is " + edgeELoss);
	return OK;
    }

    private float getMaxELoss() {
	if (eLossArray == null) {
	    initELossArry();
	}
	float[] sortedELossArray = Arrays.copyOf(eLossArray, eLossArray.length);
	Arrays.sort(sortedELossArray);
	return sortedELossArray[sortedELossArray.length - 1];
    }

    private float getMinELoss() {
	if (eLossArray == null) {
	    initELossArry();
	}
	float[] sortedELossArray = Arrays.copyOf(eLossArray, eLossArray.length);
	Arrays.sort(sortedELossArray);
	return sortedELossArray[0];
    }

    /**
     * 
     * This method takes the two highest energy loss values of the energy loss array and tries to find an edge at the
     * given interval. If no edge is listed for the given interval the median of the selected energy loss values is
     * used.
     * 
     * @return A prediction of the edge energy loss.
     */
    private float getPredictedEdgeELoss() {
	float[] sortedELossArray = Arrays.copyOf(eLossArray, eLossArray.length);
	Arrays.sort(sortedELossArray);
	float eLossHigh = eLossArray[eLossArray.length - 1];
	float eLossLow = eLossArray[eLossArray.length - 2];
	// TODO enhance the code to detect edges if there are 2 or more
	// post-edge images.
	if (findEdge(eLossLow, eLossHigh) == false) {
	    edgeELoss = (eLossHigh + eLossLow) / 2;
	}
	return edgeELoss;
    }

    /**
     * Tries to find an edge in the given energy loss interval.
     * 
     * @param eLossLow
     *            The lower limit of the interval.
     * @param eLossHigh
     *            The upper limit of the interval.
     * @return <code>true</code> if an edge was found.
     */
    private boolean findEdge(float eLossLow, float eLossHigh) {
	LinkedHashMap<Integer, String> edges = IonisationEdges.getInstance().getEdges();
	int[] possibleEdges = new int[edges.size()];
	int edgeCount = 0;
	for (int i = (int) Math.ceil(eLossLow); i < eLossHigh; i++) {
	    if (edges.get(i) != null) {
		possibleEdges[edgeCount] = i;
		edgeCount++;
	    }
	}
	if (edgeCount == 0) {
	    return false;
	}
	if (edgeCount == 1) {
	    edgeELoss = (float) possibleEdges[0];
	    return true;
	}
	float mean = (eLossHigh + eLossLow) / 2;
	int selected = 0;
	float diff = Math.abs(mean - possibleEdges[0]);
	for (int i = 1; i < edgeCount; i++) {
	    if (Math.abs(mean - possibleEdges[i]) < diff) {
		diff = Math.abs(mean - possibleEdges[i]);
		selected = i;
	    }
	}
	edgeELoss = (float) possibleEdges[selected];
	return true;
    }

    /**
     * @param edgeELoss
     *            The energy loss of the ionisation edge.
     * @return If the given energy loss is listed at the database the element and the name of the edge are written to
     *         this string.
     */
    private String getPredictedEdgeLabel(int edgeELoss) {
	String label;
	LinkedHashMap<Integer, String> edges = IonisationEdges.getInstance().getEdges();
	label = edges.get(edgeELoss);
	if (label == null) {
	    label = "Found no matching edge.";
	}
	return label;
    }

    private void initELossArry() {
	eLossArray = new float[stack.getStackSize()];
	for (int i = 0; i < eLossArray.length; i++) {
	    eLossArray[i] = EnergyLossExtractor.eLossFromTitle(stack, i);
	}
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

    /**
     * Cancel the plugin and show a status message.
     */
    private void canceled() {
	IJ.showStatus("Elemental mapping has been canceled.");
    }
}