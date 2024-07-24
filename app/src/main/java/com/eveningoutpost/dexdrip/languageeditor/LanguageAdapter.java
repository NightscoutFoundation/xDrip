package com.eveningoutpost.dexdrip.languageeditor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.JamorhamShowcaseDrawer;
import com.eveningoutpost.dexdrip.utilitymodels.ShotStateStore;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.util.List;

/**
 * Created by jamorham on 04/07/2016.
 */

// TODO Due for deletion - will no longer function
@Deprecated
public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.MyViewHolder> {

    private static final String TAG = "jamorhamlang";
    private List<LanguageItem> languageList;
    private Context context;
    private boolean showcased_undo = false;
    private boolean showcased_newline = false;
    public int first_run = 0;

    public LanguageAdapter(Context ctx, List<LanguageItem> languageList) {
        this.languageList = languageList;
        this.context = ctx;
        Log.d(TAG, "New adapter, size: " + languageList.size());
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout wholeBlock;
        ImageButton elementUndo;
        TextView english_text, id_text;
        EditText local_text;
        int position = -1;

        public MyViewHolder(View view) {
            super(view);

            id_text = (TextView) view.findViewById(R.id.language_id_reference);
            english_text = (TextView) view.findViewById(R.id.language_english_text);
            local_text = (EditText) view.findViewById(R.id.language_translated_text);
            elementUndo = (ImageButton) view.findViewById(R.id.languageElementUndo);
          //  wholeBlock = (RelativeLayout) view.findViewById(R.id.language_list_block);
        }
    }

    private void informThisRowChanged(MyViewHolder holder, TextView v) {
        final int pos = holder.getAdapterPosition();
        try {
            Log.d(TAG, "informThisRowChanged: " + pos);
            if (pos > -1) {
                LanguageAdapter.this.notifyItemChanged(pos, new LanguageItem(languageList.get(pos).item_name, languageList.get(pos).english_text, v.getText().toString().replace(" ^ ", "\n")));
            }
        } catch (IllegalStateException e) {
            Log.d(TAG,"informThisRowChanged - cannot calculate during scroll");
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.language_list_row, parent, false);

        final MyViewHolder holder = new MyViewHolder(itemView);

        // allow ime-done on multiline
        holder.local_text.setHorizontallyScrolling(false);
        holder.local_text.setMaxLines(100);

        holder.elementUndo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                LanguageAdapter.this.notifyItemChanged(holder.position, "undo");
                return true;
            }
        });

        holder.local_text.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                 informThisRowChanged(holder, v);
                     }
                return handled;
            }
        });

        holder.local_text.setOnFocusChangeListener((v, hasFocus) -> {
           if (hasFocus == false) {
               informThisRowChanged(holder, (TextView)v);
           }
        });



     /*   holder.local_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final int pos = holder.getAdapterPosition();
                // has it actually changed or was this recycle element just reused?
                if (!languageList.get(pos).local_text.equals(s.toString()))
                {
                    Log.d(TAG,"afterTextChanged: "+pos+" :"+s+":"+languageList.get(pos).local_text);
                    LanguageAdapter.this.notifyItemChanged(pos, new LanguageItem(languageList.get(pos).item_name, languageList.get(pos).english_text, s.toString()));

                }
             }
        });
*/
        return holder;
    }


    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return languageList.size();
    }
}
