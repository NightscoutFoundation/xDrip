import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Formatter;
import java.awt.SecondaryLoop;
import java.io.*;
import java.util.Collections;
import java.util.Comparator;
import java.text.ParseException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.GregorianCalendar;

/*
 * This program is used to compare 2 cgms to the finger pricking data.
 * It does the following:
 * 1) Read the data of the finger pricks. (time, val)
 * 2) Read the data of xDrip: (time, val, time from sensor start)
 * 3) Read the data of Libre (time, val, time from sensor start).
 * 
 * When doing the copmparision we will create a 3 strings structure.
 * One describing the measurment (for example time, value)
 * The second is comparing with dexcom: value, time from measurment (if signifcant) and time from sensor start.
 * The third is for comparing with lybre, same as for decxcom but will say if it is based on real measurment, or their interpulated data.
 * It then prints a table with the data.
 * 
 *  Due to the small size of this task, it is all in one file. Will be changed when it gets better.
 */

class FingerPricksData {
    long timeMs; // milly seconds
    double bg;

    FingerPricksData(long time, double bg) {
        this.timeMs = time;
        this.bg = bg;
    }
}

class CgmData {
    long timeMs;
    double bg;
    long msFromSensorStart;
    double rawValue;

    CgmData(long timeMs, double bg, long msFromSensorStart, double rawValue) {
        this.timeMs = timeMs;
        this.bg = bg;
        this.msFromSensorStart = msFromSensorStart;
        this.rawValue = rawValue;
    }
}

class Sensor {

    Sensor(long started_at, long stopped_at, String uuid, int id) {
        this.started_at = started_at;
        this.stopped_at = stopped_at;
        this.uuid = uuid;
        this.id = id;
        double hours = (stopped_at - started_at) / 60000 / 60;
        days = hours / 24;
    }

    public String toString() {
        double hours = (stopped_at - started_at) / 60000 / 60;
        days = hours / 24;
        DecimalFormat df = new DecimalFormat("#.00");

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        /*
         * return "ID         : " + id + "\nUUID       : " + uuid+
         * "\nStart date : " + dateFormat.format(started_at) + "\nEnd date   : "
         * + dateFormat.format(stopped_at) + "\nDays       : " +
         * df.format(days);
         */
        return "Start date : " + dateFormat.format(started_at) + " End date   : " + dateFormat.format(stopped_at)
                + " Days       : " + df.format(days);
    }

    long started_at;
    long stopped_at;
    String uuid;
    int id;
    double days;
}

enum LibreReading {
    CONTINUS(0), MANUAL(1);

    LibreReading(int val) {
        value = val;
    }

    public int getValue() {
        return value;
    }

    private int value;
}

class SympleHystograme {
    float total;
    int lessThan10;
    int lessThan20;
    int lessThan30;
    int others;
    
    void addValue(double finger, double cgm) {
        total++;
        double ratio = (double)cgm / finger;
        if (ratio < 1.1 && ratio > 0.9) {
            lessThan10++;
            return;
        }
        if (ratio < 1.2 && ratio > 0.8) {
            lessThan20++;
            return;
        }
        if (ratio < 1.3 && ratio > 0.7) {
            lessThan30++;
            return;
        }
        others++;
    }
        
    void Print() {
        System.out.println("total objects = " + total);
        System.out.println("  < 10% = " + lessThan10 * 100.0 / total);
        System.out.println("  < 20% = " + lessThan20 * 100.0 / total);
        System.out.println("  < 30% = " + lessThan30 * 100.0 / total);
        System.out.println("  others " + others * 100.0 / total);
    }
        
}

class SensorCalibrationTableWriter {
    private void openFile(long timeMs) throws IOException {
        String fileName = "Sensor" + df.format(new Date(timeMs)) + ".csv";
        writer = new FileWriter(fileName);
        StringBuilder sb = new StringBuilder();
        sb.append("dexcom raw data, fingers bg, days from sensor start \r\n");
        writer.append(sb.toString());
        
    }
    
    private void openFileIfNeeded(long timeMs, long timeFromSensorStart) throws IOException {
        if (writer == null) {
            openFile(timeMs);
        } else if (timeFromSensorStart < lastTimeFromSensorStart) {
            // This meachnisim is not garenteed to work, but will work on reasnable values.
            Flush();
            openFile(timeMs);
        }
        lastTimeFromSensorStart = timeFromSensorStart;
        
    }
    
    void writeEntry(double raw, double bgValue, long timeMs, long timeFromSensorStart) throws IOException{
        openFileIfNeeded(timeMs, timeFromSensorStart);
        StringBuilder sb = new StringBuilder();
        sb.append("" + raw + ", "+ bgValue + ", "+ (timeFromSensorStart / 1000.0 / 3600/ 24)+ " \r\n");
        writer.append(sb.toString());
    }
    
    void Flush() throws IOException {
        writer.flush();
        writer.close();
        writer = null;
    }
    
    FileWriter writer = null;
    long lastTimeFromSensorStart = 0;
    static java.text.DateFormat df = new SimpleDateFormat("dd_MM_yyyyHH_mm");
}



class CompareCgms {

    static java.text.DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    static TimeZone tz = TimeZone.getDefault();
    final static String NO_DATA = "------------------------";

    public static void main(String[] args) throws Exception {

        List<CgmData> libreManual = readLibre("c:\\temp\\libre.txt", "17/11/2016 18:42", LibreReading.MANUAL, null);
        List<CgmData> libreContinus = readLibre("c:\\temp\\libre.txt", "17/11/2016 18:42", LibreReading.CONTINUS, null);

        List<FingerPricksData> fpData = readFreeStyleFingerPricks("c:\\temp\\fingers.txt");

        List<Sensor> sensors = ReadSensors("C:\\Users\\nirit\\Downloads\\export20161207-003506-follower.sqlite");
        List<CgmData> xDripBgReadings = readxDripBgReadings(
                "C:\\Users\\nirit\\Downloads\\export20161207-003506-follower.sqlite", "17/11/2016 18:42", sensors);

        // Now we have all the data, let's print it...
        printResults(fpData, xDripBgReadings, libreManual, libreContinus);

    }

    static void printResults(List<FingerPricksData> fpDataList, List<CgmData> xDripBgReadings,
            List<CgmData> libreManual, List<CgmData> libreContinus) throws IOException{
        
        SensorCalibrationTableWriter sensorCalibrationTableWriter = new SensorCalibrationTableWriter();

        SympleHystograme xDripHistogram = new SympleHystograme();
        SympleHystograme libreHistogram = new SympleHystograme();
        
        System.out.println("Final results");
        System.out.println("Finger Pricks                  xdrip results                    libre results");

        for (FingerPricksData fingerPricksData : fpDataList) {

            // Create the xdrip data if needed
            String xDrip = CreateXdripResults(fingerPricksData, xDripBgReadings, xDripHistogram, sensorCalibrationTableWriter);

            // Create the libre data if needed
            String libre = CreateLibreResults(fingerPricksData, libreManual, libreContinus, libreHistogram);

            System.out.printf("%s %4.0f  %30s %30s\n", df.format(new Date(fingerPricksData.timeMs)),
                    (float) fingerPricksData.bg, xDrip, libre);
        }
        
        System.out.println("xDrip histogram\n");
        xDripHistogram.Print();
        
        System.out.println("\n\nxLibre histogram\n");
        libreHistogram.Print();
        sensorCalibrationTableWriter.Flush();

    }

    // Prepare a string that represents what xDrip has to say about the given
    // fingerprick.
    // Xdrip will look for it's closest reading and create it's result based on
    // what it has found.
    static String CreateXdripResults(FingerPricksData fpData, List<CgmData> xDripBgReadings, SympleHystograme xDripHistogram, SensorCalibrationTableWriter sensorCalibrationTableWriter) throws IOException {
        CgmData xDripPoint = getClosestPrecidingReading(xDripBgReadings, fpData.timeMs);

        if (xDripPoint == null) {
            return NO_DATA;
        }
        double xDripTimeDiffMinutes = (fpData.timeMs - xDripPoint.timeMs) / 60000.0;
        if (xDripTimeDiffMinutes < 15) {
            sensorCalibrationTableWriter.writeEntry(xDripPoint.rawValue, fpData.bg, fpData.timeMs, xDripPoint.msFromSensorStart);
            xDripHistogram.addValue(fpData.bg, xDripPoint.bg);
            
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb);
            formatter.format(" %6.1f %1.1f (sensor age = %1.1f)", xDripPoint.bg, xDripTimeDiffMinutes,
                    (float) xDripPoint.msFromSensorStart / 24 / 3600 / 1000);
            return sb.toString();
        } else {
            return NO_DATA;
        }
    }

    static boolean isTooFar(FingerPricksData fpData, CgmData cgmData) {
        double libreTimeDiffMinutes = (fpData.timeMs - cgmData.timeMs) / 60000.0;

        if (libreTimeDiffMinutes < 0) {
            System.err.printf("Point is too far away...");
        }

        return libreTimeDiffMinutes > 15;
    }

    // Prepare a string that represents what xDrip has to say about the given
    // fingerprick.
    // Xdrip will look for it's closest reading and create it's result based on
    // what it has found.
    static String CreateLibreResults(FingerPricksData fpData, List<CgmData> libreManual, List<CgmData> libreContinus, SympleHystograme libreHistogram) {
        CgmData librePoint = getClosestPrecidingReading(libreManual, fpData.timeMs);
        String description = "";
        if (librePoint == null || isTooFar(fpData, librePoint)) {
            librePoint = getClosestPrecidingReading(libreContinus, fpData.timeMs);
            if (librePoint == null || isTooFar(fpData, librePoint)) {
                return NO_DATA;
            }
            description = " (interpulated)";
        }
        double libreTimeDiffMinutes = (fpData.timeMs - librePoint.timeMs) / 60000.0;
        if (libreTimeDiffMinutes < 15) {
            libreHistogram.addValue(fpData.bg, librePoint.bg);
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb);
            formatter.format(" %6.1f %1.1f %s", librePoint.bg, libreTimeDiffMinutes, description);
            return sb.toString();
        } else {
            return NO_DATA;
        }
    }

    // Find the closest CgmData data that was before the measurment. (This is
    // the data that the user had
    // when he decided to measure.
    static CgmData getClosestPrecidingReading(List<CgmData> cgmDataList, long time) {
        if (cgmDataList == null) {
            return null;
        }
        // System.out.println("Looking for " + df.format(new Date(time)));
        ListIterator<CgmData> li = cgmDataList.listIterator(cgmDataList.size());
        // Iterate in reverse.
        while (li.hasPrevious()) {
            CgmData cgmData = li.previous();
            // System.out.println("Checking object with time " + df.format(new
            // Date(cgmData.timeMs)));
            if (cgmData.timeMs < time) {
                // We have found the first data before our data, return it.
                // System.out.println("found ??????????????????????????");
                return cgmData;
            }
        }
        // System.out.println("not found ??????????????????????????");
        return null;
    }

    // Read finger pricks data
    static List<FingerPricksData> readFreeStyleFingerPricks(String FileName) throws IOException {
        // Format of the file is:
        // DATEEVENT TIMESLOT EVENTTYPE DEVICE_MODEL DEVICE_ID
        // VENDOR_EVENT_TYPE_ID VENDOR_EVENT_ID KEY0
        // 42703.9444444444 6 1 Abbott BG Meter DCGT224-N2602 0 189 0 0 0 189
        // 42703.8416666667 5 1 Abbott BG Meter DCGT224-N2602 0 116 0 0 0 116

        List<FingerPricksData> fpData = new ArrayList<FingerPricksData>();

        FileInputStream fis = new FileInputStream(FileName);

        // Construct BufferedReader from InputStreamReader
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));

        String line = null;
        while ((line = br.readLine()) != null) {
            // System.out.println(line);
            String[] splited = line.split("\\s+");
            if (splited[0].equals("DATEEVENT")) {
                continue;
            }
            long time = EpochFrom1900(Double.parseDouble(splited[0]));
            double bgVal = Integer.parseInt(splited[8]);
            //System.out.println("finger pricks: " + df.format(new Date(time)) + " " + bgVal);

            fpData.add(0, new FingerPricksData(time, bgVal));
        }

        br.close();

        // Sort the list by time
        Collections.sort(fpData, new Comparator<FingerPricksData>() {
            @Override
            public int compare(final FingerPricksData object1, final FingerPricksData object2) {
                return (int) (object1.timeMs - object2.timeMs);
            }
        });

        return fpData;

    }

    // Read the libre Sensors (I still did not see a sensor change, so I don't
    // know how...)

    // Read the libre data
    static List<CgmData> readLibre(String FileName, String startTime, LibreReading libreReading, List<Sensor> sensors)
            throws IOException {
        // Format of the file is:
        // 87 2016/11/27 18:42 1 207
        // 90 2016/11/27 18:56 1 183
        // 92 2016/11/27 18:42 0 209

        List<CgmData> CgmDataList = new ArrayList<CgmData>();

        FileInputStream fis = new FileInputStream(FileName);

        // Construct BufferedReader from InputStreamReader
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        java.text.DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");

        String line = null;
        int i = 0;
        while ((line = br.readLine()) != null) {
            // 2 fist lines are not data
            if (i < 2) {
                i++;
                continue;
            }
            i++;

            // System.out.println(line);
            String[] splited = line.split("\\s+");

            int lineType = Integer.parseInt(splited[3]);
            if (lineType != libreReading.getValue()) {
                continue;
            }
            java.util.Date date = null;
            try {
                date = df.parse(splited[1] + " " + splited[2]); //
            } catch (ParseException e) {
                System.err.println("Error parsing date/time from libre file " + splited[1] + " " + splited[2]);
                System.exit(2);
            }

            double bgVal = Integer.parseInt(splited[4]);
            //System.out.println("libre data is " + df.format(date) + " " + bgVal);

            CgmDataList.add(new CgmData(date.getTime(), bgVal, 0, 0));
        }

        br.close();
        // sort this data
        return CgmDataList;

    }

    static long EpochFrom1900(double time1900) {
        // typical format is 42703.8416666667 which is number of days from 1900
        // and our place in the days.
        long days = (long) time1900;
        long timeSeconds = Math.round(time1900 * 24 * 3600 - 70.0 * 365 * 24 * 3600);
        // We know that 29/11/2016 22:39 == 42703.9444444444 so (does this mean
        // that there have been 19 years with februar having 29 days from 1900
        // to 1970?)
        timeSeconds -= 19 * 24 * 3600;

        // TODO: Not sure that it will work in the time that the day saving is
        // changing.
        // Don't know if this can be solved at all since the finger pricks
        // device does not have a timezone or
        // day saving notations.
        timeSeconds -= tz.getOffset(timeSeconds * 1000) / 1000;

        // System.out.println("time " + df.format(new Date(timeSeconds *
        // 1000)));
        return timeSeconds * 1000;
    }

    // Read the sensors start time data
    public static List<Sensor> ReadSensors(String dbName) {
        Connection c = null;
        Statement stmt = null;
        List<Sensor> Sensors = new ArrayList<Sensor>();
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + dbName);
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM SENSORS ORDER BY _id;");
            while (rs.next()) {
                int id = rs.getInt("_id");
                String uuid = rs.getString("uuid");
                long started_at = (long) rs.getDouble("started_at");
                long stopped_at = (long) rs.getDouble("stopped_at");
                // System.out.println("ID = " + id);
                // System.out.println("started_at = " + started_at);
                System.out.println();
                Sensor sensor = new Sensor(started_at, stopped_at, uuid, id);
                Sensors.add(sensor);
            }
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Sensors read successfully");
        return Sensors;
    }

    // Read the xDrip bg data
    public static List<CgmData> readxDripBgReadings(String dbName, String startTime, List<Sensor> sensors) {
        List<CgmData> cgmDataList = new ArrayList<CgmData>();
        java.util.Date startDate = null;
        try {
            startDate = df.parse(startTime); //
        } catch (ParseException e) {
            System.err.println("Error parsing date/time");
            System.exit(2);
        }

        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + dbName);
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM BGREADINGS ORDER BY timestamp;");
            while (rs.next()) {
                double calculated = rs.getDouble("calculated_value");
                long timestamp = (long) rs.getDouble("timestamp");
                double rawValue = rs.getDouble("raw_data");

                Date date = new Date(timestamp);
                // TODO move this to the sql command
                if (startDate.before(date)) {
                    String dateStr = df.format(date);
                    // System.out.println(dateStr + ", " + calculated);
                    
                    cgmDataList.add(new CgmData(timestamp, calculated, bgReadingStartSensorTime(timestamp, sensors), rawValue));
                }

            }
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("xDrip bg data read successfully");
        return cgmDataList;
    }

    // Calculate the time from the start of the sensor to this reading
    static long bgReadingStartSensorTime(long bgReadingTime, List<Sensor> sensors) {
        // go over the sensors from their end and find the first one that
        // started before us.
        ListIterator<Sensor> li = sensors.listIterator(sensors.size());

        // Iterate in reverse.
        while (li.hasPrevious()) {
            Sensor sensor = li.previous();
            if (bgReadingTime > sensor.started_at) {
                // This is our sensor
                // System.out.println("bgreading is " + ((double)(bgReadingTime
                // - sensor.started_at )/ 24 /3600 / 1000) + " days from sensor
                // start");
                return bgReadingTime - sensor.started_at;
            }

        }
        // Not found, this is a bug
        return 0;

    }

}