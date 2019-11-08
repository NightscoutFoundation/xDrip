package com.eveningoutpost.dexdrip.watch;

import android.support.v4.util.ArrayMap;

import java.util.Map;

public class PrefBindingFactory{
    private static volatile  Map<Class,PrefBinding> binding = new ArrayMap<>();

    public static <T extends PrefBinding> T  getInstance(Class<T> c) {
        T inst = null;
        if(!binding.containsKey(c)){
            try {
                binding.put(c, c.newInstance());
            } catch (Exception e) {
                return inst;
            }
        }
        inst = (T) binding.get(c);
        return inst;
    }
}
