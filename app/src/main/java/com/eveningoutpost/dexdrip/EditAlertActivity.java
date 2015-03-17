package com.eveningoutpost.dexdrip;

import java.util.Date;
import android.database.Cursor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
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

import android.net.Uri;

public class EditAlertActivity extends Activity {
    
    TextView viewHeader;
    
    EditText alertText;
    EditText alertThreshold;
    EditText alertMp3File;
    Button buttonalertMp3;
    
    Button buttonSave;
    Button buttonRemove;
    
    String uuid;
    boolean above;
    final int CHOOSE_FILE = 1;
    
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
        buttonalertMp3 = (Button)findViewById(R.id.Button_alert_mp3_file);
        
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
            status = "editing " + (above ? "high" : "low") + " alert";
        } else {
            // This is a new alert
            buttonRemove.setVisibility(View.GONE);
            status = "adding " + (above ? "high" : "low") + " alert";
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
                String mp3_file = alertMp3File.getText().toString();

                if (uuid != null) {
                    AlertType.update_alert(uuid, alertText.getText().toString(), above, threshold, true, 1, mp3_file);
                }  else {
                    AlertType.add_alert(alertText.getText().toString(), above, threshold, true, 1, mp3_file);
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
        
        buttonalertMp3.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
            
                // in onCreate or any event where your want the user to
                // select a file
                Intent intent = new Intent();
                intent.setType("audio/mpeg3");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"), CHOOSE_FILE);
            }
       }); //- See more at: http://blog.kerul.net/2011/12/pick-file-using-intentactiongetcontent.html#sthash.c8xtIr1Y.dpuf
    }
    
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == CHOOSE_FILE) {
                Uri selectedImageUri = data.getData();
    
                // Todo this code is very flacky. Probably need a much better understanding of how the different programs
                // select the file names. We might also have to 
                // - See more at: http://blog.kerul.net/2011/12/pick-file-using-intentactiongetcontent.html#sthash.c8xtIr1Y.cx7s9nxH.dpuf
        
                //MEDIA GALLERY
                String selectedImagePath = getPath(selectedImageUri);
                if (selectedImagePath == null) {
                    //OI FILE Manager
                    selectedImagePath = selectedImageUri.getPath();
                }
                
                //AlertPlayer.getPlayer().PlayFile(getApplicationContext(), selectedImagePath);
                alertMp3File.setText(selectedImagePath);
                
                //just to display the imagepath
                //Toast.makeText(this.getApplicationContext(), selectedImagePath, Toast.LENGTH_SHORT).show();//
            }
        }
    }
    
    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if(cursor!=null)
        {
            //HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            //THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
            int column_index;
            try {
                column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            } catch ( IllegalArgumentException e) {
                Log.e(TAG, "cursor.getColumnIndexOrThrow failed", e);
                return null;
            }
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }   else {
            return null;
        }
    }
}
