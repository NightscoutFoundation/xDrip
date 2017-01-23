package com.eveningoutpost.dexdrip;

/**
 * Created by jamorham on 14/01/2017.
 *
 * Multi-page plugin style status entry lists
 *
 */

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.RollCall;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.DoNothingService;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;
import com.eveningoutpost.dexdrip.Services.WifiCollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.JamorhamShowcaseDrawer;
import com.eveningoutpost.dexdrip.UtilityModels.ShotStateStore;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.util.ArrayList;
import java.util.List;

public class MegaStatus extends ActivityWithMenu {


    private SectionsPagerAdapter mSectionsPagerAdapter;

    private static final String menu_name = "Mega Status";
    private static final String TAG = "MegaStatus";
    private static final long autoFreshDelay = 500;

    private static boolean activityVisible = false;
    private static boolean autoFreshRunning = false;
    private static Runnable autoRunnable;
    private static int currentPage = 0;

    private static final ArrayList<String> sectionList = new ArrayList<>();
    private static final ArrayList<String> sectionTitles = new ArrayList<>();

    private ViewPager mViewPager;


    private static ArrayList<MegaStatusListAdapter> MegaStatusAdapters = new ArrayList<>();

    private void addAsection(String section, String title) {
        sectionList.add(section);
        sectionTitles.add(title);
        MegaStatusAdapters.add(new MegaStatusListAdapter());
    }

    private static final String G5_STATUS = "G5 Status";
    private static final String IP_COLLECTOR = "IP Collector";
    private static final String XDRIP_PLUS_SYNC = "Followers";

    private void populateSectionList() {

        if (sectionList.isEmpty()) {

            addAsection("Classic Status Page", "Legacy System Status");
            if (DexCollectionType.getDexCollectionType() == DexCollectionType.DexcomG5) {
                addAsection(G5_STATUS, "G5 Collector and Transmitter Status");
            }
            if (DexCollectionType.hasWifi()) {
                addAsection(IP_COLLECTOR, "Wifi Wixel / Parakeet Status");
            }
            if (Home.get_master_or_follower()) {
                addAsection(XDRIP_PLUS_SYNC,"xDrip+ Sync Group");
            }
            //addAsection("Misc", "Currently Empty");

        } else {
            UserError.Log.d(TAG, "Section list already populated");
        }
    }

    private static void populate(MegaStatusListAdapter la, String section) {
        if ((la == null) || (section == null)) {
            UserError.Log.e(TAG, "Adapter or Section were null in populate()");
            return;
        }

        la.clear(false);
        switch (section) {

            case G5_STATUS:
                la.addRows(G5CollectionService.megaStatus());
                break;
            case IP_COLLECTOR:
                la.addRows(WifiCollectionService.megaStatus());
                break;
            case XDRIP_PLUS_SYNC:
                la.addRows(DoNothingService.megaStatus());
                la.addRows(GcmListenerSvc.megaStatus());
                la.addRows(RollCall.megaStatus());
                break;
        }
        la.changed();
    }

    @Override
    public String getMenuName() {
        return menu_name;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mega_status);
        JoH.fixActionBar(this);

        sectionList.clear();
        sectionTitles.clear();
        populateSectionList();

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
            UserError.Log.d(TAG,"Page selected: " + position);
                currentPage=position;
                startAutoFresh();
        }});

    }


    @Override
    public void onPause() {
        activityVisible = false;
        super.onPause();
    }


    @Override
    protected void onResume() {
        super.onResume();
        activityVisible = true;
        if (autoRunnable != null) startAutoFresh();

        if (sectionList.size() > 1)
            startupInfo(); // show swipe message if there is a page to swipe to
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_mega_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startupInfo() {

            final boolean oneshot = true;
            final int option = Home.SHOWCASE_MEGASTATUS;
            if ((oneshot) && (ShotStateStore.hasShot(option))) return;

            // This could do with being in a utility static method also used in Home
            final int size1 = 300;
            final int size2 = 130;
            final String title = "Swipe for Different Pages";
            final String message = "Swipe left and right to see different status tabs.\n\n";
            final ViewTarget target = new ViewTarget(R.id.pager_title_strip, this);
            final Activity activity = this;

            JoH.runOnUiThreadDelayed(new Runnable() {
                                         @Override
                                         public void run() {
                                             final ShowcaseView myShowcase = new ShowcaseView.Builder(activity)

                                                     .setTarget(target)
                                                     .setStyle(R.style.CustomShowcaseTheme2)
                                                     .setContentTitle(title)
                                                     .setContentText("\n" + message)
                                                     .setShowcaseDrawer(new JamorhamShowcaseDrawer(getResources(), getTheme(), size1, size2, 255))
                                                     .singleShot(oneshot ? option : -1)
                                                     .build();
                                             myShowcase.setBackgroundColor(Color.TRANSPARENT);
                                             myShowcase.show();
                                         }
                                     }
                    , 1500);
        }

    private synchronized void startAutoFresh() {
        if (autoFreshRunning) return;
        if (autoRunnable == null) autoRunnable = new Runnable() {

            @Override
            public void run() {
                try {
                    if ((activityVisible) && (autoFreshRunning) && (currentPage != 0)) {
                        MegaStatus.populate(MegaStatusAdapters.get(currentPage), sectionList.get(currentPage));
                        JoH.runOnUiThreadDelayed(autoRunnable, autoFreshDelay);
                    } else {
                        UserError.Log.d(TAG, "AutoFresh shutting down");
                        autoFreshRunning = false;
                    }
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Exception in auto-fresh: " + e);
                    autoFreshRunning = false;
                }
            }
        };

        JoH.runOnUiThreadDelayed(autoRunnable, 200);
        autoFreshRunning = true;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {


            final int index = getArguments().getInt(ARG_SECTION_NUMBER);

            View rootView = inflater.inflate(R.layout.fragment_mega_status, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            ListView listView = (ListView) rootView.findViewById(R.id.list_label);
            UserError.Log.d(TAG, "Setting Section " + index);

            textView.setText(sectionTitles.get(index));

            listView.setAdapter(MegaStatusAdapters.get(index));
            MegaStatus.populate((MegaStatusListAdapter) listView.getAdapter(), sectionList.get(index));


            return rootView;
        }
    }

    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) return new SystemStatusFragment();
            return PlaceholderFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return sectionList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return sectionList.get(position);
        }
    }


    static class ViewHolder {
        TextView name;
        TextView value;
    }

    private class MegaStatusListAdapter extends BaseAdapter {
        private ArrayList<StatusItem> statusRows;
        private LayoutInflater mInflator;

        MegaStatusListAdapter() {
            super();
            statusRows = new ArrayList<>();
            mInflator = MegaStatus.this.getLayoutInflater();
        }

        public StatusItem getRow(int position) {
            return statusRows.get(position);
        }

        void addRow(StatusItem obj) {
            statusRows.add(obj);
        }

        void addRows(List<StatusItem> list) {
            for (StatusItem item : list) {
                addRow(item);
            }
        }

        public void changed() {
            notifyDataSetChanged();
        }

        public void clear(boolean refresh) {
            statusRows.clear();
            if (refresh) notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return statusRows.size();
        }

        @Override
        public Object getItem(int i) {
            return statusRows.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;

            if (view == null) {

                view = mInflator.inflate(R.layout.listitem_megastatus, null);
                viewHolder = new ViewHolder();

                viewHolder.value = (TextView) view.findViewById(R.id.value);
                viewHolder.name = (TextView) view.findViewById(R.id.name);
                view.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            final StatusItem row = statusRows.get(i);

            viewHolder.name.setText(row.name);
            viewHolder.value.setText(row.value);

            return view;
        }
    }
}
