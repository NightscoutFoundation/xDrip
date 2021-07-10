package com.eveningoutpost.dexdrip.watch.thinjam.firmware;

// jamorham

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;

public class BlueJayManifest {

    @Expose
    public String fileName;
    @Expose
    public int boardId;
    @Expose
    public int type;
    @Expose
    public int version;

    private static final String TAG = "BlueJayManifest";

    public static List<BlueJayManifest> parseToList(final String manifestString) {
        if (!emptyString(manifestString)) {
            try {
                final Type queueType = new TypeToken<ArrayList<BlueJayManifest>>() {
                }.getType();
                return JoH.defaultGsonInstance().fromJson(manifestString, queueType);
            } catch (JsonSyntaxException e) {
                UserError.Log.e(TAG, "Exception parsing manifest: " + e);
            }
        }
        return null;
    }
}