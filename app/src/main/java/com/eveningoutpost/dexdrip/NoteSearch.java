package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.activeandroid.Cache;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utils.ListActivityWithMenu;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Vector;

import lecho.lib.hellocharts.util.ChartUtils;

/**
 * Created by adrian on 04/10/16.
 */
public class NoteSearch extends ListActivityWithMenu {
    public static final int RESTRICT_SEARCH = 8;
    private static final int DEFAULT_TIMEFRAME = 7;
    private Button dateButton1;
    private Button dateButton2;
    private EditText searchTextField;
    private GregorianCalendar date1;
    private GregorianCalendar date2;
    private DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
    private SearchResultAdapter resultListAdapter;
    private Cursor dbCursor;

    private static final String TAG = "NoteSearch";

    private long last_keypress_time = -1;
    private static final long KEY_PAUSE = 500; // latency we use to initiate searches




    @Override
    public String getMenuName() {
        return getString(R.string.note_search);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.OldAppTheme); // or null actionbar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notesearch);

        date1 = new GregorianCalendar();
        date1.set(Calendar.HOUR_OF_DAY, 0);
        date1.set(Calendar.MINUTE, 0);
        date1.set(Calendar.SECOND, 0);
        date1.set(Calendar.MILLISECOND, 0);
        date1.add(Calendar.DATE, -DEFAULT_TIMEFRAME);


        date2 = new GregorianCalendar();
        date2.set(Calendar.HOUR_OF_DAY, 0);
        date2.set(Calendar.MINUTE, 0);
        date2.set(Calendar.SECOND, 0);
        date2.set(Calendar.MILLISECOND, 0);

        resultListAdapter = new SearchResultAdapter();
        setListAdapter(resultListAdapter);

        final Activity activity = this;

        this.getListView().setLongClickable(true);
        this.getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                final SearchResult sResult = (SearchResult) resultListAdapter.getItem(position);

                // edit treatment
                if (sResult != null) {
                    final EditText treatmentText = new EditText(activity);
                    if (sResult.note == null) sResult.note = "";
                    treatmentText.setText(sResult.note);

                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.edit_note)
                            .setMessage(R.string.adjust_note_text_here_there_is_no_undo)
                            .setView(treatmentText)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    sResult.note = treatmentText.getText().toString().trim();
                                    resultListAdapter.notifyDataSetChanged();
                                    SQLiteUtils.execSql("update Treatments set notes = ? where uuid = ?", new String[]{sResult.note, sResult.uuid});
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            })
                            .show();
                }
                return true;
            }
        });

        setupGui();
        doAll(false);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_search, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_searchnote:
                doSearch(true);
                return true;

            case R.id.menu_allnote:
                doAll(true);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void doAll(boolean from_interactive) {

        if (!JoH.ratelimit("NoteSearch-All",2)) return;

        if (from_interactive) hideKeyboard();
        resultListAdapter.clear();
        if (from_interactive) JoH.static_toast_short(getString(R.string.collecting));

        SQLiteDatabase db = Cache.openDatabase();

        if (dbCursor != null && !dbCursor.isClosed()) {
            dbCursor.close();
        }

        long from = date1.getTimeInMillis();

        Calendar endDate = (GregorianCalendar) date2.clone();
        endDate.add(Calendar.DATE, 1);
        long to = endDate.getTimeInMillis();

        dbCursor = db.rawQuery("select timestamp, notes, carbs, insulin, uuid from Treatments where notes IS NOT NULL AND timestamp < " + to + " AND timestamp >= " + from + " ORDER BY timestamp DESC", null);
        dbCursor.moveToFirst();

        int i = 0;
        for (; i < RESTRICT_SEARCH && !dbCursor.isAfterLast(); i++) {
            SearchResult result = new SearchResult(dbCursor.getLong(0), dbCursor.getString(1), dbCursor.getDouble(2), dbCursor.getDouble(3), dbCursor.getString(4));
            resultListAdapter.addSingle(result);
            dbCursor.moveToNext();
        }

        if (i == 0){
            if (from_interactive) JoH.static_toast_short(getString(R.string.nothing_found));
        }
        if (dbCursor.isAfterLast()) {
            dbCursor.close();
        } else {
            SearchResult result = new SearchResult(0, getString(R.string.load_more), 0, 0, null);
            result.setLoadMoreActionFlag();
            resultListAdapter.addSingle(result);
        }

    }

    private void doSearch(boolean from_interactive) {

        //UserError.Log.d(TAG,"Do search: "+from_interactive);
        if (from_interactive) hideKeyboard();

        // filter whitespace
        final String searchTerm = searchTextField.getText().toString().trim();

        if ("".equals(searchTerm)) {
            if (from_interactive) JoH.static_toast_short(getString(R.string.no_search_term_found));
            return;
        }

        // only automatically update when we have 2 chars or more
        if ((!from_interactive) && (searchTerm.length()<2)) return;


        resultListAdapter.clear();
        if (from_interactive) JoH.static_toast_short(getString(R.string.searching));

        SQLiteDatabase db = Cache.openDatabase();

        if (dbCursor != null && !dbCursor.isClosed()) {
            dbCursor.close();
        }


        DatabaseUtils.sqlEscapeString(searchTerm);

        long from = date1.getTimeInMillis();

        Calendar endDate = (GregorianCalendar) date2.clone();
        endDate.add(Calendar.DATE, 1);
        long to = endDate.getTimeInMillis();


        dbCursor = db.rawQuery("select timestamp, notes, carbs, insulin, uuid from Treatments where notes IS NOT NULL AND timestamp < ? AND timestamp >= ? AND notes like ? ORDER BY timestamp DESC", new String[]{Long.toString(to), Long.toString(from), "%" + searchTerm + "%"});
        dbCursor.moveToFirst();

        int i = 0;
        for (; i < RESTRICT_SEARCH && !dbCursor.isAfterLast(); i++) {
            SearchResult result = new SearchResult(dbCursor.getLong(0), dbCursor.getString(1), dbCursor.getDouble(2), dbCursor.getDouble(3), dbCursor.getString(4));
            resultListAdapter.addSingle(result);
            dbCursor.moveToNext();
        }

        if (i == 0 && from_interactive){
            JoH.static_toast_short(getString(R.string.nothing_found));
        }
        if (dbCursor.isAfterLast()) {
            dbCursor.close();
        } else {
            SearchResult result = new SearchResult(0, getString(R.string.load_more), 0, 0, null);
            result.setLoadMoreActionFlag();
            resultListAdapter.addSingle(result);
        }

    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) this
                .getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(searchTextField.getWindowToken(), 0);
    }


    @Override
    protected void onDestroy() {
        if (dbCursor != null && !dbCursor.isClosed()) {
            dbCursor.close();
        }
        super.onDestroy();
    }

    private void setupGui() {
        this.dateButton1 = (Button) findViewById(R.id.button_date1);
        this.dateButton2 = (Button) findViewById(R.id.button_date2);
        this.searchTextField = (EditText) findViewById(R.id.searchTextField);

        updateButtonText();

        //register search button on keyboard
        searchTextField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    doSearch(true);
                    return true;
                }
                return false;
            }
        });

        searchTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
            last_keypress_time = JoH.tsl();
                JoH.runOnUiThreadDelayed(new Runnable(){
                    @Override
                    public void run() {
                        searchOnKeyPress();
                    }
                },KEY_PAUSE);
            }
        });

        // add Calendar to buttons
        dateButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialog = new DatePickerDialog(NoteSearch.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        date1.set(year, monthOfYear, dayOfMonth);
                        updateButtonText();
                    }
                }, date1.get(Calendar.YEAR), date1.get(Calendar.MONTH), date1.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });

        dateButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialog = new DatePickerDialog(NoteSearch.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        date2.set(year, monthOfYear, dayOfMonth);
                        updateButtonText();
                    }
                }, date2.get(Calendar.YEAR), date2.get(Calendar.MONTH), date2.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });
    }

    private void updateButtonText() {
        dateButton1.setText(dateFormatter.format(date1.getTime()));
        dateButton2.setText(dateFormatter.format(date2.getTime()));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        SearchResult sResult = (SearchResult) resultListAdapter.getItem(position);

        if (!sResult.isLoadMoreAction) {
            Intent myIntent = new Intent(this, BGHistory.class);
            myIntent.putExtra(BGHistory.OPEN_ON_TIME_KEY, sResult.timestamp);
            startActivity(myIntent);
            finish();
        } else {
            loadMore();
        }

    }

    private void searchOnKeyPress() {
        if ((last_keypress_time > 0) && ((JoH.tsl() - last_keypress_time) > (KEY_PAUSE - 1))) {
            last_keypress_time = -1;
            doSearch(false);
        }
    }

    private void loadMore() {
        //remove last item (the action)
        resultListAdapter.removeLoadMoreAction();

        //check if cursor open
        if (dbCursor == null || dbCursor.isClosed()) return;

        //load more
        for (int i = 0; i < RESTRICT_SEARCH && !dbCursor.isAfterLast(); i++) {
            SearchResult result = new SearchResult(dbCursor.getLong(0), dbCursor.getString(1), dbCursor.getDouble(2), dbCursor.getDouble(3), dbCursor.getString(4));
            resultListAdapter.addSingle(result);
            dbCursor.moveToNext();
        }
        //add action if needed
        if (dbCursor.isAfterLast()) {
            dbCursor.close();
        } else {
            SearchResult result = new SearchResult(0, getString(R.string.load_more), 0, 0, null);
            result.setLoadMoreActionFlag();
            resultListAdapter.addSingle(result);
        }
    }

    static class ViewHolder {
        boolean modified;
        TextView note;
        TextView time;
        TextView treatments;

    }

    private class SearchResultAdapter extends BaseAdapter {

        private Vector<SearchResult> noteList;

        public SearchResultAdapter() {
            noteList = new Vector<SearchResult>(30);
        }

        @Override
        public int getCount() {
            return noteList.size();
        }

        @Override
        public Object getItem(int position) {
            return noteList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void clear() {
            noteList.clear();
            notifyDataSetChanged();
        }

        public void addSingle(SearchResult searchResult) {
            noteList.add(searchResult);
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (convertView == null || ((ViewHolder) convertView.getTag()).modified) {
                convertView = NoteSearch.this.getLayoutInflater().inflate(R.layout.notesearch_list_item, null);
                viewHolder = new ViewHolder();
                viewHolder.note = (TextView) convertView.findViewById(R.id.notesearch_note_id);
                viewHolder.time = (TextView) convertView.findViewById(R.id.notesearch_time_id);
                viewHolder.treatments = (TextView) convertView.findViewById(R.id.notesearch_treatments_id);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            SearchResult searchResult = noteList.get(position);


            if (searchResult.isLoadMoreAction) {
                viewHolder.note.setTextColor(ChartUtils.COLOR_BLUE);
                viewHolder.note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
                viewHolder.treatments.setVisibility(View.GONE);
                viewHolder.time.setVisibility(View.GONE);
                viewHolder.modified = true;
            }
            viewHolder.note.setText(searchResult.note);
            if (!"".equals(searchResult.otherTreatments)) {
                viewHolder.treatments.setText(searchResult.otherTreatments);

            } else {
                viewHolder.treatments.setVisibility(View.GONE);
                viewHolder.modified = true;
            }
            viewHolder.time.setText(new Date(searchResult.timestamp).toString());
            return convertView;
        }

        public void removeLoadMoreAction() {
            if (noteList.get(noteList.size() - 1).isLoadMoreAction) {
                noteList.remove(noteList.size() - 1);
                notifyDataSetChanged();
            }
        }
    }

    private class SearchResult {
        long timestamp;
        String uuid;
        String note;
        String otherTreatments;
        boolean isLoadMoreAction;


        public SearchResult(long timestamp, String note, double carbs, double insulin, String uuid) {
            this.timestamp = timestamp;
            this.note = note;
            this.uuid = uuid;
            this.otherTreatments = "";
            if (carbs != 0) {
                otherTreatments += getString(R.string.carbs) + ": " + carbs;
            }
            if (insulin != 0) {
                otherTreatments += " " + getString(R.string.insulin) + ": " + insulin;
            }
        }

        /*Used to add elements like "Press for more results"*/
        public void setLoadMoreActionFlag() {
            isLoadMoreAction = true;
        }

    }
}
