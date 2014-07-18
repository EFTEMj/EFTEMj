package lma.implementations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JFileChooser;

import lma.JAMAMatrix;
import lma.LMA;
import lma.LMAMatrix;
import lma.LMAMultiDimFunction;
import lma.LMAMatrix.InvertException;

/**
 * An example fit which fits a plane to some data points and prints out the resulting fit parameters.
 */
public class MultiDimExampleFit {
    /** An example function with a form of y = a0 * x0 + a1 * x1 + a2 */
    public static class MultiDimExampleFunction extends LMAMultiDimFunction {
	@Override
	public double getY(double x[], double[] a) {
	    return a[0] + a[1] * x[0] + a[2] * x[0] * x[0] + a[3] * x[1] + a[4] * x[0] * x[1] + a[5] * x[0] * x[0]
		    * x[1] + a[6] * x[1] * x[1] + a[7] * x[0] * x[1] * x[1] + a[8] * x[0] * x[0] * x[1] * x[1];
	}

	@Override
	public double getPartialDerivate(double x[], double[] a, int parameterIndex) {
	    switch (parameterIndex) {
	    case 0:
		return 0;
	    case 1:
		return x[0];
	    case 2:
		return x[0] * x[0];
	    case 3:
		return x[1];
	    case 4:
		return x[0] * x[1];
	    case 5:
		return x[0] * x[0] * x[1];
	    case 6:
		return x[1] * x[1];
	    case 7:
		return x[0] * x[1] * x[1];
	    case 8:
		return x[0] * x[0] * x[1] * x[1];

	    }
	    throw new RuntimeException("No such parameter index: " + parameterIndex);
	}
    }

    public static double[][] loadData(int colX, int colY, int colZ) throws NumberFormatException, IOException {
	double[][] matrix = null;
	ArrayList<double[]> list = new ArrayList<double[]>();
	String zeile;
	String[] split = null;
	String path = "C:\\Temp";
	JFileChooser chooser = new JFileChooser(path);
	chooser.showOpenDialog(null);
	File file = chooser.getSelectedFile();
	while (file != null) {
	    FileReader reader = new FileReader(file);
	    BufferedReader data = new BufferedReader(reader);
	    while ((zeile = data.readLine()) != null) {
		split = zeile.split("\t");
		double[] values = new double[3];
		for (int i = 0; i < split.length; i++) {
		    if (i == colX) {
			values[0] = new Double(split[i]);
		    } else if (i == colY) {
			values[1] = new Double(split[i]);
		    } else if (i == colZ) {
			values[2] = new Double(split[i]);
		    }
		}
		list.add(values);
	    }
	    data.close();
	    chooser = new JFileChooser(path);
	    chooser.showOpenDialog(null);
	    file = chooser.getSelectedFile();
	}
	matrix = new double[list.size()][3];
	for (int i = 0; i < list.size(); i++) {
	    matrix[i][0] = list.get(i)[0];
	    matrix[i][1] = list.get(i)[1];
	    matrix[i][2] = list.get(i)[2];
	}
	return matrix;
    }

    /**
     * Does the actual fitting by using the above MultiDimExampleFunction (a plane)
     */
    public static void main(String[] args) {
	double[][] values = null;
	try {
	    values = MultiDimExampleFit.loadData(1, 2, 5);
	} catch (NumberFormatException e) {
	    System.exit(0);
	} catch (IOException e) {
	    System.exit(0);
	}
	double[] parameters = new double[] { 300, 0.001, 0.001, 0.001, 0.001, 0.001, 0.001, 0.001, 0.001 };
	double[] weights = new double[values.length];
	Arrays.fill(weights, 1.0);
	LMAMatrix matrix = new JAMAMatrix(parameters.length, parameters.length);
	LMA lma = new LMA(new MultiDimExampleFunction(), parameters, values, weights, matrix);
	lma.fit();
	System.out.println("iterations: " + lma.iterationCount);
	System.out.println("chi2: " + lma.chi2 + ",\n" + "param0: " + lma.parameters[0] + ",\n" + "param1: "
		+ lma.parameters[1] + ",\n" + "param2: " + lma.parameters[2] + ",\n" + "param3: " + lma.parameters[3]
		+ ",\n" + "param4: " + lma.parameters[4] + ",\n" + "param5: " + lma.parameters[5] + ",\n" + "param6: "
		+ lma.parameters[6] + ",\n" + "param7: " + lma.parameters[7] + ",\n" + "param8: " + lma.parameters[8]);
    }
}
