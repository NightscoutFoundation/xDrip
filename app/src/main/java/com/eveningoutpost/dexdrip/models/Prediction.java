package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Created by jamorham on 11/06/2018.
 */

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Prediction", id = BaseColumns._ID)
public class Prediction extends PlusModel {

    private static boolean patched = false;
    private final static String TAG = Prediction.class.getSimpleName();
    private final static boolean d = false;

    private static final String[] schema = {
            "CREATE TABLE Prediction (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
            "ALTER TABLE Prediction ADD COLUMN timestamp INTEGER;",
            "ALTER TABLE Prediction ADD COLUMN glucose REAL;",
            "ALTER TABLE Prediction ADD COLUMN source TEXT;",
            "ALTER TABLE Prediction ADD COLUMN note TEXT;",
            "CREATE INDEX index_Prediction_source on Prediction(source);",
            "CREATE UNIQUE INDEX index_Prediction_timestamp on Prediction(timestamp);"};


    @Expose
    @Column(name = "timestamp", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public long timestamp;

    @Expose
    @Column(name = "glucose")
    public double glucose;

    @Expose
    @Column(name = "source")
    public String source;

    @Expose
    @Column(name = "note")
    public String note;


    public static Prediction create(long timestamp, int glucose, String source) {
        final Prediction prediction = new Prediction();
        prediction.timestamp = timestamp;
        prediction.glucose = glucose;
        prediction.source = source;
        return prediction;
    }

    public Prediction addNote(String note) {
        this.note = note;
        return this;
    }


    public String toS() {
        return JoH.defaultGsonInstance().toJson(this);
    }

    // static methods

    public static Prediction last() {
        try {
            return new Select()
                    .from(Prediction.class)
                    .orderBy("timestamp desc")
                    .executeSingle();
        } catch (android.database.sqlite.SQLiteException e) {
            updateDB();
            return null;
        }
    }

    public static List<Prediction> latestForGraph(int number, double startTime) {
        return latestForGraph(number, (long) startTime, Long.MAX_VALUE);
    }

    public static List<Prediction> latestForGraph(int number, long startTime) {
        return latestForGraph(number, startTime, Long.MAX_VALUE);
    }

    public static List<Prediction> latestForGraph(int number, long startTime, long endTime) {
        try {
            final List<Prediction> results = new Select()
                    .from(Prediction.class)
                    .where("timestamp >= " + Math.max(startTime, 0))
                    .where("timestamp <= " + endTime)
                    .orderBy("timestamp asc") // warn asc!
                    .limit(number)
                    .execute();

            return results;
        } catch (android.database.sqlite.SQLiteException e) {
            updateDB();
            return new ArrayList<>();
        }
    }


    public static List<Prediction> cleanup(int retention_days) {
        return new Delete()
                .from(Prediction.class)
                .where("timestamp < ?", JoH.tsl() - (retention_days * 86400000L))
                .execute();
    }


    // create the table ourselves without worrying about model versioning and downgrading
    public static void updateDB() {
        patched = fixUpTable(schema, patched);
    }
}



