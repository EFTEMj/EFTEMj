
package eftemj;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ij.IJ;
import ij.ImageJ;

public class EFTEMj_Debug {

	/**
	 * Set the plugins.dir property to make the EFTEMj appear in the Plugins menu.
	 * Finally start ImageJ.
	 *
	 * @param args Not used
	 */
	public static void main(final String[] args) {
		EFTEMj.debugLevel = EFTEMj.DEBUG_SHOW_IMAGES;
		final Class<?> clazz = EFTEMj_Debug.class;
		final String url = clazz.getResource("/" + clazz.getName().replace('.',
			'/') + ".class").toString();
		// this is the path were maven creates the jar-file
		final String pluginsDir = url.substring("file:".length(), url.length() -
			clazz.getName().length() - ".class".length() - "classes/".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();
	}

	public static void log(final String logText, final boolean highPriority) {
		final LogFileWriter logFileWriter = LogFileWriter.getInstance();
		if (EFTEMj.debugLevel >= EFTEMj.DEBUG_LOGGING) {
			logFileWriter.write(logText);
		}
		else if (EFTEMj.debugLevel >= EFTEMj.DEBUG_LOGGING & highPriority) {
			logFileWriter.write(logText);
		}
		if (EFTEMj.debugLevel >= EFTEMj.DEBUG_IN_APP_LOGGING) {
			IJ.log(logText);
		}
	}

	private static class LogFileWriter {

		private static LogFileWriter instance = null;
		private final File file;

		private LogFileWriter() {
			file = new File(EFTEMj_Prefs.getLogFile());
		}

		public static LogFileWriter getInstance() {
			if (instance == null) {
				instance = new LogFileWriter();
			}
			return instance;
		}

		public void write(final String text) {
			try {
				final FileWriter fw = new FileWriter(file, true);
				fw.write(text);
				fw.write(String.format("%n"));
				fw.close();
			}
			catch (final IOException exc) {
				IJ.showMessage("Can't write to the log file." + "\n" + exc);
			}
		}

	}
}
