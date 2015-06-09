/*
 * file:	dm3_to_tif.ijm
 * author:	Michael Entrup b. Epping (michael.entrup@wwu.de)
 * version:	20150609
 * info:	This macro converts dm3 files to tif files. The user can select input and output directory.
 * 			Subfolders are regared. The directory tree is rebuild at the output folder.
 * 			The macro will stop when trying to open unsupportet files (1d dm3 files like profiles and EEL spectra).
 */

DEBUG = 0;

input = getDirectory("Input directory");
output = getDirectory("Output directory");

Dialog.create("File type");
Dialog.addString("File suffix: ", ".dm3", 5);
Dialog.show();
suffix = Dialog.getString();

setBatchMode(true);
images = newArray();
images = findFiles(input, "", images);

if (DEBUG > 0 ) {
	Array.show(images);
} else {
	convertFiles(images, output);
}

function findFiles(input, subfolder, images) {
	list = getFileList(input + subfolder);
	for (i = 0; i < list.length; i++) {
		if (DEBUG > 0 ) {
			IJ.log(subfolder + list[i]);
		}
		if(File.isDirectory(input + subfolder + list[i]))
			images = findFiles(input, subfolder + list[i], images);
		if(endsWith(list[i], suffix))
			images = Array.concat(images, subfolder + list[i]);
	}
	return images;
}

function convertFiles(images, output) {
	showProgress(0);
	for (i = 0; i < images.length; i++) {
		file = images[i];		
		open(input + file);
		showProgress((i -0.5)  / images.length);
		createFolder(File.getParent(output + file));
		save(output + replace(file, suffix, ".tif"));
		close();
		showProgress(i / images.length);
	}
}

function createFolder(path) {
	if (!File.isDirectory(path)) {
		if (File.isDirectory(File.getParent(path))) {
			File.makeDirectory(path);
		} else {
			createFolder(File.getParent(path));
		}
	}
}