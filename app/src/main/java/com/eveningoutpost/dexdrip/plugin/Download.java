package com.eveningoutpost.dexdrip.plugin;

import static com.eveningoutpost.dexdrip.models.JoH.decompressBytesToBytes;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.Disabled;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.setDexCollectionType;
import static com.eveningoutpost.dexdrip.utils.FileUtils.writeToFile;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import lombok.val;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * JamOrHam
 *
 * Plugin downloader
 */

public class Download {

    private static final String TAG = "PluginDownload";
    private static final String SCHEME = "https://";

    private static byte[] getData(final PluginDef pluginDef) {
        if (pluginDef == null) return null;
        val url = getUrl(pluginDef);
        val client = new OkHttpClient();
        val builder = new Request.Builder().url(url);
        val request = builder.build();
        UserError.Log.d(TAG, "REQUEST URL: " + request.url());
        try {
            val response = client.newCall(request).execute();
            if (response.code() == 410) {
                UserError.Log.wtf(TAG, "Shutdown requested");
                setDexCollectionType(Disabled);
            }
            if (response.isSuccessful()) {
                return response.body().bytes();
            } else {
                throw new RuntimeException("Got failure response code: " + response.code() + "\n" + (response.body() != null ? response.body().string() : ""));
            }
        } catch (IOException e) {
            UserError.Log.e(TAG, "Exception getting plugin: " + e);
            JoH.static_toast_long("Problem downloading plugin!");
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] getSizedBlock(final ByteBuffer bb) {
        val b1size = bb.getInt();
        if (b1size < 0 || b1size > 10000000) return null;
        val b1 = new byte[b1size];
        bb.get(b1);
        return b1;
    }

    public static boolean get(final PluginDef pluginDef) {
        try {
            val bytes = getData(pluginDef);
            if (bytes == null) return false;
            val bb = ByteBuffer.wrap(bytes);
            val b1 = getSizedBlock(bb);
            val b2 = getSizedBlock(bb);
            val b3 = getSizedBlock(bb);
            val ok = Verify.verify(b1, b2);
            if (ok && b3 != null) {
                val storagePath = xdrip.getAppContext().getFilesDir().getPath();
                val fileStruct = storagePath + "/" + pluginDef.name;
                writeToFile(TAG, fileStruct + ".dex", decompressBytesToBytes(b1));
                writeToFile(TAG, fileStruct + ".sig", b3);
                writeToFile(TAG, fileStruct + ".ver", pluginDef.version.getBytes(StandardCharsets.UTF_8));
                UserError.Log.ueh(TAG, pluginDef.canonical() + " plugin successfully downloaded");
                JoH.static_toast_long("Plugin successfully downloaded");
                Home.staticRefreshBGCharts();
                return true;
            } else {
                UserError.Log.e(TAG, "Invalid verification for " + pluginDef.name);
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Exception in get() " + e);
        } finally {
            pluginDef.reset(); // move out of loading state
        }

        return false;
    }

    private static String getUrl(final PluginDef pluginDef) {
        return SCHEME + pluginDef.repository + "/" + pluginDef.canonical() + ".bin";
    }

}