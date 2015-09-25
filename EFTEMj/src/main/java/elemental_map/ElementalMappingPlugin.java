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

package elemental_map;

import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.CancellationException;

import javax.swing.JLabel;

import elemental_map.ElementalMapping.AVAILABLE_METHODS;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;
import tools.EnergyLossExtractor;
import tools.ExposureExtractor;
import tools.ExtendedImagesToStack;
import tools.IonisationEdges;

/**
 * This plugin is used to create elemental maps. A power law model estimates the
 * background signal. There are no limitations (except system memory) regarding
 * the number of used pre- and post-edge images. Several methods are available
 * to make a power law fit to the background signal. All methods are optimised
 * for parallel processing.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
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
	/**
	 * The {@link ImagePlus} that is used for elemental mapping.
	 */
	private ImagePlus impStack;
	/**
	 * An array that represents the energy losses of the stack images.
	 */
	private float[] energyLossArray;
	/**
	 * An array that represents the exposure times of the stack images.
	 */
	private float[] exposureArray;
	/**
	 * The energy loss that divides the images into pre-edge and post-edge.
	 */
	private float edgeEnergyLoss;
	/**
	 * The accuracy of the used fit method.
	 */
	private float epsilon;
	/**
	 * The selected fit method.
	 */
	private AVAILABLE_METHODS method;
	/**
	 * the {@link Calibration} of the input stack.
	 */
	private Calibration calibration;

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	@Override
	public int setup(final String arg, final ImagePlus imp) {
		if (arg == "final") {
			// TODO Implement final processing
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
	public void run(final ImageProcessor ip) {
		switch (method) {
			case LMA:
			case MLE:
			case LSE:
			case WLSE:
				ElementalMapping mapping;
				if (checkForVaryingExposure() == false) {
					mapping = new ElementalMapping(energyLossArray, impStack,
						edgeEnergyLoss, epsilon, method);
				}
				else {
					mapping = new ElementalMapping(energyLossArray, exposureArray,
						impStack, edgeEnergyLoss, epsilon, method);
				}
				mapping.startCalculation();
				// TODO Move all show-methods to the final processing
				mapping.showRMap(calibration);
				mapping.showLnAMap(calibration);
				mapping.showErrorMap(calibration);
				mapping.showElementalMap(calibration);
				break;
			default:
				IJ.showStatus(method + " has been selected.");
				IJ.showMessage(method + " is not available",
					"The method has not yet been implemented.\n" +
						"Check if a newer version of EFTEMj includes this feature.");
				break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.filter.ExtendedPlugInFilter#showDialog(ij.ImagePlus, java.lang.String,
	 * ij.plugin.filter.PlugInFilterRunner)
	 */
	@Override
	public int showDialog(final ImagePlus imp, final String command,
		final PlugInFilterRunner pfr)
	{
		try {
			// Check if imp is a stack.
			if (imp.getStackSize() <= 1) {
				// ExtendedStackToImage is a plugin
				new ExtendedImagesToStack().convertImagesToStack();
				if (IJ.getImage().getStackSize() <= 1) {
					canceled();
					return NO_CHANGES | DONE;
				}
				impStack = IJ.getImage();
			}
			else {
				impStack = imp;
			}
			calibration = imp.getCalibration();
			checkEnergyLosses();
			if (showParameterDialog(command) == CANCEL) {
				canceled();
				return NO_CHANGES | DONE;
			}
			return FLAGS;
		}
		catch (final CancellationException exc) {
			IJ.showStatus("Cancelled by user...");
			return DONE;
		}
	}

	/**
	 * Check if any energy loss is 0. The user is asked to enter values.
	 */
	private void checkEnergyLosses() {
		initELossArry();
		for (int i = 0; i < energyLossArray.length; i++) {
			if (energyLossArray[i] == 0) {
				float energy = 0;
				do {
					energy = getFloat("Enter energy loss", "Energy loss at slice " + (1 +
						i), energyLossArray[i]);
				}
				while (energy == 0);
				energyLossArray[i] = energy;
				String oldLabel = impStack.getStack().getShortSliceLabel(i + 1);
				if (oldLabel == null) oldLabel = "";
				impStack.getStack().setSliceLabel(oldLabel + "[" + energy + "eV]", i +
					1);
				impStack.changes = true;
			}
		}
		impStack.updateAndRepaintWindow();
	}

	private float getFloat(final String title, final String text,
		final float init)
	{
		final GenericDialog gd = new GenericDialog(title);
		gd.addNumericField(text, init, 2, 7, "eV");
		gd.showDialog();
		if (gd.wasCanceled()) throw new CancellationException();
		return (float) gd.getNextNumber();
	}

	/**
	 * Creates and shows the {@link GenericDialog} that is used to set the
	 * parameters for elemental mapping.
	 *
	 * @param title
	 * @return The constant <code>OK</code> or <code>CANCEL</code>.
	 */
	private int showParameterDialog(final String title) {
		final GenericDialog gd = new GenericDialog(title + " - set parameters", IJ
			.getInstance());
		// TODO Add a button to show a text window with all detected energy
		// losses.
		gd.addSlider("Edge energy loss:", getMinELoss(), getMaxELoss(),
			getPredictedEdgeELoss());
		final Panel panel = new Panel(new FlowLayout());
		panel.add(new Label("Predicted edge:"));
		panel.add(new JLabel("<html>" + getPredictedEdgeLabel(Math.round(
			edgeEnergyLoss)) + "</html>"));
		gd.addPanel(panel);
		gd.addChoice("Epsilon:", ElementalMapping.AVAILABLE_EPSILONS
			.toStringArray(), ElementalMapping.AVAILABLE_EPSILONS.toStringArray()[0]);
		gd.addChoice("Method:", ElementalMapping.AVAILABLE_METHODS.toStringArray(),
			AVAILABLE_METHODS.MLE.toString());
		gd.setResizable(false);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return CANCEL;
		}
		final Scrollbar scrollbar = (Scrollbar) gd.getSliders().get(0);
		edgeEnergyLoss = scrollbar.getValue();
		epsilon = new Float(gd.getNextChoice());
		method = AVAILABLE_METHODS.values()[gd.getNextChoiceIndex()];
		return OK;
	}

	/**
	 * @return The highest energy loss of an image at the given stack.
	 */
	private float getMaxELoss() {
		if (energyLossArray == null) {
			initELossArry();
		}
		final float[] sortedELossArray = Arrays.copyOf(energyLossArray,
			energyLossArray.length);
		Arrays.sort(sortedELossArray);
		return sortedELossArray[sortedELossArray.length - 1];
	}

	/**
	 * @return The lowest energy loss of an image at the given stack.
	 */
	private float getMinELoss() {
		if (energyLossArray == null) {
			initELossArry();
		}
		final float[] sortedELossArray = Arrays.copyOf(energyLossArray,
			energyLossArray.length);
		Arrays.sort(sortedELossArray);
		return sortedELossArray[0];
	}

	/**
	 * This method takes the two highest energy loss values of the energy loss
	 * array and tries to find an edge at the given interval. If no edge is listed
	 * for the given interval the median of the selected energy loss values is
	 * used.
	 *
	 * @return A prediction of the edge energy loss.
	 */
	private float getPredictedEdgeELoss() {
		final float[] sortedELossArray = Arrays.copyOf(energyLossArray,
			energyLossArray.length);
		Arrays.sort(sortedELossArray);
		final float eLossHigh = sortedELossArray[energyLossArray.length - 1];
		final float eLossLow = sortedELossArray[energyLossArray.length - 2];
		// TODO enhance the code to detect edges if there are 2 or more
		// post-edge images.
		if (findEdge(eLossLow, eLossHigh) == false) {
			edgeEnergyLoss = (eLossHigh + eLossLow) / 2;
		}
		return edgeEnergyLoss;
	}

	/**
	 * Tries to find an edge in the given energy loss interval. The value is saved
	 * at the field <code>edgeEnergyLoss</code>. The class {@link IonisationEdges}
	 * is used at this method.
	 *
	 * @param eLossLow The lower limit of the interval.
	 * @param eLossHigh The upper limit of the interval.
	 * @return <code>true</code> if an edge was found.
	 */
	private boolean findEdge(final float eLossLow, final float eLossHigh) {
		final LinkedHashMap<Integer, String> edges = IonisationEdges.getInstance()
			.getEdges();
		final int[] possibleEdges = new int[edges.size()];
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
		else if (edgeCount == 1) {
			edgeEnergyLoss = possibleEdges[0];
		}
		else {
			int selected = 0;
			final GenericDialog gd = new GenericDialog("Select an ionisation edge", IJ
				.getInstance());
			gd.addMessage(
				"More than one edge is qualified for the given energy losses.\nPlease select one.");
			final String[] edgeLabels = new String[edgeCount];
			for (int i = 0; i < edgeCount; i++) {
				edgeLabels[i] = possibleEdges[i] + "eV - " + edges.get(
					possibleEdges[i]);
			}
			gd.addChoice("Edges:", edgeLabels, edgeLabels[selected]);
			gd.setResizable(false);
			gd.showDialog();
			if (gd.wasOKed()) {
				selected = gd.getNextChoiceIndex();
			}
			else {
				final float mean = (eLossHigh + eLossLow) / 2;
				float diff = Math.abs(mean - possibleEdges[0]);
				for (int i = 1; i < edgeCount; i++) {
					if (Math.abs(mean - possibleEdges[i]) < diff) {
						diff = Math.abs(mean - possibleEdges[i]);
						selected = i;
					}
				}
			}
			edgeEnergyLoss = possibleEdges[selected];
		}
		return true;
	}

	/**
	 * The class {@link IonisationEdges} is used to identify an ionisation edge at
	 * the given energy loss.
	 *
	 * @param edgeELoss The energy loss of the ionisation edge.
	 * @return If the given energy loss is listed at the database the element and
	 *         the name of the edge are written to this string.
	 */
	private String getPredictedEdgeLabel(final int edgeELoss) {
		String label;
		final LinkedHashMap<Integer, String> edges = IonisationEdges.getInstance()
			.getEdges();
		label = edges.get(edgeELoss);
		if (label == null) {
			label = "Found no matching edge.";
		}
		return label;
	}

	/**
	 * Initialises the array (the field <code>energyLossArray</code>) that
	 * represents the energy losses of the stack used for elemental mapping.
	 */
	private void initELossArry() {
		energyLossArray = new float[impStack.getStackSize()];
		for (int i = 0; i < energyLossArray.length; i++) {
			energyLossArray[i] = new EnergyLossExtractor().extractFloatFromTitle(
				impStack, i);
		}
	}

	private boolean checkForVaryingExposure() {
		if (exposureArray == null) {
			initExposureArray();
		}
		if (Arrays.binarySearch(exposureArray, 0) >= 0) {
			return false;
		}
		/*
		 * We check if all exposure times are the same by creating a new array (same length as exposureArray) that
		 * contains only the first value in exposureArray.
		 */
		final float[] tempArray = new float[exposureArray.length];
		Arrays.fill(tempArray, exposureArray[0]);
		if (Arrays.equals(exposureArray, tempArray)) {
			return false;
		}
		return true;
	}

	private void initExposureArray() {
		exposureArray = new float[impStack.getStackSize()];
		for (int i = 0; i < exposureArray.length; i++) {
			exposureArray[i] = new ExposureExtractor().extractFloatFromTitle(impStack,
				i);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.filter.ExtendedPlugInFilter#setNPasses(int)
	 */
	@Override
	public void setNPasses(final int nPasses) {
		// This method is not used.
	}

	/**
	 * Cancel the plugin and show a status message.
	 */
	private void canceled() {
		IJ.showStatus("Elemental mapping has been canceled.");
	}

	public static void main(final String[] args) {
		// start ImageJ
		new ImageJ();

		// open the sample stack
		final ImagePlus image = IJ.openImage(
			"C:\\Users\\m_eppi02\\Downloads\\EFTEM-Stack_Fe_50counts.tif");
		image.show();

		// run the plugin
		final Class<?> clazz = ElementalMappingPlugin.class;
		IJ.runPlugIn(clazz.getName(), "");
	}
}
