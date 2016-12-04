import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.awt.SecondaryLoop;
import java.io.*;
import java.text.ParseException;

/*
 * This program is used to compare 2 cgms to the finger pricking data.
 * It does the following:
 * 1) Read the data of the finger pricks. (time, val)
 * 2) Read the data of xDrip: (time, val, time from sensor start)
 * 3) Read the data of Libre (time, val, time from sensor start).
 * It then prints a table with the data.
 * 
 *  Due to the small size of this task, it is all in one file. Will be changed when it gets better.
 */


class FingerPricksData {
    long timeMs; // milly seconds
    double bg;
    
    FingerPricksData(long time, double bg) {
        this.timeMs = timeMs;
        this.bg = bg;
    }
}


class CgmData {
    long timeMs;
    double bg;
    long msFromSensorStart;
    
    CgmData(long timeMs, double bg, long msFromSensorStart) {
        this.timeMs = timeMs;
        this.bg = bg;
        this.msFromSensorStart = msFromSensorStart;
    }
}

class Sensor {

    Sensor (long started_at, long stopped_at, String uuid, int id) {
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
        return   "ID         : " + id +
                "\nUUID       : " + uuid+
                "\nStart date : " + dateFormat.format(started_at) +
                "\nEnd date   : " + dateFormat.format(stopped_at) +
                "\nDays       : " + df.format(days);
*/      
        return "Start date : " + dateFormat.format(started_at) +
                " End date   : " + dateFormat.format(stopped_at) +
                " Days       : " + df.format(days);
    }

    long started_at;
    long stopped_at;
    String uuid;
    int id;
    double days;
}

class CompareCgms {
    
    static java.text.DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    
    public static void main(String[] args) throws Exception {
        readFreeStyleFingerPricks("c:\\temp\\fingers.txt");
        List<Sensor> sensors = ReadSensors("..\\..\\..\\BgAlgorithm\\export20161129-012233.sqlite" );
        readxDripBgReadings("..\\..\\..\\BgAlgorithm\\export20161129-012233.sqlite", "27/11/2016 18:42", sensors);
    }
    
    
    
    // Read finger pricks data
    static List<FingerPricksData> readFreeStyleFingerPricks(String FileName) throws IOException {
        // Format of the file is:
        // DATEEVENT    TIMESLOT    EVENTTYPE   DEVICE_MODEL    DEVICE_ID   VENDOR_EVENT_TYPE_ID    VENDOR_EVENT_ID KEY0
        // 42703.9444444444 6   1   Abbott BG Meter DCGT224-N2602   0       189 0   0   0   189
        // 42703.8416666667 5   1   Abbott BG Meter DCGT224-N2602   0       116 0   0   0   116
        
        List<FingerPricksData> fpData = new ArrayList<FingerPricksData>();
        
        FileInputStream fis = new FileInputStream(FileName);
        
        //Construct BufferedReader from InputStreamReader
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
     
        String line = null;
        while ((line = br.readLine()) != null) {
            //System.out.println(line);
            String[] splited = line.split("\\s+");
            if(splited[0].equals("DATEEVENT")) {
                continue;
            }
            long time = EpochFrom1900(Double.parseDouble(splited[0]));
            double bgVal = Integer.parseInt(splited[8]);
            System.out.println("data is " + df.format(new Date(time))  +" " + bgVal );
            
            fpData.add(new FingerPricksData(time, bgVal));
        }
     
        br.close();
        
        return fpData;
        
    }
    
    
    static long EpochFrom1900 (double time1900) {
        // typical format is 42703.8416666667 which is number of days from 1900 and our place in the days.
        long days = (long) time1900;
        long timeSeconds =  Math.round(time1900 * 24 * 3600 - 70.0 * 365 * 24 * 3600);
        // We know that 29/11/2016 22:39 ==  42703.9444444444 so (does this mean that there have been 19 years with februar having 29 days from 1900 to 1970?)
        timeSeconds -= 19 * 24 * 3600;
        
        // This is most problematic, but sine my timezone is gmt+2.0 I remove 2 hours.
        // TODO: Needs more work
        timeSeconds -= 2 * 3600;
        
        java.text.DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        //System.out.println("time " + df.format(new Date(timeSeconds * 1000)));
        return (long) timeSeconds * 1000;
    }
    
    
    // Read the sensors start time data
    public static List<Sensor> ReadSensors(String dbName )
    {
        Connection c = null;
        Statement stmt = null;
        List<Sensor> Sensors = new ArrayList <Sensor>();
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + dbName);
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery( "SELECT * FROM SENSORS ORDER BY _id;" );
            while ( rs.next() ) {
                int id = rs.getInt("_id");
                String  uuid = rs.getString("uuid");
                long started_at= (long)rs.getDouble("started_at");
                long stopped_at= (long)rs.getDouble("stopped_at");
                System.out.println( "ID = " + id );
                System.out.println( "started_at = " + started_at );
                System.out.println();
                Sensor sensor = new Sensor(started_at, stopped_at, uuid, id);
                Sensors.add(sensor);
            }
            rs.close();
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Sensors read successfully");
        return Sensors;
    }
    
    // Read the xDrip bg data 
    public static List<CgmData> readxDripBgReadings(String dbName, String startTime, List<Sensor> sensors )
    {
        List<CgmData> cgmData = new ArrayList<CgmData>();
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
            ResultSet rs = stmt.executeQuery( "SELECT * FROM BGREADINGS ORDER BY timestamp;" );
            while ( rs.next() ) {
                double calculated = rs.getDouble("calculated_value");
                long timestamp = (long)rs.getDouble("timestamp");
                
                Date date = new Date(timestamp);
                // TODO move this to the sql command
                if(startDate.before(date)) {
                    String dateStr = df.format(date);
                    System.out.println(dateStr + ", " + calculated );
                    cgmData.add(new CgmData(timestamp, calculated, bgReadingStartSensorTime(timestamp, sensors)));
                    ;
                }

            }
            rs.close();
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("xDrip bg data read successfully");
        return cgmData;
    }
    
    // Calculate the time from the start of the sensor to this reading
    static long bgReadingStartSensorTime(long bgReadingTime, List<Sensor> sensors) {
        // go over the sensors from their end and find the first one that started before us.
        ListIterator<Sensor> li = sensors.listIterator(sensors.size());

        // Iterate in reverse.
        while(li.hasPrevious()) {
            Sensor sensor = li.previous();
            System.out.println(sensor);
            if(bgReadingTime > sensor.started_at ) {
                // This is our sensor
                //cgmData.msFromSensorStart = cgmData.timeMs - sensor.started_at;
                System.out.println("bgreading is " + ((double)(bgReadingTime - sensor.started_at )/ 24 /3600 / 1000) +  " days from sensor start");
                return  bgReadingTime - sensor.started_at;
            }
            
        }
        // Not found, this is a bug
        return 0;
        
    }
    
    
}