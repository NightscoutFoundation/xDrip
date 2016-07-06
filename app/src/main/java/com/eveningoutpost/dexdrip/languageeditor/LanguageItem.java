package com.eveningoutpost.dexdrip.languageeditor;

import com.google.gson.annotations.Expose;

/**
 * Created by jamorham on 04/07/2016.
 */
public class LanguageItem {

    @Expose
    public String item_name;
    @Expose
    public String english_text;
    @Expose
    public String local_text;

    public boolean customized = false;
    public final String original_text;


    public LanguageItem(String item_name, String english_text, String local_text) {
        this(item_name, english_text, local_text, false, local_text);
    }

    public LanguageItem(String item_name, String english_text, String local_text, boolean customized,String original_text) {
        this.item_name = item_name;
        this.english_text = english_text;
        this.local_text = local_text;
        this.original_text = original_text;
        this.customized = customized;
    }

}
