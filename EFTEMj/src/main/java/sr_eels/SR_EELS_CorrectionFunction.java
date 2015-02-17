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
package sr_eels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import libs.lma.LMA;
import libs.lma.implementations.Polynomial_2D;
import sr_eels.testing.SR_EELS_CharacterisationPlugin;

/**
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 *
 */
public class SR_EELS_CorrectionFunction {

    private double[][] a;
    private double[][] b;
    private double offset;

    /**
     * Creates an object that can calculate the coordinate transformation, that is necessary for correcting a SR-EELS
     * dataset.
     *
     * @param path_borders
     * @param path_poly2D
     */
    public SR_EELS_CorrectionFunction(final String path_borders, final String path_poly2D) {
	getParametersA(path_borders);
	getParametersB(path_poly2D);
    }

    public double[] transform(final double x1_in, final double x2_in) {
	double x1 = x1_in - 2048;
	double x2 = x2_in - 2048;
	final double y1 = calc_y1(x1, x2);
	final double y2 = calc_y2(x1, x2);
	final double[] point = { y1 + 2048, y2 + 2048 };
	return point;
    }

    private double calc_y1(final double x1, final double x2) {
	final double yn = calc_yn(x2);
	return arcsinh(a[1][0] + 2 * a[2][0] * x1 + yn * (a[1][1] + a[1][2] * yn + 2 * x1 * (a[2][1] + a[2][2] * yn)))
		/ (2 * (a[2][0] + yn * (a[2][1] + a[2][2] * yn))) - arcsinh(a[1][0] + yn * (a[1][1] + a[1][2] * yn))
		/ (2 * (a[2][0] + yn * (a[2][1] + a[2][2] * yn)));
    }

    private double calc_y2(final double x1, final double x2) {
	final double yn = calc_yn(x2);
	return a[0][0] + a[1][0] * x1 + a[2][0] * x1 * x1 + yn
		* (a[0][1] + a[0][2] * yn + x1 * (a[1][1] + a[2][1] * x1 + a[1][2] * yn + a[2][2] * x1 * yn));
    }

    private double calc_yn(final double x2) {
	// TODO optimise this method by using the full 3d polynomial.
	return (x2 * x2 * x2 * b[0][2] / 3 + x2 * x2 * b[0][1] / 2 + x2 * b[0][0]) - offset - 2048;
    }

    private double arcsinh(final double x) {
	return Math.log(x + Math.sqrt(x * x + 1));
    }

    /**
     * The parameters of two 3D polynomials are parsed from a file and stored at individual arrays.
     *
     * @param path_borders
     */
    private void getParametersA(final String path_borders) {
	final DataImporter importer = new DataImporter(path_borders, true);
	final double[][] vals = importer.vals;
	final int m = 3;
	final int n = 2;
	final Polynomial_2D func = new Polynomial_2D(m, n);
	final double[] a_fit = new double[(m + 1) * (n + 1)];
	Arrays.fill(a_fit, 1.);
	final LMA lma = new LMA(func, a_fit, vals);
	lma.fit();
	a = convertParameterArray(a_fit, m, n);
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		System.out.printf("a%d%d = %g\n", i, j, a[i][j]);
	    }
	}
    }

    /**
     * The parameters of two 3D polynomials are parsed from a file and stored at individual arrays.
     *
     * @param path_poly2D
     */
    private void getParametersB(final String path_poly2D) {
	final DataImporter importer = new DataImporter(path_poly2D, false);
	final double[][] vals = importer.vals;
	final int m = 3;
	final int n = 2;
	final Polynomial_2D func = new Polynomial_2D(m, n);
	final double[] b_fit = new double[(m + 1) * (n + 1)];
	Arrays.fill(b_fit, 1.);
	final LMA lma = new LMA(func, b_fit, vals);
	lma.fit();
	b = convertParameterArray(b_fit, m, n);
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		System.out.printf("b%d%d = %g\n", i, j, b[i][j]);
	    }
	}
	setOffset();
    }

    private double[][] convertParameterArray(final double[] a_fit, final int m, final int n) {
	final double[][] b_converted = new double[m + 1][n + 1];
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		b_converted[i][j] = a_fit[(n + 1) * i + j];
	    }
	}
	return b_converted;
    }

    private void setOffset() {
	final double maximum = b[0][2] * Math.pow((-b[0][1] / (2 * b[0][2])), 2) + b[0][1] * (-b[0][1] / (2 * b[0][2]))
		+ b[0][0];
	b[0][2] = b[0][2] / maximum;
	b[0][1] = b[0][1] / maximum;
	b[0][0] = b[0][0] / maximum;
	final double x2c = -b[0][1] / (2 * b[0][2]);
	offset = x2c * x2c * x2c * b[0][2] / 3 + x2c * x2c * b[0][1] / 2 + x2c * b[0][0] - x2c;
    }

    /**
     * <p>
     * This class is used to load a data file that contains a data set for the fit of a 2D polynomial. For each y-value
     * there is are pairs of x-values that are stored at a 2D array.
     * </p>
     *
     * <p>
     * The data file must contain one data point at each line. Each data point contains of x1, x2 and y separated by
     * whitespace. Lines that contain a '#' are regarded as comments.
     * </p>
     *
     * <p>
     * The Plugin {@link SR_EELS_CharacterisationPlugin} creates files that can be processed by this class.
     * </p>
     *
     * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
     *
     */
    private static class DataImporter {

	protected double[][] vals;
	protected double[] weights;

	/**
	 * Create a new data set by loading it from a file.
	 *
	 * @param dataFilePath
	 *            is the path to the file that contains the data set.
	 */
	public DataImporter(final String dataFilePath, final boolean readWeights) {
	    boolean isBordersTxt = false;
	    final File file = new File(dataFilePath);
	    final Vector<Double[]> values = new Vector<Double[]>();
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
			    final String[] splitLine = line.split("\\s+");
			    if (readWeights == true) {
				if (splitLine.length >= 4) {
				    isBordersTxt = true;
				    final Double[] point = { Double.valueOf(splitLine[0]),
					    Double.valueOf(splitLine[1]), Double.valueOf(splitLine[2]),
					    Double.valueOf(splitLine[3]) };
				    values.add(point);
				}
			    } else {
				if (splitLine.length >= 3) {
				    final Double[] point = { Double.valueOf(splitLine[0]),
					    Double.valueOf(splitLine[1]), Double.valueOf(splitLine[2]) };
				    values.add(point);
				}
			    }
			}
		    }
		} while (containsData);
		reader.close();
	    } catch (final FileNotFoundException exc) {
		exc.printStackTrace();
	    } catch (final IOException exc) {
		exc.printStackTrace();
	    }
	    vals = new double[values.size()][3];
	    weights = new double[values.size()];
	    for (int i = 0; i < values.size(); i++) {
		if (isBordersTxt) {
		    vals[i][0] = values.get(i)[2] - 2048;
		} else {
		    vals[i][0] = values.get(i)[2];
		}
		vals[i][1] = values.get(i)[0] - 2048;
		vals[i][2] = values.get(i)[1] - 2048;
		if (readWeights == true) {
		    weights[i] = values.get(i)[3];
		}
	    }
	}
    }
}
