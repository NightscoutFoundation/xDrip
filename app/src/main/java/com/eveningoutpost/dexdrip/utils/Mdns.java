package com.eveningoutpost.dexdrip.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.MegaStatus;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.JamorhamShowcaseDrawer;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.ShotStateStore;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
import com.eveningoutpost.dexdrip.xdrip;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.eveningoutpost.dexdrip.xdrip.gs;

/**
 * Created by jamorham on 18/02/2017.
 */


public class Mdns {

    public class LookUpInfo {
        public String address;
        public Long received;
        public NsdServiceInfo serviceInfo;

        LookUpInfo(String address, long received, NsdServiceInfo serviceInfo) {
            this.address = address;
            this.received = received;
            this.serviceInfo = serviceInfo;
        }

        String prettyName() {
            return serviceInfo.getServiceName().replaceFirst(" ", ".local ");
        }
    }

    private static final HashMap<String, LookUpInfo> iplookup = new HashMap<>();
    private static volatile boolean hunt_running = false;
    private static int errorCounter = 0;

    private final AtomicInteger outstanding = new AtomicInteger();
    private volatile long locked_until = 0;
    private NsdManager mNsdManager;
    private static NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;


    private static final long CACHE_REFRESH_MS = Constants.MINUTE_IN_MS * 10;
    private static final long NORMAL_RESOLVE_TIMEOUT_MS = 5000;
    private static final long WAIT_FOR_REPLIES_TIMEOUT_MS = 10000;

    // In order for this to work on a rpi do:
    // On the file /etc/avahi/avahi-daemon.conf change
    // publish-workstation=yes
    // and restart the service (sudo /etc/init.d/avahi-daemon restart)
    private static final String SERVICE_TYPE = "_workstation._tcp.";
    private static final String TAG = "Mdns-discovery";
    private static final boolean d = true;

    // resolve a normal or .local hostname
    public static String genericResolver(String name) throws UnknownHostException {
        final String lower = name.toLowerCase();
        if (lower.endsWith(".local")) {
            String address = fastResolve(name.replaceFirst(".local$", ""));
            if (address == null)
                throw (new UnknownHostException("Can't get address for: " + lower));
            return address;
        } else {
            return InetAddress.getByName(name).getHostAddress();
        }
    }

    public static String fastResolve(String name) {
        String address = superFastResolve(name);
        if (address != null) return address;
        final long wait_until = JoH.tsl() + NORMAL_RESOLVE_TIMEOUT_MS;
        while ((address == null) && (JoH.tsl() < wait_until)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                //
            }
            address = superFastResolve(name);
        }
        return address;
    }

    public static String superFastResolve(String name) {
        final LookUpInfo li = iplookup.get(name);
        if ((li == null) || (JoH.msSince(li.received) > CACHE_REFRESH_MS)) {
            if (JoH.quietratelimit("mdns-hunting", 60)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        new Mdns().hunt();
                    }
                }).start();
            }
        }
        if (li == null) return null;
        return li.address;
    }

    private void hunt() {
        final PowerManager.WakeLock wl = JoH.getWakeLock("mdns-hunt", 30000);
        if (hunt_running) {
            UserError.Log.wtf(TAG, "Hunt already running");
        }
        hunt_running = true;
        mNsdManager = (NsdManager) (xdrip.getAppContext().getSystemService(Context.NSD_SERVICE));

        mResolveListener = initializeResolveListener();
        initializeDiscoveryListener();

        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final long terminate_at = JoH.tsl() + WAIT_FOR_REPLIES_TIMEOUT_MS;
                while (JoH.tsl() < terminate_at) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        //
                    }
                }
                UserError.Log.d(TAG, "Shutting down");
                try {
                    mNsdManager.stopServiceDiscovery(mDiscoveryListener);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    UserError.Log.e(TAG, "Could not stop timed service discovery: " + e);
                }
                hunt_running = false;
                JoH.releaseWakeLock(wl);
            }
        }).start();

    }

    private synchronized void singleResolveService(NsdServiceInfo service) {
        try {

            if ((d) && (outstanding.get() != 0))
                UserError.Log.d(TAG, "Current outstanding requests: " + outstanding.get());
            int spinner = 0;
            while (locked_until > JoH.tsl()) {
                PowerManager.WakeLock wlx = JoH.getWakeLock("mdns-resolve", 200);
                spinner++;
                if ((spinner % 10) == 0)
                    UserError.Log.d(TAG, "Waiting on Lock: " + JoH.niceTimeTill(locked_until));
                Thread.sleep(100);
                JoH.releaseWakeLock(wlx);
            }
            // TODO wly timeout excessive and not cancelled
            PowerManager.WakeLock wly = JoH.getWakeLock("mdns-resolve-x", 2000);
            if (locked_until != 0) {
                mResolveListener = initializeResolveListener();
                UserError.Log.d(TAG, "Creating additional listener");
            }
            locked_until = JoH.tsl() + WAIT_FOR_REPLIES_TIMEOUT_MS;
            outstanding.incrementAndGet();
            mNsdManager.resolveService(service, mResolveListener);

        } catch (InterruptedException e) {
            UserError.Log.e(TAG, "Interrupted waiting to resolver lock!");
        } catch (IllegalArgumentException e) {
            UserError.Log.e(TAG, "got illegal argument exception in singleResolveService: ", e);
        }
    }

    private static String shortenName(String name) {
        try {
            return name.split(" ")[0];
        } catch (Exception e) {
            return name;
        }
    }

    private void initializeDiscoveryListener() {

        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
                UserError.Log.wtf(TAG, "Discovery service was active when it shouldn't be!");
            } catch (Exception e) {
                UserError.Log.d(TAG, "Could not stop service during initialization: " + e);
            }
        }
        mDiscoveryListener = new NsdManager.DiscoveryListener() {


            @Override
            public void onDiscoveryStarted(String regType) {
            }

            @Override
            public synchronized void onServiceFound(final NsdServiceInfo service) {

                final String type = service.getServiceType();

                UserError.Log.d(TAG, "onServiceFound " + type + service.getServiceName());
                if (type.equals(SERVICE_TYPE)) {
                    final String name = service.getServiceName();
                    final LookUpInfo li = iplookup.get(shortenName(name));
                    if ((li == null) || (JoH.msSince(li.received) > CACHE_REFRESH_MS)) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                singleResolveService(service);
                            }
                        }).start();
                    } else {
                        UserError.Log.d(TAG, "Already have recent data for: " + name + " => " + li.address);
                    }
                }
            }


            @Override
            public void onServiceLost(NsdServiceInfo service) {
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                myStopServiceDiscovery();
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                if (JoH.ratelimit("mdns-onStopDiscoveryFailed", 10)) {
                    myStopServiceDiscovery();
                }
            }

            private void myStopServiceDiscovery() {
                try {
                    mNsdManager.stopServiceDiscovery(this);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    UserError.Log.e(TAG, "Could not stop service discovery: " + e);
                }
            }
        };
    }

    private NsdManager.ResolveListener initializeResolveListener() {
        return new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                if (JoH.quietratelimit("mdns-error", 30))
                    UserError.Log.e(TAG, "Resolve failed " + errorCode);
                if (errorCode == 3) {
                    errorCounter++;
                    if (errorCounter > 5) {
                        errorCounter = 0;
                        if (JoH.pratelimit("mdns-total-restart", 86400)) {
                            UserError.Log.wtf(TAG, "Had to do a complete restart due to MDNS failures");
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    }
                }
                try {
                    mNsdManager.stopServiceDiscovery(mDiscoveryListener);
                } catch (Exception e) {
                    UserError.Log.d(TAG, "Failed to stop service discovery on failure: " + e);
                }
                outstanding.decrementAndGet();
                locked_until = 0;
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {

                final InetAddress host = serviceInfo.getHost();
                final String address = host.getHostAddress();
                UserError.Log.d(TAG, serviceInfo.getServiceName() + " Resolved address = " + address);
                final String short_name = shortenName(serviceInfo.getServiceName().toLowerCase());
                if (!address.contains(":") || (iplookup.get(short_name) == null) || (JoH.msSince(iplookup.get(short_name).received) > 60000)) {
                    iplookup.put(short_name, new LookUpInfo(address, JoH.tsl(), serviceInfo));
                } else {
                    UserError.Log.d(TAG, "Skipping overwrite of " + short_name + " with " + address + " due to ipv4 priority");
                }
                outstanding.decrementAndGet();
                locked_until = 0;
            }
        };
    }


    // data for MegaStatus
    public static List<StatusItem> megaStatus(final Context context) {
        Mdns.superFastResolve("dummyentry1234");
        final List<StatusItem> l = new ArrayList<>();
        //  l.add(new StatusItem());
        l.add(new StatusItem("Local Network", StatusItem.Highlight.NORMAL));
        for (final Map.Entry<String, LookUpInfo> entry : iplookup.entrySet()) {
            final long status_time = entry.getValue().received;
            if (JoH.msSince(status_time) < Constants.HOUR_IN_MS) {
                l.add(new StatusItem(entry.getValue().prettyName().replaceFirst(" ", "\n"),
                        entry.getValue().address + ((status_time != 0) ? ("\n" + JoH.niceTimeSince(status_time) + " " + "ago").replaceFirst("[0-9]+ second", "second") : ""),
                        StatusItem.Highlight.NORMAL,
                        "long-press",
                        new Runnable() {
                            @Override
                            public void run() {

                                // TODO: probe port 50005?
                                final String receiver_list = Pref.getStringDefaultBlank("wifi_recievers_addresses").trim().toLowerCase();
                                final String new_receiver = entry.getKey().toLowerCase() + ".local" + ":50005";

                                if (!receiver_list.contains(entry.getKey().toLowerCase() + ".local")) {
                                    // add item
                                    final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    String new_receiver_list = (receiver_list.length() > 0) ? receiver_list + "," + new_receiver : new_receiver;
                                                    UserError.Log.d(TAG, "Updating receiver list to: " + new_receiver_list);
                                                    Pref.setString("wifi_recievers_addresses", new_receiver_list);
                                                    JoH.static_toast_long("Added receiver: " + JoH.ucFirst(entry.getKey()));
                                                    break;
                                            }
                                        }
                                    };
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Add " + JoH.ucFirst(entry.getKey()) + " to list of receivers?");
                                    builder.setMessage("Is this device running a collector?\n\n" + entry.getKey() + ".local can be automatically added to list of receivers").setPositiveButton("Add", dialogClickListener)
                                            .setNegativeButton(gs(R.string.no), dialogClickListener).show();
                                } else {
                                    // remove item
                                    final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    String new_receiver_list = receiver_list.replace(new_receiver, "").replace(",,", ",").replaceFirst(",$", "").replaceFirst("^,", "");
                                                    UserError.Log.d(TAG, "Updating receiver list to: " + new_receiver_list);
                                                    Pref.setString("wifi_recievers_addresses", new_receiver_list);
                                                    JoH.static_toast_long("Removed receiver: " + JoH.ucFirst(entry.getKey()));
                                                    break;
                                            }
                                        }
                                    };
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Remove " + JoH.ucFirst(entry.getKey()) + " from list of receivers?");
                                    builder.setPositiveButton("Remove", dialogClickListener)
                                            .setNegativeButton(gs(R.string.no), dialogClickListener).show();
                                }


                            }
                        }));
            }
        }
        if (l.size() > 1) {
            if (JoH.quietratelimit("mdns-check-showcase", 5)) {
                JoH.runOnUiThreadDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startupInfo(context);
                    }
                }, 1500);
            }
            return l;
        } else {
            return new ArrayList<>();
        }
    }

    // showcase info
    private static void startupInfo(Context context) {

        if (MegaStatus.runnableView == null) return;
        if (JoH.quietratelimit("mdns-showcase", 60)) {
            final boolean oneshot = true;
            final int option = Home.SHOWCASE_MDNS;
            if ((oneshot) && (ShotStateStore.hasShot(option))) return;

            // This could do with being in a utility static method also used in Home
            final int size1 = 200;
            final int size2 = 70;
            final String title = "Tap to add or remove";
            final String message = "Devices discovered on the local network can be added or removed as collectors by tapping on them.";
            final ViewTarget target = new ViewTarget(MegaStatus.runnableView);
            final Activity activity = (Activity) context;

            JoH.runOnUiThreadDelayed(new Runnable() {
                                         @Override
                                         public void run() {
                                             final ShowcaseView myShowcase = new ShowcaseView.Builder(activity)

                                                     .setTarget(target)
                                                     .setStyle(R.style.CustomShowcaseTheme2)
                                                     .setContentTitle(title)
                                                     .setContentText("\n" + message)
                                                     .setShowcaseDrawer(new JamorhamShowcaseDrawer(activity.getResources(), activity.getTheme(), size1, size2, 255))
                                                     .singleShot(oneshot ? option : -1)
                                                     .build();
                                             myShowcase.setBackgroundColor(Color.TRANSPARENT);
                                             myShowcase.setShouldCentreText(false);
                                             myShowcase.setBlocksTouches(true);
                                             myShowcase.show();
                                         }
                                     }
                    , 10);
        }
    }

}
