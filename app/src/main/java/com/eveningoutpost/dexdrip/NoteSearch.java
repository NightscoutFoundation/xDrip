package com.eveningoutpost.dexdrip;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.activeandroid.Cache;
import com.eveningoutpost.dexdrip.Models.JoH;
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

    //Strings TODO: move to strings.xml (omitted during dev to avoid merge conflicts)
    private static final String LOAD_MORE = "load more";
    public static String menu_name = "Note Search";
    private static final String SEARCHING = "searching...";
    private static final String COLLECTING = "collecting...";
    private static final String CARBS = "carbs";
    private static final String INSULIN = "insulin";




    @Override
    public String getMenuName() {
        return menu_name;
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
        this.getListView().setLongClickable(true);
        this.getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                SearchResult sResult = (SearchResult) resultListAdapter.getItem(position);
                // here a long-click action can be added later. (edit mode e.g.)
                return true;
            }
        });


        setupGui();

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
                doSearch();
                return true;

            case R.id.menu_allnote:
                doAll();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void doAll() {


        resultListAdapter.clear();
        JoH.static_toast_short(COLLECTING);

        SQLiteDatabase db = Cache.openDatabase();

        if (dbCursor != null && !dbCursor.isClosed()) {
            dbCursor.close();
        }

        long from = date1.getTimeInMillis();

        Calendar endDate = (GregorianCalendar) date2.clone();
        endDate.add(Calendar.DATE, 1);
        long to = endDate.getTimeInMillis();

        dbCursor = db.rawQuery("select timestamp, notes, carbs, insulin from Treatments where notes IS NOT NULL AND timestamp < " + to + " AND timestamp >= " + from + " ORDER BY timestamp DESC", null);
        dbCursor.moveToFirst();

        for (int i = 0; i < RESTRICT_SEARCH && !dbCursor.isAfterLast(); i++) {
            SearchResult result = new SearchResult(dbCursor.getLong(0), dbCursor.getString(1), dbCursor.getDouble(2), dbCursor.getDouble(3));
            resultListAdapter.addSingle(result);
            dbCursor.moveToNext();
        }

        if (dbCursor.isAfterLast()) {
            dbCursor.close();
        } else {
            SearchResult result = new SearchResult(0, LOAD_MORE, 0, 0);
            result.setLoadMoreActionFlag();
            resultListAdapter.addSingle(result);
        }

    }

    private void doSearch() {

        if (searchTextField.getText() == null || "".equals(searchTextField.getText().toString())) {
            JoH.static_toast_long("No search term found");
            return;
        }


        resultListAdapter.clear();
        JoH.static_toast_short(SEARCHING);

        SQLiteDatabase db = Cache.openDatabase();

        if (dbCursor != null && !dbCursor.isClosed()) {
            dbCursor.close();
        }


        String searchTerm = searchTextField.getText().toString();
        DatabaseUtils.sqlEscapeString(searchTerm);

        long from = date1.getTimeInMillis();

        Calendar endDate = (GregorianCalendar) date2.clone();
        endDate.add(Calendar.DATE, 1);
        long to = endDate.getTimeInMillis();


        dbCursor = db.rawQuery("select timestamp, notes, carbs, insulin from Treatments where notes IS NOT NULL AND timestamp < " + to + " AND timestamp >= " + from + " AND notes like '%" + searchTerm + "%' ORDER BY timestamp DESC", null);
        dbCursor.moveToFirst();

        for (int i = 0; i < RESTRICT_SEARCH && !dbCursor.isAfterLast(); i++) {
            SearchResult result = new SearchResult(dbCursor.getLong(0), dbCursor.getString(1), dbCursor.getDouble(2), dbCursor.getDouble(3));
            resultListAdapter.addSingle(result);
            dbCursor.moveToNext();
        }

        if (dbCursor.isAfterLast()) {
            dbCursor.close();
        } else {
            SearchResult result = new SearchResult(0, LOAD_MORE, 0, 0);
            result.setLoadMoreActionFlag();
            resultListAdapter.addSingle(result);
        }

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
                    doSearch();
                    return true;
                }
                return false;
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

    private void loadMore() {
        //remove last item (the action)
        resultListAdapter.removeLoadMoreAction();

        //check if cursor open
        if (dbCursor == null || dbCursor.isClosed()) return;

        //load more
        for (int i = 0; i < RESTRICT_SEARCH && !dbCursor.isAfterLast(); i++) {
            SearchResult result = new SearchResult(dbCursor.getLong(0), dbCursor.getString(1), dbCursor.getDouble(2), dbCursor.getDouble(3));
            resultListAdapter.addSingle(result);
            dbCursor.moveToNext();
        }
        //add action if needed
        if (dbCursor.isAfterLast()) {
            dbCursor.close();
        } else {
            SearchResult result = new SearchResult(0, LOAD_MORE, 0, 0);
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
        String note;
        String otherTreatments;
        boolean isLoadMoreAction;

        public SearchResult(long timestamp, String note, double carbs, double insulin) {
            this.timestamp = timestamp;
            this.note = note;
            this.otherTreatments = "";
            if (carbs != 0) {
                otherTreatments += CARBS + ": " + carbs;
            }
            if (insulin != 0) {
                otherTreatments += " " + INSULIN + ": " + carbs;
            }
        }

        /*Used to add elements like "Press for more results"*/
        public void setLoadMoreActionFlag() {
            isLoadMoreAction = true;
        }

    }
}
