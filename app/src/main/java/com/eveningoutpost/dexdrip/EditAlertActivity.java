package com.eveningoutpost.dexdrip;

import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;

public class EditAlertActivity extends Activity {
    
    TextView viewHeader;
    
    EditText alertText;
    EditText alertThreshold;
    EditText alertMp3File;
    
    Button buttonSave;
    Button buttonRemove;
    
    String uuid;
    boolean above;
    
    private final static String TAG = AlertPlayer.class.getSimpleName();
    
    String getExtra(Bundle savedInstanceState, String paramName) {
        String newString;
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                newString= null;
            } else {
                newString= extras.getString(paramName);
            }
        } else {
            newString= (String) savedInstanceState.getSerializable(paramName);
        }
        return newString;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_alert);

        viewHeader = (TextView) findViewById(R.id.view_alert_header);
        
        buttonSave = (Button)findViewById(R.id.edit_alert_save);
        buttonRemove = (Button)findViewById(R.id.edit_alert_remove);
        
        alertText = (EditText) findViewById(R.id.edit_alert_text);
        alertThreshold = (EditText) findViewById(R.id.edit_alert_threshold);
        alertMp3File = (EditText) findViewById(R.id.edit_alert_mp3_file);
        
        addListenerOnButtons();
        
        uuid = getExtra(savedInstanceState, "uuid");
        if (uuid == null) {
            above = Boolean.parseBoolean(getExtra(savedInstanceState, "above"));
        } else {
            AlertType at = AlertType.get_alert(uuid);
            if(at==null) {
                Log.wtf(TAG, "Error editing alert, when that alert does not exist...");
                Intent returnIntent = new Intent();
                setResult(RESULT_CANCELED, returnIntent);
                finish();
                return;
            }
            above =at.above;
            alertText.setText(at.name);
            alertThreshold.setText(String.valueOf((int)at.threshold));
            alertMp3File.setText(at.mp3_file);
        }

        String status;
        if (uuid != null) {
            // We are editing an alert
            status = "editing " + (above ? "high" : "low") + "alert" ;
        } else {
            // This is a new alert
            buttonRemove.setVisibility(View.GONE);
            status = "adding " + (above ? "high" : "low") + "alert" ;
        }
        
        viewHeader.setText(status);
    }

    public void addListenerOnButtons() {
        
        buttonSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                
                // Check that values are ok.
                int threshold = 0;
                try {
                    threshold = Integer.parseInt(alertThreshold.getText().toString());
                }
                    catch (NumberFormatException nfe) {
                        Log.e(TAG, "Invalid number", nfe);
                    }
                if(threshold < 40 || threshold > 400) {
                    Toast.makeText(getApplicationContext(), "threshhold has to be between 40 and 400",Toast.LENGTH_LONG).show();
                    return;
                }

                if (uuid != null) {
                    AlertType.update_alert(uuid, alertText.getText().toString(), above, threshold, true, 1);
                }  else {
                    AlertType.add_alert(alertText.getText().toString(), above, threshold, true, 1);
                }
                Intent returnIntent = new Intent();
                setResult(RESULT_OK,returnIntent);
                finish();
            }

        });
        
        buttonRemove.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                

                if (uuid == null) {
                    Log.wtf(TAG, "Error remove pressed, while we were removing an alert");
                }  else {
                    AlertType.remove_alert(uuid);
                }
                Intent returnIntent = new Intent();
                setResult(RESULT_OK,returnIntent);
                finish();
            }

        });
    }
}
