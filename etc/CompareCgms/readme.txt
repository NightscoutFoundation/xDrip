How to build/run
===============================
"C:\Program Files\Java\jdk1.8.0_05\bin\javac.exe" -classpath ./gson-2.6.2.jar;. CompareCgms.java
"C:\Program Files\Java\jdk1.8.0_05\bin\java.exe" -classpath ./gson-2.6.2.jar;./sqlite-jdbc-3.8.7.jar;.  CompareCgms

To plot a calibration graph of a specific sensor:
==================
gnuplot
set key autotitle columnhead
plot "Sensor25_11_201600_32.csv"

Or use Excell.


To plot the finger pricks data using gnuplot use:
========================================================

set timefmt "%d/%m/%Y %H:%M" 
set xdata time             

set xrange ["01/12/2016 00:00":"01/12/2016 04:00"]
set yrange[40:400]

plot "finger_pricks.csv" using 1:3, "xdrip_bg_values.csv" using 1:3 with lines, "libre_continus_values.csv" using 1:3 with lines