package com.eveningoutpost.dexdrip.Services;

import java.io.IOException;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.TransmitterData;

public class WixelReader  extends Thread {

    private final static String TAG = WixelReader.class.getName();
    private static WixelReader singleton;
    
    public synchronized static WixelReader getInstance(Context ctx) {
        if(singleton == null) {
           singleton = new WixelReader(ctx);
        }
        return singleton;
    }

    private final Context mContext;

    private volatile boolean mStop = false;
    private static boolean sStarted = false;

    public WixelReader(Context ctx) {
        mContext = ctx.getApplicationContext();
    }

    public static void sStart(Context ctx) {
        if(sStarted) {
            return;
        }
        WixelReader theWixelReader =  getInstance(ctx);
        theWixelReader.start();
        sStarted = true;
        
    }
    
    public static void sStop() {
        if(!sStarted) {
            return;
        }
        WixelReader theWixelReader =  getInstance(null);
        theWixelReader.Stop();
        try {
            theWixelReader.join();
        } catch (InterruptedException e) {
            Log.e(TAG, "cought InterruptedException, could not wait for the wixel thread to exit", e);
        }
        sStarted = false;
        // A stopped thread can not start again, so we need to kill it and will start a new one
        // on demand
        singleton = null;
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
        Log.i(TAG,"Reading From " + dbury);
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
        	
            List<TransmitterRawData> tmpList;
            if (host.startsWith("mongodb://")) {
            	tmpList = ReadFromMongo(host ,numberOfRecords);
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
        
        System.out.println("Final Results========================================================================");
        for (int i= 0; i < trd_array.length; i++) {
 //           System.out.println( trd_array[i].toTableString());
        }
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
            Socket MySocket = new Socket(hostName, port);

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
            Log.e(TAG, "Socket timed out! ", s);
        }
        catch(IOException e) {
            Log.e(TAG, "cought IOException! ", e);
        }
        return trd_list;
    }

    
    public void run()
    {
    	Long LastReportedTime = new Date().getTime();
    	TransmitterRawData LastReportedReading = null; 
    	Log.e(TAG, "Starting... LastReportedReading " + LastReportedReading);
    	try {
	        while (!mStop && !interrupted()) {
	        	// try to read one object...
                TransmitterRawData[] LastReadingArr = null;
                if(WixelReader.IsConfigured(mContext)) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                    String recieversIpAddresses = prefs.getString("wifi_recievers_addresses", "");
	        		LastReadingArr = Read(recieversIpAddresses ,1);
                }
	        	if (LastReadingArr != null  && LastReadingArr.length  > 0) {
	        		// Last in the array is the most updated reading we have.
	        		TransmitterRawData LastReading = LastReadingArr[LastReadingArr.length -1];
	        		
	        		//if (LastReading.CaptureDateTime > LastReportedReading + 5000) {
	        		// Make sure we do not report packets from the far future...
	        		if ((LastReading.CaptureDateTime > LastReportedTime ) &&
	        		        (!almostEquals(LastReading, LastReportedReading)) &&
	        		        LastReading.CaptureDateTime < new Date().getTime() + 12000) {
	        			// We have a real new reading...
	        			Log.e(TAG, "calling setSerialDataToTransmitterRawData " + LastReading.RawValue +
	        			        " LastReading.CaptureDateTime " + LastReading.CaptureDateTime + " " + LastReading.TransmissionId);
	        			setSerialDataToTransmitterRawData(LastReading.RawValue , LastReading.BatteryLife, LastReading.CaptureDateTime);
	        			LastReportedReading = LastReading;
	        			LastReportedTime = LastReading.CaptureDateTime;
	        		}
	        	}
	        	// let's sleep (right now for 30 seconds)
	        	Thread.sleep(30000);
	        }
    	} catch (InterruptedException e) {
    	    Log.e(TAG, "cought InterruptedException! ", e);
            // time to get out...            
        }
    }
    
    // this function is only a test function. It is used to set many points fast in order to allow
    // faster testing without real data.
    public void runFake()
    {
        // let's start by faking numbers....
        int i = 0;
        int added = 5;
        while (!mStop) {
            try {
                for (int j = 0 ; j < 3; j++) {
                    Thread.sleep(1000);
                    if(mStop ) {
                    // we were asked to leave, so do it....
                        return;
                    }
                }
                i+=added;
                if (i==50) {
                    added = -5;
                }
                if (i==0) {
                    added = 5;
                }

                int fakedRaw = 150000 + i * 1000;
                Log.e(TAG, "calling setSerialDataToTransmitterRawData " + fakedRaw);
                setSerialDataToTransmitterRawData(fakedRaw, 100, new Date().getTime());

               } catch (InterruptedException e) {
                   // time to get out...
                   Log.e(TAG, "cought InterruptedException! ", e);
                   break;
               }
        }
    }

    public void Stop()
    {
        mStop = true;
        interrupt();
    }
    public void setSerialDataToTransmitterRawData(int raw_data ,int sensor_battery_leve, Long CaptureTime) {

        TransmitterData transmitterData = TransmitterData.create(raw_data, sensor_battery_leve, CaptureTime);
        if (transmitterData != null) {
            Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                BgReading bgReading = BgReading.create(transmitterData.raw_data, mContext, CaptureTime);
                sensor.latest_battery_level = transmitterData.sensor_battery_level;
                sensor.save();
            } else {
                Log.w(TAG, "No Active Sensor, Data only stored in Transmitter Data");
            }
        }
    }
}