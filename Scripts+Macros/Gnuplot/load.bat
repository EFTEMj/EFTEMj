@echo off
:: With this option you can use ! instead of % to expand variables at runtime. We need this at our for loops.
Setlocal EnableDelayedExpansion
:: Count (/C) the number of Folders (/AD) that begin with "SpecMag". /B reduces the result of dir to folder names only.
dir /AD /B | find /C "SpecMag" > NUL
:: If there is no folder beginning with "SpecMag" find will report the error level 1.
IF %ERRORLEVEL% EQU 1 goto :New

:SpecMag
:: Create a list of all available SpecMag values.
echo Choose an option:
echo new	read new dataset
:: With /D only directories are considered.
for /D %%F in (SpecMag*) do (
	set var=%%F
	:: Create a sub-string, that misses the first 7 chars.
	echo !var:~7!	%%F
)
echo 0	cancel
echo.
set /P selection_sm=SpecMag: 
echo.

IF %selection_sm% EQU 0 goto :Cancel
IF %selection_sm% EQU new goto :New
IF NOT exist SpecMag%selection_sm% goto :SpecMag

:QSinK7
:: Create a list of all available QSinK7 values of the previously selected SpecMag.
echo Choose an option:
set index=0
for /D %%F in (SpecMag%selection_sm%\*) do (
	set var=%%F
	:: With /A you can evaluate a numerical expression.
	set /A index+=1
	echo !index!	!var!
)
echo b	back to SpecMag selection
echo 0	cancel
echo.
set /P selection_k7=QSinK7: 
echo.

IF %selection_k7% EQU 0 goto :Cancel
IF %selection_k7% EQU b goto :SpecMag

:: The next loop is to determine the related path of the selected index
set index=0
for /D %%F in (SpecMag%selection_sm%\*) do (
	set /A index+=1
	IF !index! == %selection_k7% (
		set data_path=%%F
	)
)

IF %selection_k7% GTR %index% (
	goto :QSinK7
)

:: The file 'sub_init.plt' will tell Gnuplot to load an existing dataset.
echo load_prev = 1 > sub_init.plt
:: Replace \ by /. Gnuplot can only handle the Unix style for paths.
echo load_path = '%data_path:\=/%/' >> sub_init.plt

goto :end
:New
:: The file 'sub_init.plt' will tell Gnuplot to create a new dataset.
echo load_prev = 0 > sub_init.plt
goto :eof
:Cancel
:: The file 'sub_init.plt' will tell Gnuplot that the script has been cancelled.
echo load_prev = -1 > sub_init.plt
goto :eof
:end