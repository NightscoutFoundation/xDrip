package com.eveningoutpost.dexdrip;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import java.util.List;

/**
 * Created by stephenblack on 8/3/15.
 */
public class ErrorsActivity extends ActivityWithMenu {
    public static String menu_name = "Errors";
    public String getMenuName() { return  menu_name; }
    private CheckBox highCheckboxView;
    private CheckBox mediumCheckboxView;
    private CheckBox lowCheckboxView;
    private ListView errorList;
    private List<UserError> errors;
    private ErrorListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_errors);
        updateErrors();

        highCheckboxView = (CheckBox) findViewById(R.id.highSeverityCheckbox);
        mediumCheckboxView = (CheckBox) findViewById(R.id.midSeverityCheckbox);
        lowCheckboxView = (CheckBox) findViewById(R.id.lowSeverityCheckBox);

        highCheckboxView.setOnClickListener(checkboxListener);
        mediumCheckboxView.setOnClickListener(checkboxListener);
        lowCheckboxView.setOnClickListener(checkboxListener);

        errorList = (ListView) findViewById(R.id.errorList);
        adapter = new ErrorListAdapter(errors, getApplicationContext());
        errorList.setAdapter(adapter);
    }

    private View.OnClickListener checkboxListener = new View.OnClickListener() {
        public void onClick(View v) {
            updateErrors();
            adapter.notifyDataSetChanged();
        }
    };

    public void updateErrors() {
        int[] severities = {};
        if (highCheckboxView.isChecked()) severities[severities.length] = 3;
        if (mediumCheckboxView.isChecked()) severities[severities.length] = 2;
        if (lowCheckboxView.isChecked()) severities[severities.length] = 1;
        errors = UserError.bySeverity(severities);
    }
}
