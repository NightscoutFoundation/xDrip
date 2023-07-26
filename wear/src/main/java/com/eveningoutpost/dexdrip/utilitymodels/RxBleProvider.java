package com.eveningoutpost.dexdrip.utilitymodels;

// jamorham

// TODO check this reference handling

import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble2.RxBleClient;

import java.util.concurrent.ConcurrentHashMap;

public class RxBleProvider {
    private static final ConcurrentHashMap<String, RxBleClient> singletons = new ConcurrentHashMap<>();

    public static synchronized RxBleClient getSingleton(final String name) {
        final RxBleClient cached = singletons.get(name);
        if (cached != null) return cached;
        //UserError.Log.wtf("RxBleProvider", "Creating new instance for: " + name); // TODO DEBUG ONLY
        final RxBleClient created = RxBleClient.create(xdrip.getAppContext());
        singletons.put(name, created);
        return created;
    }

    public static RxBleClient getSingleton() {
        return getSingleton("base");
    }

}
