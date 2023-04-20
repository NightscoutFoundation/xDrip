package com.eveningoutpost.dexdrip.services;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Base64;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.models.LibreBluetooth;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.LibreBlock;
import com.eveningoutpost.dexdrip.models.SensorSanity;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.desertsync.RouteTools;
import com.eveningoutpost.dexdrip.utilitymodels.MockDataSource;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.Mdns;
import com.google.gson.Gson;

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

public class LibreWifiReader extends AsyncTask<String, Void, Void> {

    private final static String TAG = LibreWifiReader.class.getSimpleName();

    private static final HashMap<String, String> hostStatus = new HashMap<>();
    private static final HashMap<String, Long> hostStatusTime = new HashMap<>();

    private static final Gson gson = JoH.defaultGsonInstance();

    private final static long LIBRE_1_PERIOD = 300000;
    private final static long LIBRE_2_PERIOD = 61000;

    private static OkHttpClient httpClient;

    // This variables are for fake function only
    static int i = 0;
    static int added = 5;

    LibreWifiReader(Context ctx) {
        Log.d(TAG, "LibreWifiReader init");
    }

    static boolean isLibre2() {
        return PersistentStore.getStringToInt("LibreVersion", 1) == 2;
    }

    static boolean almostEquals(LibreWifiData e1, LibreWifiData e2) {
        if (e1 == null || e2 == null) {
            return false;
        }
        // relative time is in ms
        return (Math.abs(e1.CaptureDateTime - e2.CaptureDateTime) < 120 * 1000);
    }

    // last in the array, is first in time
    private static List<LibreWifiData> Merge2Lists(List<LibreWifiData> list1, List<LibreWifiData> list2) {
        final List<LibreWifiData> merged = new LinkedList<>();
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

    private static boolean hasGeoLocation(LibreWifiData record) {
        return false;
    }

    private static List<LibreWifiData> MergeLists(List<List<LibreWifiData>> allTransmitterRawData) {
        List<LibreWifiData> MergedList;
        MergedList = allTransmitterRawData.remove(0);
        for (List<LibreWifiData> it : allTransmitterRawData) {
            MergedList = Merge2Lists(MergedList, it);
        }

        return MergedList;
    }

    private static List<LibreWifiData> readFake() {
        final LibreWifiData trd = gson.fromJson(MockDataSource.getFakeWifiData(), LibreWifiData.class);
        trd.CaptureDateTime = System.currentTimeMillis() - trd.RelativeTime;
        final List<LibreWifiData> l = new ArrayList<>();
        l.add(trd);
        return l;
    }

    private static List<LibreWifiData> ReadHost(String hostAndIp, int numberOfRecords) {
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
            statusLog(hostAndIp, JoH.hourMinuteString() + " Invalid Port: " + hostAndIp);
            return null;

        }
        if (port < 10 || port > 65535) {
            System.out.println("Invalid port " + hosts[1]);
            Log.e(TAG, "Invalid hostAndIp " + hostAndIp);
            statusLog(hostAndIp, JoH.hourMinuteString() + " Invalid Host/Port: " + hostAndIp);
            return null;
        }
        System.out.println("Reading from " + hosts[0] + " " + port);
        final List<LibreWifiData> ret;
        try {
            ret = ReadV2(hosts[0], port, numberOfRecords);
        } catch (Exception e) {
            // We had some error, need to move on...
            System.out.println("read from host failed cought expation" + hostAndIp);
            Log.e(TAG, "read from host failed " + hostAndIp, e);
            return null;
        }
        return ret;
    }

    private static List<LibreWifiData> ReadFromMongo(String dbury, int numberOfRecords) {
        Log.i(TAG, "Reading From " + dbury);
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
        List<LibreWifiData> rd = mt.ReadFromMongoLibre(numberOfRecords);
        if (rd != null) {
            long newest_timestamp = 0;
            for (LibreWifiData r : rd) {
                if (newest_timestamp < r.getCaptureDateTime()) {
                    statusLog(dbury, JoH.hourMinuteString() + " OK data from: ", r.getCaptureDateTime());
                    newest_timestamp = r.getCaptureDateTime();
                }
            }
        }
        return rd;
    }

    // read from http source like cloud hosted parakeet receiver.cgi / json.get
    private static List<LibreWifiData> readHttpJson(String url, int numberOfRecords) {
        long newest_timestamp = 0;
        final long time_start = JoH.tsl();
        final List<LibreWifiData> trd_list = new LinkedList<LibreWifiData>();
        try {
            if (httpClient == null) {
                httpClient = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(20, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build();
            }

            // simple HTTP GET request
            // n=numberOfRecords for backfilling
            // expecting json reply like the standard json server in dexterity / python pi usb / parakeet
            final Request request = new Request.Builder()
                    // Mozilla header facilitates compression
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Connection", "close")
                    .url(url + "/libre/" + numberOfRecords)
                    .build();

            final Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(TAG, "Error reading from " + url + " " + response.message());
                return trd_list;
            }
            String res = response.body().string();
            Log.d(TAG, "Data read from " + url + " " + res);

            final LibreWifiData[] libre_wifi_data_array = gson.fromJson(res, LibreWifiData[].class);
            Log.e(TAG, "LibreWifiHeader = " + libre_wifi_data_array);

            for (LibreWifiData libre_wifi_data : libre_wifi_data_array) {
                libre_wifi_data.RelativeTime = JoH.tsl() - libre_wifi_data.CaptureDateTime;

                if (newest_timestamp < libre_wifi_data.CaptureDateTime) {
                    statusLog(url, JoH.hourMinuteString() + " OK data from:", libre_wifi_data.CaptureDateTime);
                    newest_timestamp = libre_wifi_data.CaptureDateTime;
                }

                trd_list.add(0, libre_wifi_data);
                //  System.out.println( trd.toTableString());
                if (trd_list.size() == numberOfRecords) {
                    // We have the data we want, let's get out
                    break;
                }
            }
            return trd_list;
        } catch (SocketTimeoutException s) {
            Log.e(TAG, "Socket timed out! " + url + " : " + s.toString() + " after: " + JoH.msSince(time_start));
            statusLog(url, JoH.hourMinuteString() + " " + s.toString());
        } catch (IOException e) {
            Log.e(TAG, "caught IOException! " + url + " : " + " : " + e.toString());
            statusLog(url, JoH.hourMinuteString() + " " + e.toString());
        } catch (Exception e) {
            Log.e(TAG, "Got exception " + url + " " + e.toString());
        }
        return trd_list;
    }

    // format of string is ip1:port1,ip2:port2;
    public static LibreWifiData[] Read(String hostsNames, int numberOfRecords) {
        String[] hosts = hostsNames.split(",");
        if (hosts.length == 0) {
            Log.e(TAG, "Error no hosts were found " + hostsNames);
            return null;
        }
        List<List<LibreWifiData>> allTransmitterRawData = new LinkedList<List<LibreWifiData>>();

        // go over all hosts and read data from them
        for (String host : hosts) {
            host = host.trim();
            List<LibreWifiData> tmpList;
            if (host.startsWith("mongodb://")) {
                tmpList = ReadFromMongo(host, numberOfRecords);
            } else if ((host.startsWith("fake://")
                    && (Home.get_engineering_mode())
                    && (DexCollectionType.getDexCollectionType() == DexCollectionType.Mock))) {
                tmpList = readFake();
            } else if (host.startsWith("https://")) {
                tmpList = readHttpJson(host, numberOfRecords);
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
        final List<LibreWifiData> mergedData = MergeLists(allTransmitterRawData);

        int retSize = Math.min(numberOfRecords, mergedData.size());
        LibreWifiData[] trd_array = new LibreWifiData[retSize];
        mergedData.subList(mergedData.size() - retSize, mergedData.size()).toArray(trd_array);

        //      System.out.println("Final Results========================================================================");
        //       for (int i= 0; i < trd_array.length; i++) {
        //           System.out.println( trd_array[i].toString());
        //      }
        return trd_array;
    }

    public static List<LibreWifiData> ReadV2(String hostName, int port, int numberOfRecords) {
        final List<LibreWifiData> trd_list = new LinkedList<LibreWifiData>();
        Log.i(TAG, "Read called: " + hostName + " port: " + port);

        final boolean skip_lan = Pref.getBooleanDefaultFalse("skip_lan_uploads_when_no_lan");

        if (skip_lan && (hostName.endsWith(".local")) && !JoH.isLANConnected()) {
            Log.d(TAG, "Skipping due to no lan: " + hostName);
            statusLog(hostName + ":" + port, "Skipping, no LAN");
            return trd_list; // blank
        }

        final long time_start = JoH.tsl();
        String currentAddress = "null";
        long newest_timestamp = 0;

        try {
            final ComunicationHeaderV2 ch = new ComunicationHeaderV2(numberOfRecords);
            // Real client code
            final InetSocketAddress ServerAddress = new InetSocketAddress(Mdns.genericResolver(hostName), port);
            currentAddress = ServerAddress.getAddress().getHostAddress();
            if (skip_lan && currentAddress.startsWith("192.168.") && !JoH.isLANConnected()) {
                Log.d(TAG, "Skipping due to no lan: " + hostName);
                statusLog(hostName + ":" + port, "Skipping, no LAN");
                return trd_list; // blank
            }

            final Socket MySocket = new Socket();
            MySocket.connect(ServerAddress, 10000);

            MySocket.setSoTimeout(3000);

            final PrintWriter out = new PrintWriter(MySocket.getOutputStream(), true);
            final BufferedReader in = new BufferedReader(new InputStreamReader(MySocket.getInputStream()));

            String myIpAddresses = RouteTools.getBestInterfaceAddress();
            ch.xDripIpAddresses = myIpAddresses != null ? myIpAddresses : "";
            ch.btAddresses = ActiveBluetoothDevice.btDeviceAddresses();

            out.println(ch.toJson());

            String full_data = "";
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

                Log.e(TAG, "data size " + data.length() + " data = " + data);
                full_data += data;
            }

            MySocket.close();

            final LibreWifiHeader libre_Wifi_header = gson.fromJson(full_data, LibreWifiHeader.class);
            Log.e(TAG, "LibreWifiHeader = " + libre_Wifi_header);

            for (LibreWifiData libre_wifi_data : libre_Wifi_header.libre_wifi_data) {
                libre_wifi_data.CaptureDateTime = System.currentTimeMillis() - libre_wifi_data.RelativeTime;
                //MapsActivity.newMapLocation(trd.GeoLocation, trd.CaptureDateTime);

                if (newest_timestamp < libre_wifi_data.CaptureDateTime) {
                    statusLog(hostName + ":" + port, JoH.hourMinuteString() + " OK data from:", libre_wifi_data.CaptureDateTime);
                    newest_timestamp = libre_wifi_data.CaptureDateTime;
                }

                trd_list.add(0, libre_wifi_data);
                //  System.out.println( trd.toTableString());
                if (trd_list.size() == numberOfRecords) {
                    // We have the data we want, let's get out
                    break;
                }
            }
            return trd_list;
        } catch (SocketTimeoutException s) {
            Log.e(TAG, "Socket timed out! " + hostName + " : " + currentAddress + " : " + s.toString() + " after: " + JoH.msSince(time_start));
            statusLog(hostName + ":" + port, JoH.hourMinuteString() + " " + s.toString());
        } catch (IOException e) {
            Log.e(TAG, "caught IOException! " + hostName + " : " + currentAddress + " : " + " : " + e.toString());
            statusLog(hostName + ":" + port, JoH.hourMinuteString() + " " + e.toString());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Argument error on: " + hostName + " " + e.toString());
        } catch (NullPointerException e) {
            Log.e(TAG, "Got null pointer exception " + hostName + " " + e.toString());
        }

        return trd_list;
    }

    static Long timeForNextRead() {
        LibreBlock libreBlock = LibreBlock.getLatestForTrend(0L, JoH.tsl() + 5 * 60000); // Allow some packets from the future.
        if (libreBlock == null) {
            // We did not receive a packet, well someone hopefully is looking at data, return relatively fast
            Log.e(TAG, "libreBlock == null returning 60000");
            return 60 * 1000L;
        }
        Long gapTime = new Date().getTime() - libreBlock.timestamp;
        Log.d(TAG, "gapTime = " + gapTime);
        if (gapTime < 0) {
            // There is some confusion here (clock was readjusted?)
            Log.e(TAG, "gapTime <= 0 returning 60000");
            return 60 * 1000L;
        }

        long sensor_period = isLibre2() ? LIBRE_2_PERIOD : LIBRE_1_PERIOD;
        if (gapTime < sensor_period) {
            // We have received the last packet...
            // 300000 - gaptime is when we expect to have the next packet.
            return (sensor_period - gapTime) + 2000;
        }

        gapTime = gapTime % sensor_period;
        Log.d(TAG, "modulus gapTime = " + gapTime);
        if (gapTime < 10000) {
            // A new packet should arrive any second now
            return 10000L;
        }
        if (gapTime < 60000) {
            // A new packet should arrive but chance is we have missed it...
            return 30000L;
        }

        return (sensor_period - gapTime) + 2000;
    }

    public Void doInBackground(String... urls) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("LibreWifiReader", 120000);
        Log.e(TAG, "doInBackground called");
        try {
            synchronized (LibreWifiReader.class) {
                readData();
            }
        } finally {
            JoH.releaseWakeLock(wl);
            // Log.d(TAG, "wakelock released " + lockCounter);
        }
        return null;
    }

    private void readData() {
        // Start very simply by getting only one object and using it.
        Log.e(TAG, "readData called");
        Long LastReportedTime = 0L;

        // TODO change that to work based on readings as well ???
        LibreBlock libreBlock = LibreBlock.getLatestForTrend(0L, JoH.tsl() + 5 * 60000); // Allow some packets from the future.
        if (libreBlock != null) {
            LastReportedTime = libreBlock.timestamp;
            Log.e(TAG, "LastReportedTime = " + JoH.dateTimeText(LastReportedTime));

            // jamorham fix to avoid going twice to network when we just got a packet
            if ((new Date().getTime() - LastReportedTime) < Constants.MINUTE_IN_MS - 2000) {
                Log.d(TAG, "Already have a recent packet - returning");
                if (JoH.ratelimit("deferred-msg", 60)) {
                    statusLog(" Deferred", "Already have recent reading");
                }
                return;
            } else {
                statusLog(" Deferred", "");
            }
        }
        String recieversIpAddresses;

        // This is the same ip location as for the wixelreader.
        if (!WixelReader.IsConfigured()) {
            Log.e(TAG, "LibreWifiReader not configured");
            return;
        }

        if ((DexCollectionType.getDexCollectionType() == DexCollectionType.Mock) && Home.get_engineering_mode()) {
            recieversIpAddresses = "fake://FAKE_DATA";
        } else {
            recieversIpAddresses = Pref.getString("wifi_recievers_addresses", "");
        }
        int packetsToRead = isLibre2() ? 5 : 1;
        Log.d(TAG, "reading " + packetsToRead + " packets");
        LibreWifiData[] LibreWifiDataArr = Read(recieversIpAddresses, packetsToRead);

        Log.d(TAG, "After reading ..." + LibreWifiDataArr);

        if (LibreWifiDataArr == null || LibreWifiDataArr.length == 0) {
            return;
        }
        Log.d(TAG, "After verification ... size = " + LibreWifiDataArr.length);
        // Last in the array is the most updated reading we have.
        for (LibreWifiData LastReading : LibreWifiDataArr) {
            // Last in the array is the most updated reading we have.
            //TransmitterRawData LastReading = LastReadingArr[LastReadingArr.length -1];

            //if (LastReading.CaptureDateTime > LastReportedReading + 5000) {
            // Make sure we do not report packets from the far future...

            Log.d(TAG, "checking packet from " + JoH.dateTimeText(LastReading.CaptureDateTime));

            if ((LastReading.CaptureDateTime > LastReportedTime +  Constants.MINUTE_IN_MS - 5000) &&
                    LastReading.CaptureDateTime < new Date().getTime() + 2 *  Constants.MINUTE_IN_MS) {
                // We have a real new reading...
                Log.d(TAG, "will call with packet from " + JoH.dateTimeText(LastReading.CaptureDateTime));

                byte data[] = Base64.decode(LastReading.BlockBytes, Base64.DEFAULT);

                byte patchUid[] = null;
                if (LastReading.patchUid != null && (!LastReading.patchUid.isEmpty())) {
                    patchUid = Base64.decode(LastReading.patchUid, Base64.DEFAULT);
                }
                byte patchInfo[] = null;
                if (LastReading.patchInfo != null && (!LastReading.patchInfo.isEmpty())) {
                    patchInfo = Base64.decode(LastReading.patchInfo, Base64.DEFAULT);
                }


                if(data.length == 46) {
                    // This is the libre 2 case
                    LibreBluetooth.SendData(data, LastReading.CaptureDateTime);
                    LastReportedTime = LastReading.CaptureDateTime;
                } else {
                    // This is the libre 1 case
                    boolean checksum_ok = NFCReaderX.HandleGoodReading(LastReading.SensorId, data, LastReading.CaptureDateTime, false, patchUid, patchInfo);
                    if (checksum_ok) {
                        Log.d(TAG, "checksum ok updating LastReportedTime to " + JoH.dateTimeText(LastReading.CaptureDateTime));
                        // TODO use battery, and other interesting data.
                        LastReportedTime = LastReading.CaptureDateTime;

                        PersistentStore.setString("Tomatobattery", Integer.toString(LastReading.TomatoBatteryLife));
                        Pref.setInt("bridge_battery", LastReading.TomatoBatteryLife);
                        PersistentStore.setString("TomatoHArdware", LastReading.HwVersion);
                        PersistentStore.setString("TomatoFirmware", LastReading.FwVersion);
                        Log.i(TAG, "LastReading.SensorId " + LastReading.SensorId);
                        PersistentStore.setString("LibreSN", LastReading.SensorId);

                        if (SensorSanity.checkLibreSensorChangeIfEnabled(LastReading.SensorId)) {
                            Log.e(TAG, "Problem with Libre Serial Number - not processing");
                            return;
                        }
                    } else {
                        Log.e(TAG, "Recieved a pacjet with bad checksum");
                    }
                }
            }
        }
    }

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
