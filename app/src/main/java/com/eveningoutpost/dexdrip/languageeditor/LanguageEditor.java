package com.eveningoutpost.dexdrip.languageeditor;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.JamorhamShowcaseDrawer;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.SendFeedBack;
import com.eveningoutpost.dexdrip.UtilityModels.ShotStateStore;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

/**
 * Created by jamorham on 04/07/2016.
 */


public class LanguageEditor extends BaseAppCompatActivity {

    private static final String TAG = "jamorhamlanguage";
    private static final String EMAIL_KEY = "___EMAIL_KEY:";
    private static final String NAME_KEY = "___NAME_KEY:";
    private static final String CONSENT_KEY = "___CONSENT_KEY";
    private final List<LanguageItem> languageItemList = new ArrayList<>();
    private final List<LanguageItem> languageItemListBackup = new ArrayList<>();
    private RecyclerView recyclerView;
    private LanguageAdapter mAdapter;
    // private static Button cancelBtn;
    private static Button saveBtn;
    private static Button undoBtn;

    private boolean show_only_customized = false;
    private boolean show_only_untranslated = false;
    protected static String last_filter = "";

    private static Map<String, LanguageItem> user_edits = new HashMap<>();

    protected static final boolean oneshot = true;
    private static ShowcaseView myShowcase;

    private static final int SHOWCASE_LANGUAGE_INTRO = 601;
    protected static final int SHOWCASE_LANGUAGE_ELEMENT_UNDO = 602;
    protected static final int SHOWCASE_LANGUAGE_ELEMENT_NEWLINE = 603;

    private Context mContext;
    private Toolbar toolbar;


    private String getStringFromLocale(int id, Locale desired_locale) {
        final Resources my_resources = getResources();
        final Configuration cfg = my_resources.getConfiguration();
        final Locale savedLocale = cfg.locale;
        cfg.setLocale(desired_locale);
        my_resources.updateConfiguration(cfg, null);
        final String result = my_resources.getString(id);
        cfg.setLocale(savedLocale);
        my_resources.updateConfiguration(cfg, null);
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeToolBarLite); // for toolbar mode
        setContentView(R.layout.activity_language_editor);

        toolbar = (Toolbar) findViewById(R.id.language_toolbar);
        setSupportActionBar(toolbar);

        undoBtn = (Button) findViewById(R.id.languageUndoBtn);
        saveBtn = (Button) findViewById(R.id.languageSaveBtn);
        // cancelBtn = (Button) findViewById(R.id.languageCancelBtn);
        JoH.fixActionBar(this);
        forceReload();

        recyclerView = (RecyclerView) findViewById(R.id.language_recycler_view);

        mAdapter = new LanguageAdapter(this, languageItemList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        mAdapter.registerAdapterDataObserver(
                // handle incoming messages from the adapater
                new RecyclerView.AdapterDataObserver() {

                    @Override
                    public void onChanged() {
                        super.onChanged();
                        //  Log.d(TAG, "onChanged");
                    }

                    @Override
                    public void onItemRangeChanged(final int positionStart, int itemCount, Object payload) {
                        super.onItemRangeChanged(positionStart, itemCount, payload);

                        Log.d(TAG, "onItemRangeChanged: pos:" + positionStart + " cnt:" + itemCount + " p: " + payload.toString());

                        try {

                            final LanguageItem updated_element = (LanguageItem) payload;
                            final LanguageItem current_item = languageItemList.get(positionStart);
                            if (!current_item.original_text.equals(updated_element.local_text)) {
                                user_edits.put(updated_element.item_name, (LanguageItem) payload); // on change

                                if (languageItemList.get(positionStart).item_name.equals(updated_element.item_name)) {
                                    languageItemList.get(positionStart).local_text = updated_element.local_text;
                                    languageItemList.get(positionStart).customized = true;

                                } else {
                                    Log.e(TAG, "Error item at position during update does not match!");
                                }
                                saveBtn.setVisibility(View.VISIBLE);
                                undoBtn.setVisibility(View.VISIBLE);
                                forceRefresh();
                            } else {
                                Log.d(TAG, "Updated element is same as original - ignoring");
                            }
                        } catch (ClassCastException e) {
                            if (payload.toString().equals("undo")) {
                                languageItemList.get(positionStart).local_text = languageItemList.get(positionStart).original_text;
                                languageItemList.get(positionStart).customized = false;
                                forceRefresh();
                            } else {
                                Log.e(TAG, "Could not cast item-change payload to LanguageItem: " + e.toString());
                            }
                        }
                    }
                });


        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };

        recyclerView.setItemAnimator(animator);
        recyclerView.setAdapter(mAdapter);
        applyFilter(last_filter);
        forceRefresh();

        // handle case where we don't know about any translations for current language
        if (languageItemList.size() == 0 || Locale.getDefault().toString().startsWith("en")) {
            if (Locale.getDefault().toString().startsWith("en")) {
                android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(this);

                if (!Pref.getBoolean("force_english", false)) {
                    alertDialogBuilder.setMessage("To access translation features your phone or tablet must be set to a language other than English.\n\nTo achieve this, use the phone's system settings to change Language."); // don't extract/translate this string
                } else {
                    alertDialogBuilder.setMessage("To access translation features your phone or tablet must be set to a language other than English.\n\nTo achieve this, disable the Force English option within xDrip+ display settings."); // don't extract/translate this string
                }
                alertDialogBuilder.setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                alertDialogBuilder.show();

            } else {
                android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(this);
                alertDialogBuilder.setMessage("xDrip+ does not yet have the capability to translate for: " + Locale.getDefault().toString() + "\n\n" + "But we can add this if you request it!"); // don't extract/translate this string
                alertDialogBuilder.setNegativeButton("No, don't request my language", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                alertDialogBuilder.setPositiveButton("Yes, please support my language", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // fire off a request!
                        startActivity(new Intent(getApplicationContext(), SendFeedBack.class).putExtra("request_translation", Locale.getDefault().toString()));
                        finish();
                    }
                });
                alertDialogBuilder.show();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if ((languageItemList.size() > 0) && (!Locale.getDefault().toString().startsWith("en"))) {
            showcasemenu(SHOWCASE_LANGUAGE_INTRO);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_language, menu);

        MenuItem searchItem = menu.findItem(R.id.action_language_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                applyFilter(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                applyFilter(s);
                return false;
            }
        });
        return true;
    }

    public void languageResetEverything(MenuItem v) {
        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Are you sure you want to reset all edited language data?"); // don't extract/translate this string
        alertDialogBuilder.setNegativeButton("No, keep my data", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialogBuilder.setPositiveButton("Delete all language edits", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LanguageStore.resetEverything();
                user_edits.clear();
                forceReload();
                applyFilter(last_filter);
                forceRefresh();
            }
        });
        alertDialogBuilder.show();
    }

    public void languageShowOnlyCustomized(MenuItem v) {
        v.setChecked(!v.isChecked());
        show_only_customized = v.isChecked();
        applyFilter(last_filter);
        forceRefresh();
    }

    public void languageShowOnlyUntranslated(MenuItem v) {
        v.setChecked(!v.isChecked());
        show_only_untranslated = v.isChecked();
        applyFilter(last_filter);
        forceRefresh();
    }

    private void applyFilter(String filter) {
        last_filter = filter;
        // create initial backup if no filter yet applied
        if (languageItemListBackup.size() == 0) {
            languageItemListBackup.addAll(languageItemList);
        } else {
            // restore before new filter
            languageItemList.clear();
            languageItemList.addAll(languageItemListBackup);
        }

        final List<LanguageItem> filteredItemList = new ArrayList<>();
        filter = filter.toLowerCase();
        for (LanguageItem item : languageItemList) {
            if ((filter.length() == 0)
                    || item.original_text.toLowerCase().contains(filter)
                    || item.english_text.toLowerCase().contains(filter)
                    || item.local_text.toLowerCase().contains(filter)
                    || item.item_name.toLowerCase().contains(filter)) {
                if ((!show_only_untranslated) || isUntranslated(item)) {
                    if ((!show_only_customized) || (item.customized)) filteredItemList.add(item);
                }
            }
        }
        languageItemList.clear();
        languageItemList.addAll(filteredItemList);
        forceRefresh();
    }

    private boolean isUntranslated(LanguageItem item) {
        return item.english_text.equals(item.local_text);
    }

    private void getEmailAddress() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Email address?");
        builder.setMessage("To provide crowd-sourced translations for xDrip+ we request your email address so we can contact you with any questions. Your email address will not be disclosed or used for any other purpose.");
        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LanguageStore.putString(EMAIL_KEY, input.getText().toString());
                saveData();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void getThanksName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Name for thanks page?");
        builder.setMessage("We also want to have a notice in xDrip+ thanking our translators. We don't use your email address for this.\n\nPlease tell us a Name or Alias we can use to put on the thanks page for you?");
        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LanguageStore.putString(NAME_KEY, input.getText().toString());
                saveData();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void getConsent() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Copyright Disclaimer");
        builder.setMessage("If approved, your translations will be published as part of xDrip+\n\nTo ensure the stability of the project we must ask contributors to confirm that they assign the copyright of translations to the project so that we are allowed to redistribute the text.");

        builder.setPositiveButton("I ASSIGN COPYRIGHT TO THE PROJECT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LanguageStore.putString(CONSENT_KEY, "Agreed:" + JoH.ts());
                saveData();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }


    private void toast(String msg) {
        JoH.static_toast(mContext, msg, Toast.LENGTH_LONG);
    }

    private void uploadData(String data) {
        if (data.length() < 10) {
            toast("No text entered - cannot send blank"); // don't translate
            return;
        }

        final String email = LanguageStore.getString(EMAIL_KEY);
        final String name = LanguageStore.getString(NAME_KEY);
        final String consent = LanguageStore.getString(CONSENT_KEY);

        if (email.length() == 0) {
            toast("No email address stored - cannot send"); // don't translate
            return;
        }

        if (name.length() == 0) {
            toast("No thanks name stored - cannot send"); // don't translate
            return;
        }

        if (consent.length() == 0) {
            toast("No consent stored - cannot send"); // don't translate
            return;
        }


        final OkHttpClient client = new OkHttpClient();
        final String send_url = mContext.getString(R.string.wserviceurl) + "/joh-langdata";

        client.setConnectTimeout(20, TimeUnit.SECONDS);
        client.setReadTimeout(30, TimeUnit.SECONDS);
        client.setWriteTimeout(30, TimeUnit.SECONDS);

        toast("Sending.."); // don't translate

        try {
            final RequestBody formBody = new FormEncodingBuilder()
                    .add("locale", Locale.getDefault().toString())
                    .add("contact", email)
                    .add("name", name)
                    .add("consent", consent)
                    .add("data", data)
                    .build();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        final Request request = new Request.Builder()
                                .url(send_url)
                                .post(formBody)
                                .build();
                        Log.i(TAG, "Sending language data");
                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            toast("data sent successfully");
                            ((Activity) mContext).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        saveBtn.setVisibility(View.INVISIBLE);
                                        undoBtn.setVisibility(View.INVISIBLE);
                                    } catch (Exception e) {
                                        // do nothing if its gone away
                                    }
                                }
                            });
                        } else {
                            toast("Error sending data: " + response.message());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Got exception in execute: " + e.toString());
                        toast("Error with network connection"); // don't translate
                    }
                }
            }).start();
        } catch (Exception e) {
            toast(e.getMessage());
            Log.e(TAG, "General exception: " + e.toString());
        }
    }

    private void saveData() {
        if (!LanguageStore.getString(EMAIL_KEY).equals("")) {
            if (!LanguageStore.getString(NAME_KEY).equals("")) {
                if (!LanguageStore.getString(CONSENT_KEY).equals("")) {
                    if (JoH.ratelimit("language-save", 5)) {
                        for (LanguageItem item : user_edits.values()) {
                            LanguageStore.putString(item.item_name, item.local_text);
                        }
                        final String data = getJsonToSave();
                        Log.d(TAG, "Data to save: " + data);
                        uploadData(data);
                    }
                } else {
                    getConsent();
                }
            } else {
                getThanksName();
            }
        } else {
            getEmailAddress();
        }
    }

    private String getJsonToSave() {

        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                //.registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();

        return gson.toJson(user_edits.values());
    }


    public void languageCancelButton(View myview) {
        finish();
    }

    public void languageSaveButton(View myview) {
        saveData();
        //finish();
    }

    public void languageUndoButton(View myview) {

        forceReload();
        applyFilter(last_filter);
        forceRefresh(false);

    }

    private void forceReload() {
        languageItemList.clear();
        languageItemListBackup.clear();
        user_edits.clear();
        // we must preserve the existing object reference used by the adapter
        languageItemList.addAll(loadData(true));
        // TODO sanely repopulate user_edits with customized data??
    }

    private List<LanguageItem> loadData(boolean buttons) {
        final List<LanguageItem> mylanguageItemList = new ArrayList<>();

        // create string name hashset of things we want to be able to translate
        final StringTokenizer tokenizer = new StringTokenizer(getString(R.string.internal_translatable_index), ",");
        final List<String> tokenizer_list = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            tokenizer_list.add(tokenizer.nextToken());
        }
        final ImmutableSet<String> translatable_index_names = ImmutableSet.copyOf(tokenizer_list);
        tokenizer_list.clear();

        final Field[] fields = R.string.class.getFields();
        for (final Field field : fields) {
            final String name = field.getName(); //name of string

            try {
                int id = field.getInt(R.string.class); //id of string
                final String local_text = getResources().getString(id);
                final String english_text = getStringFromLocale(id, Locale.ENGLISH);
                if ((!local_text.equals(english_text) || translatable_index_names.contains(name)) && !name.startsWith("abc_") && !name.startsWith("common_") && !name.startsWith("twofortyfouram_") && !name.startsWith("zxing_")) {
                    // Log.d(TAG, "name: " + name + " / reflect id:" + id + " english:" + english_text + " / local:" + local_text);
                    final String alternate_text = LanguageStore.getString(name);
                    final boolean customized = (alternate_text.length() > 0);
                    LanguageItem item = new LanguageItem(name, english_text, customized ? alternate_text : local_text, customized, local_text);
                    mylanguageItemList.add(item);
                    if (customized) {
                        user_edits.put(item.item_name, item); // refill edits list with anything previously saved
                    }
                }
            } catch (Exception e) {
                //error
            }
        }
        if (buttons) {
            saveBtn.setVisibility(View.INVISIBLE);
            undoBtn.setVisibility(View.INVISIBLE);
        }
        Log.d(TAG, "Loaded data");
        return mylanguageItemList;
    }


    private void forceRefresh() {
        forceRefresh(true);
    }

    private void forceRefresh(boolean save) {
        mAdapter.first_run = languageItemList.size();
        recyclerView.invalidate();
        recyclerView.refreshDrawableState();
        mAdapter.notifyDataSetChanged();
    }


    private synchronized void showcasemenu(final int option) {
        if ((myShowcase != null) && (myShowcase.isShowing())) return;
        if (ShotStateStore.hasShot(option)) return;
        if (!JoH.ratelimit("language-showcase", 2)) return;
        try {

            ToolbarActionItemTarget target = null;
            ViewTarget viewtarget = null;
            String title = "";
            String message = "";

            switch (option) {

                case SHOWCASE_LANGUAGE_INTRO:
                    target = new ToolbarActionItemTarget(toolbar, R.id.action_language_search);
                    // these messages always appear in english only
                    title = "Search and Submit Translations";
                    message = "You can help crowd-source better translations for xDrip+\n\nSimply find and edit the text, click ✔ or ➡ on your keyboard to save. Match as closely as possible the English version (including preserving punctuation symbols and capitalisation) and then use Save button to Upload your suggested changes.\n\nIt is not yet possible to preview your changes in the App, but if accepted they will appear in future updates.";
                    break;

                case SHOWCASE_LANGUAGE_ELEMENT_UNDO:
                    // done inside the adapter
                    break;
            }

            // seems a bit excessive just to delay the lookup of the view to avoid it failing sometimes
            final ToolbarActionItemTarget finaltarget = target;
            final ViewTarget finalviewtarget = viewtarget;
            final String finalmessage = message;
            final String finaltitle = title;
            final Activity finalactivity = this;

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    if ((finaltarget != null) || (finalviewtarget != null)) {
                        myShowcase = new ShowcaseView.Builder(finalactivity)
                                .setTarget((finaltarget != null) ? finaltarget : finalviewtarget)
                                .setStyle(R.style.CustomShowcaseTheme2)
                                .setContentTitle(finaltitle)
                                .setContentText("\n" + finalmessage)
                                .setShowcaseDrawer(new JamorhamShowcaseDrawer(getResources(), getTheme(), 90, 14))
                                .singleShot(oneshot ? option : -1)
                                .build();

                        myShowcase.setBackgroundColor(Color.TRANSPARENT);
                        myShowcase.show();
                    }
                }
            }, 100);

        } catch (Exception e) {
            Log.e(TAG, "Exception in showcase: " + e.toString());
        }
    }


    class ToolbarActionItemTarget implements Target {

        private final Toolbar toolbar;
        private final int menuItemId;

        public ToolbarActionItemTarget(Toolbar toolbar, @IdRes int itemId) {
            this.toolbar = toolbar;
            this.menuItemId = itemId;
        }

        @Override
        public Point getPoint() {
            return new ViewTarget(toolbar.findViewById(menuItemId)).getPoint();
        }

    }
}

