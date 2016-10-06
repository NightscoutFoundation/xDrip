package com.eveningoutpost.dexdrip.Services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.MapsActivity;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.ParakeetHelper;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.utils.BgToSpeech;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;


// Important note, this class is based on the fact that android will always run it one thread, which means it does not
// need synchronization

public class WixelReader extends AsyncTask<String, Void, Void > {

    private final static String TAG = WixelReader.class.getName();
    private static BgToSpeech bgToSpeech;

    private static OkHttpClient httpClient = null;

    private final Context mContext;
    PowerManager.WakeLock wakeLock;

    private final static long DEXCOM_PERIOD=300000;
    
    private static int lockCounter = 0;
    
    // This variables are for fake function only
    static int i = 0;
    static int added = 5;

    public WixelReader(Context ctx) {
        mContext = ctx.getApplicationContext();
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WifiReader");
        Log.d(TAG,"WixelReader init");
    }

    private void getwakelock()
    {
        wakeLock.acquire();
        lockCounter++;
        Log.d(TAG,"wakelock acquired " + lockCounter);
        if (lockCounter>5) Home.toaststaticnext("Wixel Reader WakeLock bug "+lockCounter);
    }

    public static boolean IsConfigured(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String recieversIpAddresses = prefs.getString("wifi_recievers_addresses", "");
        if(recieversIpAddresses == null || recieversIpAddresses.equals("") ) {
            return false;
        }
        return true;
    }

    public static boolean almostEquals( TransmitterRawData e1, TransmitterRawData e2)
    {
        if (e1 == null || e2==null) {
            return false;
        }
        // relative time is in ms
        if ((Math.abs(e1.CaptureDateTime - e2.CaptureDateTime) < 120 * 1000 ) &&
                (e1.TransmissionId == e2.TransmissionId)) {
            return true;
        }
        return false;
    }

 // last in the array, is first in time
    public static List<TransmitterRawData> Merge2Lists(List<TransmitterRawData> list1 , List<TransmitterRawData> list2)
    {
        List<TransmitterRawData> merged = new LinkedList <TransmitterRawData>();
        while (true) {
            if(list1.size() == 0 && list2.size() == 0) {
                break;
            }
            if (list1.size() == 0) {
                merged.addAll(list2);
                break;
            }
            if (list2.size() == 0) {
                merged.addAll(list1);
                break;
            }
            if (almostEquals(list1.get(0), list2.get(0))) {
                list2.remove(0);
                merged.add(list1.remove(0));
                continue;
            }
            if(list1.get(0).RelativeTime > list2.get(0).RelativeTime) {
                merged.add(list1.remove(0));
            } else {
                merged.add(list2.remove(0));
            }

        }
        return merged;
    }

    public static List<TransmitterRawData> MergeLists(List <List<TransmitterRawData>> allTransmitterRawData)
    {
        List<TransmitterRawData> MergedList;
        MergedList = allTransmitterRawData.remove(0);
        for (List<TransmitterRawData> it : allTransmitterRawData) {
            MergedList = Merge2Lists(MergedList, it);
        }

        return MergedList;
    }

    public static List<TransmitterRawData> ReadHost(String hostAndIp, int numberOfRecords)
    {
        int port;
        System.out.println("Reading From " + hostAndIp);
        Log.i(TAG,"Reading From " + hostAndIp);
        String []hosts = hostAndIp.split(":");
        if(hosts.length != 2) {
            System.out.println("Invalid hostAndIp " + hostAndIp);
            Log.e(TAG, "Invalid hostAndIp " + hostAndIp);

            return null;
        }
        try {
            port = Integer.parseInt(hosts[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("Invalid port " +hosts[1]);
            Log.e(TAG, "Invalid hostAndIp " + hostAndIp, nfe);
            return null;

        }
        if (port < 10 || port > 65536) {
            System.out.println("Invalid port " +hosts[1]);
            Log.e(TAG, "Invalid hostAndIp " + hostAndIp);
            return null;

        }
        System.out.println("Reading from " + hosts[0] + " " + port);
        List<TransmitterRawData> ret;
        try {
            ret = Read(hosts[0], port, numberOfRecords);
        } catch(Exception e) {
            // We had some error, need to move on...
            System.out.println("read from host failed cought expation" + hostAndIp);
            Log.e(TAG, "read from host failed " + hostAndIp, e);

            return null;

        }
        return ret;
    }

    public static List<TransmitterRawData> ReadFromMongo(String dbury, int numberOfRecords)
    {
        Log.i(TAG, "Reading From " + dbury);
    	List<TransmitterRawData> tmpList;
    	// format is dburi/db/collection. We need to find the collection and strip it from the dburi.
    	int indexOfSlash = dbury.lastIndexOf('/');
    	if(indexOfSlash == -1) {
    		// We can not find a collection name
    		Log.e(TAG, "Error bad dburi. Did not find a collection name starting with / " + dbury);
    		// in order for the user to understand that there is a problem, we return null
    		return null;

    	}
    	String collection = dbury.substring(indexOfSlash + 1);
    	dbury = dbury.substring(0, indexOfSlash);

    	// Make sure that we have another /, since this is used in the constructor.
    	indexOfSlash = dbury.lastIndexOf('/');
    	if(indexOfSlash == -1) {
    		// We can not find a collection name
    		Log.e(TAG, "Error bad dburi. Did not find a collection name starting with / " + dbury);
    		// in order for the user to understand that there is a problem, we return null
    		return null;
    	}

    	MongoWrapper mt = new MongoWrapper(dbury, collection, "CaptureDateTime", "MachineNameNotUsed");
    	return mt.ReadFromMongo(numberOfRecords);
    }




    // read from http source like cloud hosted parakeet receiver.cgi / json.get
    public static List<TransmitterRawData> readHttpJson(String url, int numberOfRecords) {
        List<TransmitterRawData> trd_list = new LinkedList<TransmitterRawData>();
        int processNumberOfRecords = numberOfRecords;
        // get more records to ensure we can handle coexistence of parakeet and usb-python-wixel
        // TODO make this work on preference option for the feature
        if (true) numberOfRecords = numberOfRecords + 1;

        try {

            if (httpClient == null) {
                httpClient = new OkHttpClient();
                // suitable for GPRS
                httpClient.setConnectTimeout(30, TimeUnit.SECONDS);
                httpClient.setReadTimeout(60, TimeUnit.SECONDS);
                httpClient.setWriteTimeout(20, TimeUnit.SECONDS);
            }

            Gson gson = new GsonBuilder().create();

            // simple HTTP GET request
            // n=numberOfRecords for backfilling
            // r=sequence number to avoid any cache
            // expecting json reply like the standard json server in dexterity / python pi usb / parakeet
            Request request = new Request.Builder()

                    // Mozilla header facilitates compression
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Connection", "close")
                    .url(url + "?n=" + Integer.toString(numberOfRecords)
                            + "&r=" + Long.toString((System.currentTimeMillis() / 1000) % 9999999))
                    .build();

            Response response = httpClient.newCall(request).execute();
            // if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            if (response.isSuccessful()) {

                String lines[] = response.body().string().split("\\r?\\n");

                for (String data : lines) {

                    if (data == null) {
                        System.out.println("received null continuing");
                        continue;
                    }
                    if (data.equals("")) {
                        System.out.println("received \"\" continuing");
                        continue;
                    }

                    TransmitterRawData trd = gson.fromJson(data, TransmitterRawData.class);
                    trd.CaptureDateTime = System.currentTimeMillis() - trd.RelativeTime;

                    // Versions of the Python USB script after 20th May 2016 will
                    // submit a bogus geolocation in the middle of the ocean to differentiate
                    // themselves from actual parakeet data even though both can coexist on the
                    // parakeet web service.

                    ParakeetHelper.checkParakeetNotifications(trd.CaptureDateTime, trd.GeoLocation);
                    if ((trd.GeoLocation != null)) {
                        if (!trd.GeoLocation.equals("-15,-15")) {
                            try {
                                MapsActivity.newMapLocation(trd.GeoLocation, trd.CaptureDateTime);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception with maps activity: " + e.toString());
                            }
                        } else {
                            // look a little further if we see usb-wixel data on parakeet app engine
                            processNumberOfRecords = numberOfRecords + 1;
                        }
                    }
                    trd_list.add(0, trd);
                    //  System.out.println( trd.toTableString());
                    if (trd_list.size() == processNumberOfRecords) {
                        // We have the data we want, let's get out
                        break;
                    }
                }

                Log.i(TAG, "Success getting http json with end size: " + Integer.toString(trd_list.size()));
            }

        } catch (Exception e) {
            Log.e(TAG, "caught Exception in reading http json data " + e.toString());
        }
        return trd_list;
    }

    // format of string is ip1:port1,ip2:port2;
    public static TransmitterRawData[] Read(String hostsNames, int numberOfRecords)
    {
        String []hosts = hostsNames.split(",");
        if(hosts.length == 0) {
            Log.e(TAG, "Error no hosts were found " + hostsNames);
            return null;
        }
        List <List<TransmitterRawData>> allTransmitterRawData =  new LinkedList <List<TransmitterRawData>>();

        // go over all hosts and read data from them
        for(String host : hosts) {
            host = host.trim();
            List<TransmitterRawData> tmpList;
            if (host.startsWith("mongodb://")) {
            	tmpList = ReadFromMongo(host ,numberOfRecords);
            } else if ((host.startsWith("http://") || host.startsWith("https://"))
                    && host.contains("/json.get")) {
                tmpList = readHttpJson(host, numberOfRecords);
            } else {
            	tmpList = ReadHost(host, numberOfRecords);
            }
            if(tmpList != null && tmpList.size() > 0) {
                allTransmitterRawData.add(tmpList);
            }
        }
        // merge the information
        if (allTransmitterRawData.size() == 0) {
            System.out.println("Could not read anything from " + hostsNames);
            Log.e(TAG, "Could not read anything from " + hostsNames);
            return null;

        }
        List<TransmitterRawData> mergedData= MergeLists(allTransmitterRawData);

        int retSize = Math.min(numberOfRecords, mergedData.size());
        TransmitterRawData []trd_array = new TransmitterRawData[retSize];
        mergedData.subList(mergedData.size() - retSize, mergedData.size()).toArray(trd_array);

  //      System.out.println("Final Results========================================================================");
 //       for (int i= 0; i < trd_array.length; i++) {
 //           System.out.println( trd_array[i].toTableString());
  //      }
        return trd_array;

    }

    public static List<TransmitterRawData> Read(String hostName,int port, int numberOfRecords)
    {
        List<TransmitterRawData> trd_list = new LinkedList<TransmitterRawData>();
        try
        {
            Log.i(TAG, "Read called");
            Gson gson = new GsonBuilder().create();

            // An example of using gson.
            ComunicationHeader ch = new ComunicationHeader();
            ch.version = 1;
            ch.numberOfRecords = numberOfRecords;
            String flat = gson.toJson(ch);
            ComunicationHeader ch2 = gson.fromJson(flat, ComunicationHeader.class);
            System.out.println("Results code" + flat + ch2.version);

            // Real client code
            InetSocketAddress ServerAdress = new InetSocketAddress(hostName, port);
            Socket MySocket = new Socket();
            MySocket.connect(ServerAdress, 10000);

            System.out.println("After the new socket \n");
            MySocket.setSoTimeout(2000);

            System.out.println("client connected... " );

            PrintWriter out = new PrintWriter(MySocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(MySocket.getInputStream()));

            out.println(flat);

            while(true) {
                String data = in.readLine();
                if(data == null) {
                    System.out.println("recieved null exiting");
                    break;
                }
                if(data.equals("")) {
                    System.out.println("recieved \"\" exiting");
                    break;
                }

                //System.out.println( "data size " +data.length() + " data = "+ data);
                TransmitterRawData trd = gson.fromJson(data, TransmitterRawData.class);
                trd.CaptureDateTime = System.currentTimeMillis() - trd.RelativeTime;
                MapsActivity.newMapLocation(trd.GeoLocation,trd.CaptureDateTime);

                trd_list.add(0,trd);
                //  System.out.println( trd.toTableString());
                if(trd_list.size() == numberOfRecords) {
                	// We have the data we want, let's get out
                	break;
                }
            }


            MySocket.close();
            return trd_list;
        }catch(SocketTimeoutException s) {
            Log.e(TAG, "Socket timed out! " + s.toString());
        }
        catch(IOException e) {
            Log.e(TAG, "cought IOException! "+ e.toString());
        }
        return trd_list;
    }

    static Long timeForNextRead() {

        TransmitterData lastTransmitterData = TransmitterData.last();
        if(lastTransmitterData == null) {
            // We did not receive a packet, well someone hopefully is looking at data, return relatively fast
            Log.e(TAG, "lastTransmitterData == null returning 60000");
            return 60*1000L;
        }
        Long gapTime = new Date().getTime() - lastTransmitterData.timestamp;
        Log.d(TAG, "gapTime = " + gapTime);
        if(gapTime < 0) {
            // There is some confusion here (clock was readjusted?)
            Log.e(TAG, "gapTime <= null returning 60000");
            return 60*1000L;
        }

        if(gapTime < DEXCOM_PERIOD) {
            // We have received the last packet...
            // 300000 - gaptime is when we expect to have the next packet.
            return (DEXCOM_PERIOD - gapTime) + 2000;
        }

        gapTime = gapTime % DEXCOM_PERIOD;
        Log.d(TAG, "modulus gapTime = " + gapTime);
        if(gapTime < 10000) {
            // A new packet should arrive any second now
            return 10000L;
        }
        if(gapTime < 60000) {
            // A new packet should arrive but chance is we have missed it...
            return 30000L;
        }

        if (httpClient == null) {
            return (DEXCOM_PERIOD - gapTime) + 2000;
        } else {
            // compensate for parakeet gprs lag
            return (DEXCOM_PERIOD - gapTime) + 12000;
        }
    }

    public Void doInBackground(String... urls) {
        try {
            getwakelock();
            readData();
        } finally {
            wakeLock.release();
            lockCounter--;
            Log.d(TAG,"wakelock released " + lockCounter);
        }
        return null;
    }

    
    public void readData()
    {
        Long LastReportedTime = 0L;
    	TransmitterData lastTransmitterData = TransmitterData.last();
    	if(lastTransmitterData != null) {
    	    LastReportedTime = lastTransmitterData.timestamp;

            // jamorham fix to avoid going twice to network when we just got a packet
            if ((new Date().getTime() - LastReportedTime) < DEXCOM_PERIOD-2000) {
            Log.d(TAG, "Already have a recent packet - returning");
            return;
            }


        }
    	Long startReadTime = LastReportedTime;

    	TransmitterRawData LastReportedReading = null;
    	Log.d(TAG, "Starting... LastReportedReading " + LastReportedReading);
    	// try to read one object...
        TransmitterRawData[] LastReadingArr = null;
        if(!WixelReader.IsConfigured(mContext)) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String recieversIpAddresses = prefs.getString("wifi_recievers_addresses", "");
        
        // How many packets should we read? we look at the maximum time between last calibration and last reading time
        // and calculate how much are needed.
        
        Calibration lastCalibration = Calibration.lastValid();
        if(lastCalibration != null) {
            startReadTime = Math.max(startReadTime, (long)(lastCalibration.timestamp));
        }
        Long gapTime = new Date().getTime() - startReadTime + 120000;
        int packetsToRead = (int) (gapTime / (5 * 60000));
        packetsToRead = Math.min(packetsToRead, 200); // don't read too much, but always read 1.
        packetsToRead = Math.max(packetsToRead, 1); 
        
        Log.d(TAG,"reading " + packetsToRead + " packets");
		LastReadingArr = Read(recieversIpAddresses ,packetsToRead);
		
		if (LastReadingArr == null || LastReadingArr.length  == 0) {
		    return;
		}

		for(TransmitterRawData LastReading : LastReadingArr ) {
    		// Last in the array is the most updated reading we have.
    		//TransmitterRawData LastReading = LastReadingArr[LastReadingArr.length -1];
		    

    		//if (LastReading.CaptureDateTime > LastReportedReading + 5000) {
    		// Make sure we do not report packets from the far future...
    		if ((LastReading.CaptureDateTime > LastReportedTime + 120000 ) &&
    		        (!almostEquals(LastReading, LastReportedReading)) &&
    		        LastReading.CaptureDateTime < new Date().getTime() + 120000) {
    			// We have a real new reading...
    			Log.d(TAG, "calling setSerialDataToTransmitterRawData " + LastReading.RawValue +
    			        " LastReading.CaptureDateTime " + LastReading.CaptureDateTime + " " + LastReading.TransmissionId);
    			setSerialDataToTransmitterRawData(LastReading.RawValue,  LastReading.FilteredValue, LastReading.BatteryLife, LastReading.CaptureDateTime);
    			LastReportedReading = LastReading;
    			LastReportedTime = LastReading.CaptureDateTime;


                if (LastReading.UploaderBatteryLife > 0)
                {
                    PreferenceManager.getDefaultSharedPreferences(mContext).edit().putInt("parakeet_battery", LastReading.UploaderBatteryLife).apply();
                }

    		}
    	}
    }


    public void setSerialDataToTransmitterRawData(int raw_data, int filtered_data ,int sensor_battery_leve, Long CaptureTime) {

        TransmitterData transmitterData = TransmitterData.create(raw_data, filtered_data, sensor_battery_leve, CaptureTime);
        if (transmitterData != null) {
            Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                BgReading bgReading = BgReading.create(transmitterData.raw_data, filtered_data, mContext, CaptureTime);

                sensor.latest_battery_level = (sensor.latest_battery_level!=0)?Math.min(sensor.latest_battery_level, transmitterData.sensor_battery_level):transmitterData.sensor_battery_level;;
                sensor.save();
            } else {
                Log.d(TAG, "No Active Sensor, Data only stored in Transmitter Data");
            }
        }
    }
    
    static Long timeForNextReadFake() {
        return 10000L;
    }
    
    void readDataFake()
    {
        i+=added;
        if (i==50) {
            added = -5;
        }
        if (i==0) {
            added = 5;
        }

        int fakedRaw = 100000 + i * 3000;
        Log.d(TAG, "calling setSerialDataToTransmitterRawData " + fakedRaw);
        setSerialDataToTransmitterRawData(fakedRaw, fakedRaw ,215, new Date().getTime());
        Log.d(TAG, "returned from setSerialDataToTransmitterRawData " + fakedRaw);
    }
}
