a00 = -1.437557e+002
a01 = 1.069277e+000
a02 = -6.310422e-006
a03 = 8.692455e-010
a04 = -3.661683e-015
a10 = 7.756970e-002
a11 = -3.659638e-005
a12 = 1.580518e-009
a13 = -4.548626e-014
a14 = -2.698064e-017
a20 = -3.602745e-006
a21 = 1.371994e-009
a22 = 7.374924e-013
a23 = -1.965998e-016
a24 = 1.629919e-020

b00 = 5.511134e+002
b01 = 8.341164e-002
b02 = -2.012854e-005
b10 = -2.366382e-002
b11 = 2.686017e-006
b12 = -5.035751e-010
b20 = 6.712013e-007
b21 = 7.107207e-010
b22 = -1.434486e-013

c = b00;
b = b01;
a = b02;

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
Dialog.addString("File suffix: ", ".tif", 5);
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
	run("Flip Vertically");
	save(output + substring(title, 0, lengthOf(title) - 4)+"_corrected.tif");
	saveAs("png", output + substring(title, 0, lengthOf(title) - 4)+"_corrected.png");
	close();
	selectImage(resultImgSimple);
	run("Enhance Contrast", "saturated=0.35");
	run("Flip Vertically");
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