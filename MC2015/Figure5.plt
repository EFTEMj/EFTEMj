# File:		Figure5.plt
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

file_out = "Figure5.png"

x_label="x - position on CCD [px]"
dE_label="{/Symbol D}E - energy offset [eV]"
w_label="w - aperture width [px]"
I_label="cumulative intensity [a.u.]"

bin = 1
dispY = 0.020210
png_width = 1366
png_height = 768
font_size = 14
index_start = 1
index_stop = 8
index_inc = 1
cam_width = 4096
cam_height = 4096

set xrange [-cam_height/2*dispY:cam_height/2*dispY]
set xlabel dE_label
set yrange [cam_width/2:-cam_width/2]
set ylabel x_label
set ytics 512
set zeroaxis
unset key

term = "set terminal pngcairo size png_width,png_height enhanced font 'Meta,".font_size."' dashed"
# term = "set terminal wxt"
set sample 2000
set terminal wxt

fstrT(N) = sprintf('f%dT(x) = a%dT*x**2 + b%dT*x + c%dT', N, N, N, N)
fitstrT(N) = sprintf('fit f%dT(x) "measurements/values_Pos%d.txt" using ((bin*$2-2048)*dispY):(bin*$4-2048) via a%dT,b%dT,c%dT', N, N, N, N, N)
fstrB(N) = sprintf('f%dB(x) = a%dB*x**2 + b%dB*x + c%dB', N, N, N, N)
fitstrB(N) = sprintf('fit f%dB(x) "measurements/values_Pos%d.txt" using ((bin*$2-2048)*dispY):(bin*$5-2048) via a%dB,b%dB,c%dB', N, N, N, N, N)
plotstrT(N) = sprintf('plot f%dT(x)', N)
plotstrB(N) = sprintf('plot f%dB(x)', N)

do for [i=index_start:index_stop:index_inc] {
	tableT = 'temp/T'.i.'.txt'
	tableB = 'temp/B'.i.'.txt'
	set table tableT
    eval(fstrT(i))
	print fstrT(i)
    eval(fitstrT(i))
	eval(plotstrT(i))
	set table tableB
    eval(fstrB(i))
	print fstrB(i)
    eval(fitstrB(i))
	eval(plotstrB(i))
	unset table
}

filenameT(N) = sprintf("temp/T%d.txt", N)
filenameB(N) = sprintf("temp/B%d.txt", N)
plotstr3D(N) = sprintf('splot "measurements/values_Pos%d.txt" using ((bin*$2-2048)*dispY):(bin*$4-2048):(f%dT(0)) with points, "measurements/values_Pos%d.txt" using ((bin*$2-2048)*dispY):(bin*$5-2048):(f%dB(0)) with points', N, N, N, N)
eval(plotstr3D(index_start))

plotstr3DT(N) = sprintf('replot "measurements/values_Pos%d.txt" using ((bin*$2-2048)*dispY):(bin*$4-2048):(f%dT(0)) with points', N, N)
plotstr3DB(N) = sprintf('replot "measurements/values_Pos%d.txt" using ((bin*$2-2048)*dispY):(bin*$5-2048):(f%dB(0)) with points', N, N)
do for [i=index_start+index_inc:index_stop:index_inc] {
	eval(plotstr3DT(i))
	eval(plotstr3DB(i))
}
table3D = 'temp/borders.txt'
set table table3D
replot
unset table

f(x,y) = a00 + a10*x + a20*x**2 + a01*y + a11*x*y + a21*x**2*y + a02*y**2 + a12*x*y**2 + a22*x**2*y**2
a00 = 1.0; a10 = 1.0; a20 = 1.0; a01 = 1.0; a11 = 1.0; a21 = 1.0; a02 = 1.0; a12 = 1.0; a22 = 1.0
fit f(x,y) table3D using 1:3:(stringcolumn(4) eq "u"?1/0:$2):(1) zerror via a00, a10, a20, a01, a11, a21, a02, a12, a22

plot for [i=-640:640:128] f(x,i) notitle with lines ls 4

filename(n) = sprintf("measurements/values_Pos%d.txt", n)
replot for [i=1:index_stop] filename(i) using ((bin*$2-2048)*dispY):(bin*$4-2048) notitle with points ls 1,\
for [i=1:index_stop] filename(i) using ((bin*$2-2048)*dispY):(bin*$5-2048) notitle with points ls 8
replot f(x,f1T(0)) with lines ls 1, f(x,f1B(0)) notitle with lines ls 8
plotstr(N) = sprintf('replot f(x,f%dT(0)) notitle with lines ls 1, f(x,f%dB(0)) notitle with lines ls 8', N, N)
do for [i=1:index_stop] {
	eval(plotstr(i))
}

eval(term)
set output file_out
set yrange [cam_width/8:-cam_width/8]
set ytics 128
replot
unset output
