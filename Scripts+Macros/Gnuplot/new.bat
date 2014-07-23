@echo off
echo Please enter the name of the data files (replace the index by %%d):
set /p file=
echo Please enter the number of data files:
set /p N=
echo Please enter the used binning:
set /p  binning=
echo Please enter the used SpecMag value:
set /p SpecMag=
echo Please enter the used QSinK7 value:
set /p QSinK7=
set out=from_shell.plt
echo filename_input(n) = sprintf("%file%", n) > %out%
echo N = %N% >> %out%
echo binning = %binning% >> %out%
echo SpecMag = "%SpecMag%" >> %out%
echo QSinK7 = "%QSinK7%" >> %out%