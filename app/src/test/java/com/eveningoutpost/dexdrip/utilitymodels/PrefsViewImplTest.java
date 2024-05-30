package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

public class PrefsViewImplTest extends RobolectricTestWithConfig {


    private final PrefsViewImpl view = new PrefsViewImpl();

    @Test
    public void getboolTest() {

        assertWithMessage("test default plain false").that(view.getbool("plain-example-no-default")).isFalse();
        assertWithMessage("test default specified false").that(view.getbool("plain-example-default:false")).isFalse();
        assertWithMessage("test default specified true").that(view.getbool("plain-example-default:true")).isTrue();
        assertWithMessage("test default specified invalid 1").that(view.getbool("plain-example-default:")).isFalse();
        assertWithMessage("test default specified invalid 2").that(view.getbool("plain-example-default:blah")).isFalse();
        assertWithMessage("test default specified invalid 3").that(view.getbool("plain-example-default:true:")).isFalse();

    }

    @Test
    public void setboolTest() {

        String pref1 = "test-bool-pref1:true";

        Pref.removeItem(pref1);
        Pref.removeItem(PrefsViewImpl.PrefHandle.parse(pref1).key);
        assertWithMessage("test 1 default before").that(view.getbool(pref1)).isTrue();
        view.setbool(pref1, false);
        assertWithMessage("test 1 false after").that(view.getbool(pref1)).isFalse();
        view.setbool(pref1, true);
        assertWithMessage("test 1 true after").that(view.getbool(pref1)).isTrue();

        String pref2 = "test-bool-pref2:false";

        Pref.removeItem(pref2);
        Pref.removeItem(PrefsViewImpl.PrefHandle.parse(pref2).key);
        assertWithMessage("test 2 default before").that(view.getbool(pref2)).isFalse();
        view.setbool(pref2, false);
        assertWithMessage("test 2 false after").that(view.getbool(pref2)).isFalse();
        view.setbool(pref2, true);
        assertWithMessage("test 2 true after").that(view.getbool(pref2)).isTrue();
        assertWithMessage("test 2 false low level").that(Pref.getBooleanDefaultFalse(pref2)).isFalse();
        assertWithMessage("test 2 false low level").that(Pref.getBooleanDefaultFalse(PrefsViewImpl.PrefHandle.parse(pref2).key)).isTrue();

        String pref3 = "test-bool-pref3";

        Pref.removeItem(pref3);
        Pref.removeItem(PrefsViewImpl.PrefHandle.parse(pref3).key);
        assertWithMessage("test 3 default before").that(view.getbool(pref3)).isFalse();
        view.setbool(pref3, false);
        assertWithMessage("test 3 false after").that(view.getbool(pref3)).isFalse();
        view.setbool(pref3, true);
        assertWithMessage("test 3 true after").that(view.getbool(pref3)).isTrue();

        Pref.removeItem(pref1);
        Pref.removeItem(pref2);
        Pref.removeItem(pref3);
        Pref.removeItem(PrefsViewImpl.PrefHandle.parse(pref1).key);
        Pref.removeItem(PrefsViewImpl.PrefHandle.parse(pref2).key);
        Pref.removeItem(PrefsViewImpl.PrefHandle.parse(pref3).key);
    }


    @Test
    public void getTest() {
        assertWithMessage("test default plain false").that(view.get("plain-example-no-default1")).isFalse();
        assertWithMessage("test default specified false").that(view.get("plain-example-default2:false")).isFalse();
        assertWithMessage("test default specified false 2").that(view.get("plain-example-default2:true")).isFalse(); // cached
        assertWithMessage("test default specified true").that(view.get("plain-example-default3:true")).isTrue();
        assertWithMessage("test default specified true 2").that(view.get("plain-example-default3:false")).isTrue(); // cached
        assertWithMessage("test default specified invalid 1").that(view.get("plain-example-default4:")).isFalse();
        assertWithMessage("test default specified invalid 2").that(view.get("plain-example-default5:blah")).isFalse();
        assertWithMessage("test default specified invalid 3").that(view.get("plain-example-default6:true:")).isFalse();
    }

    @Test
    public void putTest() {

        assertWithMessage("test default plain false 1").that(view.get("plain-example-put-test-1")).isFalse();
        view.put("plain-example-put-test-1", false);
        assertWithMessage("test default plain false 2").that(view.get("plain-example-put-test-1")).isFalse();
        view.put("plain-example-put-test-1", true);
        assertWithMessage("test default plain false 3").that(view.get("plain-example-put-test-1")).isTrue();

        String pref = "plain-example-put-test-2:true";
        assertWithMessage("test default plain true 1").that(view.get(pref)).isTrue();
        view.put(pref, false);
        assertWithMessage("test default plain true 2").that(view.get(pref)).isFalse();
        view.put(pref, true);
        assertWithMessage("test default plain true 3").that(view.get(pref)).isTrue();
    }

}