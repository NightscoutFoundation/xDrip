/*
package com.eveningoutpost.dexdrip;

import android.util.Log;

import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.drive.events.DriveEventService;

*/
/**
 * Created by jamorham on 10/01/16.
 *//*


public class MyDriveEventService extends DriveEventService {
    static final String TAG = "jamorham drive EVENT";

    @Override
    public void onChange(ChangeEvent event) {
        Log.i(TAG, "GOT SERVICE ONCHANGE EVENT: " + event.toString());
    }

    @Override
    public void onCompletion(CompletionEvent event) {
        super.onCompletion(event);
        DriveId driveId = event.getDriveId();
        Log.d(TAG, "onComplete: " + driveId.getResourceId());
        switch (event.getStatus()) {
            case CompletionEvent.STATUS_CONFLICT:
                Log.d(TAG, "STATUS_CONFLICT");
                event.dismiss();
                break;
            case CompletionEvent.STATUS_FAILURE:
                Log.d(TAG, "STATUS_FAILURE");
                event.dismiss();
                break;
            case CompletionEvent.STATUS_SUCCESS:
                Log.d(TAG, "STATUS_SUCCESS ");
                event.dismiss();
                break;
        }
    }
}
*/
