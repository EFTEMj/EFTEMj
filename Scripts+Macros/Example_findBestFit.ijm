/*
 * file:	Example_findBestFit.ijm
 * author:	Michael Entrup b. Epping (michael.entrup@wwu.de)
 * version:	20140404
 * info:	This macro tests all fit functions that are available in imagej.
 * 			It uses the mean intensity of the slices of the example image 'Fly Brain (1MB)' as data points.
 * 			
 * 			The code is partially taken from http://rsb.info.nih.gov/ij/macros/examples/CurveFittingDemo.txt
 */

run("Fly Brain (1MB)");
run("Clear Results");
for (i=1; i<=27; i++) {
	setSlice(i);
	run("Measure");
}
n = nResults();
x = newArray(n);
for (i=0; i<x.length; i++)
{
	x[i] = i;
}

y = newArray(n);
for (i=0; i<y.length; i++)
{
	y[i] = getResult("Mean", i);
}


// Do all possible fits, plot them and add the plots to a stack
setBatchMode(true);
for (i = 0; i < Fit.nEquations; i++) {
Fit.doFit(i, x, y);
Fit.plot();
if (i == 0)
	stack = getImageID;
else {
	run("Copy");
	close();
	selectImage(stack);
	run("Add Slice");
	run("Paste");
}
Fit.getEquation(i, name, formula);
print(""); print(name+ " ["+formula+"]");
print("   R^2="+d2s(Fit.rSquared,3));
for (j=0; j<Fit.nParams; j++)
	print("   p["+j+"]="+d2s(Fit.p(j),6));
}
setBatchMode(false);
run("Select None");
rename("Curve Fits");
close("flybrain*");