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

class QuadraticProblem implements DifferentiableMultivariateVectorFunction {

    private List<Double> x;
    private List<Double> y;

    public QuadraticProblem() {
	x = new ArrayList<Double>();
	y = new ArrayList<Double>();
    }

    public void addPoint(double x, double y) {
	this.x.add(x);
	this.y.add(y);
    }

    public double[] calculateTarget() {
	double[] target = new double[y.size()];
	for (int i = 0; i < y.size(); i++) {
	    target[i] = y.get(i).doubleValue();
	}
	return target;
    }

    private double[][] jacobian(double[] variables) {
	double[][] jacobian = new double[x.size()][4];
	for (int i = 0; i < jacobian.length; ++i) {
	    jacobian[i][0] = x.get(i) * x.get(i) * x.get(i);
	    jacobian[i][1] = x.get(i) * x.get(i);
	    jacobian[i][2] = x.get(i);
	    jacobian[i][3] = 1.0;
	}
	return jacobian;
    }

    public double[] value(double[] variables) {
	double[] values = new double[x.size()];
	for (int i = 0; i < values.length; ++i) {
	    values[i] = variables[0] * Math.pow(x.get(i), 3) + (variables[1] * x.get(i) + variables[2]) * x.get(i)
		    + variables[3];
	}
	return values;
    }

    public MultivariateMatrixFunction jacobian() {
	return new MultivariateMatrixFunction() {

	    public double[][] value(double[] point) {
		return jacobian(point);
	    }
	};
    }

    public static void main(final String[] cmdline) {
	QuadraticProblem problem = new QuadraticProblem();

	problem.addPoint(1032.555, 300.8438);
	problem.addPoint(289.5005, 275.125);
	problem.addPoint(620.8233, 292.375);
	problem.addPoint(1808.2422, 276.125);
	problem.addPoint(1436.317, 294.875);

	LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();

	final double[] weights = { 1, 1, 1, 1, 1 };

	final double[] initialSolution = { 1, 1, 1, 1 };

	PointVectorValuePair optimum = optimizer.optimize(100, problem, problem.calculateTarget(), weights,
		initialSolution);

	final double[] optimalValues = optimum.getPoint();

	System.out.println("A: " + optimalValues[0]);
	System.out.println("B: " + optimalValues[1]);
	System.out.println("C: " + optimalValues[2]);
	System.out.println("D: " + optimalValues[3]);
    }

}
