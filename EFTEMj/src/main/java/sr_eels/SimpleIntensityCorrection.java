
package sr_eels;

import ij.process.FloatProcessor;

public class SimpleIntensityCorrection extends IntensityCorrector {

	public SimpleIntensityCorrection(final FloatProcessor inputImage,
		final CoordinateCorrector coordinateCorrector)
	{
		super(inputImage, coordinateCorrector);
	}

	@Override
	public float getIntensity(final int x1, final int x2) {
		final float[] point_0;
		final float[] point_1;
		try {
			point_0 = coordinateCorrector.transformCoordinate(x1, x2);
			point_1 = coordinateCorrector.transformCoordinate(x1 + 1, x2 + 1);
		}
		catch (final SR_EELS_Exception exc1) {
			return 0f;
		}
		final float y1_0 = point_0[0];
		final int start1 = (int) Math.floor(y1_0);
		final float y2_0 = point_0[1];
		final int start2 = (int) Math.floor(y2_0);
		final float y1_1 = point_1[0];
		final int stop1 = (int) Math.floor(y1_1);
		final float y2_1 = point_1[1];
		final int stop2 = (int) Math.floor(y2_1);
		try {
			float sum = 0;
			for (int j = start2; j <= stop2; j++) {
				for (int i = start1; i <= stop1; i++) {
					float dx = 1;
					float dy = 1;
					if (i == start1) dx -= y1_0 - start1;
					if (i == stop1) dx -= (stop1 + 1) - y1_1;
					if (j == start2) dy -= y2_0 - start2;
					if (j == stop2) dy -= (stop2 + 1) - y2_1;
					sum += dx * dy * input.getf(i, j);
				}
			}
			return sum;
		}
		catch (final ArrayIndexOutOfBoundsException exc) {
			return 0f;
		}
	}

}
