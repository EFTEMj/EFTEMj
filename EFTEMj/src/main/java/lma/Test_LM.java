package lma;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JFileChooser;

import Jama.Matrix;

public class Test_LM {

    static class TestFunc implements LMfunc {

	@Override
	public double val(double[] x, double[] a) {
	    assert a.length == 9;
	    assert x.length == 2;

	    double a00 = a[0];
	    double a10 = a[1];
	    double a20 = a[2];
	    double a01 = a[3];
	    double a11 = a[4];
	    double a21 = a[5];
	    double a02 = a[6];
	    double a12 = a[7];
	    double a22 = a[8];

	    double x1 = x[0];
	    double x2 = x[1];

	    return a00 + a10 * x1 + a20 * x1 * x1 + a01 * x2 + a11 * x1 * x2 + a21 * x1 * x1 * x2 + a02 * x2 * x2 + a12
		    * x1 * x2 * x2 + a22 * x1 * x1 * x2 * x2;
	}

	@Override
	public double grad(double[] x, double[] a, int ak) {
	    assert a.length == 9;
	    assert x.length == 2;
	    assert ak < 10 : "a_k=" + ak;

	    double x1 = x[0];
	    double x2 = x[1];

	    switch (ak) {
	    case 0:
		return 0;
	    case 1:
		return x1;
	    case 2:
		return x1 * x1;
	    case 3:
		return x2;
	    case 4:
		return x1 * x2;
	    case 5:
		return x1 * x1 * x2;
	    case 6:
		return x2 * x2;
	    case 7:
		return x1 * x2 * x2;
	    case 8:
		return x1 * x1 * x2 * x2;
	    default:
		return 0;
	    }
	}

	@Override
	public double[] initial() {
	    double[] a = new double[9];
	    a[0] = 549.883120731574;
	    a[1] = -0.0232999572004277;
	    a[2] = 6.78661879843638e-007;
	    a[3] = 0.0854331764757123;
	    a[4] = 2.20440307736334e-006;
	    a[5] = 6.8817427162422e-010;
	    a[6] = -2.06003501147918e-005;
	    a[7] = -3.52438910848436e-010;
	    a[8] = -1.46998807795553e-013;
	    return a;
	}

	@Override
	public Object[] testdata() {
	    Object[] o = new Object[4];

	    int colX = 1;
	    int colY = 2;
	    int colZ = 5;

	    double[][] x = null;
	    double[] y = null;
	    double[] s = null;
	    ArrayList<double[]> list = new ArrayList<double[]>();
	    String zeile;
	    String[] split = null;
	    String path = "C:\\Temp";
	    JFileChooser chooser = new JFileChooser(path);
	    chooser.showOpenDialog(null);
	    File file = chooser.getSelectedFile();
	    while (file != null) {
		FileReader reader;
		try {
		    reader = new FileReader(file);
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
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		    System.exit(0);
		} catch (NumberFormatException e) {
		    e.printStackTrace();
		    System.exit(0);
		} catch (IOException e) {
		    e.printStackTrace();
		    System.exit(0);
		}
	    }
	    x = new double[list.size()][2];
	    y = new double[list.size()];
	    s = new double[list.size()];
	    for (int i = 0; i < list.size(); i++) {
		x[i][0] = 2 * list.get(i)[0];
		x[i][1] = 2 * list.get(i)[1];
		y[i] = 2 * list.get(i)[2];
		s[i] = Math.sqrt(list.get(i)[2]);
	    }

	    double[] a = new double[9];

	    a[0] = 549.883120731574;
	    a[1] = -0.0232999572004277;
	    a[2] = 6.78661879843638e-007;
	    a[3] = 0.0854331764757123;
	    a[4] = 2.20440307736334e-006;
	    a[5] = 6.8817427162422e-010;
	    a[6] = -2.06003501147918e-005;
	    a[7] = -3.52438910848436e-010;
	    a[8] = -1.46998807795553e-013;

	    o[0] = x;
	    o[1] = a;
	    o[2] = y;
	    o[3] = s;
	    return o;
	}

    }

    public static void main(String[] cmdline) {
	LMfunc f = new TestFunc();

	double[] aguess = f.initial();
	Object[] test = f.testdata();
	double[][] x = (double[][]) test[0];
	double[] areal = (double[]) test[1];
	double[] y = (double[]) test[2];
	double[] s = (double[]) test[3];
	boolean[] vary = new boolean[aguess.length];
	for (int i = 0; i < aguess.length; i++)
	    vary[i] = false;
	assert aguess.length == areal.length;
	vary[0] = true;

	try {
	    LM.solve(x, aguess, y, s, vary, f, 0.001, 0.01, 100, 2);
	} catch (Exception ex) {
	    System.err.println("Exception caught: " + ex.getMessage());
	    System.exit(1);
	}

	System.out.print("desired solution ");
	(new Matrix(areal, areal.length)).print(10, 2);

	System.exit(0);
    }
}
