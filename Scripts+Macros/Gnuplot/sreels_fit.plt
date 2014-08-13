# File:		sreels_fit.plt
# Author:	Michael Entrup
# Version:	1.0.1
# Date:		28.07.2014

# Info:		This script is part of the Gnuplot_sreels package. The package is used to process the data that has been created by https://github.com/EFTEMj/EFTEMj/blob/master/Scripts%2BMacros/SR-EELS_char2acterisation.ijm.

#########################
#	Setup				#
#########################

if (!exists('path')) {
	print ''
	Print 'Please run "sreels_init" first.'
	print ''
}

# Order of the polynomial of the pseudo3D fit:
m = 2	# polynomial along the energy axis
n = 2	# polynomial that describes the variation of the parameters of the polynomial

# Order of the polynomial of the 3D fit:
k = 2	# polynomial along the energy axis
l = 2	# polynomial along the lateral axis

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
f_str(N, S1, S2) = sprintf('%s%d(x) = %s', S1, N, S2)
fit_str(N, S1, S2) = sprintf('fit %s%d(x) filename_%s(%d) via %s', S1, N, S1, N, S2)

do for [i=index_start:index_stop:index_inc] {
	do for [str in "T C B"] {
		str_f_it = ''
		str_via = ''
		do for [j=0:m] {
			str_f_it = str_f_it.sprintf(' + a%d%s%d*x**%d', j, str, i, j)
			str_via = str_via.sprintf(',a%d%s%d', j, str, i)
		}
		str_f_it = substr(str_f_it, 4, strlen(str_f_it))
		str_via = substr(str_via, 2, strlen(str_via))
		eval(f_str(i, str, str_f_it))
		eval(fit_str(i, str, str_via))
	}
}

do for [i=index_start:index_stop:index_inc] {
	do for [str in "T C B"] {
		# We want to overwrite the old file, that is why we don't use the option 'append' for the first line.
		set print path.'parameters/'.str.i.'.txt'
		print '# This file contains the parameters for a polynomial "y(x) = a0*x**0 + a1*x**1 + .. + am*x**m". with m='.m
		set print path.'parameters/'.str.i.'.txt' append
		do for [j=0:m] {
			# "parameter = value('parameter')"
			print sprintf('a%d%s%d = %e', j, str, i, value('a'.j.str.i))
		}		
	}
}
unset print

#########################
#	Fit - pseudo3D			#
#########################

# We don't want to show a plot or write to a file until the last call of 'replot'.
set terminal unknown

# As we only use 'replot' at a FOR-loop, we have to create an empty graph first.
splot NaN

plot_str_pseudo3D(N, S) = sprintf('replot filename_%s(%d) using ($1):(%s%d(2048)):($2) with points', S, N, S, N)
do for [i=index_start:index_stop:index_inc] {
	do for [str in "T C B"] {
		eval(plot_str_pseudo3D(i, str))
	}
}

# Now we set the table, we want to write to.
set table filename_pseudo3D
replot
unset table
set terminal wxt

str_f_it = ''
str_via =''
do for  [i=0:m] {
	do for [j=0:n] {
		str_f_it = str_f_it.sprintf(' + a%d%d*x**%d*y**%d', i, j, i, j)
		str_via = str_via.sprintf(',a%d%d',i,j)
		eval(sprintf('a%d%d = %e', i, j, 100.0**(-i-j)))
	}
}
str_f_it = substr(str_f_it, 4, strlen(str_f_it))
str_via = substr(str_via, 2, strlen(str_via))
eval('f(x,y)='.str_f_it)
# Using a fourth column for the weight is necessary. Even a constant value of 1 will do.
eval('fit f(x,y) filename_pseudo3D using 1:2:(strcol(4) eq "u"?1/0:$3):(1) via '.str_via)

set print path.'parameters/pseudo3D.txt'
print '# This file contains the parameters for "z(x,y) = "'.str_f_it.'.'
set print path.'parameters/pseudo3D.txt' append
do for  [i=0:m] {
	do for [j=0:n] {		
		# "parameter = value('parameter')"
		print sprintf('a%d%d = %e', i, j ,value(sprintf('a%d%d', i, j)))
	}
}
# We will print the same values to a file that is used as input for EFTEMj. We will append more values at the section "Fit - 3D".
set print path.'parameters/EFTEMj_input.txt'
print '# This file is used as input for the SR-EELS correction of EFTEMj.'
set print path.'parameters/EFTEMj_input.txt' append
do for  [i=0:m] {
	do for [j=0:n] {		
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

set print path.'parameters/3D.txt'
print '# This file contains the parameters for "z(x,y) = "'.str_f2_it.'.'
set print path.'parameters/3D.txt' append
do for  [i=0:k] {
	do for [j=0:l] {		
		# "parameter = value('parameter')"
		print sprintf('b%d%d = %e', i, j ,value(sprintf('b%d%d', i, j)))
	}
}
set print path.'parameters/EFTEMj_input.txt' append
do for  [i=0:k] {
	do for [j=0:l] {		
		# "parameter = value('parameter')"
		print sprintf('b%d%d = %e', i, j ,value(sprintf('b%d%d', i, j)))
	}
}
unset print