package sr_eels;

import ij.process.FloatProcessor;

/**
 * {@link SR_EELS_FloatProcessor} extends the {@link FloatProcessor} by regarding the binning and an origin that not
 * equals 0,0.
 *
 * @author Michael Entrup b. Epping <michael.entrup@wwu.de>
 *
 */
public class SR_EELS_FloatProcessor extends FloatProcessor {

    private int binningX;
    private int binningY;
    private int originX;
    private int originY;
    private SR_EELS_Polynomial_2D widthFunction;
    private SR_EELS_Polynomial_2D borderFunction;

    public SR_EELS_FloatProcessor(FloatProcessor fp, int binning, int originX, int originY) {
	super(fp.getWidth(), fp.getHeight(), (float[]) fp.getPixels());
	this.binningX = binning;
	this.binningY = binning;
	this.originX = originX;
	this.originY = originY;
    }

    public SR_EELS_FloatProcessor(FloatProcessor fp, int binningX, int binningY, int originX, int originY) {
	super(fp.getWidth(), fp.getHeight(), (float[]) fp.getPixels());
	this.binningX = binningX;
	this.binningY = binningY;
	this.originX = originX;
	this.originY = originY;
    }

    public SR_EELS_FloatProcessor(int width, int height, int binning, int originX, int originY) {
	super(width, height, new float[width * height]);
	this.binningX = binning;
	this.binningY = binning;
	this.originX = originX;
	this.originY = originY;
    }

    public SR_EELS_FloatProcessor(int width, int height, int binningX, int binningY, int originX, int originY) {
	super(width, height, new float[width * height]);
	this.binningX = binningX;
	this.binningY = binningY;
	this.originX = originX;
	this.originY = originY;
    }

    public float getf(int x, int y, boolean useTransform) {
	if (useTransform) {
	    return getf(x / binningX + originX, y / binningY + originY);
	}
	return getf(x, y);
    }

    public int getBinningX() {
	return binningX;
    }

    public int getBinningY() {
	return binningY;
    }

    public int getOriginX() {
	return originX;
    }

    public int getOriginY() {
	return originY;
    }

    public void setWidthFunction(SR_EELS_Polynomial_2D widthFunction) {
	this.widthFunction = widthFunction;
	this.widthFunction.setInputProcessor(this);
	this.widthFunction.setupWidthCorrection();
    }

    public void setBorderFunction(SR_EELS_Polynomial_2D borderFunction) {
	this.borderFunction = borderFunction;
	this.borderFunction.setInputProcessor(this);
    }

    public SR_EELS_Polynomial_2D getWidthFunction() {
	return widthFunction;
    }

    public SR_EELS_Polynomial_2D getBorderFunction() {
	return borderFunction;
    }

    public float[] convertToFunctionCoordinates(float[] x2) {
	float[] point = new float[2];
	point[0] = (x2[0] - originX) * binningX;
	point[1] = (x2[1] - originY) * binningY;
	return point;
    }

    public float[] convertToFunctionCoordinates(float x1, float x2) {
	float[] x = new float[] { x1, x2 };
	return convertToFunctionCoordinates(x);
    }

    public float[] convertToImageCoordinates(float[] x2) {
	float[] point = new float[2];
	point[0] = x2[0] / binningX + originX;
	point[1] = x2[1] / binningY + originY;
	return point;
    }

    public float[] convertToImageCoordinates(float x1, float x2) {
	float[] x = new float[] { x1, x2 };
	return convertToImageCoordinates(x);
    }
}
