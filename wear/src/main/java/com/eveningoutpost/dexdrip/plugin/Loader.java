package com.eveningoutpost.dexdrip.plugin;

import static com.eveningoutpost.dexdrip.plugin.Cache.getPath;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;

import java.lang.reflect.Method;
import java.util.HashMap;

import dalvik.system.PathClassLoader;
import lombok.AllArgsConstructor;
import lombok.val;

/**
 * JamOrHam
 *
 * Plugin virtual environment cache loader
 */

public class Loader {

    private static final String TAG = "Plugin";
    private static final HashMap<String, Environ> loaderCache = new HashMap<>();

    public static void clear() {
        synchronized (loaderCache) {
            loaderCache.clear();
        }
    }

    public static void unload(final String name) {
        synchronized (loaderCache) {
            loaderCache.remove(name);
        }
    }

    @AllArgsConstructor
    public static class Environ {
        PathClassLoader virtualLoader;
        Method getInstance;

        public Environ(PathClassLoader loader) {
            this.virtualLoader = loader;
        }
    }

    public static synchronized IPluginDA getLocalInstance(final PluginDef def, final String parameter) {
        switch (def.name) {
            case "keks":
                return jamorham.keks.Plugin.getInstance(parameter);
            default:
                throw new RuntimeException("Unknown local plugin " + def.name);
        }
    }

    public static synchronized IPluginDA getInstance(final PluginDef def, final String parameter) {
        if (def == null) return null;
        try {
            if (!Consent.isGiven(def)) {
                UserError.Log.wtf(TAG, "User has not yet consented to use of plugin: " + def.name);
                return null;
            }

            if (!def.isReady()) {
                Cache.refresh(def);
                if (!def.isReady()) {
                    return null;
                }
            }
            Environ environ;
            synchronized (loaderCache) {
                if (!loaderCache.containsKey(def.name)) {
                    val loader = new PathClassLoader(getPath(def), xdrip.getAppContext().getClassLoader());
                    loaderCache.put(def.name, new Environ(loader));
                }
                environ = loaderCache.get(def.name);
            }
            if (environ == null) {
                UserError.Log.e(TAG, "Cannot get loader");
                return null;
            }
            try {
                if (environ.getInstance == null) {
                    val c = environ.virtualLoader.loadClass(def.pname() + TAG);
                    UserError.Log.d(TAG, "Loaded from file: " + c.getCanonicalName());
                    environ.getInstance = c.getMethod("getInstance", String.class);
                }
                return (IPluginDA) environ.getInstance.invoke(null, parameter);
            } catch (Exception e) {
                UserError.Log.e(TAG, "Got load exception: " + e);
                unload(def.name);
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception in getInstance: " + e);
        }
        return null;
    }

}
