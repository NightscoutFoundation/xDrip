package com.eveningoutpost.dexdrip.ui.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import android.util.LruCache;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;

import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.renderer.BitmapCacheProvider;

/**
 * jamorham
 *
 * Bitmap loader / rotator / scaler + cache which avoids holding native memory
 */


public class BitmapLoader implements BitmapCacheProvider {

    private static final String TAG = BitmapLoader.class.getSimpleName();
    private static final LruCache<String, Bundle> cache = new LruCache<>(150); // items


    private BitmapLoader() {
        // use getInstance();
    }

    private static class SingletonHelper {
        private static final BitmapLoader INSTANCE = new BitmapLoader();
    }

    public static BitmapLoader getInstance() {
        return SingletonHelper.INSTANCE;
    }

    public static Bitmap load(int resource) {

        return load(resource, 0f);
    }

    public static void loadAndSetKey(PointValue point, int resource, float rotation) {
        load(resource, rotation); // exercise cache
        point.setLabelBitMapKey(getCacheName(resource, rotation, 1f));
    }

    public static Bitmap bitmapFromBundleCache(final String key) {
        final Bundle bundle = cache.get(key);
        if (bundle != null) {
            final Bitmap result = bundle.getParcelable("bitmap");
            return result;
        } else {
            return null;
        }
    }

    // This is a bit crappy, find a better way of storing the details in the point value
    private static Bitmap load(String key) {
        final String[] splits = key.split("-");
        return load(Integer.parseInt(splits[0]), Float.parseFloat(splits[1]));
    }

    public static Bitmap load(int resource, float rotation) {
        final String cacheName = getCacheName(resource, rotation, 1f);
        Bitmap result = bitmapFromBundleCache(cacheName);
        if (result == null) {
            // TODO is this backwards compatible?
            try {
                result = BitmapFactory.decodeResource(xdrip.getAppContext().getResources(), resource);
            } catch (Exception e) {
                // meh
            }
            try {
                if (result == null) {
                    result = getBitmapFromVectorDrawable(resource);
                }
                if (rotation != 0f) {
                    final Matrix matrix = new Matrix();
                    matrix.postRotate(rotation);
                    result = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), matrix, true);
                }
                // cache.put(cacheName, result);
                saveBitmapAsBundle(cacheName, result);
            } catch (Exception e) {
                UserError.Log.wtf(TAG, "Could not load vector drawable!");
            }
        } else {

        }
        return result;
    }

    public static void deleteCacheEntry(String key) {
        if (key != null) {
            cache.remove(key);
        }
    }

    public static void saveBitmapAsBundle(String key, Bitmap bitmap) {
        final Bundle bundle = new Bundle();
        bundle.putParcelable("bitmap", bitmap);
        cache.put(key, bundle);
    }


    private static Bitmap getBitmapFromVectorDrawable(int drawableId) {
        Drawable drawable = AppCompatResources.getDrawable(xdrip.getAppContext(), drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }
        final Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private static String getCacheName(int resourceId, float rotation, float scale) {
        if (scale == 1f) {
            return resourceId + "-" + rotation;
        } else {
            return resourceId + "-" + rotation + "-" + scale;
        }
    }

    private static int getBundleSizeInBytes(Bundle bundle) {
        final Parcel parcel = Parcel.obtain();
        parcel.writeBundle(bundle);
        final int size = parcel.dataSize();
        parcel.recycle();
        return size;
    }


    // Accessed via interface - protect with proguard or silently fails?
    public String getScaledKeyName(String key, final double scaler) {
        if (scaler != 1d) {
            key += "-" + scaler;
        }
        return key;
    }

    // Accessed via interface
    //    @Override
    public Bitmap loadScaledBitmap(final String key) {
        return bitmapFromBundleCache(key);
    }

    // Accessed via interface
    public String prepareScaledBitmap(final String originalKey, final double scaler) {
        final String key = getScaledKeyName(originalKey, scaler);
        if (cache.get(key) == null) {
            final Bitmap discard = loadScaledBitmap(originalKey, scaler);
        }
        return key;
    }

    // Accessed via interface - protect with proguard or silently fails
    //  @Override
    public Bitmap loadScaledBitmap(final String originalKey, final double scaler) {
        String key = originalKey;
        if (scaler != 1f) {
            key += "-" + scaler;
        }

        Bitmap result = bitmapFromBundleCache(key);

        if (result == null) {
            final Bitmap labelBitmap = load(originalKey);
            result = Bitmap.createScaledBitmap(labelBitmap,
                    (int) (labelBitmap.getWidth() * scaler),
                    (int) (labelBitmap.getHeight() * scaler),
                    false);
            saveBitmapAsBundle(key, result);
        }
        return result;
    }

}
