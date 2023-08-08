package com.eveningoutpost.dexdrip.utils;

// created by jamorham

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.activeandroid.Cache;
import com.eveningoutpost.dexdrip.models.UserError;

import lombok.NonNull;


/* Wouldn't it be nice if you could search and replace inside an sqlite schema?
 * That is what the SqliteRejigger is for!
 */

public class SqliteRejigger {

    private static final String TAG = SqliteRejigger.class.getSimpleName();
    private static final boolean d = false;

    public static synchronized boolean rejigSchema(@NonNull String table_name, @NonNull String search, @NonNull String replace) {

        final String temp_table = table_name + "_temp";

        final String existing_schema = getSchema(table_name);

        if (d) UserError.Log.d(TAG, existing_schema);

        if (existing_schema.contains(search)) {
            final String sql =
                    "PRAGMA foreign_keys=off;\n" +
                            "BEGIN TRANSACTION;\n" +
                            "DROP TABLE IF EXISTS " + temp_table + ";\n" +
                            "ALTER TABLE " + table_name + " RENAME TO " + temp_table + ";\n" +
                            existing_schema.replace(search, replace).replace("CREATE INDEX index_", "CREATE INDEX index__") +
                            "INSERT INTO " + table_name + " SELECT * FROM " + temp_table + ";\n" +
                            "DROP TABLE " + temp_table + ";\n" +
                            "COMMIT;\n" +
                            "PRAGMA foreign_keys=on;\n";
            if (d) UserError.Log.d(TAG, sql);
            try {
                executeBatchSQL(sql);
                UserError.Log.d(TAG, "Rejig probably successful for " + table_name + " " + replace);
                return true;
            } catch (SQLException e) {
                UserError.Log.e(TAG, "Unable to rejig " + e + " on " + table_name + " " + replace);
                return false;
            }

        } else {
            UserError.Log.d(TAG, search + " not found in schema for " + table_name + " presumably this patch has already been applied");
            return false;
        }

    }

    private static void executeBatchSQL(String batch) throws SQLException {
        final String[] statements = batch.split("\n");
        final SQLiteDatabase db = Cache.openDatabase();
        db.beginTransaction();
        try {
            for (String query : statements) {
                db.execSQL(query);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static String getSchema(String table_name) {
        final StringBuilder sb = new StringBuilder();
        final Cursor cursor = Cache.openDatabase().rawQuery("select sql from sqlite_master where type in ('table', 'index') and tbl_name = ?", new String[]{table_name});
        while (cursor.moveToNext()) {
            final String line = cursor.getString(0);
            if (line != null) {
                sb.append(line);
                sb.append(";\n");
            }
        }
        cursor.close();
        return sb.toString();
    }
}
