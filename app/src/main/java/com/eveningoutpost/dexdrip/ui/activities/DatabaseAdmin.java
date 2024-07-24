package com.eveningoutpost.dexdrip.ui.activities;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.databinding.ObservableField;
import android.os.Bundle;

import com.activeandroid.Cache;
import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.databinding.ActivityDatabaseAdminBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.models.JoH.mapSortedByValue;

/**
 * jamorham
 *
 * UI for various database administration tasks
 */

public class DatabaseAdmin extends BaseAppCompatActivity {

    private static final String TAG = "DatabaseAdmin";
    private static final boolean D = false;

    private ActivityDatabaseAdminBinding binding;
    private final DbAdminProcessor consoleProcessor = new ConsoleResults();
    private final DbAdminProcessor databaseSize = new DatabaseSize();
    // view model
    public final ObservableField<String> console = new ObservableField<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDatabaseAdminBinding.inflate(getLayoutInflater());
        binding.setVm(this);
        setContentView(binding.getRoot());
        JoH.fixActionBar(this);

        console.set("Ready\n");
        getDbSize();

    }

    public void getDbSize() {
        dbTask(databaseSize, "pragma database_list");
    }

    public void quickCheck() {
        dbTask(consoleProcessor, "pragma quick_check");
    }

    public void longCheck() {
        dbTask(consoleProcessor, "pragma integrity_check");
    }

    public void compact() {
        dbTask(new CompactSequenceProcessor(), (String) null);
    }

    public void statistics() {
        dbTask(new Statistics(), "analyze", "select * from sqlite_stat1");
    }


    // perform a task of multiple sql queries and feed results to a processor
    private void dbTask(final DbAdminProcessor processor, final String... sql) {
        dbTask(true, processor, sql);
    }

    // perform a task of multiple sql queries and feed results to a processor
    private synchronized void dbTask(boolean pleaseWait, final DbAdminProcessor processor, final String... sql) {
        if (pleaseWait) {
            console.set("Please wait\n");
        }
        Inevitable.task("database-admin-task", 100, () -> {
            try {
                final List<String> results = new ArrayList<>();
                if (sql != null) {
                    for (final String query : sql) {
                        if (query == null) {
                            continue;
                        }
                        results.addAll(executeSQL(query));
                    }
                }
                processor.process(results);

            } catch (Exception e) {
                console.set("ERROR: " + e);
            }
        });
    }

    // executes some SQL and returns all the results as a string list
    private synchronized List<String> executeSQL(final String query) {
        final List<String> results = new ArrayList<>();
        final SQLiteDatabase db = Cache.openDatabase();
        final boolean transaction = !query.equals("vacuum");
        if (transaction) db.beginTransaction();
        try {
            final Cursor cursor = db.rawQuery(query, null);
            Log.d(TAG, "Got query results: " + query + " " + cursor.getCount());

            while (cursor.moveToNext()) {
                for (int c = 0; c < cursor.getColumnCount(); c++) {
                    if (D) Log.d(TAG, "Column: " + cursor.getColumnName(c));
                    results.add(cursor.getString(c));
                }
            }
            cursor.close();

            if (transaction) db.setTransactionSuccessful();
        } finally {
            if (transaction) db.endTransaction();
        }
        return results;
    }

    // prints results to the console
    class ConsoleResults implements DbAdminProcessor {
        @Override
        public void process(List<String> results) {
            final StringBuilder sb = new StringBuilder();
            for (final String result : results) {
                if (!emptyString(result)) {
                    sb.append(result);
                    sb.append("\n");
                }
            }
            if (sb.length() > 1) console.set(sb.toString());
        }
    }

    // report on database size / readability
    class DatabaseSize implements DbAdminProcessor {

        private String insert = "";

        DatabaseSize() {
        }

        DatabaseSize(final String insert) {
            this.insert = insert;
        }

        @Override
        public void process(List<String> results) {
            if (results.size() == 0) {
                console.set("Cannot locate database!!");
            } else {
                try {
                    String filename = "";
                    for (int p = 0; p < results.size(); p += 3) {
                        if (results.get(p + 1).equals("main")) {
                            filename = results.get(p + 2);
                            break;
                        }
                    }
                    final File dbFile = new File(filename);
                    if (dbFile.exists()) {
                        consoleAppend("Database " + insert + " size: " + JoH.roundFloat(((float) dbFile.length()) / (1024 * 1024), 2) + "M " + (dbFile.canWrite() ? "" : " CANNOT WRITE") + (dbFile.canRead() ? "" : " CANNOT READ") + "\n");
                    } else {
                        console.set("Cannot find database file! " + filename);
                    }
                } catch (Exception e) {
                    consoleAppend("ERROR: " + e);
                }
            }
        }
    }


    // report on index sizes
    class Statistics implements DbAdminProcessor {

        @Override
        public void process(List<String> results) {

            console.set("");
            final HashMap<String, Integer> output = new HashMap<>();
            String table = "";
            int state = 0;
            for (final String result : results) {
                if (result != null && result.length() > 3) {
                    switch (state) {
                        case 0:
                            table = result;
                            break;
                        case 2:
                            try {
                                final int firstValue = JoH.tolerantParseInt(result.substring(0, result.indexOf(" ")), 0);
                                if (!output.containsKey(table) || output.get(table) < firstValue) {
                                    Log.d(TAG, "New high value: " + table + " " + firstValue);
                                    output.put(table, firstValue);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Exception in statistics: " + e);
                            }
                            break;
                    }
                }
                state = (state + 1) % 3;
            }
            final SortedSet<Map.Entry<String, Integer>> sorted = mapSortedByValue(output, true);
            boolean stuff = false;
            for (Map.Entry<String, Integer> entry : sorted) {
                if (entry.getValue() > 100) {
                    stuff = true;
                    consoleAppend(entry.getKey() + ": " + entry.getValue() + "\n");
                }
            }
            if (!stuff) {
                consoleAppend("No significant table data detected\n");
            }
        }
    }

    // database compaction sequencer
    private class CompactSequenceProcessor extends BaseSequenceProcessor implements DbAdminProcessor {

        CompactSequenceProcessor() {
            this.startMessage = "Compacting - please wait";
            this.endMessage = "Done";
            this.queries = new String[]{"pragma database_list", "vacuum", "pragma database_list"};
            this.processors = new DbAdminProcessor[]{new DatabaseSize("Before"), consoleProcessor, new DatabaseSize("After")};
        }
    }

    // process a sequence of queries and processors - base class
    abstract class BaseSequenceProcessor implements DbAdminProcessor {

        String startMessage = "";
        String endMessage = "";
        String[] queries = null;
        DbAdminProcessor[] processors = null;

        private volatile int state = 0;

        @Override
        public synchronized void process(final List<String> results) {
            if (queries == null || processors == null) {
                Log.e(TAG, this.getClass().getSimpleName() + " not properly initialized");
                return;
            }
            try {
                final int runState = state;
                state++;

                if (runState > 0) {
                    if (D)
                        Log.d(TAG, "Using processor: " + processors[runState - 1].getClass().getSimpleName());
                    processors[runState - 1].process(results);
                } else {
                    // no results on first item
                    console.set(startMessage + "\n");
                }
                if (runState < queries.length) {
                    if (D) Log.d(TAG, "Running query: " + queries[runState]);
                    dbTask(false, this, queries[runState]);
                } else {
                    consoleAppend(endMessage + "\n");
                }
            } catch (Exception e) {
                console.set("" + e);
            }
        }
    }


    // interface for processing results from executeSQL
    interface DbAdminProcessor {
        void process(List<String> results);
    }

    // this is probably not very efficient but isn't used very much
    private void consoleAppend(final String text) {
        console.set(console.get() + text);
    }

}
