# File:		Figure4.plt
# Version:	MC2015
# Date:		17.04.2015

set loadpath 'Q:\git\Gnuplot_config\'
# set loadpath '.'

reset
set encoding utf8
width_line = 2
size_point = 1

load 'xyborder.cfg'
load 'moreland.pal'

set output "Figure4.png"

x_label="x - position on CCD [px]"
dE_label="{/Symbol D}E - energy offset [eV]"
w_label="w - aperture width [px]"
I_label="cumulative intensity [a.u.]"

bin = 1
dispY = 0.020210
png_width = 1366
png_height = 768
font_size = 14
index = 1
cam_width = 4096
cam_height = 4096

set xrange [-cam_height/2*dispY:cam_height/2*dispY]
set xlabel dE_label
set yrange [cam_width/2:-cam_width/2]
set ylabel x_label
set ytics 512
set zeroaxis
set grid
unset key

term = "set terminal pngcairo size png_width,png_height enhanced font 'Meta,".font_size."' dashed"
# term = "set terminal wxt"
set sample 2000
set terminal wxt

filename(n) = sprintf("measurements/values_Pos%d.txt", n)

ft(x) = t0 + t1*x + t2*x**2 + t3*x**3
fb(x) = b0 + b1*x + b2*x**2 + b3*x**3
fit ft(x) filename(index) using ((bin*$2-2048)*dispY):(bin*$4-2048) via t0, t1, t2, t3
fit fb(x) filename(index) using ((bin*$2-2048)*dispY):(bin*$5-2048) via b0, b1, b2, b3

set label 1 box 
set label 2 box
# \n does only work in "" and not in ''
set label 1 sprintf("top border:\nf_t(x) = t_0 + t_1x + t_2x^2 + t_3x^3\n\nt_0 = %g\nt_1 = %g\nt_2 = %g\nt_3 = %g", t0, t1, t2, t3) at -20,512 center
set label 2 sprintf("bottom border:\nf_b(x) = b_0 + b_1x + b_2x^2 + b_3x^3\n\nb_0 = %g\nb_1 = %g\nb_2 = %g\nb_3 = %g", b0, b1, b2, b3) at 20,512 center

eval(term)
plot filename(index) using ((bin*$2-2048)*dispY):(bin*$4-2048) notitle with points ls 1,\
filename(index) using ((bin*$2-2048)*dispY):(bin*$5-2048) notitle with points ls 8,\
ft(x) with lines ls 1,\
fb(x) with lines ls 8

unset output
