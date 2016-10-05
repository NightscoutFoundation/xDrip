package com.eveningoutpost.dexdrip;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
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

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.utils.ListActivityWithMenu;

import java.text.DateFormat;
import java.util.ArrayList;
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
    private static final int DEFAULT_TIMEFRAME = 7;
    public static String menu_name = "Note Search";

    private Button dateButton1;
    private Button dateButton2;
    private EditText searchTextField;
    private GregorianCalendar date1;
    private GregorianCalendar date2;
    private DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
    private SearchResultAdapter reslutListAdapter;


    @Override
    public String getMenuName() {
        return menu_name;
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
        //TODO do something

        for(int i = 0; i < 30; i++){
            reslutListAdapter.addSingle(new SearchResult(System.currentTimeMillis() - (i*1000*60*60*24), "A note text " + i, i, i ));
        }



        JoH.static_toast_long("collecting...");
    }

    private void doSearch() {
        //TODO do something
        JoH.static_toast_long("searching...");
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

        reslutListAdapter = new SearchResultAdapter();
        setListAdapter(reslutListAdapter);
        this.getListView().setLongClickable(true);
        this.getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                SearchResult sResult = (SearchResult) reslutListAdapter.getItem(position);
                //JoH.static_toast_long("long pressed: " + sResult.note);
                // TODO: here a long-click action can be added later. (edit mode e.g.)
                return true;
            }
        });


        setupGui();



       //TODO setup List adapter



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


    private class SearchResultAdapter extends BaseAdapter {

        private Vector<SearchResult> noteList;

        public SearchResultAdapter(){
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

        public void addSingle(SearchResult searchResult){
            noteList.add(searchResult);
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (convertView == null) {
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


            if(searchResult.flagInteractionItem) {
                viewHolder.note.setTextColor(ChartUtils.COLOR_BLUE);
            }
            viewHolder.note.setText(searchResult.note);
            viewHolder.treatments.setText(searchResult.otherTreatments);

            //TODO: better format timestamp
            viewHolder.time.setText(new Date(searchResult.timestamp).toString());


            return convertView;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        SearchResult sResult = (SearchResult) reslutListAdapter.getItem(position);

        if(!sResult.flagInteractionItem){
            Intent myIntent = new Intent(this, BGHistory.class);
            myIntent.putExtra(BGHistory.OPEN_ON_TIME_KEY, sResult.timestamp);
            startActivity(myIntent);
            finish();
        } else {
            // do some interaction later
        }

    }

    static class ViewHolder {
        TextView note;
        TextView time;
        TextView treatments;

    }


    private class SearchResult {
        long timestamp;
        String note;
        String otherTreatments;
        boolean flagInteractionItem;

        public SearchResult(long timestamp, String note, double carbs, double insulin) {
            this.timestamp = timestamp;
            this.note = note;
            this.otherTreatments = "";
            if(carbs != 0) {
                otherTreatments += "carbs: "  + carbs;
            }
            if(insulin != 0) {
                otherTreatments += " insulin: "  + carbs;
            }
        }

        /*Used to add elements like "Press for more results"*/
        public void flagInteractionItem(){
            flagInteractionItem = true;
        }

    }
}
