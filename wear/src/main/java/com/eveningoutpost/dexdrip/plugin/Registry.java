package com.eveningoutpost.dexdrip.plugin;

import static com.eveningoutpost.dexdrip.plugin.Cache.erase;

import java.util.HashMap;

import lombok.val;

/**
 * JamOrHam
 * <p>
 * Plugin registry with name, author, version and repository location
 */

public class Registry {

    private static final HashMap<String, PluginDef> registry = new HashMap<>();

    static {
        add(new PluginDef("zarquon", "jamorham", "2.1", "rgate1.local"));
        add(new PluginDef("zarquot", "jamorham", "2.1", "rgate-staging.local"));
        add(new PluginDef("keks", "jamorham", "1.3", "plugin1.beonlabs.net"));
    }

    private static void add(final PluginDef pluginDef) {
        registry.put(pluginDef.name, pluginDef);
    }

    public static PluginDef get(final String name) {
        return registry.get(name);
    }

    public static synchronized void eraseAll() {
       for (val p : registry.entrySet()) {
           erase(p.getValue());
           p.getValue().reset();
       }
    }

}
