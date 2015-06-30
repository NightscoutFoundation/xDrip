package com.eveningoutpost.dexdrip.stats;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

public class StatsActivity extends ActivityWithMenu {

    public static final String MENU_NAME = "Statistics";

    // When requested, this adapter returns a DemoObjectFragment,
    // representing an object in the collection.
    DemoCollectionPagerAdapter mDemoCollectionPagerAdapter;
    ViewPager mViewPager;
    TextView[] indicationDots;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        mDemoCollectionPagerAdapter =
                new DemoCollectionPagerAdapter(
                        getSupportFragmentManager());
        // set dots for indication
        indicationDots = new TextView[mDemoCollectionPagerAdapter.getCount()];
        LinearLayout indicator = (LinearLayout)findViewById(R.id.indicator_layout);
        for (int i = 0; i < indicationDots.length; i++) {
            indicationDots[i] = new TextView(this);
            indicationDots[i].setText("\u25EF");
            indicationDots[i].setTextSize(15);
            indicator.addView(indicationDots[i], new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        indicationDots[0].setText("\u26AB");
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mDemoCollectionPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < indicationDots.length; i++) {
                    indicationDots[i].setText("\u25EF"); //U+2B24
                }
                indicationDots[position].setText("\u26AB");
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mViewPager.setCurrentItem(0);

        registerButtonListeners();

    }

    private void registerButtonListeners() {
        Button buttonTD = (Button) findViewById(R.id.button_stats_today);
        Button buttonYTD = (Button) findViewById(R.id.button_stats_yesterday);
        Button button7d = (Button) findViewById(R.id.button_stats_7d);
        Button button30d = (Button) findViewById(R.id.button_stats_30d);
        Button button90d = (Button) findViewById(R.id.button_stats_90d);

        //TODO: real listeners, not just repaint dummies


        View.OnClickListener myListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("DrawStats", "button pressed, invalidating");
                mDemoCollectionPagerAdapter.notifyDataSetChanged();
                mViewPager.invalidate();
            }
        };
        buttonTD.setOnClickListener(myListener);


    }

    @Override
    public String getMenuName() {
        return MENU_NAME;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stats, menu);
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



    // Since this is an object collection, use a FragmentStatePagerAdapter,
// and NOT a FragmentPagerAdapter.
    public class DemoCollectionPagerAdapter extends FragmentStatePagerAdapter {
        public DemoCollectionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {

            if (i ==1) {
                return new ChartFragment();
            }

            Fragment fragment = new DemoObjectFragment();
            Bundle args = new Bundle();
            // Our object is just an integer :-P
            args.putInt(DemoObjectFragment.ARG_OBJECT, i + 1);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "OBJECT " + (position + 1);
        }

        @Override
        public int getItemPosition(Object object) {
            // return POSITION_NONE to update/repaint the views if notifyDataSetChanged()+invalidate() is called
            return POSITION_NONE;
        }


    }

    // Instances of this class are fragments representing a single
// object in our collection.
    public static class DemoObjectFragment extends Fragment {
        public static final String ARG_OBJECT = "object";

        @Override
        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            // The last two arguments ensure LayoutParams are inflated
            // properly.
            View rootView = inflater.inflate(
                    R.layout.stats_layout_test, container, false);
            Bundle args = getArguments();
            ((TextView) rootView.findViewById(android.R.id.text1)).setText(
                    Integer.toString(args.getInt(ARG_OBJECT)));
            return rootView;
        }
    }




}
