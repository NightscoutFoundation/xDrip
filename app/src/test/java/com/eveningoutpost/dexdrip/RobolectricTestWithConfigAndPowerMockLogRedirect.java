package com.eveningoutpost.dexdrip;

import android.util.Log;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Abstract config and setup for tests.
 *
 * Starts ActiveAndroid and initiates xdrip with appContext.
 *
 * @author jamorham on 01/10/2017 - added powermock on 2018.07
 * @author Asbjørn Aarrestad, asbjorn@aarrestad.com - 2018.03
 */

@RunWith(RobolectricTestRunner.class)
@Config(sdk = BuildConfig.targetSDK,
        packageName = "com.eveningoutpost.dexdrip",
        application = TestingApplication.class)

// In order to be in the same universe as activeandroid, any classes using it must be ignored. This means power mock instrumentation
// will not run in those ignored classes. So to intercept static methods the class arrangement and test inheritance classes may
// need to be customized for each test using powermock. This is a right pain and leads to all sorts of confusing issues that normally
// should result in simple active android exceptions, but you better watch out for edge cases!

// We also intercept UserError so we can redirect logs from there but then mock the instantiation to avoid it trying to use
// active android.

@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "com.activeandroid.*", "com.eveningoutpost.dexdrip.models.*"})
@PrepareForTest({android.util.Log.class, com.eveningoutpost.dexdrip.models.UserError.class})

//@PowerMockRunnerDelegate(RobolectricTestRunner.class)
// TODO can we make this neater using delegate or avoiding warnings about org/powermock/default.properties is found in 2 places ?

public abstract class RobolectricTestWithConfigAndPowerMockLogRedirect {

    @Before
    public void setUp() {
        xdrip.checkAppContext(RuntimeEnvironment.application);
        setupLogging();
    }

    @Rule
    public PowerMockRule rule = new PowerMockRule();  // fix class preparation

    public void setupLogging() {

        PowerMockito.mockStatic(android.util.Log.class);

        when(Log.v(anyString(), anyString())).thenAnswer((Answer<Void>) invocation -> {
            logit("Verbose", invocation);
            return null;
        });

        when(Log.d(anyString(), anyString())).thenAnswer((Answer<Void>) invocation -> {
            logit("Debug", invocation);
            return null;
        });

        when(Log.e(anyString(), anyString())).thenAnswer((Answer<Void>) invocation -> {
            logit("Error", invocation);
            return null;
        });

        when(Log.w(anyString(), anyString())).thenAnswer((Answer<Void>) invocation -> {
            logit("Warn", invocation);
            return null;
        });

        when(Log.i(anyString(), anyString())).thenAnswer((Answer<Void>) invocation -> {
            logit("Info", invocation);
            return null;
        });

        when(Log.wtf(anyString(), anyString())).thenAnswer((Answer<Void>) invocation -> {
            logit("WTF", invocation);
            return null;
        });

        try {
            PowerMockito.whenNew(com.eveningoutpost.dexdrip.models.UserError.class).withAnyArguments().thenReturn(null);
        } catch (Exception e) {
            System.out.println("Exception mocking usererror: " + e);
        }

        //android.util.Log.d("TEST", "TESTING");
    }

    private static void logit(final String type, final InvocationOnMock invocation) {
        final Object[] args = invocation.getArguments();
        if (args.length > 1) {
            System.out.println(type + " Log: " + args[0] + " :: " + args[1]);
        }

    }
}


