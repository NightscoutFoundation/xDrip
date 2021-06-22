package com.eveningoutpost.dexdrip.watch.thinjam.assets;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.watch.thinjam.io.GetURL;

import java.util.Locale;

import lombok.val;

import static com.eveningoutpost.dexdrip.watch.thinjam.assets.AssetPackage.parse;

// jamorham

public class AssetDownload {

    private static final String TAG = "BlueJayAsset";
    private static final String ASSET_URL = "https://cdn159875.bluejay.website/cdn/assets/";

    public static byte[] getAsset(final String mac, final int assetId, final String locale) {
        return parse(getAssetRemote(mac, assetId, locale), assetId);
    }

    public static byte[] getAssetRemote(final String mac, final int assetId, final String locale) {
        val filename = "asset-" + assetId + ".dat";
        val localePath = JoH.emptyString(locale) ? "" : locale + "/";
        return GetURL.getURLbytes(String.format(Locale.US, "%s%s%s?mac=%s", ASSET_URL, localePath, filename, mac));
    }

}
