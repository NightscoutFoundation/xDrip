package com.eveningoutpost.dexdrip.profileeditor;


import android.app.Activity;
import android.content.Context;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;

import java.util.List;

/**
 * Created by jamorham on 21/06/2016.
 */


public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.MyViewHolder> {

    private static final String TAG = "jamorhamprofile";
    private List<ProfileItem> profileList;
    private int sensTopScale = 1;
    private int carbsTopScale = 1;
    private int sensTopMax = 1;
    private int carbTopMax = 1;
    private Context context;
    int first_run = 0;
    public int max_position = -1;
    private boolean doMgdl;

    public interface TimePickerCallbacks {
        void onTimeUpdated(int newmins);
    }
    public interface DatePickerCallbacks {
        void onDateSet(int year, int month, int day);
    }


    ProfileAdapter(Context ctx, List<ProfileItem> profileList, boolean doMgdl) {
        this.profileList = profileList;
        this.context = ctx;
        this.doMgdl = doMgdl;
        calcTopScale();
    }

    void resetTopMax()
    {
        sensTopMax=1;
        carbTopMax=1;
    }

    private int sensibleMax(int value) {
        return Math.max(10, (int) (((double) value * 1.2) * 10) / 10);
    }

    private int sensibleScale(int max_value) {
        return (max_value < 15) ? 10 : 1;
    }

    // calculate top details for this list
    void calcTopScale() {
        for (ProfileItem item : profileList) {
            int this_scale = sensibleScale((int) item.sensitivity);
            sensTopScale = Math.max(sensTopScale, this_scale);
            carbsTopScale = Math.max(carbsTopScale, sensibleScale((int) item.carb_ratio));
            sensTopMax = Math.max(sensTopMax, sensibleMax((int) item.sensitivity));
            carbTopMax = Math.max(carbTopMax, sensibleMax((int) item.carb_ratio));
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        public TextView title, day;
        SeekBar carbsSeekBar, sensSeekBar;
        TextView startTime, endTime;
        RelativeLayout wholeBlock;
        int value;
        TextView result, carbsresult, sensresult;
        int sensitivity_scaling = 1;
        int carbs_scaling = 1;
        int position = -1;

        MyViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);

            day = (TextView) view.findViewById(R.id.whichday);
            carbsSeekBar = (SeekBar) view.findViewById(R.id.carbsSeekBar);
            sensSeekBar = (SeekBar) view.findViewById(R.id.sensitivitySeekBar);

            carbsresult = (EditText) view.findViewById(R.id.profileCarbsText);
            sensresult = (EditText) view.findViewById(R.id.profileSensText);

            startTime = (TextView) view.findViewById(R.id.profileTextClockStart);
            endTime = (TextView) view.findViewById(R.id.profileTextClockEnd);

            wholeBlock = (RelativeLayout) view.findViewById(R.id.profile_list_row_block);


        }

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.profile_list_row, parent, false);

        final MyViewHolder holder = new MyViewHolder(itemView);
        holder.position = holder.getAdapterPosition();

        Log.d(TAG, "oncreate position: " + holder.position);
        first_run++;

        holder.wholeBlock.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ProfileAdapter.this.notifyItemChanged(holder.position, "long-split");
                return true;
            }
        });

        holder.carbsresult.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (v.getText().length() > 0) {
                        profileList.get(holder.position).carb_ratio = JoH.tolerantParseDouble(v.getText().toString());
                    }
                    ProfileAdapter.this.notifyItemChanged(holder.position, "carb edittext payload ime " + v.getText().toString());
                    //handled = true;
                }
                return handled;
            }
        });
        holder.sensresult.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (v.getText().length() > 0) {
                        profileList.get(holder.position).sensitivity = JoH.tolerantParseDouble(v.getText().toString());
                    }
                    ProfileAdapter.this.notifyItemChanged(holder.position, "sens edittext payload ime " + v.getText().toString());
                }
                return handled;
            }
        });


// carbs seek bar
        holder.carbsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                holder.carbsresult.setText(JoH.qs(((double) progress) / holder.carbs_scaling, 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if (seekBar.getProgress() == seekBar.getMax()) {
                    seekBar.setMax(seekBar.getMax() * 2);
                    // showcase tip
                }
                double value = seekBar.getProgress();
                if (value < 1) value = 1;
                value = value / holder.carbs_scaling;

                profileList.get(holder.position).carb_ratio = value;

                ProfileAdapter.this.notifyItemChanged(holder.position, "carbs seek payload " + value);
            }
        });

        // Sensitivity Seek Bar
        holder.sensSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                holder.sensresult.setText(JoH.qs((double) progress / holder.sensitivity_scaling, 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if (seekBar.getProgress() == seekBar.getMax()) {
                    seekBar.setMax(seekBar.getMax() * 2);
                    // = true;
                    // showcase tip
                }
                double value = seekBar.getProgress();
                if (value < 1) value = 1;
                value = value / holder.sensitivity_scaling;

                profileList.get(holder.position).sensitivity = value;
                ProfileAdapter.this.notifyItemChanged(holder.position, "sens seek payload " + value);
            }
        });


        return holder;
    }


    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final ProfileItem profileItem = profileList.get(position);
        holder.position = position;

        // first run
        if (first_run > 0) {

            calcTopScale();
            //holder.carbsSeekBar.setMax(carbTopMax);

            holder.carbs_scaling = carbsTopScale;
            holder.carbsSeekBar.setMax(carbTopMax * holder.carbs_scaling);

            holder.sensitivity_scaling = sensTopScale;
            holder.sensSeekBar.setMax(sensTopMax * holder.sensitivity_scaling);


            Log.d(TAG, "first run: " + first_run + " carbTopMax:"+carbTopMax+" Sensitivity: pos:" + position + "  scaling:" + sensTopScale + "/" + holder.sensitivity_scaling + " sensiblemax:" + sensTopMax + "/" + sensibleMax((int) profileItem.sensitivity));

            first_run--;

        }

        holder.title.setText(profileItem.getTimePeriod());

        holder.carbsSeekBar.setProgress((int) (profileItem.carb_ratio) * holder.carbs_scaling);
        holder.carbsresult.setText(JoH.qs(profileItem.carb_ratio, 1));
        holder.sensSeekBar.setProgress((int) (profileItem.sensitivity) * holder.sensitivity_scaling);
        holder.sensresult.setText(JoH.qs(profileItem.sensitivity, 1));


        if (profileList.size()>1) {
            holder.startTime.setText(profileItem.getTimeStart());
            holder.endTime.setText(profileItem.getTimeEnd());

            holder.startTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TimePickerFragment newFragment = new TimePickerFragment();
                    newFragment.setTimeObject(profileItem.start_min);
                    newFragment.setTimeCallback(new TimePickerCallbacks() {
                        @Override
                        public void onTimeUpdated(int newmins) {
                            profileItem.start_min = newmins;

                            if (profileItem.start_min > profileItem.end_min) {
                                profileItem.end_min = (profileItem.start_min + 5) % ProfileEditor.MINS_PER_DAY;
                            }


                            notifyItemChanged(holder.position, "time start updated");
                        }
                    });
                    newFragment.show(((Activity) context).getFragmentManager(), "TimePicker");
                }
            });
            holder.endTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TimePickerFragment newFragment = new TimePickerFragment();
                    newFragment.setTimeObject(profileItem.end_min);
                    newFragment.setTimeCallback(new TimePickerCallbacks() {
                        @Override
                        public void onTimeUpdated(int newmins) {
                            profileItem.end_min = newmins;

                            if (profileItem.end_min < profileItem.start_min) {
                                profileItem.start_min = (profileItem.end_min - 5) % ProfileEditor.MINS_PER_DAY;
                            }

                            ProfileAdapter.this.notifyItemChanged(holder.position, "time end updated");
                        }
                    });
                    newFragment.show(((Activity) context).getFragmentManager(), "TimePicker");
                }
            });

            holder.startTime.setVisibility(View.VISIBLE);
            holder.endTime.setVisibility(View.VISIBLE);
        } else {
            holder.startTime.setVisibility(View.INVISIBLE);
            holder.endTime.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return profileList.size();
    }


}
