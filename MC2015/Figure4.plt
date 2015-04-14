# File:		Figure4.plt
# Version:	MC2015
# Date:		14.04.2015

reset
set encoding utf8
width_line = 2
size_point = 1

set output "Figure4.svg"

x_label="x - position on CCD [px]"
dE_label="{/Symbol D}E - energy offset [eV]"
w_label="w - aperture width [px]"
I_label="cumulative intensity [a.u.]"

bin = 1
dispY = 0.020210
png_width = 1024
png_height = 1024
font_size = 18
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

#set key outside top center horizontal Left
unset key

term = "set terminal svg size png_width,png_height enhanced font 'Meta,".font_size."' dashed"
term = "set terminal wxt"
set sample 2000
set terminal wxt

fstrL(N) = sprintf('f%dL(x) = a%dL*x**2 + b%dL*x + c%dL', N, N, N, N)
fitstrL(N) = sprintf('fit f%dL(x) ''measurements/values_Pos%d.txt'' using ((bin*$2-2048)*dispY):(bin*$4-2048) via a%dL,b%dL,c%dL', N, N, N, N, N)
fstrR(N) = sprintf('f%dR(x) = a%dR*x**2 + b%dR*x + c%dR', N, N, N, N)
fitstrR(N) = sprintf('fit f%dR(x) ''measurements/values_Pos%d.txt'' using ((bin*$2-2048)*dispY):(bin*$5-2048) via a%dR,b%dR,c%dR', N, N, N, N, N)
plotstrL(N) = sprintf('plot f%dL(x)', N)
plotstrR(N) = sprintf('plot f%dR(x)', N)

do for [i=index_start:index_stop:index_inc] {
	tableL = 'temp/L'.i.'.txt'
	tableR = 'temp/R'.i.'.txt'
	set table tableL
    eval(fstrL(i))
	print fstrL(i)
    eval(fitstrL(i))
	eval(plotstrL(i))
	set table tableR
    eval(fstrR(i))
	print fstrR(i)
    eval(fitstrR(i))
	eval(plotstrR(i))
	unset table
}

filenameL(N) = sprintf("temp/L%d.txt", N)
filenameR(N) = sprintf("temp/R%d.txt", N)
plotstr3D(N) = sprintf('splot "measurements/values_Pos%d.txt" using ((bin*$2-2048)*dispY):(bin*$4-2048):(f%dL(0)) with points, "measurements/values_Pos%d.txt" using ((bin*$2-2048)*dispY):(bin*$5-2048):(f%dR(0)) with points', N, N, N, N)
eval(plotstr3D(index_start))

plotstr3DL(N) = sprintf('replot "measurements/values_Pos%d.txt" using ((bin*$2-2048)*dispY):(bin*$4-2048):(f%dL(0)) with points', N, N)
plotstr3DR(N) = sprintf('replot "measurements/values_Pos%d.txt" using ((bin*$2-2048)*dispY):(bin*$5-2048):(f%dR(0)) with points', N, N)
do for [i=index_start+index_inc:index_stop:index_inc] {
	eval(plotstr3DL(i))
	eval(plotstr3DR(i))
}
table3D = 'temp/borders.txt'
set table table3D
replot
unset table

f(x,y) = a00 + a10*x + a20*x**2 + a01*y + a11*x*y + a21*x**2*y + a02*y**2 + a12*x*y**2 + a22*x**2*y**2
a00 = 1.0; a10 = 1.0; a20 = 1.0; a01 = 1.0; a11 = 1.0; a21 = 1.0; a02 = 1.0; a12 = 1.0; a22 = 1.0
fit f(x,y) table3D using 1:3:(stringcolumn(4) eq "u"?1/0:$2):(1) via a00, a10, a20, a01, a11, a21, a02, a12, a22

# Die gestrichelten Linien möchte ich beim Poster nicht haben.
#plot for [i=-2000:2000:100] f(x,i) notitle with lines linetype 99

filename(n) = sprintf("measurements/values_Pos%d.txt", n)
plot for [i=1:index_stop] filename(i) using ((bin*$2-2048)*dispY):(bin*$4-2048) notitle with points,\
for [i=1:index_stop] filename(i) using ((bin*$2-2048)*dispY):(bin*$5-2048) notitle with points
replot f(x,f1L(0)) with lines , f(x,f1R(0)) notitle with lines
plotstr(N) = sprintf('replot f(x,f%dL(0)) notitle with lines linetype 100 , f(x,f%dR(0)) notitle with lines', N, N)
do for [i=1:index_stop] {
	eval(plotstr(i))

}


eval(term)
replot
unset output
