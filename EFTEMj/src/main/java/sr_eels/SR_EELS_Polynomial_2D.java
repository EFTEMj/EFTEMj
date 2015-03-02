package sr_eels;

import eftemj.EFTEMj;
import eftemj.EFTEMj_Debug;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.CurveFitter;
import ij.plugin.frame.Fitter;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;

import libs.lma.implementations.Polynomial_2D;

public class SR_EELS_Polynomial_2D extends Polynomial_2D {

    public static final int BORDERS = 1;
    public static final int WIDTH_VS_POS = 2;

    private SR_EELS_FloatProcessor inputProcessor;

    private static SR_EELS_FloatProcessor transformWidth;
    private static SR_EELS_FloatProcessor transformY1;

    public SR_EELS_Polynomial_2D(final int m, final int n) {
	super(m, n);
    }

    public SR_EELS_Polynomial_2D(final int m, final int n, final double[] params) {
	super(m, n, params);
    }

    public void setInputProcessor(final SR_EELS_FloatProcessor inputProcessor) {
	this.inputProcessor = inputProcessor;
    }

    public void setupWidthCorrection() {
	/*
	 * The correction of the width needs some steps of preparation. The result of this preparation is an image, that
	 * contains the uncorrected coordinate for each corrected coordinate.
	 * 
	 * First we have to calculate the roots of the polynomial along the x2-direction. If the roots are outside the
	 * image boundaries, they are replaced by the image boundaries.
	 * 
	 * Read on at the next comment.
	 */
	double rootL = -Math.sqrt(Math.pow(getParam(0, 1), 2) / (4 * Math.pow(getParam(0, 2), 2)) - getParam(0, 0)
		/ getParam(0, 2))
		- getParam(0, 1) / (2 * getParam(0, 2));
	double rootH = Math.sqrt(Math.pow(getParam(0, 1), 2) / (4 * Math.pow(getParam(0, 2), 2)) - getParam(0, 0)
		/ getParam(0, 2))
		- getParam(0, 1) / (2 * getParam(0, 2));
	final double maxPos = -getParam(0, 1) / (2 * getParam(0, 2));
	final double[] maxPoint = { 0, maxPos };
	final double maxValue = val(maxPoint);
	if (Double.isNaN(rootL) || rootL < -CameraSetup.getFullHeight() / 2)
	    rootL = -CameraSetup.getFullHeight() / 2;
	if (Double.isNaN(rootH) || rootH > CameraSetup.getFullHeight() / 2 - 1)
	    rootH = CameraSetup.getFullHeight() / 2 - 1;
	EFTEMj_Debug.log(rootL + ", " + maxPos + ", " + rootH, false);
	/*
	 * The second step is to map uncorrected and corrected coordinates. For each uncorrected coordinate the
	 * corrected coordinate is calculated. The inverse function is hard to determine Instead we switch the axes and
	 * fit a polynomial of 7rd order that fits very well. For most images a 3rd order polynomial is sufficient.
	 */
	final LinkedHashMap<Integer, Double> map = new LinkedHashMap<Integer, Double>();
	final double a00 = getParam(0, 0) / maxValue;
	final double a01 = getParam(0, 1) / maxValue;
	final double a02 = getParam(0, 2) / maxValue;
	int index = 0;
	for (int x = (int) Math.ceil(rootL); x <= rootH; x++) {
	    final double num = 3 * Math.pow(2 * x + a01 / a02, 2);
	    final double denum = (4 * a02 * Math.pow(x, 3) + 6 * a01 * Math.pow(x, 2) + 12 * a00 * x - Math.pow(a01, 3)
		    / Math.pow(a02, 2) + 6 * a00 * a01 / a02);
	    final double sum1 = num / denum;
	    final double sum2 = -a01 / (2 * a02);
	    map.put(x, sum1 + sum2);
	}
	final double[] x = new double[map.size()];
	final double[] xc = new double[map.size()];
	final Collection<Integer> set = map.keySet();
	final Iterator<Integer> iterator = set.iterator();
	index = 0;
	while (iterator.hasNext()) {
	    final int key = iterator.next();
	    x[index] = key;
	    xc[index] = map.get(key);
	    index++;
	}
	/*
	 * The minimum and maximum value of xc determine the height of the corrected image.
	 */
	rootL = xc[0];
	rootH = xc[xc.length - 1];
	final CurveFitter fit = new CurveFitter(xc, x);
	try {
	    fit.doFit(CurveFitter.POLY7);
	} catch (final ArrayIndexOutOfBoundsException exc) {
	    fit.doFit(CurveFitter.STRAIGHT_LINE);
	}
	final double[] fitParams = fit.getParams();
	transformWidth = new SR_EELS_FloatProcessor(inputProcessor.getWidth(),
		(int) (2 * Math.max(-rootL, rootH) / inputProcessor.getBinningY()), inputProcessor.getBinningX(),
		inputProcessor.getBinningY(), inputProcessor.getOriginX(), (int) Math.max(-rootL, rootH)
			/ inputProcessor.getBinningY());
	/*
	 * transformY1 is only created but not filled with values. The calculation is done at the method getY1().
	 */
	transformY1 = new SR_EELS_FloatProcessor(transformWidth.getWidth(), transformWidth.getHeight(),
		transformWidth.getBinningX(), transformWidth.getBinningY(), transformWidth.getOriginX(),
		transformWidth.getOriginY());

	transformY1.set(Float.NaN);
	for (int x2 = 0; x2 < transformWidth.getHeight(); x2++) {
	    final double x2_func = transformWidth.convertToFunctionCoordinates(0, x2)[1];
	    final float value = (float) fit.f(fitParams, x2_func);
	    for (int x1 = 0; x1 < transformWidth.getWidth(); x1++) {
		transformWidth.setf(x1, x2, value);
	    }
	}
	if (EFTEMj.debugLevel >= EFTEMj.DEBUG_SHOW_IMAGES) {
	    final ImagePlus imp = new ImagePlus("transform width", transformWidth);
	    imp.show();
	}
	Fitter.plot(fit);
    }

    public String compareWithGnuplot(final int functionType) {
	String filename = "";
	String using = "";
	final String offsetX1 = "offsetX1 = " + CameraSetup.getFullWidth() / 2;
	final String offsetX2 = "offsetX2 = " + CameraSetup.getFullHeight() / 2;
	switch (functionType) {
	case BORDERS:
	    filename = "Borders.txt";
	    using = "using ($1-offsetX1):($2-offsetX2):($3-offsetX2):4";
	    break;
	case WIDTH_VS_POS:
	    filename = "Width.txt";
	    using = "using ($1-offsetX1):($2-offsetX2):3:(1)";
	    break;
	default:
	    break;
	}
	String functionGnu = "f(x,y) = ";
	String fit = "fit f(x,y) '" + filename + "' " + using + " zerror via ";
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		functionGnu += String.format(Locale.UK, "a%d%d*x**%d*y**%d", i, j, i, j);
		fit += String.format(Locale.UK, "a%d%d", i, j);
		if (i != m | j != n) {
		    functionGnu += " + ";
		    fit += ",";
		}
	    }
	}
	final String title = String.format(Locale.UK, "%n#####%n# Fit of '%s':%n#####%n", filename);
	fit = title + "\n" + offsetX1 + "\n" + offsetX2 + "\n" + functionGnu + "\n" + fit;
	String residuals = "";
	switch (functionType) {
	case BORDERS:
	    filename = "Borders.txt";
	    using = "using ($1-offsetX1):($2-offsetX2):($3-offsetX2)";
	    residuals = "using ($1-offsetX1):($2-offsetX2):(abs( $3 - offsetX2 - fJ($1-offsetX1,$2-offsetX2))**2)";
	    break;
	case WIDTH_VS_POS:
	    filename = "Width.txt";
	    using = "using ($1-offsetX1):($2-offsetX2):3";
	    residuals = "using ($1-offsetX1):($2-offsetX2):(abs( $3 - fJ($1-offsetX1,$2-offsetX2))**2)";
	    break;
	default:
	    break;
	}
	final String splot = String
		.format("splot '%s' %s notitle,\\%nf(x,y) title 'Gnuplot', fJ(x,y) title 'Java LMA',\\%n'%1$s' %s title 'residuals'",
			filename, using, residuals);
	String functionJava = "fJ(x,y) = ";
	String compare = "#Java LMA";
	for (int i = 0; i <= m; i++) {
	    for (int j = 0; j <= n; j++) {
		compare += String.format(Locale.UK, "\naJ%d%d = %+6e", i, j, params[(n + 1) * i + j]);
		functionJava += String.format(Locale.UK, "aJ%d%d*x**%d*y**%d", i, j, i, j);
		if (i != m | j != n) {
		    functionJava += " + ";
		}
	    }
	}
	return fit + "\n\n" + functionJava + "\n\n" + compare + "\n\n" + splot;
    }

    public float getY1(final float[] x2) {
	final float[] x2_img = transformY1.convertToImageCoordinates(x2);
	final float pixel_value = transformY1.getf((int) Math.floor(x2_img[0]), (int) Math.floor(x2_img[1]));
	if (Float.isNaN(pixel_value)) {
	    final int low = -CameraSetup.getFullWidth() / 2;
	    final int high = CameraSetup.getFullWidth() / 2;
	    final LinkedHashMap<Integer, Double> map = new LinkedHashMap<Integer, Double>();
	    map.put(0, 0.);
	    for (int i = -1; i >= low; i--) {
		final double path = map.get(i + 1)
			- Math.sqrt(1 + Math
				.pow(val(new double[] { i, x2[1] }) - val(new double[] { i + 1, x2[1] }), 2));
		map.put(i, path);
	    }
	    for (int i = 1; i < high; i++) {
		final double path = map.get(i - 1)
			+ Math.sqrt(1 + Math
				.pow(val(new double[] { i, x2[1] }) - val(new double[] { i - 1, x2[1] }), 2));
		map.put(i, path);
	    }
	    final double[] x = new double[map.size()];
	    final double[] xc = new double[map.size()];
	    final Collection<Integer> set = map.keySet();
	    final Iterator<Integer> iterator = set.iterator();
	    int index = 0;
	    while (iterator.hasNext()) {
		final int key = iterator.next();
		x[index] = key;
		xc[index] = map.get(key);
		index++;
	    }
	    final CurveFitter fit = new CurveFitter(xc, x);
	    try {
		fit.doFit(CurveFitter.POLY7);
	    } catch (final ArrayIndexOutOfBoundsException exc) {
		fit.doFit(CurveFitter.STRAIGHT_LINE);
	    }
	    final double[] fitParams = fit.getParams();
	    for (int i = 0; i < transformY1.getWidth(); i++) {
		final float[] x2_func = transformY1.convertToFunctionCoordinates(i, x2[1]);
		final float value = (float) fit.f(fitParams, x2_func[0]);
		transformY1.setf(i, (int) x2_img[1], value);
	    }
	    if (EFTEMj.debugLevel >= EFTEMj.DEBUG_SHOW_IMAGES) {
		final int[] ids = WindowManager.getIDList();
		boolean found = false;
		synchronized (transformY1) {
		    for (final int id : ids) {
			if (WindowManager.getImage(id).getTitle() == "transform y1") {
			    WindowManager.getImage(id).updateAndDraw();
			    found = true;
			}
		    }
		    if (!found) {
			final ImagePlus imp = new ImagePlus("transform y1", transformY1);
			imp.show();
		    }
		}
	    }
	}
	final float y1 = transformY1.getf((int) x2_img[0], (int) x2_img[1]);
	return y1;
    }

    public float getY2n(final float[] x2) {
	final float[] x2_img = transformWidth.convertToImageCoordinates(x2);
	try {
	    return transformWidth.getf((int) x2_img[0], (int) x2_img[1]);
	} catch (final ArrayIndexOutOfBoundsException exc) {
	    return -1;
	}
    }

    public SR_EELS_FloatProcessor createOutputImage() {
	final SR_EELS_FloatProcessor fp = new SR_EELS_FloatProcessor(transformWidth.getWidth(),
		transformWidth.getHeight(), transformWidth.getBinningX(), transformWidth.getBinningY(),
		transformWidth.getOriginX(), transformWidth.getOriginY());
	return fp;
    }

    public float getY2(final float[] x2) {
	final float value = (float) val(new double[] { x2[0], x2[1] });
	return value;
    }
}
