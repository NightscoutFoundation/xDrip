package com.eveningoutpost.dexdrip.utils;

import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getBestCollectorHardwareName;
import static com.eveningoutpost.dexdrip.utils.NewRelicCrashReporting.StateMonitor.checkReportingInterval;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.xdrip;
import com.newrelic.agent.android.AgentConfiguration;
import com.newrelic.agent.android.AndroidAgentImpl;
import com.newrelic.agent.android.FeatureFlag;
import com.newrelic.agent.android.background.ApplicationStateMonitor;
import com.newrelic.agent.android.harvest.Harvest;
import com.newrelic.agent.android.logging.AgentLog;
import com.newrelic.agent.android.logging.AgentLogManager;
import com.newrelic.agent.android.logging.NullAgentLog;

import lombok.val;

public class NewRelicCrashReporting {

    private static final int REPORTING_INTERVAL = 14400;
    private static final String APPLICATION_T = "eu01xxc813" + "91fe7bb8416" + "5cc15a1b644" + "8f76378aa7" + "-NRMA";
    private static final String TAG = NewRelicCrashReporting.class.getSimpleName();
    private static final String CONFIGURATION = "agentConfiguration";
    private static final String STARTED = "started";
    private static volatile boolean started = false;

    public synchronized static void start() {
        if (started) {
            android.util.Log.e(TAG, "Already started!");
            return;
        }
        started = true;
        if (JoH.pratelimit("crash-reporting-start", 240) ||
                JoH.pratelimit("crash-reporting-start2", 240)) {
            ApplicationStateMonitor.setInstance(new StateMonitor());
            setFeatures();
            com.newrelic.agent.android.NewRelic.withApplicationToken(APPLICATION_T)
                    .withInteractionTracing(false)
                    .withDefaultInteractions(false)
                    .withLoggingEnabled(false)
                    .withCrashReportingEnabled(true)
                    .withHttpResponseBodyCaptureEnabled(false)
                    .withAnalyticsEvents(false)
                    .withApplicationBuild("" + BuildConfig.buildVersion);
            try {
                if (!com.newrelic.agent.android.NewRelic.isStarted()) {
                    AgentLogManager.setAgentLog(new NullAgentLog());
                    val remoteConfiguration
                            = com.newrelic.agent.android.NewRelic.class.getDeclaredField(CONFIGURATION);
                    remoteConfiguration.setAccessible(true);
                    val agentConfiguration = (AgentConfiguration) remoteConfiguration.get(null);
                    AndroidAgentImpl.init(xdrip.getAppContext(), agentConfiguration);
                    val remoteStarted
                            = com.newrelic.agent.android.NewRelic.class.getDeclaredField(STARTED);
                    remoteStarted.setAccessible(true);
                    remoteStarted.set(null, true);
                    if (!com.newrelic.agent.android.NewRelic.isStarted()) {
                        if (JoH.pratelimit("crash-reporting-start-failed", 3600)) {
                            UserError.Log.wtf(TAG, "Failed to start");
                        }
                    }
                }
            } catch (Throwable e) {
                if (JoH.pratelimit("crash-reporting-start-exception", 3600)) {
                    UserError.Log.wtf(TAG, "Unable to start crash reporter: " + e);
                }
            }
            setFeatures();
            checkReportingInterval();
            Inevitable.task("Register-start", 5000, new Runnable() {
                @Override
                public void run() {
                    checkReportingInterval();
                    registerStart();
                }
            });

        } else {
            if (JoH.pratelimit("crash-reporting-start-failure", 3600)) {
                UserError.Log.wtf(TAG, "Unable to start crash reporter as app is restarting too frequently - if you are a developer then you can ignore this message");
            }
        }
        Inevitable.task("Commit-start", 100, new Runnable() {
            @Override
            public void run() {
                try {
                    PersistentStore.commit();
                } catch (Exception e) {
                    //
                }
            }
        });

    }

    private static void setFeatures() {
        com.newrelic.agent.android.NewRelic.disableFeature(FeatureFlag.HandledExceptions);
        com.newrelic.agent.android.NewRelic.disableFeature(FeatureFlag.AnalyticsEvents);
        com.newrelic.agent.android.NewRelic.disableFeature(FeatureFlag.NetworkErrorRequests);
        com.newrelic.agent.android.NewRelic.disableFeature(FeatureFlag.NetworkRequests);
        com.newrelic.agent.android.NewRelic.disableFeature(FeatureFlag.DefaultInteractions);
        com.newrelic.agent.android.NewRelic.disableFeature(FeatureFlag.GestureInstrumentation);
        com.newrelic.agent.android.NewRelic.disableFeature(FeatureFlag.HttpResponseBodyCapture);
        com.newrelic.agent.android.NewRelic.disableFeature(FeatureFlag.DistributedTracing);
        try {
            com.newrelic.agent.android.NewRelic.setAttribute("Collector", getBestCollectorHardwareName());
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Unable to determine collector type");
        }
    }

    private static void registerStart() {
        ApplicationStateMonitor.getInstance().activityStarted();
    }

    static public class StateMonitor extends com.newrelic.agent.android.background.ApplicationStateMonitor {

        private static final AgentLog log = AgentLogManager.getAgentLog();

        @Override
        public void uiHidden() {
            log.info("UI has become hidden (app backgrounded)");
            checkReportingInterval();
        }

        @Override
        public void activityStopped() {
            log.info("Activity stopped: " + this.foregrounded.get());
        }

        public static void checkReportingInterval() {
            val harvest = Harvest.getInstance();
            if (harvest != null) {
                val config = harvest.getConfiguration();
                if (config != null) {
                    if (config.getData_report_period() != REPORTING_INTERVAL) {
                        config.setData_report_period(REPORTING_INTERVAL);
                        config.setCollect_network_errors(false);
                        harvest.setConfiguration(config);
                        log.info("Updated configuration: " + config.toString());
                        Harvest.stop();
                        Harvest.start();
                    } else {
                        log.info("Reporting interval is fine");
                    }
                } else {
                    UserError.Log.e(TAG, "Cannot get configuration");
                }
            } else {
                UserError.Log.e(TAG, "Cannot get instance");
            }
        }
    }
}
