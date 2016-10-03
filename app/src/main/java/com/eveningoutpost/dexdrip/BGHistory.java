package com.eveningoutpost.dexdrip;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.stats.StatsResult;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ViewportChangeListener;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;

// by AdrianLxM

public class BGHistory extends ActivityWithMenu {
   // public static String menu_name = "History";
    static String TAG = BGHistory.class.getName();
    private boolean updatingPreviewViewport = false;
    private boolean updatingChartViewport = false;
    private LineChartView chart;
    private PreviewLineChartView previewChart;
    private GregorianCalendar date1;
    private DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
    private Button dateButton1;
    private Spinner daysSpinner;
    private int noDays = 1;
    private SharedPreferences prefs;
    private TextView statisticsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bghistory);
        this.statisticsTextView = (TextView) findViewById(R.id.historystats);


        date1 = new GregorianCalendar();
        date1.set(Calendar.HOUR_OF_DAY, 0);
        date1.set(Calendar.MINUTE, 0);
        date1.set(Calendar.SECOND, 0);
        date1.set(Calendar.MILLISECOND, 0);

        setupButtons();
        setupCharts();

        Toast.makeText(this, R.string.double_tap_or_pinch_to_zoom,
                Toast.LENGTH_LONG).show();
    }

    private void setupButtons() {
        Button prevButton = (Button) findViewById(R.id.button_prev);
        Button nextButton = (Button) findViewById(R.id.button_next);
        this.dateButton1 = (Button) findViewById(R.id.button_date1);
        this.daysSpinner = (Spinner) findViewById(R.id.daysspinner);


        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                date1.add(Calendar.DATE, -noDays);
                setupCharts();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                date1.add(Calendar.DATE, +noDays);
                setupCharts();
            }
        });

        dateButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialog = new DatePickerDialog(BGHistory.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        date1.set(year, monthOfYear, dayOfMonth);
                        setupCharts();
                    }
                }, date1.get(Calendar.YEAR), date1.get(Calendar.MONTH), date1.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });

        String[] vals = new String[14];
        vals[0] = 1 + " day";
        for (int i = 1; i< vals.length; i++ ){
            vals[i] = (i+1) + " days";
        }

        daysSpinner.setAdapter(new ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, vals));
        daysSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                noDays = position + 1;
                setupCharts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                noDays = 1;
                setupCharts();
            }
        });
    }


    @Override
    public String getMenuName() {
        return getString(R.string.history);
    }

    private void setupCharts() {
        dateButton1.setText(dateFormatter.format(date1.getTime()));

        Calendar endDate = (GregorianCalendar) date1.clone();
        endDate.add(Calendar.DATE, noDays);
        int numValues = noDays * (60 / 2) * 24; // LimiTTer sample rate 1 per 2 minutes
        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(this, date1.getTimeInMillis(), endDate.getTimeInMillis(), numValues, false);

        chart = (LineChartView) findViewById(R.id.chart);
        chart.setZoomType(ZoomType.HORIZONTAL);
        previewChart = (PreviewLineChartView) findViewById(R.id.chart_preview);
        previewChart.setZoomType(ZoomType.HORIZONTAL);

        chart.setLineChartData(bgGraphBuilder.lineData());
        chart.setOnValueTouchListener(bgGraphBuilder.getOnValueSelectTooltipListener(false));
        previewChart.setLineChartData(bgGraphBuilder.previewLineData(chart.getLineChartData()));

        previewChart.setViewportCalculationEnabled(true);
        chart.setViewportCalculationEnabled(true);
        previewChart.setViewportChangeListener(new ViewportListener());
        chart.setViewportChangeListener(new ChartViewPortListener());

        setupStatistics(date1.getTimeInMillis(), endDate.getTimeInMillis());
    }

    private void setupStatistics(long from, long to) {

        if (Home.getPreferencesBoolean("show_history_stats", true)) {
            StatsResult statsResult = new StatsResult(PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()), from, to);

            StringBuilder sb = new StringBuilder();

            sb.append(statsResult.getAverageUnitised());
            sb.append(' ');
            sb.append(statsResult.getA1cDCCT());
            sb.append(" | ");
            sb.append(statsResult.getA1cIFCC(true));
            sb.append('\n');
            sb.append(statsResult.getInPercentage());
            sb.append(' ');
            sb.append(statsResult.getHighPercentage());
            sb.append(' ');
            sb.append(statsResult.getLowPercentage());
            sb.append('\n');
            sb.append(statsResult.getCapturePercentage(true));
            sb.append(' ');

            statisticsTextView.setText(sb);
            statisticsTextView.setVisibility(View.VISIBLE);

        } else {
            statisticsTextView.setVisibility(View.GONE);
        }
    }


    private class ChartViewPortListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingPreviewViewport) {
                updatingChartViewport = true;
                previewChart.setZoomType(ZoomType.HORIZONTAL);
                previewChart.setCurrentViewport(newViewport);
                updatingChartViewport = false;
            }
        }
    }

    private class ViewportListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingChartViewport) {
                updatingPreviewViewport = true;
                chart.setZoomType(ZoomType.HORIZONTAL);
                chart.setCurrentViewport(newViewport);
                updatingPreviewViewport = false;
            }
        }
    }

    private int daysBetween(Calendar calendar1, Calendar calendar2) {
        Calendar first, second;
        if (calendar1.compareTo(calendar2) > 0) {
            first = calendar2;
            second = calendar1;
        } else {
            first = calendar1;
            second = calendar2;
        }
        int days = second.get(Calendar.DAY_OF_YEAR) - first.get(Calendar.DAY_OF_YEAR);
        Calendar temp = (Calendar) first.clone();
        while (temp.get(Calendar.YEAR) < second.get(Calendar.YEAR)) {
            days = days + temp.getActualMaximum(Calendar.DAY_OF_YEAR);
            temp.add(Calendar.YEAR, 1);
        }
        return days;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);

        MenuItem menuItem = menu.findItem(R.id.action_toggle_historystats);
        menuItem.setChecked(Home.getPreferencesBoolean("show_history_stats", true));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_historystats) {
            Home.setPreferencesBoolean("show_history_stats", !Home.getPreferencesBoolean("show_history_stats", true));
            invalidateOptionsMenu();
            setupCharts();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    }
