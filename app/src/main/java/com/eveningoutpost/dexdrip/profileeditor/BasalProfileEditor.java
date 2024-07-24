package com.eveningoutpost.dexdrip.profileeditor;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.ColorCache;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.ui.charts.BasalChart;
import com.eveningoutpost.dexdrip.ui.dialog.GenericConfirmDialog;
import com.eveningoutpost.dexdrip.ui.helpers.ColorUtil;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.animation.ChartAnimationListener;
import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.ColumnChartView;
import lombok.val;

import static com.eveningoutpost.dexdrip.utilitymodels.ColorCache.getCol;
import static com.eveningoutpost.dexdrip.ui.helpers.UiHelper.convertDpToPixel;

// jamorham

public class BasalProfileEditor extends AppCompatActivity implements AdapterView.OnItemSelectedListener, ChartAnimationListener {

    private static final String TAG = "BasalProfileEditor";
    private static final String PREF_STORED_BASAL_STEP = "PREF_STORED_BASAL_STEP";

    private ColumnChartView chart;

    private long lastSelection = -1;
    private int selectedColumnA = -1;
    private int selectedColumnB = -1;
    private int flipper = 0;

    private volatile boolean controlsLocked = false;

    @ColorInt
    private final int SELECTED_COLUMN_COLOR = ColorUtil.adjustHue(getCol(ColorCache.X.color_basal_tbr), -180);
    @ColorInt
    private final int UNSELECTED_COLUMN_COLOR = getCol(ColorCache.X.color_basal_tbr);


    // TODO calculate total daily before and after
    // TODO Scale top items better
    // TODO round labels - update labels?
    // TODO bolden significant numbers in subtitle?
    // TODO long press on title bar swap subtitle / title?
    // TODO jump graph scale better
    // TODO add spline (later)
    // TODO leading zero on qs items in title bar
    // TODO block till operation finished??
    // TODO sync with nightscout button????
    // TODO data binding for UI element ease?
    // TODO undo redo buttons in title bar??


    EditText setValue;
    MenuItem plusMenuItem;
    MenuItem minusMenuItem;
    Button setButton;
    Button plusButton;
    Button minusButton;

    TextView basalStepLabel;

    Spinner basalSelectSpinner;
    Spinner basalStepSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basal_profile_editor);
        JoH.fixActionBar(this);
        //  getSupportActionBar().setCustomView(R.layout.custom_action_bar_title);
        //  getSupportActionBar().setDisplayShowCustomEnabled(true);

        chart = (ColumnChartView) findViewById(R.id.basalChart);
        basalSelectSpinner = findViewById(R.id.basalProfileSpinner);
        basalStepSpinner = findViewById(R.id.basalStepSpinner);
        basalStepLabel = findViewById(R.id.basalStepLabel);
        setValue = (EditText) findViewById(R.id.basalSetText);
        setButton = (Button) findViewById(R.id.basalSetButton);
        plusButton = (Button) findViewById(R.id.basalPlusButton);
        minusButton = (Button) findViewById(R.id.basalMinusButton);

        chart.setDataAnimationListener(this);
        chart.setOnValueTouchListener(new ValueTouchListener());
        setChartFromSpinnerSelection();

        autoSetViewPort(true);
        refreshScreenElements();
        refreshZoomFeatures();

        populateBasalNameSpinner();
        populateBasalStepSpinner();

    }

    private void refreshZoomFeatures() {
        chart.setZoomType(ZoomType.HORIZONTAL);
        chart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        //chart.setZoomLevel(0,0,1);
        chart.setMaxZoom(4f);
        chart.setZoomLevel(getNewMaxViewport().centerX(), getNewMaxViewport().centerY(), 1f);
        chart.setTapZoomEnabled(false);
    }

    private static final String LAST_BASAL_PROFILE_NAME = "LAST_BASAL_PROFILE_NAME";

    private static void setLastBasalProfileName(final String value) {
        PersistentStore.setString(LAST_BASAL_PROFILE_NAME, value);
    }

    private static String getLastBasalProfileName() {
        return PersistentStore.getString(LAST_BASAL_PROFILE_NAME, "1");
    }

    private Rect goodMargin;

    private void setChartFromSpinnerSelection() {
        chart.getChartComputator().resetContentRect();
        if (chart.getChartComputator().getContentRectMinusAxesMargins().right != 0) {
            goodMargin = new Rect();
            val currentMargin = chart.getChartComputator().getContentRectMinusAxesMargins();
            goodMargin.bottom = currentMargin.bottom;
        }

        chart.setColumnChartData(BasalChart.columnData(getLastBasalProfileName()));

        if (goodMargin != null) {
            chart.getChartComputator().getContentRectMinusAllMargins().bottom = goodMargin.bottom;
            chart.getChartComputator().getContentRectMinusAxesMargins().bottom = goodMargin.bottom;

            BasalChart.refreshAxis(chart);
            autoSetViewPort(true);
            refreshZoomFeatures();
        }
        chart.getAxesRenderer().onChartDataChanged();
    }

    private void fixElipsus(ViewGroup root) {
        if (root == null)
            root = (ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content).getRootView();
        final int children = root.getChildCount();
        android.util.Log.d(TAG, "CHILDREN: " + children);
        for (int i = 0; i < children; i++) {
            final View view = root.getChildAt(i);
            if (view instanceof TextView) {
                final String txt = ((TextView) view).getText().toString();
                android.util.Log.d(TAG, txt);
                // TODO better safer localized match or something
                if (txt.contains("Daily")) {
                    android.util.Log.d(TAG, "CHILDREN hit");
                    android.util.Log.d(TAG, "" + ((TextView) view).getMaxEms());
                    android.util.Log.d(TAG, "" + ((TextView) view).getMaxWidth());
                    android.util.Log.d(TAG, "" + ((TextView) view).getWidth());
                    ((TextView) view).setEllipsize(null);
                    ((TextView) view).setMinWidth(convertDpToPixel(300));
                    //  ((LinearLayout) ((TextView) view).getParent()).setMinimumWidth(convertDpToPixel(200));

                    return;
                }
            } else if (view instanceof ViewGroup) {
                fixElipsus((ViewGroup) view);
            }
        }
    }

    private int findSpinnerPositionForName(final String name) {
        val count = basalSelectSpinner.getAdapter().getCount();
        for (int i = 0; i < count; i++) {
            val item = basalSelectSpinner.getItemAtPosition(i);
            if (item.toString().equals(name)) return i;
        }
        return 0;
    }

    private void populateBasalNameSpinner() {
        ArrayList<String> nameSpinnerArray = new ArrayList<>();
        nameSpinnerArray.add("1");
        nameSpinnerArray.add("2");
        nameSpinnerArray.add("3");
        nameSpinnerArray.add("4");
        nameSpinnerArray.add("5");
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nameSpinnerArray);
        basalSelectSpinner.setAdapter(spinnerArrayAdapter);
        basalSelectSpinner.setSelection(findSpinnerPositionForName(getLastBasalProfileName()));
        basalSelectSpinner.setOnItemSelectedListener(this);
    }

    private void populateBasalStepSpinner() {
        ArrayList<String> stepSpinnerArray = new ArrayList<>();
        stepSpinnerArray.add("0.5");
        stepSpinnerArray.add("0.2");
        stepSpinnerArray.add("0.1");
        stepSpinnerArray.add("0.05");
        stepSpinnerArray.add("0.02");
        stepSpinnerArray.add("0.01");
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, stepSpinnerArray);
        basalStepSpinner.setAdapter(spinnerArrayAdapter);

        basalStepSpinner.setOnItemSelectedListener(this);

        final int stored = (int) PersistentStore.getLong(PREF_STORED_BASAL_STEP);
        // check if unset
        if (stored == 0) {
            basalStepSpinner.setSelection(3); // 0.05 default
        } else {
            basalStepSpinner.setSelection(stored - 1);
        }

        buttonsToMatchStep();
    }

    @SuppressLint("SetTextI18n")
    private void buttonsToMatchStep() {
        plusButton.setText("+" + (String) basalStepSpinner.getSelectedItem());
        minusButton.setText("-" + (String) basalStepSpinner.getSelectedItem());
    }

    private float getStepValue() {
        return Float.parseFloat(basalStepSpinner.getSelectedItem().toString());
    }

    public void basalEditPlus(MenuItem x) {
        adjustSelectedColumns(getStepValue(), false);
    }

    public void basalEditLoad(MenuItem x) {
        setChartFromSpinnerSelection();
        refreshTotals();
    }

    public void basalEditSave(MenuItem x) {
        GenericConfirmDialog.show(this, "Confirm Overwrite", "Are you sure you want to save this to profile " + basalSelectSpinner.getSelectedItem().toString() + " ?", new Runnable() {
            @Override
            public void run() {
                BasalProfile.save(getLastBasalProfileName(), getListOfValues());
            }
        });
    }

    public void basalEditMinus(MenuItem x) {
        adjustSelectedColumns(-getStepValue(), false);
    }

    public void basalButtonPlus(View x) {
        adjustSelectedColumns(getStepValue(), false);
    }

    public void basalButtonMinus(View x) {
        adjustSelectedColumns(-getStepValue(), false);
    }

    public void onBasalSet(View v) {
        try {
            float value = (float) JoH.tolerantParseDouble(setValue.getText().toString());
            adjustSelectedColumns(value, true);
        } catch (NumberFormatException | NullPointerException e) {
            JoH.static_toast_short("Number out of range");
        }
    }

    private void refreshTotals() {

        final float current = BasalChart.getTotalBasal(chart.getColumnChartData());
        final float previous = BasalChart.getTotalImmutableBasal(chart.getColumnChartData());
        final float difference = current - previous;

        String result = "";
        if (current == previous) {
            result = String.format("Daily basal: ^%s^ U", JoH.qs0(current, 2));
        } else {
            String differenceText = (difference > 0 ? "+" : "") + JoH.qs0(difference, 2);
            result = String.format("Daily basal: ^%s^ U, was: %s, change: ^%s^", JoH.qs0(current, 2), JoH.qs0(previous, 2), differenceText);
        }

        // TODO extract this to span class
        final List<Integer> positions = new ArrayList<>();
        int matchpos = -1;
        while ((matchpos = result.indexOf('^', matchpos + 1)) > -1) {
            positions.add(matchpos - positions.size());
        }
        final SpannableStringBuilder span = new SpannableStringBuilder(result.replaceAll("\\^", ""));

        for (matchpos = 0; matchpos < positions.size() - 1; matchpos += 2) {
            final StyleSpan style = new StyleSpan(android.graphics.Typeface.BOLD);
            span.setSpan(style, positions.get(matchpos), positions.get(matchpos + 1), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }


        try {
            // TODO i18n string format
            getSupportActionBar().setSubtitle(getString(R.string.basal_editor) + "    (loaded profile: " + getLastBasalProfileName() + ")");
            getSupportActionBar().setTitle(span);
            fixElipsus(null); // how often do we actually need to do this??
            //get
        } catch (Exception e) {
            android.util.Log.e(TAG, "Got exception: " + e);
        }
    }

    private void refreshScreenElements() {
        JoH.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoSetViewPort(false);
                refreshTotals();
            }
        });
    }

    private Viewport getNewMaxViewport() {
        final Viewport moveViewPort = new Viewport(chart.getMaximumViewport());
        final float max = BasalChart.getMaxYvalue(chart.getChartData());
        final float calc = ((float) Math.round((max * 2) + 0.5f + 0.2f)) / 2 + 0.1f;
        UserError.Log.d(TAG, "BASAL max: " + max + " " + calc);
        moveViewPort.top = calc;
        //moveViewPort.set(0, 24, 0, 2);
        return moveViewPort;
    }

    private Viewport autoSetViewPort(boolean reset) {

        final Viewport moveViewPort = getNewMaxViewport();
        android.util.Log.d(TAG, "Setting viewport: " + moveViewPort.top + " " + moveViewPort.right);

        chart.setViewportCalculationEnabled(false);
        chart.setMaximumViewport(moveViewPort);

        if (reset) {
            chart.setCurrentViewport(moveViewPort);
        } else {
            final Viewport current = chart.getCurrentViewport();
            current.top = moveViewPort.top; // expand top
            chart.setCurrentViewport(current);
        }
        //chart.setZoomLevel
        BasalChart.refreshAxis(chart);
        return moveViewPort;
    }

    private void adjustSelectedColumns(final float adjust, final boolean set) {
        if (controlsLocked) {
            UserError.Log.d(TAG, "controls locked");
            return;
        }
        final List<SubcolumnValue> changed = new ArrayList<>(48);
        for (int col : getSelectColumnList()) {
            final SubcolumnValue v = chart.getChartData().getColumns().get(col).getValues().get(0);
            if (set) {
                //v.setValue(Math.max(0, adjust));
                v.setTarget(Math.max(0, adjust));
            } else {
                //v.setValue(Math.max(0, v.getValue() + adjust));
                v.setTarget(Math.max(0, v.getValue() + adjust));
                UserError.Log.d(TAG, "SET TARGET: " + v.getValue());
            }

            changed.add(v);
        }
      /*  Inevitable.stackableTask("refresh-columns", 100, new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 15; i++) {
                    for (SubcolumnValue v : changed) {
                        BasalChart.setLabelForSubcolumn(v);
                      //  refreshScreenElements();
                        JoH.threadSleep(100);
                    }

                }
            }
        });
        //BasalChart.refreshAxis(chart.getChartData());*/

        chart.startDataAnimation(); // best way to refresh? seems to reset zoom
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_basal_profile_edit, menu);

        plusMenuItem = menu.findItem(R.id.basalPlus);
        minusMenuItem = menu.findItem(R.id.basalMinus);

        setPlusMinusVisible(false); // hide until something selected

        return true;
    }

    private void selectedColumn(int col) {

        // single column clicked again
        if (col == selectedColumnA && selectedColumnA == selectedColumnB) {
            selectedColumnA = -1;
            selectedColumnB = -1;
            setPlusMinusVisible(false);
            showSelectedColumns();
            return;
        }

        setPlusMinusVisible(true);
        // A not set
        if (selectedColumnA == -1) {
            selectedColumnA = col;
        } else {

            if (selectedColumnB == -1) {
                selectedColumnB = col;
            } else {
                if (flipper % 2 == 0) {
                    selectedColumnA = col;
                } else {
                    selectedColumnB = col;
                }
                flipper++;
            }
        }
        showSelectedColumns();
    }

    private void unselectAllColumns() {
        for (Column column : chart.getChartData().getColumns()) {
            column.getValues().get(0).setColor(UNSELECTED_COLUMN_COLOR);
        }
    }

    private void showSelectedColumns() {
        unselectAllColumns();
        for (int i : getSelectColumnList()) {
            selectColumn(i);
        }
    }

    private List<Integer> getSelectColumnList() {
        final List<Integer> selected = new ArrayList<>();
        // do we have a range?
        if ((selectedColumnA != -1) && (selectedColumnB != -1)) {
            // sort to avoid going crazy
            final int first = Math.min(selectedColumnA, selectedColumnB);
            final int second = Math.max(selectedColumnA, selectedColumnB);
            // range can be only a single column also
            for (int i = first; i <= second; i++) {
                selected.add(i);
            }
            // must just be one column selected then
        } else {
            if (selectedColumnA != -1) selected.add(selectedColumnA);
            else if (selectedColumnB != -1) selected.add(selectedColumnB);
        }
        return selected;
    }

    private void selectColumn(int col) {
        if (col < 0) return;
        chart.getChartData().getColumns().get(col).getValues().get(0).setColor(SELECTED_COLUMN_COLOR);
        // chart.getChartData().getColumns().get(col).
    }

    private void setPlusMinusVisible(final boolean visible) {
        plusMenuItem.setVisible(visible);
        minusMenuItem.setVisible(visible);
        setValue.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        setButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        plusButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        minusButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        basalStepSpinner.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        basalStepLabel.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    private List<Double> getListOfValues() {
        final int columns = chart.getChartData().getColumns().size();
        final ArrayList<Double> values = new ArrayList<>(columns);
        for (int col = 0; col < columns; col++) {
            values.add(JoH.roundDouble(chart.getChartData().getColumns().get(col).getValues().get(0).getValue(), 2));
        }
        return values;
    }

    // spinner
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == basalStepSpinner) {
            UserError.Log.d(TAG, "Step Spinner selected it: " + position);
            basalStepSpinner.setSelection(position);
            PersistentStore.setLong(PREF_STORED_BASAL_STEP, position + 1); // increment so we know 0 is unset
            buttonsToMatchStep();
        } else if (parent == basalSelectSpinner) {
            UserError.Log.d(TAG, "Name Spinner selected it: " + position);
            setLastBasalProfileName(parent.getSelectedItem().toString());
        } else {
            UserError.Log.wtf(TAG, "Unknown spinner selected");
        }
    }

    // spinner
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onAnimationStarted() {
        UserError.Log.d(TAG, "Animation started");
        controlsLocked = true;
    }

    @Override
    public void onAnimationFinished() {
        UserError.Log.d(TAG, "Animation finished");

        // TODO DO ALL COLUMNS??
        for (int col : getSelectColumnList()) {
            final SubcolumnValue v = chart.getChartData().getColumns().get(col).getValues().get(0);
            BasalChart.setLabelForSubcolumn(v);
        }

        refreshScreenElements();
        controlsLocked = false;
    }

    // chart touch
    private class ValueTouchListener implements ColumnChartOnValueSelectListener {

        @Override
        public void onValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
            selectedColumn(columnIndex);
        }

        @Override
        public void onValueDeselected() {
        }

    }
}
