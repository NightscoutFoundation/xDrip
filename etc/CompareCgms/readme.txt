What is this program used to:
===================================

This program is used to compare finger pricks data to results of two CGMs:
1) xDrip that is using Dexcom G4 sensors. (Should likely work with other input sources)
2) Abbott libre (the official model).

In the future, and once I'll find a way to read the libre data directly, it can also be used to compare
the different algorithms that are used for converting sensor data to BG.

I have (tried to) write the program in order that adding different data should be relatively easy.
I guess that if this program will grow, we will need to split it to more files.

Libre data is composed from two types:
1) Manual scans that were done.
2) Automatic data that is saved by the sensor. It points are 15 minutes apart.
The main issue that I see here, is that abbott "changes the history" which means that they fix
the past data once they have more information. 
However, if one needs to decide at real time which bolus to give he does not have that information.
As a result, I try to compare finger pricks data to manual scan and not to automatic data wherever it
is possible.



One note about timing.
It is important to have all clocks synced to get good data.
From my experience this devices have a big drift and even if calibrated they will have a few minutes 
difference in a week or two. Try to keep them synced with real time.
In any case, the program allow for 3 minutes grace.

In this readme I use gnuplot for displaying data, obviously other programs such as excel can be used.

The input of the program:
==========================
Input is based on 3 files and one date:
The files are:
1) finger pricks data in the format that is created by abbott freestyle lite meter.
     From freestyle copilot program choose File->Export and choose csv format to create it.
2) xDrip database file.
     From xdrip-plus select export data base, and share the zip file.
3) Free style libre data.
     Use Freestyle libre program and choose file export data.

Since there might be a lot of data in this files, the date is used as the first date of comparison. (In
other words, data before this date is ignored).


The output of the program:
===================================

Output of the program is composed from the following parts:
1) Table of finger pricks compared to what the different CGMs show.
For example:

Finger Pricks                  xdrip results                    libre results
30/11/2016 02:10  223    189.6 3.5 (sensor age = 5.1)                     199.0 2.0
30/11/2016 06:50  183    198.6 3.5 (sensor age = 0.2)                    171.0 -3.0
01/12/2016 06:10  144    164.7 3.5 (sensor age = 1.2)      146.0 4.0  (interpulated)
07/12/2016 17:06  251        ------------------------      247.0 0.0  (interpulated)

What does this lines mean?
Well on "30/11/2016 02:10" the finger pricks data was 223.
xDrip has showed the value 189.6 data is based on data from 3.5 minutes ago. Libre was showing 199 data is 2 minutes ago.

On the second line we see for libre: "171.0 -3.0"
This means that the measurement was done 3 minutes after the finger pricks data, and I believe that it was done before
but there is clock differences.

On the third line we see "146.0 4.0  (interpulated)" this means that there was no manual measurment for libre, so
interpulated data is used.

On the forth line there is no data for xDrip. This is marked with "------------------------"

2) Histogram of the results based on the results above. We calculate the differences in percentage between
the fingers data and cgm data and create an histogram of it. For example:

xDrip histogram

total objects = 93.0
  < 10% = 25.806451612903224
  < 20% = 20.43010752688172
  < 30% = 22.580645161290324
  others 31.182795698924732


xLibre histogram

total objects = 94.0
  < 10% = 39.361702127659576
  < 20% = 25.53191489361702
  < 30% = 12.76595744680851
  others 22.340425531914892

This means that 25% of xDrip results were accurate (diff <10%)
                20% of xDrip results were somewhat accurate (10 < diff <20%)
                22% of xDrip results were not that accurate (<20 diff < 30%)
                31% of xDrip results were way off (diff > 30%)


Next the program creates the following files:
3) Files that describe the sensors calibration file. 
   The file name is Sensor+start data, for example: Sensor03_12_201601_52.csv
   Each of this files has 3 fields
     dexcom raw data, fingers bg, days from sensor start.
     For example:
     122.128, 108.0, 0.03564219907407407 
   In order to create a calibration graph using gnuplot use:
      set key autotitle columnhead
      plot "Sensor25_11_201600_32.csv"
   Hope you will have a straight line :-)

4) 3 files that contain the data of the different measurments methods. 
   Files are: finger_pricks.csv, libre_continus_values.csv, xdrip_bg_values.csv
   This program brings them to a unified format:
      Date Time, value.
   This makes it much easier to plot them now.
   For example using gnuplot run:
     set timefmt "%d/%m/%Y %H:%M" 
     set xdata time             

     set xrange ["01/12/2016 00:00":"01/12/2016 04:00"]
     set yrange[40:400]

     plot "finger_pricks.csv" using 1:3, "xdrip_bg_values.csv" using 1:3 with lines, "libre_continus_values.csv" using 1:3 with lines



How to build/run
===============================
compile:
  "C:\Program Files\Java\jdk1.8.0_05\bin\javac.exe" -classpath . CompareCgms.java
run:
  "C:\Program Files\Java\jdk1.8.0_05\bin\java.exe" -classpath ./sqlite-jdbc-3.8.7.jar;.  CompareCgms <abbott_fingers.txt> <date> <xdrip_db.sqlite> <libre_file.txt>

for example:
  "C:\Program Files\Java\jdk1.8.0_05\bin\java.exe" -classpath ./sqlite-jdbc-3.8.7.jar;.  CompareCgms c:\\temp\\fingers13_12_16.txt "17/11/2016 18:42" .\\export20161214-001727.sqlite c:\\temp\\snir_libre_13_12_2016.txt 

