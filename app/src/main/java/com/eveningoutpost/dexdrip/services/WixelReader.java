package com.eveningoutpost.dexdrip.services;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.MapsActivity;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.TransmitterData;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.ParakeetHelper;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.MockDataSource;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utils.CheckBridgeBattery;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.Mdns;
import com.google.gson.Gson;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


// Important note, this class is based on the fact that android will always run it one thread, which means it does not
// need synchronization

public class WixelReader extends AsyncTask<String, Void, Void> {

    private final static String TAG = WixelReader.class.getSimpleName();

    private static OkHttpClient httpClient = null;
    private static final HashMap<String, String> hostStatus = new HashMap<>();
    private static final HashMap<String, Long> hostStatusTime = new HashMap<>();

    private static final Gson gson = JoH.defaultGsonInstance();

    private final static long DEXCOM_PERIOD = 300000;

    // This variables are for fake function only
    static int i = 0;
    static int added = 5;

    WixelReader(Context ctx) {
        Log.d(TAG, "WixelReader init");
    }

    public static boolean IsConfigured() {
        if ((DexCollectionType.getDexCollectionType() == DexCollectionType.Mock) && (Home.get_engineering_mode()))
            return true;
        final String recieversIpAddresses = Pref.getString("wifi_recievers_addresses", "");
        return !recieversIpAddresses.equals("");
    }

    static boolean almostEquals(TransmitterRawData e1, TransmitterRawData e2) {
        if (e1 == null || e2 == null) {
            return false;
        }
        // relative time is in ms
        return (Math.abs(e1.CaptureDateTime - e2.CaptureDateTime) < 120 * 1000) &&
                (e1.TransmissionId == e2.TransmissionId);
    }

    // last in the array, is first in time
    private static List<TransmitterRawData> Merge2Lists(List<TransmitterRawData> list1, List<TransmitterRawData> list2) {
        final List<TransmitterRawData> merged = new LinkedList<>();
        while (true) {
            if (list1.size() == 0 && list2.size() == 0) {
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
                // favour records which have real parakeet geolocation
                if (hasGeoLocation(list1.get(0))) {
                    list2.remove(0);
                    merged.add(list1.remove(0));
                } else {
                    list1.remove(0);
                    merged.add(list2.remove(0));
                }
                continue;
            }

            if (list1.get(0).RelativeTime > list2.get(0).RelativeTime) {
                merged.add(list1.remove(0));
            } else {
                merged.add(list2.remove(0));
            }

        }
        return merged;
    }

    private static boolean hasGeoLocation(TransmitterRawData record) {
        return record != null && record.GeoLocation != null && record.GeoLocation.length() > 0 && !record.GeoLocation.equals("-15,-15");
    }

    private static List<TransmitterRawData> MergeLists(List<List<TransmitterRawData>> allTransmitterRawData) {
        List<TransmitterRawData> MergedList;
        MergedList = allTransmitterRawData.remove(0);
        for (List<TransmitterRawData> it : allTransmitterRawData) {
            MergedList = Merge2Lists(MergedList, it);
        }

        return MergedList;
    }

    private static List<TransmitterRawData> readFake() {
        final TransmitterRawData trd = gson.fromJson(MockDataSource.getFakeWifiData(), TransmitterRawData.class);
        trd.CaptureDateTime = System.currentTimeMillis() - trd.RelativeTime;
        final List<TransmitterRawData> l = new ArrayList<>();
        l.add(trd);
        if (Ob1G5CollectionService.usingMockPreCalibrated()) {
            BgReading.bgReadingInsertFromG5(trd.RawValue / 1000.0, trd.getCaptureDateTime(), "Mock");
        }
        return l;
    }

    private static List<TransmitterRawData> ReadHost(String hostAndIp, int numberOfRecords) {
        int port;
        //System.out.println("Reading From " + hostAndIp);
        Log.i(TAG, "Reading From " + hostAndIp);
        String[] hosts = hostAndIp.split(":");
        if (hosts.length != 2) {
          //  System.out.println("Invalid hostAndIp " + hostAndIp);
            Log.e(TAG, "Invalid hostAndIp " + hostAndIp);

            return null;
        }
        try {
            port = Integer.parseInt(hosts[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("Invalid port " + hosts[1]);
            Log.e(TAG, "Invalid hostAndIp " + hostAndIp, nfe);
            statusLog(hosts[0], JoH.hourMinuteString() + " Invalid Port: " + hostAndIp);
            return null;

        }
        if (port < 10 || port > 65535) {
            System.out.println("Invalid port " + hosts[1]);
            Log.e(TAG, "Invalid hostAndIp " + hostAndIp);
            statusLog(hosts[0], JoH.hourMinuteString() + " Invalid Host/Port: " + hostAndIp);
            return null;

        }
        System.out.println("Reading from " + hosts[0] + " " + port);
        final List<TransmitterRawData> ret;
        try {
            ret = Read(hosts[0], port, numberOfRecords);
        } catch (Exception e) {
            // We had some error, need to move on...
            System.out.println("read from host failed cought expation" + hostAndIp);
            Log.e(TAG, "read from host failed " + hostAndIp, e);

            return null;

        }
        return ret;
    }

    private static List<TransmitterRawData> ReadFromMongo(String dbury, int numberOfRecords) {
        Log.i(TAG, "Reading From " + dbury);
        List<TransmitterRawData> tmpList;
        // format is dburi/db/collection. We need to find the collection and strip it from the dburi.
        int indexOfSlash = dbury.lastIndexOf('/');
        if (indexOfSlash == -1) {
            // We can not find a collection name
            Log.e(TAG, "Error bad dburi. Did not find a collection name starting with / " + dbury);
            // in order for the user to understand that there is a problem, we return null
            return null;

        }
        final String collection = dbury.substring(indexOfSlash + 1);
        dbury = dbury.substring(0, indexOfSlash);

        // Make sure that we have another /, since this is used in the constructor.
        indexOfSlash = dbury.lastIndexOf('/');
        if (indexOfSlash == -1) {
            // We can not find a collection name
            Log.e(TAG, "Error bad dburi. Did not find a collection name starting with / " + dbury);
            // in order for the user to understand that there is a problem, we return null
            return null;
        }

        final MongoWrapper mt = new MongoWrapper(dbury, collection, "CaptureDateTime", "MachineNameNotUsed");
        List<TransmitterRawData> rd = mt.ReadFromMongo(numberOfRecords);
        if (rd != null) {
            long newest_timestamp = 0;
            for (TransmitterRawData r : rd) {
                if (newest_timestamp < r.getCaptureDateTime()) {
                    statusLog(dbury, JoH.hourMinuteString() + " OK data from:", r.getCaptureDateTime());
                    newest_timestamp = r.getCaptureDateTime();
                }
            }
        }
        return rd;
    }


    // read from http source like cloud hosted parakeet receiver.cgi / json.get
    private static List<TransmitterRawData> readHttpJson(String url, int numberOfRecords) {
        final List<TransmitterRawData> trd_list = new LinkedList<>();
        int processNumberOfRecords = numberOfRecords;
        // get more records to ensure we can handle coexistence of parakeet and usb-python-wixel
        // TODO make this work on preference option for the feature
        if (true) numberOfRecords = numberOfRecords + 1;
        long newest_timestamp = 0;
        try {

            if (httpClient == null) {
                httpClient = new OkHttpClient();
                // suitable for GPRS
                httpClient.setConnectTimeout(30, TimeUnit.SECONDS);
                httpClient.setReadTimeout(60, TimeUnit.SECONDS);
                httpClient.setWriteTimeout(20, TimeUnit.SECONDS);
            }


            // simple HTTP GET request
            // n=numberOfRecords for backfilling
            // r=sequence number to avoid any cache
            // expecting json reply like the standard json server in dexterity / python pi usb / parakeet
            final Request request = new Request.Builder()

                    // Mozilla header facilitates compression
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Connection", "close")
                    .url(url + "?n=" + Integer.toString(numberOfRecords)
                            + "&r=" + Long.toString((System.currentTimeMillis() / 1000) % 9999999))
                    .build();

            final Response response = httpClient.newCall(request).execute();
            // if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            if (response.isSuccessful()) {

                String lines[] = response.body().string().split("\\r?\\n");

                for (String data : lines) {

                    if (data == null) {
                        Log.d(TAG, "received null continuing");
                        continue;
                    }
                    if (data.equals("")) {
                        Log.d(TAG, "received \"\" continuing");
                        continue;
                    }

                    final TransmitterRawData trd = gson.fromJson(data, TransmitterRawData.class);
                    trd.CaptureDateTime = System.currentTimeMillis() - trd.RelativeTime;

                    // Versions of the Python USB script after 20th May 2016 will
                    // submit a bogus geolocation in the middle of the ocean to differentiate
                    // themselves from actual parakeet data even though both can coexist on the
                    // parakeet web service.

                   // if (JoH.ratelimit("parakeet-check-notification", 9)) {
                        ParakeetHelper.checkParakeetNotifications(trd.CaptureDateTime, trd.GeoLocation);
                    //}
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
                    if (newest_timestamp < trd.getCaptureDateTime()) {
                        statusLog(url, JoH.hourMinuteString() + " OK data from:", trd.getCaptureDateTime());
                        newest_timestamp = trd.CaptureDateTime;
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
    public static TransmitterRawData[] Read(String hostsNames, int numberOfRecords) {
        String[] hosts = hostsNames.split(",");
        if (hosts.length == 0) {
            Log.e(TAG, "Error no hosts were found " + hostsNames);
            return null;
        }
        List<List<TransmitterRawData>> allTransmitterRawData = new LinkedList<List<TransmitterRawData>>();

        // go over all hosts and read data from them
        for (String host : hosts) {
            host = host.trim();
            List<TransmitterRawData> tmpList;
            if (host.startsWith("mongodb://")) {
                tmpList = ReadFromMongo(host, numberOfRecords);
            } else if ((host.startsWith("http://") || host.startsWith("https://"))
                    && host.contains("/json.get")) {
                tmpList = readHttpJson(host, numberOfRecords);
            } else if ((host.startsWith("fake://")
                    && (Home.get_engineering_mode())
                    && (DexCollectionType.getDexCollectionType() == DexCollectionType.Mock))) {
                tmpList = readFake();
            } else {
                tmpList = ReadHost(host, numberOfRecords);
            }
            if (tmpList != null && tmpList.size() > 0) {
                allTransmitterRawData.add(tmpList);
            }
        }
        // merge the information
        if (allTransmitterRawData.size() == 0) {
            //System.out.println("Could not read anything from " + hostsNames);
            Log.e(TAG, "Could not read anything from " + hostsNames);
            return null;

        }
        final List<TransmitterRawData> mergedData = MergeLists(allTransmitterRawData);

        int retSize = Math.min(numberOfRecords, mergedData.size());
        TransmitterRawData[] trd_array = new TransmitterRawData[retSize];
        mergedData.subList(mergedData.size() - retSize, mergedData.size()).toArray(trd_array);

        //      System.out.println("Final Results========================================================================");
        //       for (int i= 0; i < trd_array.length; i++) {
        //           System.out.println( trd_array[i].toTableString());
        //      }
        return trd_array;

    }

    public static List<TransmitterRawData> Read(String hostName, int port, int numberOfRecords) {
        final List<TransmitterRawData> trd_list = new LinkedList<TransmitterRawData>();
        Log.i(TAG, "Read called: " + hostName + " port: " + port);

        final boolean skip_lan = Pref.getBooleanDefaultFalse("skip_lan_uploads_when_no_lan");

        if (skip_lan && (hostName.endsWith(".local")) && !JoH.isLANConnected()) {
            Log.d(TAG, "Skipping due to no lan: " + hostName);
            statusLog(hostName, "Skipping, no LAN");
            return trd_list; // blank
        }

        final long time_start = JoH.tsl();
        String currentAddress = "null";
        long newest_timestamp = 0;

        try {


            // An example of using gson.
            final ComunicationHeader ch = new ComunicationHeader(numberOfRecords);
            //ch.version = 1;
            //ch.numberOfRecords = numberOfRecords;
            // String flat = gson.toJson(ch);
            //ComunicationHeader ch2 = gson.fromJson(flat, ComunicationHeader.class);
            //System.out.println("Results code" + flat + ch2.version);

            // Real client code
            final InetSocketAddress ServerAddress = new InetSocketAddress(Mdns.genericResolver(hostName), port);
            currentAddress = ServerAddress.getAddress().getHostAddress();
            if (skip_lan && currentAddress.startsWith("192.168.") && !JoH.isLANConnected()) {
                Log.d(TAG, "Skipping due to no lan: " + hostName);
                statusLog(hostName, "Skipping, no LAN");
                return trd_list; // blank
            }

            final Socket MySocket = new Socket();
            MySocket.connect(ServerAddress, 10000);

            //System.out.println("After the new socket \n");
            MySocket.setSoTimeout(3000);

            //System.out.println("client connected... " );

            final PrintWriter out = new PrintWriter(MySocket.getOutputStream(), true);
            final BufferedReader in = new BufferedReader(new InputStreamReader(MySocket.getInputStream()));

            out.println(ch.toJson());

            while (true) {
                String data = in.readLine();
                if (data == null) {
                    Log.d(TAG, "recieved null exiting");
                    break;
                }
                if (data.equals("")) {
                    Log.d(TAG, "recieved \"\" exiting");
                    break;
                }

                //System.out.println( "data size " +data.length() + " data = "+ data);
                final TransmitterRawData trd = gson.fromJson(data, TransmitterRawData.class);
                trd.CaptureDateTime = System.currentTimeMillis() - trd.RelativeTime;
                MapsActivity.newMapLocation(trd.GeoLocation, trd.CaptureDateTime);

                if (newest_timestamp < trd.getCaptureDateTime()) {
                    statusLog(hostName, JoH.hourMinuteString() + " OK data from:", trd.getCaptureDateTime());
                    newest_timestamp = trd.getCaptureDateTime();
                }

                trd_list.add(0, trd);
                //  System.out.println( trd.toTableString());
                if (trd_list.size() == numberOfRecords) {
                    // We have the data we want, let's get out
                    break;
                }
            }


            MySocket.close();
            return trd_list;
        } catch (SocketTimeoutException s) {
            Log.e(TAG, "Socket timed out! " + hostName + " : " + currentAddress + " : " + s.toString() + " after: " + JoH.msSince(time_start));
            statusLog(hostName, JoH.hourMinuteString() + " " + s.toString());
        } catch (IOException e) {
            Log.e(TAG, "caught IOException! " + hostName + " : " + currentAddress + " : " + " : " + e.toString());
            statusLog(hostName, JoH.hourMinuteString() + " " + e.toString());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Argument error on: " + hostName + " " + e.toString());
        } catch (NullPointerException e) {
            Log.e(TAG, "Got null pointer exception " + hostName + " " + e.toString());
        }
        return trd_list;
    }

    static Long timeForNextRead() {

        TransmitterData lastTransmitterData = TransmitterData.last();
        if (lastTransmitterData == null) {
            // We did not receive a packet, well someone hopefully is looking at data, return relatively fast
            Log.e(TAG, "lastTransmitterData == null returning 60000");
            return 60 * 1000L;
        }
        Long gapTime = new Date().getTime() - lastTransmitterData.timestamp;
        Log.d(TAG, "gapTime = " + gapTime);
        if (gapTime < 0) {
            // There is some confusion here (clock was readjusted?)
            Log.e(TAG, "gapTime <= null returning 60000");
            return 60 * 1000L;
        }

        if (gapTime < DEXCOM_PERIOD) {
            // We have received the last packet...
            // 300000 - gaptime is when we expect to have the next packet.
            return (DEXCOM_PERIOD - gapTime) + 2000;
        }

        gapTime = gapTime % DEXCOM_PERIOD;
        Log.d(TAG, "modulus gapTime = " + gapTime);
        if (gapTime < 10000) {
            // A new packet should arrive any second now
            return 10000L;
        }
        if (gapTime < 60000) {
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
        final PowerManager.WakeLock wl = JoH.getWakeLock("WifiReader", 120000);
        try {
            //getwakelock();
            readData();
        } finally {
            JoH.releaseWakeLock(wl);
           // Log.d(TAG, "wakelock released " + lockCounter);
        }
        return null;
    }


    private void readData() {
        Long LastReportedTime = 0L;
        TransmitterData lastTransmitterData = TransmitterData.last();
        if (lastTransmitterData != null) {
            LastReportedTime = lastTransmitterData.timestamp;

            // jamorham fix to avoid going twice to network when we just got a packet
            if ((new Date().getTime() - LastReportedTime) < DEXCOM_PERIOD - 2000) {
                Log.d(TAG, "Already have a recent packet - returning");
                if (JoH.ratelimit("deferred-msg", 60)) {
                    statusLog(" Deferred", "Already have recent reading");
                }
                return;
            } else {
                statusLog(" Deferred", "");
            }


        }
        Long startReadTime = LastReportedTime;

        TransmitterRawData LastReportedReading = null;
        Log.d(TAG, "Starting... LastReportedReading " + LastReportedReading);
        // try to read one object...
        TransmitterRawData[] LastReadingArr = null;

        String recieversIpAddresses;

        if (!WixelReader.IsConfigured()) {
            return;
        }

        if ((DexCollectionType.getDexCollectionType() == DexCollectionType.Mock) && Home.get_engineering_mode()) {
            recieversIpAddresses = "fake://FAKE_DATA";
        } else {
            recieversIpAddresses = Pref.getString("wifi_recievers_addresses", "");
        }

        // How many packets should we read? we look at the maximum time between last calibration and last reading time
        // and calculate how much are needed.

        final Calibration lastCalibration = Calibration.lastValid();
        if (lastCalibration != null) {
            startReadTime = Math.max(startReadTime, lastCalibration.timestamp);
        }
        Long gapTime = new Date().getTime() - startReadTime + 120000;
        int packetsToRead = (int) (gapTime / (5 * 60000));
        packetsToRead = Math.min(packetsToRead, 200); // don't read too much, but always read 1.
        packetsToRead = Math.max(packetsToRead, 1);


        Log.d(TAG, "reading " + packetsToRead + " packets");
        LastReadingArr = Read(recieversIpAddresses, packetsToRead);

        if (LastReadingArr == null || LastReadingArr.length == 0) {
            return;
        }

        for (TransmitterRawData LastReading : LastReadingArr) {
            // Last in the array is the most updated reading we have.
            //TransmitterRawData LastReading = LastReadingArr[LastReadingArr.length -1];


            //if (LastReading.CaptureDateTime > LastReportedReading + 5000) {
            // Make sure we do not report packets from the far future...
            if ((LastReading.CaptureDateTime > LastReportedTime + 120000) &&
                    (!almostEquals(LastReading, LastReportedReading)) &&
                    LastReading.CaptureDateTime < new Date().getTime() + 120000) {
                // We have a real new reading...
                Log.d(TAG, "calling setSerialDataToTransmitterRawData " + LastReading.RawValue +
                        " LastReading.CaptureDateTime " + LastReading.CaptureDateTime + " " + LastReading.TransmissionId);
                setSerialDataToTransmitterRawData(LastReading.RawValue, LastReading.FilteredValue, LastReading.BatteryLife, LastReading.CaptureDateTime);
                LastReportedReading = LastReading;
                LastReportedTime = LastReading.CaptureDateTime;


                if (LastReading.UploaderBatteryLife > 0) {
                    Pref.setInt("parakeet_battery", LastReading.UploaderBatteryLife);
                    CheckBridgeBattery.checkParakeetBattery();
                    if (Home.get_master()) {
                        GcmActivity.sendParakeetBattery(LastReading.UploaderBatteryLife);
                    }
                }

            }
        }
    }


    private void setSerialDataToTransmitterRawData(int raw_data, int filtered_data, int sensor_battery_leve, Long CaptureTime) {

        final TransmitterData transmitterData = TransmitterData.create(raw_data, filtered_data, sensor_battery_leve, CaptureTime);
        if (transmitterData != null) {
            final Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                BgReading bgReading = BgReading.create(transmitterData.raw_data, filtered_data, null, CaptureTime);

                //sensor.latest_battery_level = (sensor.latest_battery_level!=0)?Math.min(sensor.latest_battery_level, transmitterData.sensor_battery_level):transmitterData.sensor_battery_level;
                sensor.latest_battery_level = transmitterData.sensor_battery_level; // don't lock it only going downwards
                sensor.save();
            } else {
                Log.d(TAG, "No Active Sensor, Data only stored in Transmitter Data");
            }
        }
    }
    
  /*  static Long timeForNextReadFake() {
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
    }*/

    // data for MegaStatus
    static List<StatusItem> megaStatus() {
        final List<StatusItem> l = new ArrayList<>();
        for (Map.Entry<String, String> entry : hostStatus.entrySet()) {
            final long status_time = hostStatusTime.get(entry.getKey());
            if (entry.getValue().length() > 0)
                l.add(new StatusItem(entry.getKey(), entry.getValue() + ((status_time != 0) ? (" " + JoH.niceTimeSince(status_time) + " " + "ago") : ""), JoH.msSince(status_time) <= BgGraphBuilder.DEXCOM_PERIOD ? StatusItem.Highlight.GOOD : JoH.msSince(status_time) <= BgGraphBuilder.DEXCOM_PERIOD * 2 ? StatusItem.Highlight.NOTICE : StatusItem.Highlight.NORMAL));
        }
        return l;
    }

    static void statusLog(String key, String msg) {
        statusLog(key, msg, 0); // default no time since
    }

    // timestamp or 0 = don't use or -1 = now
    static void statusLog(String key, String msg, long time) {
        hostStatus.put(key, msg);
        hostStatusTime.put(key, (time != -1) ? time : JoH.tsl());
    }
}
