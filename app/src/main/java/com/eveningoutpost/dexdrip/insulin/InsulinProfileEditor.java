package com.eveningoutpost.dexdrip.insulin;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.R;
import java.util.HashMap;

/**
 * Created by gruoner on 28/07/2019.
 */

public class InsulinProfileEditor extends BaseAppCompatActivity {

    private static final String TAG = "insulinprofileeditor";
    private Button cancelBtn;
    private static Button saveBtn;
    private static Button undoBtn;
    private LinearLayout linearLayout;
    private HashMap<Insulin, CheckBox> checkboxes;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insulinprofile_editor);

        checkboxes = new HashMap<Insulin, CheckBox>();

        undoBtn = (Button) findViewById(R.id.profileUndoBtn);
        saveBtn = (Button) findViewById(R.id.profileSaveBtn);
        cancelBtn = (Button) findViewById(R.id.profileCancelbtn);
        linearLayout = (LinearLayout) findViewById(R.id.profile_layout_view);

        for (Insulin i: InsulinManager.getAllProfiles())
        {
            LinearLayout v = new LinearLayout(this);
            v.setOrientation(LinearLayout.HORIZONTAL);
            CheckBox cb = new CheckBox(this);
            if (InsulinManager.isProfileEnabled(i))
                cb.setChecked(true);
            else
                cb.setChecked(false);
            cb.setText(i.getDisplayName());
            cb.setTextSize(20);
            checkboxes.put(i, cb);
            cb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (InsulinManager.isProfileEnabled(i))
                        InsulinManager.disableProfile(i);
                    else
                        InsulinManager.enableProfile(i);
                    if (InsulinManager.isProfileEnabled(i))
                        cb.setChecked(true);
                    else
                        cb.setChecked(false);
                }
            });
            v.addView(cb);
            linearLayout.addView(v);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void profileCancelButton(View myview) {
        InsulinManager.LoadDisabledProfilesFromPrefs();
        finish();
    }

    public void profileSaveButton(View myview) {
        InsulinManager.saveDisabledProfilesToPrefs();
        finish();
    }

    public void profileUndoButton(View myview) {
        InsulinManager.LoadDisabledProfilesFromPrefs();
        for (Insulin i: InsulinManager.getAllProfiles())
            if (InsulinManager.isProfileEnabled(i))
                checkboxes.get(i).setChecked(true);
            else
                checkboxes.get(i).setChecked(false);
    }
}

