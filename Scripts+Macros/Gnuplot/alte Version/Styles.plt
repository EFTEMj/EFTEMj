rgb(r,g,b) = 65536 * int(r) + 256 * int(g) + int(b)
line_width = 2

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

set style increment userstyles

set term svg enhanced size 1024,768 dynamic enhanced font 'Verdana,12'
set output "Styles.svg"
set sample 100
set xrange [0:pi]
set key outside right center
plot for [i=1:14:1] sin(x+i*2*pi/14) title sprintf("style %i",i) with lines
