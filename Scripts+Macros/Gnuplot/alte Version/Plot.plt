# File:		Plot.plt
# Author:	Michael Entrup
# Version:	1.4
# Date:		17.06.2014

# Info:		This script is used to procress the data that has been created by https://github.com/EFTEMj/EFTEMj/blob/master/Scripts%2BMacros/SR-EELS_characterisation.ijm.

#########################
#	Head				#
#########################

reset
set encoding utf8
set fit errorvariables

#########################
#	Settings			#
#########################

# Output layout settings
png_width = 2048
png_height = 1536
font_size = 24
line_width = 4

# Microscope and camera settings
cam_width = 4096	# number of energy channels
cam_height = 4096	# number of lateral channels

# Dispersion
Disp_SpecMag100 = 0.0666667
Disp_SpecMag125 = 0.0517241
Disp_SpecMag163 = 0.0396605
Disp_SpecMag200 = 0.0316656
Disp_SpecMag250 = 0.0256902
Disp_SpecMag315 = 0.0202102

# 'Settings.plt' contains settings that may differ for each measurement.
load "Settings.plt"

# Select the right dispersion
if (SpecMag == 100) {
	disp_y = Disp_SpecMag100
	} else {
		if (SpecMag == 125) {
			disp_y = Disp_SpecMag125
			} else {
				if (SpecMag == 163) {
					disp_y = Disp_SpecMag163
					} else {
						if (SpecMag == 200) {
							disp_y = Disp_SpecMag200
							} else {
								if (SpecMag == 250) {
									disp_y = Disp_SpecMag250
									} else {
										if (SpecMag == 315) {
											disp_y = Disp_SpecMag315
										} else {
											quit
										}
									}
							}
					}
			}
	}

# A string that is used for the title of each diagram
head = "SpecMag=".SpecMag." QSinK7=".QSinK7." ΔE=".eloss."eV"
# A string that is used for the file name of each diagram
output = "SpecMag=".SpecMag."_QSinK7=".QSinK7."_".eloss."eV"

#########################
#	Styles				#
#########################

# We define some user styles that look better than the default ones.
rgb(r,g,b) = 65536 * int(r) + 256 * int(g) + int(b)

set style line 1 linetype 1 linewidth line_width linecolor rgb rgb(0,0,0)
set style line 2 linetype 1 linewidth line_width linecolor rgb rgb(255,0,0)
set style line 3 linetype 1 linewidth line_width linecolor rgb rgb(0,255,0)
set style line 4 linetype 1 linewidth line_width linecolor rgb rgb(0,0,255)
set style line 5 linetype 1 linewidth line_width linecolor rgb rgb(192,192,0)
set style line 6 linetype 1 linewidth line_width linecolor rgb rgb(255,0,255)
set style line 7 linetype 1 linewidth line_width linecolor rgb rgb(0,255,255)
set style line 8 linetype 1 linewidth line_width linecolor rgb rgb(127,0,0)
set style line 9 linetype 1 linewidth line_width linecolor rgb rgb(0,127,0)
set style line 10 linetype 1 linewidth line_width linecolor rgb rgb(0,0,127)
set style line 11 linetype 1 linewidth line_width linecolor rgb rgb(127,127,0)
set style line 12 linetype 1 linewidth line_width linecolor rgb rgb(127,0,127)
set style line 13 linetype 1 linewidth line_width linecolor rgb rgb(0,127,127)
set style line 14 linetype 1 linewidth line_width linecolor rgb rgb(127,127,127)

# This will enable userstyles even when 'linestyle' is not used at 'plot'.
set style increment userstyles

#########################
#	Files & Terminal	#
#########################

terminal = "set term pngcairo enhanced size png_width,png_height font 'Verdana,".font_size."'"
set fit logfile "fit_".output.".txt"
set print "summery_".output.".txt"
temp_folder = "temp"
if (strstrt(GPVAL_PWD, "/") != 1) {
	# Windows
	system "md ".temp_folder
} else {
	# Unix
	system "mkdir ".temp_folder
}
output_folder = output_folder."/"
input_filename(n) = input_folder.sprintf("/".input_prefix."%d.txt", n)
eval(terminal)

#########################
#	Styles				#
#########################

set key inside right top spacing 3
set key box linetype -1 linewidth 1 
set key opaque

#########################
#	Plot Borders		#
#########################

set title "Spectrum Borders - ".head
output_file =  output_folder."1_Borders_".output.".png"

set xrange [0:cam_width]
set yrange [cam_height:0]

f_str_L(N) = sprintf('f%dL(x) = a%dL*x**2 + b%dL*x + c%dL', N, N, N, N)
f_str_R(N) = sprintf('f%dR(x) = a%dR*x**2 + b%dR*x + c%dR', N, N, N, N)
fit_str_L(N) = sprintf('fit f%dL(x) input_filename(%d) using (bin*$2):(bin*$4) via a%dL,b%dL,c%dL', N, N, N, N, N)
fit_str_R(N) = sprintf('fit f%dR(x) input_filename(%d) using (bin*$2):(bin*$5) via a%dR,b%dR,c%dR', N, N, N, N, N)
plot_str_L(N) = sprintf('plot f%dL(x)', N)
plot_str_R(N) = sprintf('plot f%dR(x)', N)

print ""
print output_file
print_a_L(N) = sprintf('print "a%dL = ", a%dL, " /pm ", a%dL_err', N, N, N)
print_b_L(N) = sprintf('print "b%dL = ", b%dL, " /pm ", b%dL_err', N, N, N)
print_c_L(N) = sprintf('print "c%dL = ", c%dL, " /pm ", c%dL_err', N, N, N)
print_a_R(N) = sprintf('print "a%dR = ", a%dR, " /pm ", a%dR_err', N, N, N)
print_b_R(N) = sprintf('print "b%dR = ", b%dR, " /pm ", b%dR_err', N, N, N)
print_c_R(N) = sprintf('print "c%dR = ", c%dR, " /pm ", c%dR_err', N, N, N)

do for [i=index_start:index_stop:index_inc] {
	table_L = 'temp/L'.i.'.txt'
	table_R = 'temp/R'.i.'.txt'
	set table table_L
    eval(f_str_L(i))
	print f_str_L(i)
    eval(fit_str_L(i))
	eval(print_a_L(i))
	eval(print_b_L(i))
	eval(print_c_L(i))
	eval(plot_str_L(i))
	set table table_R
    eval(f_str_R(i))
	print f_str_R(i)
    eval(fit_str_R(i))
	eval(print_a_R(i))
	eval(print_b_R(i))
	eval(print_c_R(i))
	eval(plot_str_R(i))
	unset table
}

set xrange [0:cam_width]
set yrange [cam_height:0]

filenameL(N) = sprintf("temp/L%d.txt", N)
filenameR(N) = sprintf("temp/R%d.txt", N)

set output output_file

plot for [i=index_start:index_stop:index_inc] input_filename(i) using (bin*$2):(bin*$4) notitle with points linestyle (i/index_inc), \
for [i=index_start:index_stop:index_inc] input_filename(i) using (bin*$2):(bin*$5) notitle with points linestyle (i/index_inc), \
for [i=index_start:index_stop:index_inc] filenameL(i) using ($1):($2) notitle with lines linestyle (i/index_inc), \
for [i=index_start:index_stop:index_inc] filenameR(i) using ($1):($2) title "Position ".i with lines linestyle (i/index_inc)

#########################
#	3D Plot - Prepare	#
#########################

# With splot we write w(x,y) to a text file. plot is used to determine min/max of w.
set isosample 16
temp_table = temp_folder."/temp.txt"
set table temp_table
set xrange [*:*]
set yrange [*:*]
# width vs x- and y-position
splot for [i=index_start:index_stop:index_inc] input_filename(i) using (bin*$2):(bin*$3):(bin*$6) with points
unset table

# We select the next smaller/larger value that can be divided by 10.
stats temp_table using 3 name "WIDTH"
z_min = 10*floor(WIDTH_min/10)
pause -1 sprintf("min: %d", WIDTH_min)
z_max = 10*ceil(WIDTH_max/10)
pause -1 sprintf("max: %d", WIDTH_max)

printstr(S, M, N) = sprintf('print "a%s%d%d = ", a%s%d%d, " /pm ", a%s%d%d_err', S, M, N, S, M, N, S, M, N)

#########################
#	3D Plot - Layout	#
#########################

set xrange [0:cam_width]
set yrange [cam_height:0]
set zrange [z_min:z_max]
set xyplane at z_min
set xtics cam_width/8 offset 0.2,-0.2
set ytics cam_width/8 offset 0.4,-0.4
set ztics offset 0,0.4
set xlabel "energy axis on CCD [px]" offset -0.4,0.0 rotate parallel
set ylabel "lateral axis on CCD [px]" offset 0.0,-0.4
set zlabel "spectrum width [px]" rotate parallel
set grid xtics ytics ztics
set view rot_x,rot_z

#########################
#	3D Plot - Simple	#
#########################

set title "Simple 3D View - ".head
set output output_folder."2_Simple3D_".output.".png"
splot for [i=index_start:index_stop:index_inc] input_filename(i) using (bin*$2):(bin*$3):(bin*$6) title "Position ".i with impuls linestyle (i-index_start)/index_inc+1 linewidth line_width/2

#########################
#	3D Plots - Fit		#
#########################

# f(x,y)
set title "3D View with f(x,y) - ".head
f(x,y) = af00 + af10*x + af20*x**2 + af01*y + af11*x*y + af21*x**2*y + af02*y**2 + af12*x*y**2 + af22*x**2*y**2
af00 = 1.0; af10 = 1.0; af20 = 1.0; af01 = 1.0; af11 = 1.0; af21 = 1.0; af02 = 1.0; af12 = 1.0; af22 = 1.0
fit f(x,y) temp_table using ($1):($2):($3):(1) via af00, af10, af20, af01, af11, af21, af02, af12, af22

output_file = output_folder."3_3D_f(x,y)_surface_".output.".png"
set output output_file
set hidden3d
splot f(x,y) title "f(x,y)" with lines
unset hidden3d

output_file = output_folder."3_3D_f(x,y)_".output.".png"
set output output_file
splot temp_table using ($1):($2):(f($1,$2)) title "f(x,y)" with lines linestyle 2,\
temp_table using ($1):($2):($3) title "Messwerte" with impuls linestyle 1 linewidth line_width/2

print ""
print output_file
print "f(x,y) = af00 + af10*x + af20*x**2 + af01*y + af11*x*y + af21*x**2*y + af02*y**2 + af12*x*y**2 + af22*x**2*y**2"
do for [i=0:2] {
	do for [j=0:2] {
		eval(printstr('f', j, i))
	}
}

# g(x,y)
set title "3D View with g(x,y) - ".head
g(x,y) = ag00 + ag10*x + ag20*x**2 + ag01*y + ag02*y**2
ag00 = 1.0; ag10 = 1.0; ag20 = 1.0; ag01 = 1.0; ag02 = 1.0
fit g(x,y) temp_table using ($1):($2):($3):(1) via ag00, ag10, ag20, ag01, ag02

output_file = output_folder."3_3D_g(x,y)_surface_".output.".png"
set output output_file
set hidden3d
splot g(x,y) title "g(x,y)" with lines
unset hidden3d

output_file = output_folder."3_3D_g(x,y)_".output.".png"
set output output_file
splot temp_table using ($1):($2):(g($1,$2)) title "g(x,y)" with lines linestyle 2, \
temp_table using ($1):($2):($3) title "Messwerte" with impuls linestyle 1 linewidth line_width/2

print ""
print output_file
print "g(x,y) = ag00 + ag10*x + ag20*x**2 + ag01*y + ag02*y**2"
do for [i=0:2] {
	do for [j=0:2] {
		if ( i == 0 | j == 0) {
			eval(printstr('g', j, i))
		} 
	}
}

# h(x)
set title "3D View with h(x) - ".head
h(x) = ah00 + ah01*x + ah02*x**2
ah00 = 1.0; ah01 = 1.0; ah02 = 1.0
fit h(x) temp_table using ($1):($2):($3):(1) via ah00, ah01, ah02

output_file = output_folder."3_3D_h(x)_surface_".output.".png"
set output output_file
set hidden3d
splot h(x) title "h(x)" with lines
unset hidden3d

output_file = output_folder."3_3D_h(x)_".output.".png"
set output output_file
splot temp_table using ($1):($2):(h($1)) title "h(x)" with lines linestyle 2, \
temp_table using ($1):($2):($3):(h($1)) title "Messwerte" with impuls linestyle 1 linewidth line_width/2

print ""
print output_file
print "h(y) = ah00 + ah01*y + ah02*y**2"
do for [j=0:2] {
	i = 0
	eval(printstr('h', i, j))
}

#########################
#	Plot Borders - Cal	#
#########################

set title "Spectrum Borders Cal - ".head
output_file = output_folder."4_Borders_Cal_".output.".png"
set output output_file
#set key horizontal outside bottom center

set terminal unknown

set xrange [0:cam_width]
set yrange [cam_height:0]
set zrange [*:*]

plot_str_3D(N) = sprintf('splot input_filename(%d) using (bin*$2):(f%dL(0)):(bin*$4) with points, \
	input_filename(%d) using (bin*$2):(f%dR(0)):(bin*$5) with points', N, N, N, N)
plot_str_3D_L(N) = sprintf('replot input_filename(%d) using (bin*$2):(f%dL(0)):(bin*$4) with points', N, N)
plot_str_3D_R(N) = sprintf('replot input_filename(%d) using (bin*$2):(f%dR(0)):(bin*$5) with points', N, N)
eval(plot_str_3D(index_start))
do for [i=index_start+index_inc:index_stop:index_inc] {
	eval(plot_str_3D_L(i))
	eval(plot_str_3D_R(i))
}
table3D = temp_folder."/borders.txt"
set table table3D
replot
unset table

k(x,y) = ak00 + ak10*x + ak20*x**2 + ak01*y + ak11*x*y + ak21*x**2*y + ak02*y**2 + ak12*x*y**2 + ak22*x**2*y**2
ak00 = 1.0; ak10 = 1.0; ak20 = 1.0; ak01 = 1.0; ak11 = 1.0; ak21 = 1.0; ak02 = 1.0; ak12 = 1.0; ak22 = 1.0
fit k(x,y) table3D using ($1):($2):($3):(1) via ak00, ak10, ak20, ak01, ak11, ak21, ak02, ak12, ak22

table_field = temp_folder."/field.txt"
set table table_field

plot for [i=0:cam_height:cam_height/64] k(x,i) with lines

unset table
eval(terminal)

plot table_field using ($1):($2) title "k(x,y)" with lines linestyle 3 linewidth line_width/2, \
table3D using ($1):($3) title "Messwerte" with points

#########################
#	End of script		#
#########################

if (strstrt(GPVAL_PWD, "/") != 1) {
	# Windows
	system "move gp_image*.png ".output_folder
	system "rd /S /Q ".temp_folder
} else {
	# Unix
	system "mv gp\_image*.png ".output_folder
	system "rm -r ".temp_folder
}

unset xlabel
unset ylabel
unset zlabel
unset title
unset output
set terminal wxt