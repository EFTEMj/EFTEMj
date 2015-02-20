/*
 * file:	SR-EELS_PlotCharacterisationResults.ijm
 * author:	Michael Entrup b. Epping (michael.entrup@wwu.de)
 * version:	20150209
 * info:	This macro creates plt files to plot graphs with Gnuplot [1].
 * 			You have to build a hirachical structure of directories that contain the the SR-EELS calibration images and the results of their characterisation.
 * 			<path defined at input>/<date with 8 digits> SM<value> <value>% <comment>/results_<parameters>
 * 			Comments are optional and the results are generated by the SR-EELS characterisation macro/plugin.
 *
 * 			The main feature is to filter the rsults by defining SpecMag and QSinK7 values.
 * 			The macro can combine an arbitary number of data sets in one diagram.
 *
 * 			If you set the variable 'gnuplotExe'  the plots are displayed by the macro.
 *
 * 			This macro is part of the Fiji plugin EFTEMj (https://github.com/EFTEMj/EFTEMj).
 *
 * [1] http://www.gnuplot.info/
 */

/*
 * Pfade sollten mit '/' angegeben werden. Dies verhindert diverse Probleme. Möchte man trotzdem '\' verwenden, so muss man diesen maskieren ('\\').
 */
input = "Q:/Aktuell/SR-EELS Calibration measurements/";	//getDirectory("Input directory");
output = "Q:/Aktuell/SR-EELS Calibration measurements/Plots/";
gnuplotExe = "P:/PortableApps/gnuplot/bin/wgnuplo.exe";

var fName = ""; // Wird für das Anzeigen der Diagramme mit Gnuplot benötigt.

if (!File.isDirectory(input)) exit("Please set the input path.");
if (!File.isDirectory(output)) exit("Please set the output path.");

Dialog.create("Set filter");
message1 = "Add one ore more SpecMag values.";
message2 = "Use , as the delimiter. Leave empty for any.";
Dialog.addMessage(message1 + "\n" + message2);
Dialog.addString("SpecMag: ", "", 20);
message3 = "Add one ore more QSinK7 values (without %).";
Dialog.addMessage(message3 + "\n" + message2);
Dialog.addString("QSinK7: ", "", 20);
Dialog.addMessage("Diagrams:");
Dialog.addCheckbox("Show width vs position", true);
Dialog.addCheckbox("Show y-position vs x-position", true);
Dialog.addCheckbox("Show width in 3D", true);
Dialog.show();

specMags = split(Dialog.getString(), ",");
qSinK7s = split(Dialog.getString(), ",");

showWidth = Dialog.getCheckbox();
showPosition = Dialog.getCheckbox();
show3d = Dialog.getCheckbox();

if (!showWidth & !showPosition & !show3d) exit();

if (specMags.length == 0) specMags = newArray("\\d+");
if (qSinK7s.length == 0) qSinK7s = newArray("-?\\d+");

results = processFolder(input, specMags, qSinK7s);

selections = selectResultsDialog(results);
if (showWidth) createWidthPlot(selections);
if (showPosition) createPositionPlot(selections);
if (show3d) create3dPlot(selections);

function processFolder(folder, array1, array2) {
	found = newArray();
	list = getFileList(folder);
	for (i = 0; i < array1.length; i++) {
		for (j = 0; j < array2.length; j++) {
			for (k = 0; k < list.length; k++) {
				if (matches(list[k], "\\d{8}\\sSM" + array1[i] + "\\s" + array2[j] + "%.*/")) {
					sublist = getFileList(folder + list[k]);
					for (l = 0; l < sublist.length; l++) {
						if (matches(sublist[l], "^results.+")) {
							found = Array.concat(found, folder + list[k] + sublist[l]);
						}
					}
				}
			}
		}
	}
	return found;
}

function selectResultsDialog(results) {
	if (results.length == 0) exit("No dataset has been found!");
	else {
		Dialog.create("Select results for plot");
		for (i = 0; i < results.length; i++) {
			label = createStringFromPath(results[i]);
			Dialog.addCheckbox(label, false);
		}
		Dialog.show();
		selections = newArray();
		for (i = 0; i < results.length; i++) {
			if (Dialog.getCheckbox()) {
				selections = Array.concat(selections, results[i]);
		}
	}
	return selections;
}

function createStringFromPath(result) {
	result = replace(result, "\\", "/");
	code = "var Ausdruck = /.*(\\d{8})\\sSM(\\d+)\\s(-?\\d+%)\\s?([\\w|,]*)\\/.*/;\n";
	code += "Ausdruck.exec('" + result + "');\n";
	code += "var comment = ((RegExp.$4 != '') ? ', Comment = ' + RegExp.$4 : '');\n";
	// In der nächsten Zeile wird nur ein String erzeugt. Dies ist der Rückgabewert der Funktion 'eval'.
	code += "'SpecMag = ' + RegExp.$2 + ', QSinK7 = ' + RegExp.$3 + ', Date = ' + RegExp.$1 + comment;";
	return eval("script", code);
}

function initGnuplot(plotType) {
	fName = getString("Enter a file name for the " + plotType + " diagram:", "name");
	pltFile = output + fName + ".plt";
	if (File.exists(pltFile)) ok = File.delete(pltFile);
	f = File.open(pltFile);
	print(f, "set sample 100");
	print(f, "set terminal wxt size 800,600 enhanced");
	//print(f, "set terminal svgcairo size 800,600 noenhanced");
	//print(f, "set output " + title + ".svg'");
	print(f, "set grid");
	print(f, "set style data points");
	print(f, "set key outside center bottom");
	return f;
}

function createPositionPlot(paths) {
	f = initGnuplot("y-position vs x-position");
	print(f, "set xlabel 'x-position [px]'");
	print(f, "set ylabel 'y-position [px]'");
	print(f, "set xrange[0:4096]");
	print(f, "set xtics 512");
	print(f, "set yrange[0:4096]");
	print(f, "set ytics 512");
	str = "plot '" + paths[0] + "Borders.txt' using 1:3 title '" + createStringFromPath(paths[0]) + "'";
	if (paths.length != 1) str += ",\\";
	print(f, str);
	for (i = 1; i < paths.length; i++) {
		str = "'" + paths[i] + "Borders.txt' using 1:3 title '" + createStringFromPath(paths[i]) + "'";
		if (i + 1 != paths.length) str += ",\\";
		print(f, str);
	}
	//print(f, "unset output");
	File.close(f);
	if (File.exists(gnuplotExe)) {
		exec("cmd", "/c", "start", gnuplotExe, "-persist", output + fName + ".plt");
	}
}

function createWidthPlot(paths) {
	f = initGnuplot("width vs position");
	print(f, "set xlabel 'y-position [px]'");
	print(f, "set ylabel 'width [px]'");
	print(f, "set xrange[0:4096]");
	print(f, "set xtics 512");
	print(f, "set yrange[0:*]");
	str = "plot '" + paths[0] + "Width.txt' using 2:3 title '" + createStringFromPath(paths[0]) + "'";
	if (paths.length != 1) str += ",\\";
	print(f, str);
	for (i = 1; i < paths.length; i++) {
		str = "'" + paths[i] + "Width.txt' using 2:3 title '" + createStringFromPath(paths[i]) + "'";
		if (i + 1 != paths.length) str += ",\\";
		print(f, str);
	}
	//print(f, "unset output");
	File.close(f);
	if (File.exists(gnuplotExe)) {
		exec("cmd", "/c", "start", gnuplotExe, "-persist", output + fName + ".plt");
	}
}

function create3dPlot(paths) {
	f = initGnuplot("3D");
	print(f, "set xlabel 'y-position [px]'");
	print(f, "set ylabel 'width [px]'");
	print(f, "set xrange[0:4096]");
	print(f, "set xtics 1024");
	print(f, "set yrange[0:4096]");
	print(f, "set ytics 1024");
	print(f, "set zrange[0:*]");
	print(f, "set xyplane at 0");
	print(f, "set isosample 9"); // Alle 512 px eine Linie.
	for (i = 0; i < paths.length; i++) {
		print(f, "f" + i + "(x,y) = a00" + i + " + a10" + i + "*x + a20" + i + "*x**2 + a01" + i + "*y + a11" + i + "*x*y + a21" + i + "*x**2*y + a02" + i + "*y**2 + a12" + i + "*x*y**2 + a22" + i + "*x**2*y**2");
		print(f, "fit f" + i + "(x,y) '" + paths[i] + "Width.txt' using 1:2:3:(1) via a00" + i + ", a10" + i + ", a20" + i + ", a01" + i + ", a11" + i + ", a21" + i + ", a02" + i + ", a12" + i + ", a22" + i + "");
	}
	str = "splot '" + paths[0] + "Width.txt' title '" + createStringFromPath(paths[0]) + "',\\\n";
	str += "f" + 0 + "(x,y) notitle";
	if (paths.length != 1) str += ",\\";
	print(f, str);
	for (i = 1; i < paths.length; i++) {
		str = "'" + paths[i] + "Width.txt' title '" + createStringFromPath(paths[i]) + "',\\\n";
		str += "f" + i + "(x,y) notitle";
		if (i + 1 != paths.length) str += ",\\";
		print(f, str);
	}
	//print(f, "unset output");
	File.close(f);
	if (File.exists(gnuplotExe)) {
		exec("cmd", "/c", "start", gnuplotExe, "-persist", output + fName + ".plt");
	}
}