package sr_eels.testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import javax.swing.JFileChooser;

import libs.lma.LMA;
import libs.lma.implementations.Polynomial_2D;

public class SR_EELS_CalibrationDataSet {

    Vector<SR_EELS_CalibrationDataSetFile> dataSetFiles;

    public SR_EELS_CalibrationDataSet() {
	dataSetFiles = new Vector<SR_EELS_CalibrationDataSet.SR_EELS_CalibrationDataSetFile>();
	final JFileChooser fChooser = new JFileChooser("C:\\Temp");
	fChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	fChooser.showOpenDialog(null);
	final String path = fChooser.getSelectedFile().getAbsolutePath() + File.separatorChar;
	final String[] files = fChooser.getSelectedFile().list();
	for (final String file : files) {
	    if (file.contains("values_")) {
		dataSetFiles.add(new SR_EELS_CalibrationDataSetFile(path + file));
	    }
	}
	System.out.println(dataSetFiles.size());
    }

    public static void main(final String[] cmdline) {
	final SR_EELS_CalibrationDataSet dataSet = new SR_EELS_CalibrationDataSet();
	final Polynomial_2D func = new Polynomial_2D(2, 2);
	final double[][] vals = dataSet.prepareValuesForPolynomial2DFit();
	final double[] a_fit = func.getInitialParameters();
	final LMA lma2 = new LMA(func, a_fit, vals);
	lma2.fit();
	final double[] a1_gnuplot = { 301.177, 0.00301992, -4.38781e-005, -0.0140776, 2.1228e-006, -4.28352e-009,
		2.53962e-006, 2.19209e-010, -4.12894e-013 };
	System.out.println("");
	for (int i = 0; i < a_fit.length; i++) {
	    System.out.println(a_fit[i] + "\t\t" + a1_gnuplot[i] + "\t\t" + Math.abs(a1_gnuplot[i] - a_fit[i])
		    / a_fit[i]);
	}
	System.out.println("");
    }

    private double[][] prepareValuesForPolynomial2DFit() {
	final Vector<Double> x1_vals = new Vector<Double>();
	final Vector<Double> x2_vals = new Vector<Double>();
	final Vector<Double> y_vals = new Vector<Double>();
	for (final SR_EELS_CalibrationDataSetFile file : dataSetFiles) {
	    x1_vals.addAll(file.x1_values);
	    x2_vals.addAll(file.centre_values);
	    y_vals.addAll(file.width_values);
	}
	final double[][] vals = new double[y_vals.size()][3];
	for (int i = 0; i < vals.length; i++) {
	    vals[i][0] = y_vals.get(i);
	    vals[i][1] = x1_vals.get(i) - 1024;
	    vals[i][2] = x2_vals.get(i) - 1024;
	}
	return vals;
    }

    private class SR_EELS_CalibrationDataSetFile {

	private final int X1_VALUES = 1;
	private final int TOP_VALUES = 3;
	private final int CENTRE_VALUES = 2;
	private final int BOTTOM_VALUES = 4;
	private final int WIDTH_VALUES = 5;

	Vector<Double> x1_values;
	Vector<Double> top_values;
	Vector<Double> centre_values;
	Vector<Double> bottom_values;
	Vector<Double> width_values;

	SR_EELS_CalibrationDataSetFile(final String path) {
	    x1_values = new Vector<Double>();
	    top_values = new Vector<Double>();
	    centre_values = new Vector<Double>();
	    bottom_values = new Vector<Double>();
	    width_values = new Vector<Double>();
	    final File file = new File(path);
	    try {
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		boolean containsData = true;
		do {
		    final String line = reader.readLine();
		    if (line == null) {
			containsData = false;
		    } else {
			/*
			 * Only read the line if if does not contain any comment.
			 */
			if (line.indexOf('#') == -1) {
			    final String[] splitLine = line.split("\\s");
			    x1_values.add(Double.valueOf(splitLine[X1_VALUES]));
			    top_values.add(Double.valueOf(splitLine[TOP_VALUES]));
			    centre_values.add(Double.valueOf(splitLine[CENTRE_VALUES]));
			    bottom_values.add(Double.valueOf(splitLine[BOTTOM_VALUES]));
			    width_values.add(Double.valueOf(splitLine[WIDTH_VALUES]));
			}
		    }
		} while (containsData);
		reader.close();
	    } catch (final FileNotFoundException exc) {
		exc.printStackTrace();
	    } catch (final IOException exc) {
		exc.printStackTrace();
	    }
	}
    }

}
