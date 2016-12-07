How to build/run
===============================
"C:\Program Files\Java\jdk1.8.0_05\bin\javac.exe" -classpath ./gson-2.6.2.jar;. CompareCgms.java
"C:\Program Files\Java\jdk1.8.0_05\bin\java.exe" -classpath ./gson-2.6.2.jar;./sqlite-jdbc-3.8.7.jar;.  CompareCgms

To plot a sensor:
==================
gnuplot
set key autotitle columnhead
plot "Sensor25_11_201600_32.csv"

Or use Excell.