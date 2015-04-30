package com.eveningoutpost.dexdrip;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

/**
 * Created by stephenblack on 3/21/15.
 */
@ReportsCrashes(
        formUri = "https://yoursolace.cloudant.com/acra-xdrip/_design/acra-storage/_update/report",
        reportType = HttpSender.Type.JSON,
        httpMethod = HttpSender.Method.POST,
        formUriBasicAuthLogin = "nateriverldstiondrephery",
        formUriBasicAuthPassword = "GEK5Nv7NtMkloAkufNvFgast",
        formKey = "", // This is required for backward compatibility but not used
        customReportContent = {
                ReportField.APP_VERSION_CODE,
                ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PACKAGE_NAME,
                ReportField.REPORT_ID,
                ReportField.BUILD,
                ReportField.STACK_TRACE
        },
        mode = ReportingInteractionMode.TOAST,
        logcatArguments = {"-t", "500", "-v", "time"},
        resToastText = R.string.toast_crash
)

public class xdrip extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        // The following line triggers the initialization of ACRA
        //ACRA.init(this);
    }
}
