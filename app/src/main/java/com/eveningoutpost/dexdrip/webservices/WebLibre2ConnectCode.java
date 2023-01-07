package com.eveningoutpost.dexdrip.webservices;

import android.util.Base64;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.LibreOOPAlgorithm;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class WebLibre2ConnectCode extends BaseWebService {

    private static String TAG = "WebLibre2ConnectCode";

    // process the request and produce a response object
    public WebResponse request(String query) {
        final JSONObject reply = new JSONObject();
        boolean readOnly = true;

        final Map<String, String> cgi = getQueryParameters(query);
        if (cgi.containsKey("ReadOnly")) {
            try {
                readOnly = Boolean.valueOf(cgi.get("ReadOnly"));
            } catch (Exception e) {
                UserError.Log.e(TAG, "Error parsing ReadOnly val, continuing as read only");
            }
        }


        // populate json structures
        try {
            final ActiveBluetoothDevice btDevice = ActiveBluetoothDevice.first();
            if (btDevice == null || btDevice.address == null) {
                UserError.Log.e(TAG, "ActiveBluetoothDevice has no valid bt device");
                return new WebResponse(reply.toString()); // I wonder how this will work with the recieving side???
            }
            final String deviceAddress = btDevice.address;

            byte[] btUnlockBuffer = LibreOOPAlgorithm.btSendgetBluetoothEnablePayload(!readOnly);
            if(btUnlockBuffer == null) {
                UserError.Log.e(TAG, "btSendgetBluetoothEnablePayload returned null");
                return new WebResponse(reply.toString()); // I wonder how this will work with the recieving side???
            }
            reply.put(Intents.BT_UNLOCK_BUFFER, Base64.encodeToString(btUnlockBuffer, Base64.NO_WRAP));
            reply.put(Intents.DEVICE_MAC_ADDRESS, btDevice.address);

            Log.d(TAG, "Output: " + reply.toString());
        } catch (JSONException e) {
            UserError.Log.wtf(TAG, "Got json exception: " + e);
        }

        return new WebResponse(reply.toString());
    }

}
