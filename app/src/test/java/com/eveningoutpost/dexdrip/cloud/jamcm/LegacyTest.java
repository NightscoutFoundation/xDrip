package com.eveningoutpost.dexdrip.cloud.jamcm;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import java.lang.reflect.Method;

/**
 * @author Asbjørn Aarrestad
 */
public class LegacyTest extends RobolectricTestWithConfig {

    @Test
    public void getRequestWithParams_doesNotThrowWhenParamIsNull() throws Exception {
        // :: Setup
        // (null params should cause early return)

        // :: Act & Verify - no exception thrown
        invokeGetRequestWithParams("https://example.com", null, "p2", "p3", "p4", "p5");
        invokeGetRequestWithParams("https://example.com", "p1", null, "p3", "p4", "p5");
        invokeGetRequestWithParams("https://example.com", "p1", "p2", null, "p4", "p5");
        invokeGetRequestWithParams("https://example.com", "p1", "p2", "p3", null, "p5");
        invokeGetRequestWithParams("https://example.com", "p1", "p2", "p3", "p4", null);
    }

    @Test
    public void getRequestWithParams_doesNotThrowWhenAllParamsNull() throws Exception {
        // :: Setup
        // (all null params)

        // :: Act & Verify - no exception thrown
        invokeGetRequestWithParams("https://example.com", null, null, null, null, null);
    }

    private static void invokeGetRequestWithParams(String url, String p1, String p2, String p3, String p4, String p5) throws Exception {
        Method method = Legacy.class.getDeclaredMethod("getRequestWithParams",
                String.class, String.class, String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        method.invoke(null, url, p1, p2, p3, p4, p5);
    }
}
