from ij import IJ
import math
import time
from ij.gui import NewImage

a00            = 0.000924797
a10            = 0.0719424
a20            = -3.6496e-006
a01            = 0.999999
a11            = -3.21945e-005
a21            = 2.2795e-009
a02            = 5.45902e-011
a12            = 4.08705e-010
a22            = -1.56416e-014

step_size_z = 30
step_size_y = 10

def calc_yn(x2):
	return (x2**3*a/3 + x2**2*b/2 + x2*c) - const

def arcsinh(x):
	return math.log(x+math.sqrt(x**2+1))

def calc_y1(x1,x2):
	yn = calc_yn(x2)
	return arcsinh(a10+2*a20*x1+yn*(a11+a12*yn+2*x1*(a21+a22*yn)))/(2*(a20+yn*(a21+a22*yn)))-arcsinh(a10+yn*(a11+a12*yn))/(2*(a20+yn*(a21+a22*yn)))

def calc_y2(x1,x2):
	yn = calc_yn(x2)
	y1 = calc_y1(x1, x2)
	return a00 + a10*x1 + a20*x1**2 + yn*(a01 + a02*yn + x1*(a11 + a21*x1 + a12*yn + a22*x1*yn))
	

def get_corrected_intensity(x1, x2):
	intensity = 0
	rectangle_l = math.floor(step_size_y * min(calc_y1(x1, x2), calc_y1(x1, x2+bin))) / step_size_y
	rectangle_b = math.ceil(step_size_y * max(calc_y2(x1, x2+bin), calc_y2(x1+bin, x2+bin))) / step_size_y
	rectangle_r = math.ceil(step_size_y * max(calc_y1(x1+bin, x2), calc_y1(x1+bin, x2+bin))) / step_size_y
	rectangle_t = math.floor(step_size_y * max(calc_y2(x1, x2), calc_y2(x1+bin, x2))) / step_size_y
	if rectangle_l < 0 and rectangle_t < 0 and rectangle_r >= width*bin and rectangle_b >= height*bin:
		return 0
	rectangle_width = rectangle_r - rectangle_l
	rectangle_height = rectangle_b - rectangle_t
	temp_width = int(math.ceil(rectangle_width/bin*step_size_y))+1
	temp_height = int(math.ceil(rectangle_height/bin*step_size_y))+1
	pixels_temp = [0] * (temp_width * temp_height)
	for z2 in range(0, step_size_z):
		z2 = 1.0 * z2 / step_size_z
		for z1 in range(0, step_size_z):
			z1 = 1.0 * z1 / step_size_z
			y1 = round(step_size_y*(calc_y1(x1+bin*z1, x2+bin*z2) - rectangle_l)/bin)
			y2 = round(step_size_y*(calc_y2(x1+bin*z1, x2+bin*z2) - rectangle_t)/bin)
			index = int(y1 + y2*temp_width)
			pixels_temp[index] = 1
	count = 0
	for y2i in range(0, temp_height):
		y2 = y2i/step_size_y + rectangle_t/bin
		for y1i in range(0, temp_width):
			y1 = y1i/step_size_y + rectangle_l/bin
			if pixels_temp[y1i + y2i*temp_width] == 1:
				intensity = intensity + pixels_in[int(math.floor(y1)+math.floor(y2)*width)]/step_size_y**2
	return intensity

image_in = IJ.openImage()
pixels_in = image_in.getProcessor().getPixels()
width = image_in.getWidth()
height = image_in.getHeight()
if width != height:
	raise StandardError ('width does not equal height!')

if 4096%width == 0:
	bin = 4096 / width
else:
	raise StandardError (width,'is no factor of 4096!')

c = 549.883120731574
b = 0.0854331764757123
a = -2.06003501147918e-005

maximum = (a*(-b/(2*a))**2 + b*(-b/(2*a)) + c)

a = a/maximum
b = b/maximum
c = c/maximum

x2c = -b/(2*a)
const = x2c**3*a/3 + x2c**2*b/2 + x2c*c - x2c

p = b/a
q = c/a
y1 = (-p/2+math.sqrt((p/2)**2-q))
y2 = (-p/2-math.sqrt((p/2)**2-q))
if y1 < y2:
	y_start = y1
	y_stop = y2
else:
	y_start = y2
	y_stop = y1
if y_start < 0:
	y_start = 0
if y_stop > bin*height:
	y_stop = bin*height

start_time = time.time()
title = image_in.getShortTitle()
bit_depth = image_in.getBitDepth()
options = NewImage.FILL_BLACK
image_out = NewImage.createImage(title+'_corrected', width, height, 1, bit_depth, options)
pixels_out = image_out.getProcessor().getPixels()
image_noI = NewImage.createImage(title+'_corrected_noI', width, height, 1, bit_depth, options)
pixels_noI = image_noI.getProcessor().getPixels()


for x2 in range(y_start, y_stop):
	if x2%bin != 0:
		continue
	IJ.showProgress(x2 - y_start, y_stop - y_start)
	start_time_row = time.time()
	for x1 in range(0, bin*width):
		if x1%bin != 0:
			continue
		pixels_out[x2/bin*width+x1/bin] = get_corrected_intensity(x1, x2)
		pixels_noI[x1/bin + x2/bin*width] = pixels_in[int(round(calc_y1(x1, x2)/bin)) + int(round(calc_y2(x1, x2)/bin))*width]
	print "Calculation of row", x2/bin, ":", int(round(time.time() - start_time_row)),"s"

duration = time.time() - start_time
print 'This calculation took',int(round(duration)),'s (',int(round(width*height/duration)),'px/s )'
IJ.showProgress(1.0)
image_out.show()
image_noI.show()