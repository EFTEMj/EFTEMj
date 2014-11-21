package sr_eels.testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.JFileChooser;

import org.apache.commons.math3.analysis.DifferentiableMultivariateVectorFunction;
import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.optimization.PointVectorValuePair;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

class QuadraticProblem2D implements DifferentiableMultivariateVectorFunction {

    private final List<Double> x1;
    private final List<Double> x2;
    private final List<Double> y;

    public QuadraticProblem2D() {
	x1 = new ArrayList<Double>();
	x2 = new ArrayList<Double>();
	y = new ArrayList<Double>();
    }

    public void addPoint(final double x1, final double x2, final double y) {
	this.x1.add(x1);
	this.x2.add(x2);
	this.y.add(y);
    }

    public double[] calculateTarget() {
	final double[] target = new double[y.size()];
	for (int i = 0; i < y.size(); i++) {
	    target[i] = y.get(i).doubleValue();
	}
	return target;
    }

    private double[][] jacobian(final double[] variables) {
	final double[][] jacobian = new double[x1.size()][9];
	for (int i = 0; i < jacobian.length; ++i) {
	    jacobian[i][0] = 1.0;
	    jacobian[i][1] = x2.get(i);
	    jacobian[i][2] = x2.get(i) + x2.get(i);
	    jacobian[i][3] = x1.get(i);
	    jacobian[i][4] = x1.get(i) * x2.get(i);
	    jacobian[i][5] = x1.get(i) * x2.get(i) * x2.get(i);
	    jacobian[i][6] = x1.get(i) * x1.get(i);
	    jacobian[i][7] = x1.get(i) * x1.get(i) * x2.get(i);
	    jacobian[i][8] = x1.get(i) * x1.get(i) * x2.get(i) * x2.get(i);
	}
	return jacobian;
    }

    @Override
    public double[] value(final double[] variables) {
	final double[] values = new double[x1.size()];
	for (int i = 0; i < values.length; ++i) {
	    values[i] = variables[0] + variables[1] * x2.get(i) + variables[2] * x2.get(i) * x2.get(i) + variables[3]
		    * x1.get(i) + variables[4] * x1.get(i) * x2.get(i) + variables[5] * x1.get(i) * x2.get(i)
		    * x2.get(i) + variables[6] * x1.get(i) * x1.get(i) + variables[7] * x1.get(i) * x1.get(i)
		    * x2.get(i) + variables[8] * x1.get(i) * x1.get(i) * x2.get(i) * x2.get(i);
	}
	return values;
    }

    @Override
    public MultivariateMatrixFunction jacobian() {
	return new MultivariateMatrixFunction() {
	    @Override
	    public double[][] value(final double[] point) {
		return jacobian(point);
	    }
	};
    }

    @SuppressWarnings("deprecation")
    public static void main(final String[] cmdline) {
	final QuadraticProblem2D problem = new QuadraticProblem2D();

	int pointCount = PolyDataImport.importData(problem);

	final LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();

	final double[] weights = new double[pointCount];
	Arrays.fill(weights, 1.0);

	final double[] initialSolution = { 275, 8e-2, -1, -2e-2, 1e-5, -4e-9, -8e5, 2e-10, 8e5 };

	final PointVectorValuePair optimum = optimizer.optimize(10000, problem, problem.calculateTarget(), weights,
		initialSolution);

	final double[] optimalValues = optimum.getPoint();

	System.out.println("a00 = " + optimalValues[0]);
	System.out.println("a01 = " + optimalValues[1]);
	System.out.println("a02=  " + optimalValues[2]);
	System.out.println("a10 = " + optimalValues[3]);
	System.out.println("a11 = " + optimalValues[4]);
	System.out.println("a12 = " + optimalValues[5]);
	System.out.println("a20 = " + optimalValues[6]);
	System.out.println("a21 = " + optimalValues[7]);
	System.out.println("a22 = " + optimalValues[8]);
    }

    private static class PolyDataImport {

	public static int importData(QuadraticProblem2D problem) {
	    JFileChooser fChooser = new JFileChooser("C:\\Temp");
	    fChooser.showOpenDialog(null);
	    File file = fChooser.getSelectedFile();
	    Vector<Double[]> values = new Vector<Double[]>();
	    try {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		boolean containsData = true;
		do {
		    String line = reader.readLine();
		    if (line == null) {
			containsData = false;
		    } else {
			/*
			 * Only read the line if if does not contain any comment.
			 */
			if (line.indexOf('#') == -1) {
			    String[] splitLine = line.split("\\s");
			    Double[] point = { Double.valueOf(splitLine[0]), Double.valueOf(splitLine[1]),
				    Double.valueOf(splitLine[2]) };
			    values.add(point);
			}
		    }
		} while (containsData);
		reader.close();
	    } catch (FileNotFoundException exc) {
		exc.printStackTrace();
	    } catch (IOException exc) {
		exc.printStackTrace();
	    }
	    for (int i = 0; i < values.size(); i++) {
		problem.addPoint(values.get(i)[0], values.get(i)[1], values.get(i)[2]);
	    }
	    return values.size();
	}
    }
}