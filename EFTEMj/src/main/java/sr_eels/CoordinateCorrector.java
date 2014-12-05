package sr_eels;

import libs.lma.implementations.Polynomial_2D;

public abstract class CoordinateCorrector {

    Polynomial_2D functionWidth;
    Polynomial_2D functionBorder;

    public float[] transformCoordinate(float x1, float x2) {
	float[] point = new float[2];
	float y2n = calcY2n(x1, x2);
	point[0] = calcY1(x1, y2n);
	point[1] = calcY2(point[0], y2n);
	return point;
    }

    abstract float calcY2n(float x1, float x2);

    abstract float calcY1(float x1, float y2n);

    abstract float calcY2(float y1, float y2n);

}
