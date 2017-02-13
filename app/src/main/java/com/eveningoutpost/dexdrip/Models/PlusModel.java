package com.eveningoutpost.dexdrip.Models;

import com.activeandroid.Model;
import com.activeandroid.util.SQLiteUtils;

/**
 * Created by jamorham on 01/02/2017.
 */

public class PlusModel extends Model {

    private static boolean patched = false;

    protected synchronized static void fixUpTable(String[] schema) {
        if (patched) return;

        for (String patch : schema) {
            try {
                SQLiteUtils.execSql(patch);
            } catch (Exception e) {
                //
            }
        }
        patched = true;
    }

}
