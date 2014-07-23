# File:		sub_new.plt
# Author:	Michael Entrup
# Version:	1.0.1
# Date:		16.07.2014

# Info:		This script is part of the Gnuplot_sreels package. The package is used to process the data that has been created by https://github.com/EFTEMj/EFTEMj/blob/master/Scripts%2BMacros/SR-EELS_characterisation.ijm.

#########################
#	Read user input		#
#########################

# The script needs 5 parameters that the user has to enter. Batch-/Shell-scripts are used for this task.
if (strstrt(GPVAL_PWD, "/") != 1) {
	# Windows
	# A batch script is used to read the parameters. They are saved to a temporary file.
	system 'new.bat'
} else {
	# Unix
	# A shell script is used to read the parameters. They are saved to a temporary file.
	# ToDo: Read values from shell.
}
load 'from_shell.plt'
bin = binning
# The dataset will be saved to the following directory:
path='SpecMag'.SpecMag.'/'.QSinK7.'/'
# For Windows we have to use the backslash. It is necessary to escape to backslash.
path_win='SpecMag'.SpecMag.'\\'.QSinK7.'\\'

#########################
#	Main part			#
#########################

# 'mv from_shell.plt' will be moved (and renamed) to the folder of the dataset. The first line will be removed, because when moving the input files, the given function 'filename_input(N)' is no longer valid.
if (strstrt(GPVAL_PWD, "/")) {
	# Unix
	system 'mkdir SpecMag'.SpecMag.'/'.QSinK7
	# ToDo: Delete first line of the file.
	system 'mv from_shell.plt '.path.'dataset_init.plt'
} else {
	# Windows
	system 'md SpecMag'.SpecMag.'\\'.QSinK7
	system 'more +1 from_shell.plt > '.path_win.'dataset_init.plt'
	system 'del from_shell.plt'
}
# This script will write additional data to 'dataset_init.plt', using the print redirection.
set print path.'dataset_init.plt' append
print 'path = "'.path.'"'
print 'path_win = "'.path_win.'"'

# If you need other indices than 1,2,...,N you can overwrite the default behaviour by setting the parameters before running 'sreels_init.plt'.
if (!exists("index_start")) {index_start=1}
if (!exists("index_stop")) {index_stop=N}
if (!exists("index_inc")) {index_inc=1}

print 'index_start = '.index_start
print 'index_stop = '.index_stop
print 'index_inc = '.index_inc

filename_T(N) = path.sprintf('spec_%dT.txt', N)
filename_C(N) = path.sprintf('spec_%dC.txt', N)
filename_B(N) = path.sprintf('spec_%dB.txt', N)
plot_border(N, n) = sprintf('plot filename_input(%d) using (bin*$2):(bin*$%d)', N, n)
do for [i=index_start:index_stop:index_inc] {
	set table filename_T(i)
	eval(plot_border(i, 4))
	set table filename_C(i)
	eval(plot_border(i, 3))
	set table filename_B(i)
	eval(plot_border(i, 5))
}

filename_3D = path.'3D.txt'
set table filename_3D
splot for [i=index_start:index_stop:index_inc] filename_input(i) using (bin*$2):(bin*$3):(bin*abs($5-$4))

filename_cod3D = path.'cod3D.txt'

print 'filename_T(N) = path.sprintf("spec_%dT.txt", N)'
print 'filename_C(N) = path.sprintf("spec_%dC.txt", N)'
print 'filename_B(N) = path.sprintf("spec_%dB.txt", N)'
print 'filename_3D = path."3D.txt"'
print 'filename_cod3D = path."cod3D.txt"'

unset table
unset print