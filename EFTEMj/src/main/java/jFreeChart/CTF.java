
package jFreeChart;

import java.awt.Color;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

public class CTF implements PlugIn {

	private final double C_s = 1.2e-3;
	private final double C_c = 1.2e-3;
	private final double h = 4.135667516e-15;
	private final double e = 1;
	private final double U = 200e3;
	private final double dU = 0.7;
	private final double c = 299792458;
	private final double m_0 = 510998.928 / Math.pow(c, 2);
	private final double lambda = h / Math.sqrt(e * U * (2 * m_0 + e * U / Math
		.pow(c, 2)));

	@Override
	public void run(final String arg0) {
		// http://www.math.hu-berlin.de/~ccafm/lehre_BZQ_Numerik/allg/JAVA_Pakete/JFreeChart/JFreeChart-Tutorial.html
		final String title = "Contrast Transfer Function";
		final XYSplineRenderer spline = new XYSplineRenderer();
		spline.setSeriesPaint(0, Color.RED);
		spline.setSeriesPaint(1, Color.BLUE);
		spline.setSeriesShapesVisible(0, false);
		spline.setSeriesShapesVisible(1, false);
		final NumberAxis xax = new NumberAxis("q in 1/nm");
		final NumberAxis yax = new NumberAxis("CTF");
		final XYPlot plot = new XYPlot(createDataset(), xax, yax, spline);
		final JFreeChart chart = new JFreeChart(plot);
		final ChartPanel chartPanel = new ChartPanel(chart);
		final ApplicationFrame chartFrame = new ApplicationFrame(title);
		chartFrame.setContentPane(chartPanel);
		chartFrame.pack();
		RefineryUtilities.centerFrameOnScreen(chartFrame);
		chartFrame.setVisible(true);
	}

	private XYDataset createDataset() {
		final XYSeriesCollection dataset = new XYSeriesCollection();

		final double df_opt = Math.sqrt(2. * C_s * lambda);
		final XYSeries ctf_opt = new XYSeries(String.format("CTF @ %.1f nm defocus",
			df_opt * 1e9));
		final XYSeries ctf_0 = new XYSeries("CTF @ 0 nm defocus");
		final int start = 0;
		final int stop = 10;
		final int steps = 1000;
		final double step = 1. * stop / steps;
		for (int i = 0; i <= steps; i++) {
			final double x = (step * i + start) * 1e9;
			final double x_val = step * i + start;
			ctf_opt.add(x_val, ctf(x, df_opt));
			ctf_0.add(x_val, ctf(x, 0));
		}
		dataset.addSeries(ctf_opt);
		dataset.addSeries(ctf_0);
		return dataset;
	}

	private Number ctf(final double x, final double df) {
		final double value = Math.sin(0.5 * Math.PI * (C_s * Math.pow(lambda, 3) *
			Math.pow(x, 4) - 2. * df * lambda * Math.pow(x, 2))) * Math.exp(-Math.pow(
				Math.PI, 2) * Math.pow(C_c, 2) * Math.pow(dU / U, 2) * Math.pow(lambda,
					2) * Math.pow(x, 4));
		return value;
	}

	public static void main(final String[] args) {
		// start ImageJ
		new ImageJ();

		// run the plugin
		final Class<?> clazz = CTF.class;
		IJ.runPlugIn(clazz.getName(), "");
	}

}
