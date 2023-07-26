package com.eveningoutpost.dexdrip.webservices;

import android.content.Intent;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.localeTasker.bundle.PluginBundleManager;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.BuildConfig.APPLICATION_ID;
import static com.eveningoutpost.dexdrip.localeTasker.bundle.PluginBundleManager.BUNDLE_EXTRA_INT_VERSION_CODE;

/**
 * Created by jamorham on 06/01/2018.
 * <p>
 * forwards requests to tasker interface via an intent
 * <p>
 * eg http://127.0.0.1:17580/tasker/SPEAK+NOW
 * <p>
 * always responds positively even if the command is completely invalid
 */

public class WebServiceTasker extends BaseWebService {

    private static String TAG = "WebServiceTasker";

    // process the request and produce a response object
    public WebResponse request(String query) {

        query = stripFirstComponent(query);
        if (query.length() > 0) {
            UserError.Log.d(TAG, "Processing " + query);

            // package up the string and send it to the tasker interface
            final Bundle bundle = new Bundle();
            bundle.putString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE, query);
            bundle.putInt(BUNDLE_EXTRA_INT_VERSION_CODE, 1);

            final Intent messageIntent = new Intent(com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING);
            messageIntent.setPackage(APPLICATION_ID);
            messageIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, bundle);

            xdrip.getAppContext().sendBroadcast(messageIntent);

            return new WebResponse("Forwarded to tasker interface\n", 200, "text/plain");
        } else {
            return new WebResponse("No tasker command specified\n", 404, "text/plain");
        }
    }
}
