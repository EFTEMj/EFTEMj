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
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

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
     * @param path
     *            The path to the parameter file that has been created by Gnuplot.
     * @throws IOException
     * @throws ArrayIndexOutOfBoundsException
     */
    public SR_EELS_CorrectionFunction(String path) throws IOException, ArrayIndexOutOfBoundsException {
	readParameters(path);
    }

    public double[] transform(double x1, double x2) {
	double y1 = calc_y1(x1, x2);
	double y2 = calc_y2(x1, x2);
	double[] point = { y1, y2 };
	return point;
    }

    /**
     * @param x1
     * @param x2
     * @return
     */
    private double calc_y1(double x1, double x2) {
	double yn = calc_yn(x2);
	return arcsinh(a[1][0] + 2 * a[2][0] * x1 + yn * (a[1][1] + a[1][2] * yn + 2 * x1 * (a[2][1] + a[2][2] * yn)))
		/ (2 * (a[2][0] + yn * (a[2][1] + a[2][2] * yn))) - arcsinh(a[1][0] + yn * (a[1][1] + a[1][2] * yn))
		/ (2 * (a[2][0] + yn * (a[2][1] + a[2][2] * yn)));
    }

    /**
     * @param x1
     * @param x2
     * @return
     */
    private double calc_y2(double x1, double x2) {
	double yn = calc_yn(x2);
	return a[0][0] + a[1][0] * x1 + a[2][0] * x1 * x1 + yn
		* (a[0][1] + a[0][2] * yn + x1 * (a[1][1] + a[2][1] * x1 + a[1][2] * yn + a[2][2] * x1 * yn));
    }

    /**
     * @param x2
     * @return
     */
    private double calc_yn(double x2) {
	// TODO optimise this method by using the full 3d polynomial.
	return (x2 * x2 * x2 * b[0][2] / 3 + x2 * x2 * b[0][1] / 2 + x2 * b[0][0]) - offset;
    }

    private double arcsinh(double x) {
	return Math.log(x + Math.sqrt(x * x + 1));
    }

    /**
     * The parameters of two 3D polynomials are parsed from a file and stored at individual arrays.
     * 
     * @param path
     *            The path to the parameter file that has been created by Gnuplot.
     * @throws IOException
     * @throws ArrayIndexOutOfBoundsException
     */
    private void readParameters(String path) throws IOException, ArrayIndexOutOfBoundsException {
	// We assume, that only polynomials up to an order of 10 are used.
	a = new double[10][10];
	b = new double[10][10];
	File file = new File(path);
	FileReader reader = new FileReader(file);
	BufferedReader data = new BufferedReader(reader);
	String line;
	String[] splitItems;
	while ((line = data.readLine()) != null) {
	    line = line.replace(" ", "");
	    splitItems = line.split("=");
	    int i = Integer.parseInt(splitItems[0].substring(1, 2));
	    int j = Integer.parseInt(splitItems[0].substring(2, 3));
	    if (splitItems[0].startsWith("a")) {
		a[i][j] = Double.parseDouble(splitItems[1]);
	    }
	    if (splitItems[0].startsWith("b")) {
		b[i][j] = Double.parseDouble(splitItems[1]);
	    }
	}
	data.close();
	a = trimArray(a);
	b = trimArray(b);
	setOffset();
    }

    private void setOffset() {
	double maximum = b[0][2] * Math.pow((-b[0][1] / (2 * b[0][2])), 2) + b[0][1] * (-b[0][1] / (2 * b[0][2]))
		+ b[0][0];
	b[0][2] = b[0][2] / maximum;
	b[0][1] = b[0][1] / maximum;
	b[0][0] = b[0][0] / maximum;
	double x2c = -b[0][1] / (2 * b[0][2]);
	offset = x2c * x2c * x2c * b[0][2] / 3 + x2c * x2c * b[0][1] / 2 + x2c * b[0][0] - x2c;
    }

    /**
     * We need this method to remove all unnecessary columns and rows from the parameter arrays.
     * 
     * @param array
     *            An MxN array that can contain empty columns and rows (empty means only zeros).
     * @return An that contains no empty columns at the right and no empty row at the bottom.
     */
    private double[][] trimArray(double[][] array) {
	int i_max = array.length;
	do {
	    i_max--;
	} while (Arrays.equals(array[i_max], new double[array[i_max].length]));
	int j_max = 0;
	for (int i = 0; i < i_max; i++) {
	    int j = array[i].length;
	    do {
		j--;
	    } while (array[i][j] == 0.);
	    j_max = Math.max(j_max, j);
	}
	double[][] trimmed = new double[i_max + 1][j_max + 1];
	for (int m = 0; m <= i_max; m++) {
	    for (int n = 0; n <= j_max; n++) {
		trimmed[m][n] = array[m][n];
	    }
	}
	return trimmed;
    }
}
