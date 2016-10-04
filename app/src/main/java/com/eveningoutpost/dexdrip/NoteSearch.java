package com.eveningoutpost.dexdrip;

import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.utils.ListActivityWithMenu;
import com.eveningoutpost.dexdrip.utils.LocationHelper;

/**
 * Created by adrian on 04/10/16.
 */
public class NoteSearch extends ListActivityWithMenu {
    public static String menu_name = "Note Search";

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
                //TODO: do something
                return true;

            case R.id.menu_allnote:
                //TODO: do soemthing
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.OldAppTheme); // or null actionbar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notesearch);








       //TODO setup List adapter


        /*editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    //TODO: query
                    return true;
                }
                return false;
            }
        });*/



    }



}
