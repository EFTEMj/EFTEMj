/*
 * file:	measure_curve_length.ijm
 * author:	Michael Entrup b. Epping (michael.entrup@wwu.de)
 * version:	20140521
 * info:	This macro calculates the curve length of a 2nd order polynomial numericaly.
 * 			The calculation is done for differnt step sizes.
 */

// Values taken from the SR-EELS measurement done on 07.05.2014 with SpecMag=125 QSinK7=-30 QSinK4=-9.57
a00             = -0.000709933;
a10             = -0.0243578;
a20             = -1.36971e-006;
a01             = 0.999999;
a11             = -2.11494e-005;
a21             = 7.75423e-009;
a02             = 3.58715e-010;
a12             = -6.1792e-010;
a22             = 8.20723e-014;

//	Full Polynomial f(x,y)
print("Full Polynomial f(x,y)");
print("f(1,0) = " + f(1,0));
for (step = 1; step>0.001; step = step/2){
	y3 = 0;
	y1 = 0;
	sum = 0;
	count = 0;
	for (i = step; i<=2048; i = i+step) {
		x1 = i;
		y2 = y3;
		y3 = a00 + a10*x1 + a20*pow(x1,2) + a01*y1 + a11*x1*y1 + a21*pow(x1,2)*y1 + a02*pow(y1,2) + a12*x1*pow(y1,2) + a22*pow(x1,2)*pow(y1,2);
		sum += sqrt(pow(step, 2) + pow(y3-y2, 2));
		count++;
	}
	print(count + ": " + sum/i);
}
print("");
//	Reduced Polynomial f(x,0)
print("Reduced Polynomial f(x,0)");
print("f(1,0) = " + f(1,0));
for (step = 1; step>0.001; step = step/2){
	y3 = 0;
	sum = 0;
	count = 0;
	for (i = step; i<=2048; i = i+step) {
		x1 = i;
		y2 = y3;
		y3 = a00 + a10*x1 + a20*pow(x1,2);
		sum += sqrt(pow(step, 2) + pow(y3-y2, 2));
		count++;
	}
	print(count + ": " + sum/i);
}

function f(x,y) {
	return a00 + a10*x + a20*pow(x,2) + a01*y + a11*x*y + a21*pow(x,2)*y + a02*pow(y,2) + a12*x*pow(y,2) + a22*pow(x,2)*pow(y,2);
}
