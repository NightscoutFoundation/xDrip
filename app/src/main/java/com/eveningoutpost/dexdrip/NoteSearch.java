package com.eveningoutpost.dexdrip;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.utils.ListActivityWithMenu;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

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


}
