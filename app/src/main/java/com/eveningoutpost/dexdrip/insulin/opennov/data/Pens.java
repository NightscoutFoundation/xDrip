package com.eveningoutpost.dexdrip.insulin.opennov.data;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.LinkedList;
import java.util.List;

import lombok.Data;
import lombok.val;

@Data
public class Pens {

    private static final String TAG = "OpenNov";
    private static final String KNOWN_PENS = "KNOWN_OPENNOV_PENS";

    public List<Pen> pens = new LinkedList<>();

    @SuppressWarnings("unchecked")
    static List<Pen> getListFromJson(final String json) {
        try {
            val list = (List<Pen>) JoH.defaultGsonInstance().fromJson(json, new TypeToken<LinkedList<Pen>>() {
            }.getType());
            return list;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    static Pens fromJson(final String json) {
        val pens = new Pens();
        try {
            val list = getListFromJson(json);
            if (list != null) {
                pens.pens = list;
                UserError.Log.d(TAG, "Loaded pens size: " + list.size());
            }
        } catch (Exception e) {
            UserError.Log.d(TAG, "Got exception trying to load pens");
        }
        return pens;
    }

    static String getSaved() {
        return Pref.getString(KNOWN_PENS, "");
    }

    public static Pens load() {
        return fromJson(getSaved());
    }

    public boolean hasPenWithSerial(final String serial) {
        for (val pen : pens) {
            if (pen.serial.equals(serial)) return true;
        }
        return false;
    }

    public Pen getPenBySerial(final String serial) {
        synchronized (Pens.class) {
            for (val pen : pens) {
                if (pen.serial.equals(serial)) return pen;
            }
            val pen = new Pen(serial, "");
            pens.add(pen);
            return pen;
        }
    }

    public String getPenTypeBySerial(final String serial) {
        return getPenBySerial(serial).getType();
    }

    public Pens updatePenBySerial(final String serial, final String type) {
        val pen = getPenBySerial(serial);
        pen.setType(type);
        return this;
    }

    String toJson() {
        val gson = new GsonBuilder().create();
        return gson.toJson(this.pens);
    }

    public Pens reload() {
        val list = getListFromJson(getSaved());
        if (list != null) {
            this.pens = list;
        }
        return this;
    }

    public void save() {
        //UserError.Log.d(TAG, "save json: " + toJson());
        Pref.setString(KNOWN_PENS, toJson());
    }

}
