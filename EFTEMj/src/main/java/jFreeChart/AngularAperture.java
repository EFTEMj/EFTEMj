
package jFreeChart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.ui.TextAnchor;

import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

public class AngularAperture implements PlugIn {

	private final double C_s = 1.2e-3;
	private final double fl = 1.72e-3;
	private final double h = 4.135667516e-15;
	private final double e = 1;
	private final double U = 200e3;
	private final double c = 299792458;
	private final double m_0 = 510998.928 / Math.pow(c, 2);
	private final double lambda = h / Math.sqrt(e * U * (2 * m_0 + e * U / Math
		.pow(c, 2)));
	private final double alpha_opt = Math.pow(2. * 0.3721 * Math.pow(lambda, 2) /
		(6. * Math.pow(C_s, 2)), 0.125) * 1e3;

	@Override
	public void run(final String arg0) {
		// http://www.tutorialspoint.com/jfreechart/index.htm
		final ApplicationFrame chart = new ApplicationFrame("Angular aperture");
		final JFreeChart xyChart = ChartFactory.createXYLineChart(
			"Angular aperture", "aperture diameter in µm", "delta in nm",
			createDataset(), PlotOrientation.VERTICAL, true, true, false);
		final ChartPanel chartPanel = new ChartPanel(xyChart);
		chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
		final XYPlot plot = xyChart.getXYPlot();
		final ValueAxis axis = plot.getRangeAxis();
		axis.setAutoRange(false);
		axis.setUpperBound(2.0);
		final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesPaint(0, Color.RED);
		renderer.setSeriesPaint(1, Color.GREEN);
		renderer.setSeriesPaint(2, Color.BLUE);
		renderer.setSeriesStroke(0, new BasicStroke(2f));
		renderer.setSeriesShapesVisible(0, false);
		renderer.setSeriesShapesVisible(1, false);
		renderer.setSeriesShapesVisible(2, false);
		plot.setRenderer(renderer);

		final double x_opt = mrad2µm(alpha_opt);
		final double y_opt = 0.9 * Math.pow(C_s * Math.pow(lambda, 3), 0.25) * 1e9;
		final XYLineAnnotation line = new XYLineAnnotation(0, y_opt, mrad2µm(Math
			.ceil(2 * alpha_opt)), y_opt);
		plot.addAnnotation(line);
		final XYPointerAnnotation pointer = new XYPointerAnnotation(String.format(
			"Smallest error disc: %.2g nm @ %.1f µm", y_opt, x_opt), x_opt, y_opt,
			5.0 * Math.PI / 4.0);
		stylePointer(pointer);
		plot.addAnnotation(pointer);
		final double aperture_alpha = 0.5 * 20 * 1e-6 / fl;
		final XYPointerAnnotation resolution = new XYPointerAnnotation(String
			.format("Resolution with %.0f µm aperture: %.2f nm", mrad2µm(
				aperture_alpha * 1e3), delta(aperture_alpha) * 1e9), mrad2µm(
					aperture_alpha * 1e3), delta(aperture_alpha) * 1e9, 1.0 * Math.PI /
						4.0);
		stylePointer(resolution);

		plot.addAnnotation(resolution);

		chart.setContentPane(chartPanel);
		chart.pack();
		RefineryUtilities.centerFrameOnScreen(chart);
		chart.setVisible(true);
	}

	private void stylePointer(final XYPointerAnnotation pointer) {
		pointer.setLabelOffset(15);
		pointer.setBaseRadius(50.0);
		pointer.setTipRadius(5);
		pointer.setFont(new Font("SansSerif", Font.PLAIN, 12));
		pointer.setPaint(Color.blue);
		pointer.setTextAnchor(TextAnchor.HALF_ASCENT_CENTER);
	}

	private XYDataset createDataset() {
		final XYSeriesCollection dataset = new XYSeriesCollection();
		final XYSeries all = new XYSeries("delta");
		final XYSeries s = new XYSeries("s");
		final XYSeries b = new XYSeries("b");
		final int start = 0;
		final int stop = (int) Math.ceil(2 * alpha_opt);
		final int steps = 100;
		final double step = 1. * stop / steps;
		for (int i = 0; i <= steps; i++) {
			final double x = (step * i + start) * 1e-3;
			final double x_val = mrad2µm(step * i + start);
			s.add(x_val, delta_s(x) * 1e9);
			b.add(x_val, delta_b(x) * 1e9);
			final double delta = delta(x);
			all.add(x_val, delta * 1e9);
		}
		dataset.addSeries(all);
		dataset.addSeries(s);
		dataset.addSeries(b);
		return dataset;
	}

	private double delta_s(final double x) {
		final double val = C_s * Math.pow(x, 3);
		return val;
	}

	private double delta_b(final double x) {
		final double val = 0.61 * lambda / x;
		return val;
	}

	private double delta(final double x) {
		final double val = Math.sqrt(Math.pow(delta_s(x), 2) + Math.pow(delta_b(x),
			2));
		return val;
	}

	private double mrad2µm(final double alpha) {
		final double d = 2 * alpha * fl * 1e3;
		return d;
	}

	public static void main(final String[] args) {
		// start ImageJ
		new ImageJ();

		// run the plugin
		final Class<?> clazz = AngularAperture.class;
		IJ.runPlugIn(clazz.getName(), "");
	}

}
