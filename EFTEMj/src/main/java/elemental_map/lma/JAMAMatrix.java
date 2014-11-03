package elemental_map.lma;

import Jama.Matrix;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;

public class JAMAMatrix extends Matrix implements LMAMatrix {
    private static final long serialVersionUID = -8925816623803983503L;

    public JAMAMatrix(final double[][] elements) {
	super(elements);
    }

    public JAMAMatrix(final int rows, final int cols) {
	super(rows, cols);
    }

    @Override
    public void invert() throws LMAMatrix.InvertException {
	try {
	    final Matrix m = inverse();
	    setMatrix(0, this.getRowDimension() - 1, 0, getColumnDimension() - 1, m);
	} catch (final RuntimeException e) {
	    final StringWriter s = new StringWriter();
	    final PrintWriter p = new PrintWriter(s);
	    p.println(e.getMessage());
	    p.println("Inversion failed for matrix:");
	    this.print(p, NumberFormat.getInstance(), 5);
	    throw new LMAMatrix.InvertException(s.toString());
	}
    }

    @Override
    public void setElement(final int row, final int col, final double value) {
	set(row, col, value);
    }

    @Override
    public double getElement(final int row, final int col) {
	return get(row, col);
    }

    @Override
    public void multiply(final double[] vector, final double[] result) {
	for (int i = 0; i < this.getRowDimension(); i++) {
	    result[i] = 0;
	    for (int j = 0; j < this.getColumnDimension(); j++) {
		result[i] += this.getElement(i, j) * vector[j];
	    }
	}
    }

    public static void main(final String[] args) {
	final StringWriter s = new StringWriter();
	final PrintWriter out = new PrintWriter(s);
	out.println("jakkajaaa");
	System.out.println(s);
    }
}
