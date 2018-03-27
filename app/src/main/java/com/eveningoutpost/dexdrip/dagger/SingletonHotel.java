package com.eveningoutpost.dexdrip.dagger;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Created by jamorham on 18/01/2018.
 *
 * Are you a lonely singleton object?
 *
 * Do you wish you could gain the advantages of lazy instantiation, shared resources with
 * flexible dependency injection, but without all that extra boiler plate code?
 *
 * Would you like to separate the concern of creating your objects but also keep things
 * as clear and readable as possible without multiple lines of annotations?
 *
 * What about avoiding the risk of failure to add interface methods for every child class
 * resulting in run-time null pointer exceptions that are not visible at compile time?
 *
 * If this sounds good to you, then come on in to the Singleton Hotel!
 *
 * This is the parent template class that handles lookup and caching
 *
 */

@SuppressWarnings("WeakerAccess")
abstract class SingletonHotel {

    private final HashMap<String, Object> cache = new HashMap<>();
    private final Class searchClass;
    private Field[] fields = null;

    // set the search class
    protected SingletonHotel(Class sClass) {
        this.searchClass = sClass;
    }

    // get a cached object or find and add it to the cache
    protected Object getObject(String singleton) {
        final Object cached = cache.get(singleton);
        if (cached == null) {
            final Object fieldObject = getFieldObject(singleton);
            if (fieldObject == null) {
                throw new RuntimeException("Cannot resolve object: " + singleton + " have you named it properly at both ends and placed it in the Singleton hotel?");
            }
            synchronized (cache) {
                cache.put(singleton, fieldObject);
            }
            return fieldObject;
        } else {
            return cached;
        }

    }

    // case insensitive reflective field search
    private Object getFieldObject(String singleton) {
        if (fields == null) {
            initFields();
        }
        try {
            singleton = singleton.toLowerCase();
            for (Field field : fields) {
                if (field.getName().equalsIgnoreCase(singleton)) {
                    return field.get(this);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
        return null;
    }

    // populate the list of fields
    private synchronized void initFields() {
        if (fields == null) {
            fields = searchClass.getDeclaredFields();
        }
    }
}
