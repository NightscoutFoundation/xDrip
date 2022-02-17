package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableList;
import android.databinding.ViewDataBinding;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.SendFeedBack;
import com.eveningoutpost.dexdrip.databinding.ActivityEventLogBinding;
import com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil;
import com.eveningoutpost.dexdrip.utils.ExtensionMethods;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;
import me.tatarka.bindingcollectionadapter2.BindingRecyclerViewAdapter;
import me.tatarka.bindingcollectionadapter2.ItemBinding;
import me.tatarka.bindingcollectionadapter2.collections.MergeObservableList;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;

/*
 * New style event log viewer
 *
 * Created by jamorham 24/03/2018
 *
 */
@ExtensionMethod({java.util.Arrays.class, ExtensionMethods.class})
public class EventLogActivity extends BaseAppCompatActivity {

    private static final List<Integer> severitiesList = new ArrayList<>();
    private static final boolean D = false;
    private static final String TAG = EventLogActivity.class.getSimpleName();
    private static final String PREF_SEVERITY_SELECTION = "event-log-severity-enabled-";
    private static final String PREF_LAST_SEARCH = "event-log-last-search-";

    static {
        severitiesList.add(1);
        severitiesList.add(2);
        severitiesList.add(3);
        severitiesList.add(5);
        severitiesList.add(6);
    }

    private ScaleGestureDetector scaleGestureDetector;
    private Animation pulseAnimation;
    private final ViewModel model = new ViewModel();
    private RecyclerView recyclerView;

    /*
    @Override
     public String getMenuName() {
         return getString(R.string.event_logs);
     }
     */
    private MenuItem searchItem;
    private SearchView searchView;
    private volatile boolean runRefresh = false;
    private volatile long highest_id = 0;
    private volatile int lastScrollPosition = 0;
    private volatile boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);
        super.onCreate(savedInstanceState);

        final ActivityEventLogBinding binding = ActivityEventLogBinding.inflate(getLayoutInflater());
        binding.setViewModel(model);
        setContentView(binding.getRoot());

        JoH.fixActionBar(this);

        scaleGestureDetector = new ScaleGestureDetector(this, new SimpleOnScaleGestureListener(model));

        refreshData();

        getOlderData();

    }

    // check if should stream wear logs
    private boolean shouldStreamWearLogs() {
        return Pref.getBooleanDefaultFalse("wear_sync") && Pref.getBooleanDefaultFalse("sync_wear_logs");
    }

    // ask for wear updated logs
    private void getWearData() {
        startWatchUpdaterService(this, WatchUpdaterService.ACTION_SYNC_LOGS, TAG);
    }

    // load in bulk of remaining data
    private void getOlderData() {

        if (model.initial_items.size() == 0) {
            UserError.Log.d(TAG, "No initial items loaded yet to find index from");
            return;
        }
        final long highestId = model.initial_items.get(model.initial_items.size() - 1).getId();

        final Thread t = new Thread(() -> {
            JoH.threadSleep(300);
            // show loading spinner if load time takes > 1s and has non-default filter
            Inevitable.task("show-events-loading-if-slow", 1000, () -> {
                if (loading && !model.isDefaultFilters()) {
                    model.showLoading.set(loading);
                }
            });
            loading = true;
            model.older_items.addAll(UserError.olderThanID(highestId, 100000));
            model.refresh();
            loading = false;
            model.showLoading.set(false);
        }
        );
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRefresh();
        updateToTopButtonVisibility(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRefresh();
    }

    // start streaming data
    private synchronized void startRefresh() {
        runRefresh = true;
        new Thread(() -> {
            int c;
            int turbo = 0;

            final boolean streamWearLogs = shouldStreamWearLogs();

            while (runRefresh) {
                if (D) UserError.Log.d(TAG, "refreshing data " + highest_id);
                if (refreshData()) {
                    turbo = 60;
                }
                if (turbo > 0) {
                    // short sleep
                    JoH.threadSleep(100);
                    turbo--;
                } else {
                    if (streamWearLogs && JoH.quietratelimit("stream-wear-logs", 2)) {
                        getWearData();
                    }
                    // long sleep
                    c = 0;
                    while (c < 2 && runRefresh) {
                        JoH.threadSleep(500);
                        c++;
                    }
                }
            }
        }).start();
    }

    // stop streaming data
    private void stopRefresh() {
        runRefresh = false;
    }

    private boolean refreshData() {

        final List<UserError> new_entries = UserError.newerThanID(highest_id, 500);
        if ((new_entries != null) && (new_entries.size() > 0)) {
            final long new_highest = new_entries.get(0).getId();
            UserError.Log.d(TAG, "New streamed data size: " + new_entries.size() + " Highest: " + new_highest);
            if (highest_id == 0) {
                model.newData(new_entries);
            } else {
                model.appendData(new_entries);
            }
            highest_id = new_highest;

            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_eventlog_activity, menu);

        searchItem = menu.findItem(R.id.eventlog_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);


        // show any previous persistent filter
        if (model.getCurrentFilter().length() > 0) {
            pushSearch(model.getCurrentFilter(), false);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                model.filterChanged(s);

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                model.filterChanged(s);
                return false;
            }
        });

        return true;
    }

    // menu item action
    public void viewErrorLog(MenuItem x) {
        startActivity(new Intent(this, ErrorsActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("events", ""));
    }

    // menu item action
    public void returnHome(MenuItem x) {
        startActivity(new Intent(this, Home.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    // push a query in to search box
    private void pushSearch(String query, boolean submit) {
        if (searchItem != null && searchView != null) {
            searchItem.expandActionView();
            searchView.setQuery(query, submit);
            searchView.clearFocus();
        } else {
            UserError.Log.e(TAG, "SearchView is null!");
        }
    }

    // try to determine if we are scrolled to the topmost position
    private boolean isAtTop() {
        if (recyclerView != null) {
            int first_visible_item = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
            if (D) UserError.Log.d(TAG, " First visible item position: " + first_visible_item);
            if (first_visible_item > 1) return false;
        }
        return true;
    }

    // whether to show the TOP navigation button
    private void updateToTopButtonVisibility(boolean force) {
        final boolean top_button_visible = !isAtTop();
        if (force || top_button_visible != model.showScrollToTop.get()) {
            // if (JoH.quietratelimit("event log scroll button change debounce", 1)) {
            model.showScrollToTop.set(top_button_visible);
            // }
        }
    }

    // prepare current visible logs for upload
    public synchronized void uploadEventLogs(View v) {
        final StringBuilder builder = new StringBuilder(50000);
        builder.append("The following logs will be sent to the developers: \n\nPlease also include your email address or we will not know who they are from!\n\nFilter: "
                + (model.allSeveritiesEnabled() ? "ALL" : model.whichSeveritiesEnabled()) + (model.getCurrentFilter() != "" ? " Search: " + model.getCurrentFilter() : "") + "\n\n");
        for (UserError item : model.visible) {
            builder.append(item.toString());
            builder.append("\n");
            if (builder.length() > 200000) {
                JoH.static_toast_long(this, "Could not package up all logs, using most recent");
                break;
            }
        }
        startActivity(new Intent(getApplicationContext(), SendFeedBack.class).putExtra("generic_text", builder.toString()));
    }

    // View model container - accessible binding methods must be declared public
    public class ViewModel implements View.OnTouchListener {

        public final ObservableList<UserError> initial_items = new ObservableArrayList<>();
        public final ObservableList<UserError> older_items = new ObservableArrayList<>();
        public final ObservableList<UserError> streamed_items = new ObservableArrayList<>();
        public final MergeObservableList<UserError> items = new MergeObservableList<UserError>()
                .insertList(streamed_items)
                .insertList(initial_items)
                .insertList(older_items);
        public final ObservableList<UserError> visible = new ObservableArrayList<>();
        public final ItemBinding<UserError> itemBinding = ItemBinding.<UserError>of(BR.error, R.layout.item_event_log).bindExtra(BR.viewModel, this);
        public final EventLogViewAdapterChain adapterChain = new EventLogViewAdapterChain();
        public final ObservableBoolean showScrollToTop = new ObservableBoolean(false);
        public final ObservableBoolean showLoading = new ObservableBoolean(false);
        private final SparseBooleanArray severities = new SparseBooleanArray();
        @Getter
        public View last_clicked_view = null;
        private String last_click_filter = "";
        private String currentFilter = null;

        {
            // persistence load for severities selection
            for (int severity : severitiesList) {
                severities.put(severity, PersistentStore.getBoolean(PREF_SEVERITY_SELECTION + severity, true));
            }
        }

        // populate initial data set
        void newData(List<UserError> newItems) {
            initial_items.clear();
            initial_items.addAll(newItems);
            refresh();
        }

        // append new streamed data to the display
        void appendData(List<UserError> newItems) {
            JoH.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (items) {
                        streamed_items.addAll(0, newItems);
                    }
                    refreshNewItems(newItems.size());
                    if (isAtTop()) {
                        // If unmoved or already at top then scroll to new values
                        JoH.runOnUiThreadDelayed(() -> scrollToTop(true), 300);
                    }
                }
            });
        }

        // return the current filter, load from persistent store if not yet loaded
        public String getCurrentFilter() {
            if (currentFilter == null) {
                currentFilter = PersistentStore.getString(PREF_LAST_SEARCH);
            }
            return currentFilter;
        }

        // refresh the display always applying the current filter
        void refresh() {
            JoH.runOnUiThread(() -> filter(getCurrentFilter()));
        }

        // refresh the display always applying the current filter
        void refreshNewItems(final int count) {
            if (count > 0) {
                JoH.runOnUiThread(() -> insertFilteredNewItems(getCurrentFilter(), count));
            }
        }

        // filter has changed on text input, refresh display
        void filterChanged(String filter) {
            currentFilter = filter.toLowerCase().trim();

            Inevitable.task("event-log-filter-update", 200, () -> {
                refresh();
                PersistentStore.setString(PREF_LAST_SEARCH, currentFilter);
            });
        }

        // is every single severity we filter on enabled
        boolean allSeveritiesEnabled() {
            for (int severity : severitiesList) {
                if (!severity(severity)) return false;
            }
            return true;
        }

        // which severities are enabled (human readable)
        String whichSeveritiesEnabled() {
            String result = "";
            for (int severity : severitiesList) {
                if (severity(severity)) result += severity + " ";
            }
            return result;
        }

        // apply a filter and refresh display
        void filter(final String filter) {
            currentFilter = filter.or(getCurrentFilter()).toLowerCase().trim();
            visible.clear();
            synchronized (items) {
                // skip filter on initial defaults for speed
                if (isDefaultFilters()) {
                    visible.addAll(items);
                } else {
                    for (UserError item : items) {
                        if (filterMatch(item)) {
                            visible.add(item);
                        }
                    }
                }
            }
            adapterChain.notifyDataSetChanged();
        }

        // apply filter just to some new items and update accordingly
        private void insertFilteredNewItems(final String filter, int count) {
            currentFilter = filter.or(getCurrentFilter()).toLowerCase().trim();
            int c = 0;
            int added = 0;
            synchronized (items) {
                for (UserError item : items) {
                    if (filterMatch(item)) {
                        visible.add(0, item);
                        added++;
                    }
                    c++;
                    if (c >= count) break;
                }
            }
            adapterChain.notifyItemRangeChanged(0, added);
            // avoid duplicate titles
            if (visible.size() > 1) {
                adapterChain.notifyItemChanged(added);
            }

        }


        // check if current filter is the out-of-the-box default
        public boolean isDefaultFilters() {
            return ((currentFilter.or("").length() == 0) && allSeveritiesEnabled());
        }

        // check severity enabled and case insensitive contains match using optimized extension method
        public boolean filterMatch(final UserError item) {
            return severities.get(item.severity)
                    && (item.shortError.containsIgnoreCaseF(currentFilter) || (item.message.containsIgnoreCaseF(currentFilter)));
        }


        // whether to show a particular title, eg if it is not a duplicate
        public boolean showThisTitle(UserError item) {

            try {
                final int location = visible.indexOf(item); // cpu intensive?? add cache?
                // if the previous item is the same title then don't show the header
                if (visible.get(location - 1).shortError.equals(item.shortError)) {
                    return false;
                }
            } catch (IndexOutOfBoundsException e) {
                return true;
            }
            return true;
        }


        // instantly scroll to top and update button visibility accordingly
        public void scrollToTop() {
            scrollToTop(false);
        }

        // scroll to top and update button visibility accordingly with smooth option
        public void scrollToTop(boolean smooth) {
            showScrollToTop.set(false);
            if (recyclerView != null) {
                if (smooth) {
                    recyclerView.smoothScrollToPosition(0);
                } else {
                    recyclerView.scrollToPosition(0);
                }
            }
        }


        // store reference to clicked view and pulse it to give feedback to user
        public void lastClicked(View v) {
            last_clicked_view = v;
            v.startAnimation(pulseAnimation);
        }


        // receive call back and populate local instance
        public void initRecycler(View v) {
            if (v instanceof RecyclerView) {
                recyclerView = (RecyclerView) v;
            }
            addScaleDetector(v);
        }

        // add listener for scale detection (on recycler view)
        public void addScaleDetector(View v) {
            v.setOnTouchListener(this);
        }

        // adjust filter to title name/invert when long clicked
        public boolean titleButtonLongClick(View v) {
            String filter = ((TextView) v).getText().toString();
            if (last_click_filter.equals(filter)) {
                filter = "";
            }
            last_click_filter = filter;
            final String ffilter = filter;
            JoH.runOnUiThreadDelayed(() -> pushSearch(ffilter, true), 100);
            return false;
        }

        // choose text color for title to match background
        public int titleColorForSeverity(int severity) {
            switch (severity) {
                case 1:
                    return Color.parseColor("#FFB3E5FC");
                default:
                    return Color.WHITE;
            }
        }

        // choose background color for severity
        public int colorForSeverity(int severity) {

            switch (severity) {
                case 1:
                    return Color.TRANSPARENT;
                case 2:
                    return Color.DKGRAY;
                case 3:
                    return Color.RED;
                case 4:
                    return Color.DKGRAY;
                case 5:
                    return Color.parseColor("#ff337777"); // turquoise
                case 6:
                    return Color.parseColor("#ff337733"); // green

                default:
                    return Color.TRANSPARENT;
            }
        }

        // alternate text colors for clarity
        public int colorForPosition(int position) {
            if (position % 2 == 0) {
                return Color.WHITE; // off white?
            } else {
                return Color.parseColor("#ffffffdd");
            }
        }

        // reformat text size for long messages
        public float textSize(String message) {
            //   final float scale = 4f;
            final float scale = 2.0f * BitmapUtil.getScreenDensity();
            if (message.length() > 100) return 5f * scale;
            return 7f * scale;
        }

        // is this severity enabled?
        public boolean severity(int i) {
            return severities.get(i);
        }

        // severity checkbox clicked, update store and refresh screen
        public void setSeverity(CompoundButton v, boolean value, int i) {
            severities.put(i, value);
            refresh();
            PersistentStore.setBoolean(PREF_SEVERITY_SELECTION + i, value);
        }


        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // pass up
            scaleGestureDetector.onTouchEvent(event);
            return false;
        }

    }

    // scale gesture listener to handler element pinch zoom
    @RequiredArgsConstructor
    public class SimpleOnScaleGestureListener extends
            ScaleGestureDetector.SimpleOnScaleGestureListener {

        private final ViewModel viewModel;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            if (viewModel.last_clicked_view != null) {
                ((TextView) viewModel.last_clicked_view).setTextSize(TypedValue.COMPLEX_UNIT_PX, ((TextView) viewModel.last_clicked_view).getTextSize() * factor);
            }
            return true;
        }
    }

    // recycler view adapter chain to give a little extra under the hood control
    public class EventLogViewAdapterChain<T> extends BindingRecyclerViewAdapter<T> {

        @Override
        public ViewDataBinding onCreateBinding(LayoutInflater inflater, @LayoutRes int layoutId, ViewGroup viewGroup) {
            ViewDataBinding binding = super.onCreateBinding(inflater, layoutId, viewGroup);
            return binding;
        }

        @Override
        public void onBindBinding(ViewDataBinding binding, int bindingVariable, @LayoutRes int layoutId, int position, T item) {
            super.onBindBinding(binding, bindingVariable, layoutId, position, item);
            lastScrollPosition = position;
            updateToTopButtonVisibility(false);
            final TextView tv = binding.getRoot().findViewById(R.id.event_log_item_text);
            if (tv != null) tv.setTextColor(model.colorForPosition(position));
        }

    }
}



