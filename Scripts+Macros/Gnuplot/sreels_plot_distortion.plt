# File:		sreels_plot_distortion.plt
# Date:		04.09.2014

reset
set encoding utf8
width_line = 1.5
size_point = 1
load "sub_styles.plt"

#0. prepare the output.
set terminal svg size 1024,768 enhanced fname "Meta" fsize 20
output_file1 = "distortion_".substr(QSinK7,0,strstrt(QSinK7, "%"))."_equalDist.svg"
output_file2 = "distortion_".substr(QSinK7,0,strstrt(QSinK7, "%")).".svg"

#1. Calculate the position of the maximum (I don't check the second deviation, because I know that it can't be a minimum):
x2_max = -b01 / (2*b02)

#2. Calculate the corresponding y-value:
w_max = f2(0,x2_max)

#3. Normalize the maximum to 1:
do for [i=0:2] {eval(sprintf("b%d = b0%d / w_max", i, i))}

#4. This function represents the integral of the parable:
F(x2) = (b00*x2 + (b01*x2**2)/2 + (b02*x2**3)/3)/w_max

#5. Calculate the transformed y-coordinate:
#5.1. F(x2) - F(x2_max) is the area under the parable between x2_max and x2. This area corresponds to the distance between x2_max and x2 at the distorted image.
#5.2. x2_max is added to get image coordinates.
x2n(x2) =  x2_max + (F(x2) - F(x2_max))

#6. We only look at the region where the parable is positive, as a negative width is not physical correct.
x2l = -sqrt((b1**2)/(b2**2)/4-b0/b2)-b1/b2/2	# lower intersection of the parable with the x2-axis
x2h = sqrt((b1**2)/(b2**2)/4-b0/b2)-b1/b2/2		# upper intersection of the parable with the x2-axis
#ToDo: Implement an automatic selection of disp depending on SpecMag.
disp = 0.020210
set xrange [-2048*disp:2048*disp]
set yrange [-2048:2048]
# Try to place 9 xtics and only use multiples of 10eV
x_tics = ceil(4096*disp/90)*10
set xtics x_tics
set ytics 512
set title "Distortion when using QSinK7 = ".substr(QSinK7,0,strstrt(QSinK7, "%"))
set xlabel dE_label
set ylabel x_label
set grid ytics linetype 99
set sample 1024

#7. This is a security check. Gnuplot wont plot anything if x2h is larger than x2l.
if (x2l > x2h) {
	temp = x2l
	x2l = x2h
	x2h = temp
}

#8. We plot f(x1,i) where i is increased in 256 steps. f(x,i) has been calculated for image coordinates (0 to 4096) instead of -2048 to 2048.
set output output_file1
fd(x, i) = f(x/disp,i) - 2048
plot  for [i=0:4096:256] fd(x,i) notitle linetype 101

#9. unset output to close the file.
print ''
print 'Diagram has been saved as "'.output_file1.'"'
unset output

#10. We plot f(x1,i) where the parameter i is calculated by the function defined at #5.
set output output_file2
#10.1. The parameter starts at x2_max and goes up until it reaches x2h.
#10.2. The parameter starts at x2_max-128 and goes down until it reaches x2l.
fc(x, i) = f(x/disp,x2n(i)) - 2048
plot  for [i=x2_max:x2h:256] fc(x,i) notitle linetype 101, \
for [i=x2_max-256:x2l:-256] fc(x,i) notitle linetype 101, \
for [i=x2_max+128:x2h:256] fc(x,i) notitle linetype 102, \
for [i=x2_max-128:x2l:-256] fc(x,i) notitle linetype 102

#11. unset output to close the file.
print ''
print 'Diagram has been saved as "'.output_file2.'"'
print ''
unset output

#12. show the diagram on screen
set terminal wxt
replot