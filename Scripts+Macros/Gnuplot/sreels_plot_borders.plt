# File:		sreels_plot_borders.plt
# Date:		04.09.2014

reset
set encoding utf8
width_line = 1.5
size_point = 0.5
load "sub_styles.plt"

bin = 1
dispY = 0.020210
index_start = 1
index_stop = 8
index_inc = 1
cam_width = 4096
cam_height = 4096

set xrange [-2048*disp:2048*disp]
set yrange [-2048:2048]
# Try to place 9 xtics and only use multiples of 10eV
x_tics = ceil(4096*disp/90)*10
set xtics x_tics
set ytics 512
set title "Borders of the calibration spectra using QSinK7 = ".substr(QSinK7,0,strstrt(QSinK7, "%"))
set xlabel dE_label
set ylabel x_label
set grid ytics linetype 99
set sample 1024
set zeroaxis
unset key

output_file = "borders_".substr(QSinK7,0,strstrt(QSinK7, "%")).".svg"
set terminal unknown

plot_str(i, s) = sprintf("%splot filename_T(%d) using (($1 - cam_width/2)*disp):($2-cam_height/2) with points linestyle 101, filename_B(%d) using (($1 - cam_width/2)*disp):($2-cam_height/2) with points linestyle 101, f(x/disp+2048,T%d(0))-2048 linestyle 102, f(x/disp+2048,B%d(0))-2048 linestyle 102", s, i, i, i, i)
eval(plot_str(index_start, ""))

do for [i=index_start+1:index_stop:index_inc] {
	eval(plot_str(i, "re"))
}

set terminal svg size 1024,768 enhanced fname "Meta" fsize 20
set output output_file
replot

print ''
print 'Diagram has been saved as "'.output_file2.'"'
print ''
unset output

set terminal wxt
replot