from ij import IJ
import random
import string

imp = IJ.createImage("Drift-Stack_max64px", "32-bit black", 256, 256, 10)
width, height = 32, 32
x, y, x_start, y_start = 112, 112, 128, 128
imp.setRoi(x,y,width,height)
IJ.run(imp, "Set...", "value=255 slice")
imp.getStack().setSliceLabel(str(x) + ',' + str(y), 1)
for i in range(2,11):
	imp.setSlice(i)
	x = x_start + random.randint(-32.0,32.0) - width/2
	y = y_start + random.randint(-32.0,32.0) - height/2
	print x, y
	imp.setRoi(x,y,width,height)
	IJ.run(imp, "Set...", "value=255 slice")
	imp.getStack().setSliceLabel(str(x) + ',' + str(y), i)
imp.setRoi(80,80,96,96)
imp.show()
IJ.run("Start Animation [\\]")