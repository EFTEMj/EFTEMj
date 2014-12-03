package sr_eels.testing;

import eftemj.EFTEMj;
import ij.measure.CurveFitter;

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

    private static final String FOLDER_DATA_SET = "sr_eels/lma/results_642562568Li";
    private Vector<SR_EELS_CalibrationDataSetFile> dataSetFiles;
    private String path;

    public SR_EELS_CalibrationDataSet() {
	selectDataSet();
	loadDataSet();
    }

    private void selectDataSet() {
	final JFileChooser fChooser = new JFileChooser("C:\\Temp");
	fChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	fChooser.showOpenDialog(null);
	path = fChooser.getSelectedFile().getAbsolutePath() + File.separatorChar;
    }

    private void loadDataSet() {
	dataSetFiles = new Vector<SR_EELS_CalibrationDataSet.SR_EELS_CalibrationDataSetFile>();
	final String[] files = new File(path).list();
	for (final String file : files) {
	    if (file.contains("values_")) {
		dataSetFiles.add(new SR_EELS_CalibrationDataSetFile(path + file));
	    }
	}
    }

    public SR_EELS_CalibrationDataSet(final boolean loadFromJAR) {
	if (loadFromJAR == false) {
	    selectDataSet();
	} else {
	    path = getClass().getResource(EFTEMj.PATH_TESTING + FOLDER_DATA_SET).getPath() + File.separator;
	}
	loadDataSet();
    }

    public static void main(final String[] cmdline) {
	final SR_EELS_CalibrationDataSet dataSet = new SR_EELS_CalibrationDataSet(true);
	final Polynomial_2D func = new Polynomial_2D(3, 2);
	final double[][] vals = dataSet.prepareValuesForPolynomial2DFit();
	final double[] a_fit = func.getInitialParameters();
	final LMA lma1 = new LMA(func, a_fit, vals);
	lma1.fit();

	final double[] a_gnuplot = { 185.193, 0.0114697, -0.000230046, -0.0142633, 4.09706e-006, -3.30355e-008,
		1.02076e-007, 1.17627e-009, -1.5941e-012, 5.4138e-011, 4.61404e-013, 8.10883e-016 };
	System.out.println("");
	System.out.println(func.getGnuplotCommands(Polynomial_2D.WIDTH_VS_POS));
	System.out.println("");
	System.out.println(func.compareParameters(Polynomial_2D.WIDTH_VS_POS, a_gnuplot));
	System.out.println("");

	final Polynomial_2D func2 = new Polynomial_2D(2, 2);
	final double[][] vals2 = dataSet.prepareValuesForBordersFit();
	final double[] b_fit = func2.getInitialParameters();
	final LMA lma2 = new LMA(func2, b_fit, vals2);
	lma2.fit();
	final double[] b_gnuplot = { 15.4528, 0.862132, -1.35147e-005, 0.00716796, -6.30902e-005, -6.46892e-009,
		7.99608e-007, 4.07466e-009, -4.64831e-012 };
	System.out.println("");
	System.out.println(func2.getGnuplotCommands(Polynomial_2D.BORDERS));
	System.out.println("");
	System.out.println(func2.compareParameters(Polynomial_2D.BORDERS, b_gnuplot));
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
	    vals[i][1] = x1_vals.get(i) - 2048;
	    vals[i][2] = x2_vals.get(i) - 2048;
	}
	return vals;
    }

    private double[][] prepareValuesForBordersFit() {
	final Vector<Double> x1_vals = new Vector<Double>();
	final Vector<Double> x2_vals = new Vector<Double>();
	final Vector<Double> y_vals = new Vector<Double>();
	for (final SR_EELS_CalibrationDataSetFile file : dataSetFiles) {
	    x1_vals.addAll(file.x1_values);
	    x1_vals.addAll(file.x1_values);
	    x1_vals.addAll(file.x1_values);
	    final Vector<Double> temp_top = new Vector<Double>();
	    final Vector<Double> temp_centre = new Vector<Double>();
	    final Vector<Double> temp_bottom = new Vector<Double>();
	    for (int i = 0; i < file.x1_values.size(); i++) {
		temp_top.add(file.fitter_top.f(file.fitter_top.getParams(), 0.));
		temp_centre.add(file.fitter_centre.f(file.fitter_centre.getParams(), 0.));
		temp_bottom.add(file.fitter_bottom.f(file.fitter_bottom.getParams(), 0.));
	    }
	    x2_vals.addAll(temp_top);
	    x2_vals.addAll(temp_centre);
	    x2_vals.addAll(temp_bottom);
	    y_vals.addAll(file.top_values);
	    y_vals.addAll(file.centre_values);
	    y_vals.addAll(file.bottom_values);
	}
	final double[][] vals = new double[y_vals.size()][3];
	for (int i = 0; i < vals.length; i++) {
	    vals[i][0] = y_vals.get(i) - 2048;
	    vals[i][1] = x1_vals.get(i) - 2048;
	    vals[i][2] = x2_vals.get(i) - 2048;
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
	CurveFitter fitter_top;
	CurveFitter fitter_centre;
	CurveFitter fitter_bottom;

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
	    fitter_top = fitPoly(x1_values, top_values);
	    fitter_centre = fitPoly(x1_values, centre_values);
	    fitter_bottom = fitPoly(x1_values, bottom_values);
	}

	private CurveFitter fitPoly(final Vector<Double> x_vals, final Vector<Double> y_vals) {
	    final double[] x_array = new double[x_vals.size()];
	    final double[] y_array = new double[y_vals.size()];
	    for (int i = 0; i < x_array.length; i++) {
		x_array[i] = x_vals.get(i);
		y_array[i] = y_vals.get(i);
	    }
	    final CurveFitter fitter = new CurveFitter(x_array, y_array);
	    fitter.doFit(CurveFitter.POLY3);
	    return fitter;
	}
    }

}
