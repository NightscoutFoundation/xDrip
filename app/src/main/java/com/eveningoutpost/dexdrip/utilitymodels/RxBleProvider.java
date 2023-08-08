package com.eveningoutpost.dexdrip.utilitymodels;

// jamorham

// TODO check this reference handling

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble2.RxBleClient;

import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.plugins.RxJavaPlugins;

public class RxBleProvider {
    private static final ConcurrentHashMap<String, RxBleClient> singletons = new ConcurrentHashMap<>();

    public static synchronized RxBleClient getSingleton(final String name) {
        final RxBleClient cached = singletons.get(name);
        if (cached != null) return cached;
        //UserError.Log.wtf("RxBleProvider", "Creating new instance for: " + name); // TODO DEBUG ONLY
        final RxBleClient created = RxBleClient.create(xdrip.getAppContext());
        singletons.put(name, created);
        RxJavaPlugins.setErrorHandler(e -> UserError.Log.d("RXBLE" + name, "RxJavaError: " + e.getMessage()));
        return created;
    }

    public static RxBleClient getSingleton() {
        return getSingleton("base");
    }

}
