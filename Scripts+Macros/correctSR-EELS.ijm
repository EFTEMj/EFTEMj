a00            = 0.000924797;
a10            = 0.0719424:
a20            = -3.6496e-006:
a01            = 0.999999:
a11            = -3.21945e-005:
a21            = 2.2795e-009:
a02            = 5.45902e-011:
a12            = 4.08705e-010:
a22            = -1.56416e-014:

c = 549.883120731574;
b = 0.0854331764757123;
a = -2.06003501147918e-005;

width = 4096;
height = 4096;

max = a*pow(-b/(2*a),2) + b*(-b/(2*a)) + c;
a = a/max;
b = b/max;
c = c/max;
bin = 2;
center_x = -b/(2*a);
center_int = pow(center_x,3)*a/3 + pow(center_x,2)*b/2 + center_x*c;

p = b/a;
q = c/a;
y1 = -p/2+sqrt(pow(p/2,2)-q);
y2 = -p/2-sqrt(pow(p/2,2)-q);
if (y1 < y2) {
	y_start = bin*y1;
	y_stop = bin*y2;
} else {
	y_start = bin*y2;
	y_stop = bin*y1;
}
if (y_start < 0 ) y_start = 0;
if (y_stop > height) y_stop = height;

path = getDirectory("Input directory");

Dialog.create("File type");
Dialog.addString("File suffix: ", ".dm3", 5);
Dialog.show();
suffix = Dialog.getString();

start_time = getTime();
setBatchMode(true);
processFolder(path);

function processFolder(path) {
	list = getFileList(path);
	for (i = 0; i < list.length; i++) {
		if(endsWith(list[i], "/")) {
			processFolder("" + path + list[i]);
		}
		if(endsWith(list[i], suffix)) {
			processFile(path, path, list[i]);
		}
	}
}

function processFile(input, output, file) {
	open(input + file);
	tempImg = getImageID();
	run("Rotate 90 Degrees Left");
	title = getTitle();
	run("Scale...", "x=- y=- width="+width+" height="+height+" interpolation=None average create title="+substring(title, 0, lengthOf(title) - 4)+".tif");
	inImg = getImageID();
	selectImage(tempImg);
	close();

	newImage(title+"_corrected", "32-bit black", width, height, 1);
	resultImg = getImageID();
	newImage(title+"_corrected_simple", "32-bit black", width, height, 1);
	resultImgSimple = getImageID();
	showStatus("Correct SR-EELS...");
	for (y=y_start;y<y_stop;y++) {
		showProgress(y/height);
		y1 = calc_y1(y);
		for (x=0; x<width; x++) {
			x1 = x;
			x2 = calc_g(x1,y1);
			y2 = calc_f(x1,y1);
			selectImage(inImg);
			value = getPixel(x2, y2);
			selectImage(resultImg);
			setPixel(x, y, value);
			selectImage(inImg);
			value = getPixel(x, y2);
			selectImage(resultImgSimple);
			setPixel(x, y, value);
		}
	}
	selectImage(resultImg);
	run("Enhance Contrast", "saturated=0.35");
	save(output + substring(title, 0, lengthOf(title) - 4)+"_corrected.tif");
	saveAs("png", output + substring(title, 0, lengthOf(title) - 4)+"_corrected.png");
	close();
	selectImage(resultImgSimple);
	run("Enhance Contrast", "saturated=0.35");
	save(output + substring(title, 0, lengthOf(title) - 4)+"_corrected_simple.tif");
	saveAs("png", output + substring(title, 0, lengthOf(title) - 4)+"_corrected_simple.png");
	close();
	selectImage(inImg);
	run("Enhance Contrast", "saturated=0.35");
	saveAs("png", output + substring(title, 0, lengthOf(title) - 4)+".png");
	close();
}
setBatchMode("exit and display");

function calc_g(x,y) {
	value = sub_g(x,y) - sub_g(0,y);
	return value;
}

function sub_g(x,y) {
	value = log(abs(2*sqrt(calc_a(y)*(calc_a(y)*pow(x,2)+calc_b(y)*x+calc_c(y)))+2*calc_a(y)*x+calc_b(y)))/sqrt(calc_a(y));
	return value;
}

function calc_f(x,y) {
	value = calc_a0(y)+calc_a1(y)*x+calc_a2(y)*pow(x,2);
	return value;
}

function calc_a(y) {
	a = 4*pow(calc_a2(y),2);
	return a;
}

function calc_b(y) {
	b = 4*calc_a1(y)*calc_a2(y);
	return b;
}

function calc_c(y) {
	c = pow(calc_a1(y),2)+1;
	return c;
}

function calc_a0(y) {
	a0 = a00+a01*y+a02*pow(y,2);
	return a0;
}

function calc_a1(y) {
	a1 = a10+a11*y+a12*pow(y,2);
	return a1;
}

function calc_a2(y) {
	a2 = a20+a21*y+a22*pow(y,2);
	return a2;
}

function calc_y1(y) {
	y = y/bin;
	temp = pow(y,3)*a/3 + pow(y,2)*b/2 + y*c;
	y1 = center_int - temp;
	return bin*y1;
}
showMessage("Benötigte Zeit: " + (getTime()-start_time)/1000 + "s");