# File:		sreels_init.plt
# Author:	Michael Entrup
# Version:	1.0.2
# Date:		15.07.2014

# Info:		This script is part of the Gnuplot_sreels package. The package is used to process the data that has been created by https://github.com/EFTEMj/EFTEMj/blob/master/Scripts%2BMacros/SR-EELS_characterisation.ijm.

#########################
#	Setup				#
#########################

# Some default settings that are always usefull.
set encoding utf8
set fit errorvariables
set macros

#########################
#	Styles				#
#########################

# We define some user styles that look better than the default ones.
rgb(r,g,b) = 65536 * int(r) + 256 * int(g) + int(b)
line_width = 1
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

# We use the reverse option to adapt the y-axis in Gnuplot to the y-axis for images (origin at the top left).
set yrange [*:*] reverse

#########################
#	Load saved data		#
#	or read new data	#
#########################

if (strstrt(GPVAL_PWD, "/")) {
	# Unix
	#ToDo: implement shell version
} else {
	# Windows
	system 'load.bat'
}
load 'sub_init.plt'
if (load_prev == -1) {
	exit
} else { 
	if (load_prev == 1) {
		load load_path.'dataset_init.plt'
		print ''
		print 'Successfully loaded the dataset from "'.path.'".'
		print ''
	} else {
		load 'sub_new.plt'
		print ''
		print 'Successfully createt a new dataset at "'.path.'".'
		print ''
	}
}
if (strstrt(GPVAL_PWD, "/")) {
	# Unix
	#ToDo: implement shell version
} else {
	# Windows
	system 'del sub_init.plt'
}