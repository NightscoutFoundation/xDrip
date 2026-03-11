package com.eveningoutpost.dexdrip.cgm.webfollow;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.Test;

import java.lang.reflect.Method;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class TemplateTest extends RobolectricTestWithConfig {

    @Test
    public void getUrl_returnsNullWhenCkIsNull() throws Exception {
        // :: Setup
        Pref.setString("webfollow_master_domain", null);

        // :: Act
        String result = invokeGetUrl();

        // :: Verify
        assertThat(result).isNull();
    }

    @Test
    public void getUrl_returnsNullWhenCkIsEmpty() throws Exception {
        // :: Setup
        Pref.setString("webfollow_master_domain", "");

        // :: Act
        String result = invokeGetUrl();

        // :: Verify
        assertThat(result).isNull();
    }

    @Test
    public void getUrl_buildsUrlFromSimpleCk() throws Exception {
        // :: Setup
        Pref.setString("webfollow_master_domain", "myserver");

        // :: Act
        String result = invokeGetUrl();

        // :: Verify
        assertThat(result).isEqualTo("https://community-script.myserver.net/v2");
    }

    @Test
    public void getUrl_returnsCkDirectlyWhenContainsDot() throws Exception {
        // :: Setup
        Pref.setString("webfollow_master_domain", "custom.server.com/path");

        // :: Act
        String result = invokeGetUrl();

        // :: Verify
        assertThat(result).isEqualTo("custom.server.com/path");
    }

    private static String invokeGetUrl() throws Exception {
        Method method = Template.class.getDeclaredMethod("getUrl");
        method.setAccessible(true);
        return (String) method.invoke(null);
    }
}
