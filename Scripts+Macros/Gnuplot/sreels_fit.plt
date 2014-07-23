# File:		sreels_fit.plt
# Author:	Michael Entrup
# Version:	1.0.0
# Date:		17.07.2014

# Info:		This script is part of the Gnuplot_sreels package. The package is used to process the data that has been created by https://github.com/EFTEMj/EFTEMj/blob/master/Scripts%2BMacros/SR-EELS_char2acterisation.ijm.

#########################
#	Setup				#
#########################

if (!exists('path')) {
	print ''
	Print 'Please run "sreels_init" first.'
	print ''
}

# Order of the polynomial of the cod3D fit:
n = 2
m = 4

# Order of the polynomial of the 3D fit:
k = 2
l = 2

if (strstrt(GPVAL_PWD, "/")) {
	# Unix
	system 'mkdir '.path.'parameters'
} else {
	# Windows
	system 'md '.path_win.'parameters'
}

set xrange[0:4096]
set yrange[0:4096]
set zrange[0:4096]

#########################
#	Fit - 2D			#
#########################

# ToDo: Change the implementation of the 2D-Fit to use polynomials of arbitrary order.

# We can create a function that can be used to create an arbitrary number of functions. In this case the first line will create one function per input file and per position (top, center, bottom). The command eval() is essential for this approach. A function creates a string that depends on indexes and eval will run the command that is represented by the string.
f_str(N, S) = sprintf('f%d%s(x) = a%d%s + b%d%s*x + c%d%s*x**2 + d%d%s*x**3 + e%d%s*x**4', N, S, N, S, N, S, N, S, N, S, N, S)
fit_str(N, S) = sprintf('fit f%d%s(x) filename_%s(%d) via a%d%s,b%d%s,c%d%s,d%d%s,e%d%s', N, S, S, N, N, S, N, S, N, S, N, S, N, S)

do for [i=index_start:index_stop:index_inc] {
	do for [str in "T C B"] {		
		eval(f_str(i, str))
		eval(fit_str(i, str))
	}
}

do for [i=index_start:index_stop:index_inc] {
	do for [str1 in "T C B"] {
		# We want to overwrite the old file, that is why we don't use the option 'append' for the first line.
		set print path.'parameters/'.str1.i.'.txt'
		print '# This file contains the parameters for "y(x) = a + b*x + c*x**2 + d*x**3 + e*x**4".
		set print path.'parameters/'.str1.i.'.txt' append
		do for [str2 in "a b c d e"] {
			# "parameter = value('parameter')"
			print sprintf(str2.'%d'.str1.' = %e',i,value(str2.i.str1))
		}		
	}
}
unset print

#########################
#	Fit - cod3D			#
#########################

# We don't want to show a plot or write to a file until the last call of 'replot'.
set terminal unknown

# As we only use 'replot' at a FOR-loop, we have to create an empty graph first.
splot NaN

plot_str_cod3D(N, S) = sprintf('replot filename_%s(%d) using ($1):(f%d%s(2048)):($2) with points', S, N, N, S)
do for [i=index_start:index_stop:index_inc] {
	do for [str in "T C B"] {
		eval(plot_str_cod3D(i, str))
	}
}

# Now we set the table, we want to write to.
set table filename_cod3D
replot
unset table
set terminal wxt

str_f_it = ''
str_via =''
do for  [i=0:n] {
	do for [j=0:m] {
		str_f_it = str_f_it.sprintf(' + a%d%d*x**%d*y**%d', i, j, i, j)
		str_via = str_via.sprintf(',a%d%d',i,j)
		eval(sprintf('a%d%d = %e', i, j, 100.0**(-i-j)))
	}
}
str_f_it = substr(str_f_it, 4, strlen(str_f_it))
str_via = substr(str_via, 2, strlen(str_via))
eval('f(x,y)='.str_f_it)
# Using a fourth column for the weight is necessary. Even a constant value of 1 will do.
eval('fit f(x,y) filename_cod3D using 1:2:(strcol(4) eq "u"?1/0:$3):(1) via '.str_via)

set print path.'parameters/f(x,y).txt'
print '# This file contains the parameters for "z(x,y) = "'.str_f_it.'.'
set print path.'parameters/f(x,y).txt' append
do for  [i=0:n] {
	do for [j=0:m] {		
		# "parameter = value('parameter')"
		print sprintf('a%d%d = %e', i, j ,value(sprintf('a%d%d', i, j)))
	}
}
unset print

#########################
#	Fit	- 3D			#
#########################

str_f2_it = ''
str_via2 =''
do for  [i=0:k] {
	do for [j=0:l] {
		str_f2_it = str_f2_it.sprintf(' + b%d%d*x**%d*y**%d', i, j, i, j)
		str_via2 = str_via2.sprintf(',b%d%d',i,j)
		eval(sprintf('b%d%d = %e', i, j, 100.0**(-i-j)))
	}
}
str_f2_it = substr(str_f2_it, 4, strlen(str_f2_it))
str_via2 = substr(str_via2, 2, strlen(str_via2))
eval('f2(x,y)='.str_f2_it)
# Using a fourth column for the weight is necessary. Even a constant value of 1 will do.
eval('fit f2(x,y) filename_3D using 1:2:3:(1) via '.str_via2)

set print path.'parameters/f2(x,y).txt'
print '# This file contains the parameters for "z(x,y) = "'.str_f2_it.'.'
set print path.'parameters/f2(x,y).txt' append
do for  [i=0:k] {
	do for [j=0:l] {		
		# "parameter = value('parameter')"
		print sprintf('b%d%d = %e', i, j ,value(sprintf('b%d%d', i, j)))
	}
}
unset print