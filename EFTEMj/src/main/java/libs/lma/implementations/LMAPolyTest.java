package libs.lma.implementations;

import libs.lma.LMA;
import libs.lma.LMAMultiDimFunction;


public class LMAPolyTest {

    public LMAPolyTest() {
    }

    static class LMPolyTest extends LMAMultiDimFunction {

	public double[] initial() {
	    // final double[] a = { 1., 0.01, 0.0001, 0.01, 0.0001, 1e-6, 0.0001, 1e-6, 1e-8 };
	    final double[] a = { 1., 1., 1., 1., 1., 1., 1., 1., 1. };
	    return a;
	} // initial

	public double[][] yValues() {
	    final double[] y_vals = { 630, 630, 628, 628, 626, 624, 622, 624, 622, 622, 620, 618, 618, 616, 614, 614,
		    616, 610, 612, 608, 608, 608, 606, 604, 606, 604, 604, 602, 602, 602, 602, 600, 600, 594, 596, 594,
		    594, 594, 594, 592, 590, 592, 592, 590, 588, 590, 588, 586, 584, 584, 586, 584, 582, 584, 578, 582,
		    580, 580, 580, 576, 576, 576, 574, 574, 572, 570, 570, 568, 568, 564, 566, 562, 564, 562, 560, 558,
		    560, 558, 556, 556, 554, 556, 552, 552, 550, 550, 550, 546, 548, 546, 544, 546, 542, 544, 542, 540,
		    540, 540, 538, 536, 538, 536, 536, 534, 534, 532, 532, 532, 530, 528, 528, 526, 614, 616, 614, 612,
		    612, 608, 610, 608, 606, 604, 602, 604, 602, 600, 600, 598, 598, 598, 594, 594, 594, 592, 588, 590,
		    588, 588, 588, 588, 584, 584, 586, 584, 582, 580, 580, 580, 578, 576, 576, 576, 576, 576, 574, 576,
		    574, 572, 572, 572, 572, 570, 568, 568, 566, 566, 568, 564, 580, 580, 576, 576, 576, 574, 574, 570,
		    572, 570, 570, 568, 568, 566, 566, 564, 562, 564, 564, 560, 560, 558, 560, 556, 556, 554, 554, 552,
		    552, 550, 550, 550, 548, 550, 546, 546, 546, 548, 546, 542, 544, 544, 542, 542, 540, 538, 538, 538,
		    536, 536, 536, 536, 536, 534, 532, 532, 620, 618, 616, 616, 614, 616, 612, 612, 610, 608, 608, 608,
		    604, 608, 606, 604, 600, 602, 600, 596, 594, 594, 596, 594, 594, 594, 592, 592, 590, 588, 588, 586,
		    588, 586, 584, 584, 584, 582, 582, 582, 580, 582, 578, 578, 578, 576, 576, 574, 576, 574, 574, 572,
		    572, 572, 574, 572 };
	    final double[][] vals = new double[2][y_vals.length];
	    for (int i = 0; i < y_vals.length; i++) {
		vals[0][i] = y_vals[i];
		//vals[1][i] = 1 / y_vals[i];
		vals[1][i] = 1;
	    }
	    return vals;
	}

	public double[][] xValues() {
	    final double[] x1_vals = { 287.859, 351.834, 415.842, 479.841, 543.846, 607.831, 671.85, 735.848, 799.859,
		    863.868, 927.84, 991.848, 1055.85, 1119.85, 1183.86, 1247.87, 1311.84, 1375.85, 1439.89, 1503.86,
		    1567.86, 1631.85, 1695.87, 1759.85, 1823.85, 1887.86, 1951.87, 2015.93, 2079.84, 2143.86, 2207.87,
		    2271.86, 2335.87, 2399.85, 2463.89, 2527.86, 2591.86, 2655.84, 2719.89, 2783.85, 2847.89, 2911.85,
		    2975.88, 3039.87, 3103.9, 3167.89, 3231.89, 3295.84, 3359.89, 3423.89, 3487.9, 3551.83, 3615.85,
		    3679.88, 3743.92, 3807.87, 287.847, 351.845, 415.843, 479.873, 543.844, 607.843, 671.847, 735.847,
		    799.858, 863.842, 927.857, 991.848, 1055.86, 1119.84, 1183.87, 1247.84, 1311.84, 1375.87, 1439.86,
		    1503.83, 1567.88, 1631.84, 1695.87, 1759.86, 1823.86, 1887.85, 1951.87, 2015.89, 2079.85, 2143.85,
		    2207.91, 2271.86, 2335.86, 2399.84, 2463.85, 2527.86, 2591.86, 2655.85, 2719.85, 2783.89, 2847.9,
		    2911.87, 2975.85, 3039.86, 3103.86, 3167.86, 3231.88, 3295.91, 3359.88, 3423.88, 3487.89, 3551.89,
		    3615.88, 3679.88, 3743.85, 3807.86, 287.846, 351.854, 415.838, 479.853, 543.85, 607.84, 671.846,
		    735.853, 799.859, 863.829, 927.871, 991.84, 1055.84, 1119.86, 1183.84, 1247.86, 1311.86, 1375.84,
		    1439.84, 1503.86, 1567.87, 1631.84, 1695.85, 1759.89, 1823.87, 1887.84, 1951.86, 2015.87, 2079.86,
		    2143.85, 2207.87, 2271.88, 2335.89, 2399.85, 2463.85, 2527.85, 2591.88, 2655.87, 2719.87, 2783.86,
		    2847.88, 2911.92, 2975.86, 3039.9, 3103.88, 3167.87, 3231.87, 3295.88, 3359.89, 3423.89, 3487.89,
		    3551.9, 3615.86, 3679.86, 3743.88, 3807.87, 287.838, 351.868, 415.82, 479.869, 543.833, 607.864,
		    671.838, 735.873, 799.864, 863.845, 927.864, 991.867, 1055.86, 1119.85, 1183.87, 1247.84, 1311.86,
		    1375.87, 1439.86, 1503.85, 1567.85, 1631.88, 1695.88, 1759.85, 1823.87, 1887.87, 1951.86, 2015.87,
		    2079.88, 2143.86, 2207.88, 2271.86, 2335.88, 2399.87, 2463.89, 2527.87, 2591.86, 2655.86, 2719.91,
		    2783.86, 2847.87, 2911.93, 2975.87, 3039.85, 3103.91, 3167.9, 3231.84, 3295.87, 3359.87, 3423.92,
		    3487.86, 3551.85, 3615.9, 3679.84, 3743.91, 3807.89, 287.848, 351.819, 415.864, 479.84, 543.863,
		    607.861, 671.86, 735.85, 799.851, 863.854, 927.848, 991.839, 1055.86, 1119.83, 1183.85, 1247.85,
		    1311.87, 1375.84, 1439.85, 1503.84, 1567.85, 1631.87, 1695.84, 1759.86, 1823.87, 1887.86, 1951.86,
		    2015.86, 2079.86, 2143.89, 2207.85, 2271.87, 2335.86, 2399.85, 2463.87, 2527.89, 2591.88, 2655.89,
		    2719.86, 2783.86, 2847.86, 2911.9, 2975.88, 3039.86, 3103.84, 3167.87, 3231.87, 3295.87, 3359.86,
		    3423.89, 3487.88, 3551.91, 3615.86, 3679.86, 3743.94, 3807.9 };

	    final double[] x2_vals = { 2045.48, 2046.57, 2047.31, 2048.06, 2048.77, 2049.78, 2050.74, 2051.48, 2052.14,
		    2052.99, 2053.82, 2054.6, 2054.96, 2055.93, 2056.13, 2057.08, 2057.92, 2058.39, 2059.1, 2059.68,
		    2060.07, 2060.7, 2061.34, 2061.82, 2062.68, 2063.49, 2063.98, 2064.47, 2065.15, 2065.7, 2066.11,
		    2067.22, 2067.71, 2068.45, 2069.49, 2070.27, 2071.07, 2071.67, 2072.34, 2073.1, 2073.95, 2074.51,
		    2075.11, 2076.68, 2077.23, 2078.31, 2079.11, 2080.36, 2081.48, 2082.38, 2083.58, 2085.24, 2085.93,
		    2087.37, 2088.22, 2090.05, 485.76, 489.629, 493.954, 497.724, 501.66, 505.274, 509.077, 512.631,
		    516.104, 519.698, 523.16, 526.346, 529.858, 533.442, 536.816, 540.078, 543.215, 546.269, 549.529,
		    552.429, 555.54, 558.583, 561.378, 564.35, 567.306, 570.213, 573.35, 575.839, 578.998, 581.752,
		    584.429, 587.379, 589.852, 593.052, 595.54, 598.387, 601.189, 603.827, 606.323, 609.054, 611.846,
		    614.72, 617.578, 619.609, 622.818, 625.349, 628.016, 630.864, 633.393, 636.743, 638.802, 641.702,
		    644.651, 647.24, 649.763, 652.675, 1182.67, 1185.11, 1187.81, 1190.22, 1193.08, 1195.29, 1197.49,
		    1199.97, 1202.12, 1204.48, 1206.62, 1208.8, 1211.13, 1213.02, 1214.98, 1217.08, 1219.31, 1220.94,
		    1223.03, 1225.21, 1226.89, 1228.62, 1230.37, 1232.79, 1234.57, 1236.42, 1238.19, 1239.92, 1241.71,
		    1243.83, 1245.11, 1247.33, 1249.29, 1250.45, 1252.69, 1254.25, 1255.82, 1257.93, 1259.82, 1261.45,
		    1263.66, 1264.98, 1266.89, 1268.66, 1270.09, 1272.39, 1274.14, 1276.19, 1278.26, 1279.94, 1281.79,
		    1284.04, 1285.76, 1288.05, 1289.91, 1291.58, 3668.15, 3665.57, 3663.64, 3661.67, 3660.04, 3657.98,
		    3655.67, 3654.34, 3651.9, 3650.06, 3648.12, 3646.18, 3644.2, 3642.22, 3640.53, 3638.78, 3636.94,
		    3635.02, 3633.02, 3631.47, 3629.57, 3628.09, 3626.15, 3624.8, 3622.89, 3621.18, 3619.64, 3618.04,
		    3616.42, 3614.72, 3612.9, 3611.9, 3610.15, 3608.81, 3607.64, 3606.09, 3604.89, 3603.69, 3602.69,
		    3601.41, 3600.66, 3600.13, 3599.03, 3597.7, 3597.01, 3596.53, 3595.85, 3595.6, 3594.98, 3594.81,
		    3594.45, 3594.19, 3593.94, 3593.75, 3593.92, 3594.44, 2889.65, 2889.15, 2888.77, 2888.02, 2887.59,
		    2886.88, 2886.13, 2885.48, 2884.88, 2884.44, 2883.74, 2883.05, 2882.42, 2881.59, 2881.21, 2880.56,
		    2879.77, 2879.27, 2878.41, 2878.17, 2877.38, 2876.7, 2876.16, 2876.09, 2875.13, 2874.56, 2874.14,
		    2873.57, 2872.73, 2872.53, 2872.09, 2871.53, 2871.32, 2871.13, 2870.26, 2870.01, 2869.84, 2869.65,
		    2869.53, 2869.03, 2869.18, 2869.09, 2869.21, 2869.17, 2869.37, 2869.51, 2869.81, 2869.91, 2870.49,
		    2870.58, 2871.05, 2871.94, 2872.23, 2873.12, 2873.33, 2873.92 };

	    assert x1_vals.length == x2_vals.length;
	    final double[][] x_vals = new double[x1_vals.length][2];
	    for (int i = 0; i < x1_vals.length; i++) {
		x_vals[i][0] = x1_vals[i];
		x_vals[i][1] = x2_vals[i];
	    }
	    return x_vals;
	}

	@Override
	public double getY(final double[] x, final double[] a) {
	    return Polynomial_2D.val(x, a, 2, 2);
	}

	@Override
	public double getPartialDerivate(final double[] x, final double[] a, final int parameterIndex) {
	    return Polynomial_2D.grad(x, a, 2, 2, parameterIndex);
	}
    }

    public static void main(final String[] cmdline) {
	final LMPolyTest func = new LMPolyTest();
	final double[] a_fit = func.initial();
	final double[][] vals = func.yValues();
	final LMA lma = new LMA(func, a_fit, vals[0], func.xValues(), vals[1], new JAMAMatrix(a_fit.length,
		a_fit.length));
	lma.fit();
	final double[] a_gnuplot = { 551.113, 0.0834116, -2.01285e-005, -0.0236638, 2.68602e-006, -5.03575e-010,
		6.71201e-007, 7.10721e-010, -1.43449e-013 };
	for (int i = 0; i < a_fit.length; i++) {
	    System.out
		    .println(a_fit[i] + "\t\t" + a_gnuplot[i] + "\t\t" + Math.abs(a_gnuplot[i] - a_fit[i]) / a_fit[i]);
	}
    }
}
