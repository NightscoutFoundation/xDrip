package com.eveningoutpost.dexdrip.languageeditor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
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

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.JamorhamShowcaseDrawer;
import com.eveningoutpost.dexdrip.UtilityModels.ShotStateStore;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.util.List;

/**
 * Created by jamorham on 04/07/2016.
 */


public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.MyViewHolder> {

    private static final String TAG = "jamorhamlang";
    private List<LanguageItem> languageList;
    private Context context;
    private boolean showcased_undo = false;
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
                    final int pos = holder.getAdapterPosition();
                    LanguageAdapter.this.notifyItemChanged(pos, new LanguageItem(languageList.get(pos).item_name, languageList.get(pos).english_text, v.getText().toString()));
                }
                return handled;
            }
        });

        return holder;
    }


    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final LanguageItem languageItem = languageList.get(position);
        holder.position = position;


        holder.english_text.setText(languageItem.english_text);
        holder.local_text.setText(languageItem.local_text);
        holder.id_text.setText(languageItem.item_name);


        if (languageItem.customized) {
            holder.local_text.setTextColor(Color.parseColor("#d6a5a7"));
            holder.elementUndo.setVisibility(View.VISIBLE);

            // optimize with flag
            if (!showcased_undo) {
                if (JoH.ratelimit("language-showcase",2)) {
                    if (!ShotStateStore.hasShot(LanguageEditor.SHOWCASE_LANGUAGE_ELEMENT_UNDO)) {
                        ShowcaseView myShowcase = new ShowcaseView.Builder((Activity) context)

                                .setTarget(new ViewTarget(holder.elementUndo))
                                .setStyle(R.style.CustomShowcaseTheme2)
                                .setContentTitle("Item Undo Button") // always in english
                                .setContentText("\n" + "You can Undo a single change by Long-pressing the Undo button.") // always in english
                                .setShowcaseDrawer(new JamorhamShowcaseDrawer(context.getResources(), context.getTheme(), 90, 14))
                                .singleShot(LanguageEditor.oneshot ? LanguageEditor.SHOWCASE_LANGUAGE_ELEMENT_UNDO : -1)
                                .build();

                        myShowcase.setBackgroundColor(Color.TRANSPARENT);
                        myShowcase.show();
                        showcased_undo = true;
                    }
                }
            }

        } else {
            holder.local_text.setTextColor(Color.parseColor("#a5d6a7"));
            holder.elementUndo.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return languageList.size();
    }
}
