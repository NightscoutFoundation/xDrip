package com.eveningoutpost.dexdrip;

import android.util.Log;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

/**
 * Abstract config and setup for tests.
 * <p>
 * Starts ActiveAndroid and initiates xdrip with appContext.
 *
 * @author jamorham on 01/10/2017
 * @author Asbjørn Aarrestad, asbjorn@aarrestad.com - 2018.03
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = BuildConfig.targetSDK,
        packageName = "com.eveningoutpost.dexdrip",
        application = TestingApplication.class
)
public abstract class RobolectricTestWithConfig {

    @Before
    public void setUp() {
        // The next line can be used to output all logs from test-run to System.out
        // ShadowLog.stream = System.out;

        xdrip.checkAppContext(RuntimeEnvironment.application);
    }

    /** Print all log messages for given tag */
    protected void printLogs(String tag) {
        System.out.println("\n\n============================== Start of '" + tag + "' logs ==============================");
        ShadowLog.getLogsForTag(tag)
                .forEach(log -> System.out.println(logTypeToString(log.type) + ": " + log.msg));
        System.out.println("==============================  End of '" + tag + "' logs  ==============================\n\n");
    }

    private static String logTypeToString(int type) {
        switch (type) {
            case Log.ASSERT:
                return "Assert";
            case Log.DEBUG:
                return "Debug";
            case Log.ERROR:
                return "Error";
            case Log.WARN:
                return "Warn";
            case Log.INFO:
                return "Info";
            case Log.VERBOSE:
                return "Verbose";
            default:
                return "?";
        }
    }
}
