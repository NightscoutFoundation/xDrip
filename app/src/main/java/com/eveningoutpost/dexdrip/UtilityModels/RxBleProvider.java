package com.eveningoutpost.dexdrip.UtilityModels;

// jamorham

// TODO check this reference handling

import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble2.RxBleClient;

import com.polidea.rxandroidble2.LogConstants;
import com.polidea.rxandroidble2.LogOptions;

import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.plugins.RxJavaPlugins;

public class RxBleProvider {
    private static final ConcurrentHashMap<String, RxBleClient> singletons = new ConcurrentHashMap<>();

    public static synchronized RxBleClient getSingleton(final String name) {
        final RxBleClient cached = singletons.get(name);
        if (cached != null) return cached;
        //UserError.Log.wtf("RxBleProvider", "Creating new instance for: " + name); // TODO DEBUG ONLY
        final RxBleClient created = RxBleClient.create(xdrip.getAppContext());
/*        RxBleClient.updateLogOptions(new LogOptions.Builder()
                .setLogLevel(LogConstants.VERBOSE)
                .setMacAddressLogSetting(LogConstants.MAC_ADDRESS_FULL)
                .setUuidsLogSetting(LogConstants.UUIDS_FULL)
                .setShouldLogAttributeValues(true)
                .build()
        );*/
        singletons.put(name, created);
        RxJavaPlugins.setErrorHandler(e -> UserError.Log.d("RXBLE" + name, "RxJavaError: " + e.getMessage()));
        return created;
    }

    public static RxBleClient getSingleton() {
        return getSingleton("base");
    }

}
