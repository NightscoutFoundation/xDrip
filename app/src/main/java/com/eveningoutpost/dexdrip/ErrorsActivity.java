package com.eveningoutpost.dexdrip;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import java.util.ArrayList;
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

        highCheckboxView = (CheckBox) findViewById(R.id.highSeverityCheckbox);
        mediumCheckboxView = (CheckBox) findViewById(R.id.midSeverityCheckbox);
        lowCheckboxView = (CheckBox) findViewById(R.id.lowSeverityCheckBox);

        highCheckboxView.setOnClickListener(checkboxListener);
        mediumCheckboxView.setOnClickListener(checkboxListener);
        lowCheckboxView.setOnClickListener(checkboxListener);

        updateErrors();
        errorList = (ListView) findViewById(R.id.errorList);
        adapter = new ErrorListAdapter(getApplicationContext(), errors);
        errorList.setAdapter(adapter);
    }

    private View.OnClickListener checkboxListener = new View.OnClickListener() {
        public void onClick(View v) {
            updateErrors();
            adapter.notifyDataSetChanged();
        }
    };

    public void updateErrors() {
        List<Integer> severitiesList = new ArrayList<>();
        if (highCheckboxView.isChecked()) severitiesList.add(3);
        if (mediumCheckboxView.isChecked()) severitiesList.add(2);
        if (lowCheckboxView.isChecked()) severitiesList.add(1);
        if(errors == null) {
            errors = UserError.bySeverity(severitiesList.toArray(new Integer[severitiesList.size()]));
        } else {
            errors.clear();
            errors.addAll(UserError.bySeverity(severitiesList.toArray(new Integer[severitiesList.size()])));
        }
    }
}
