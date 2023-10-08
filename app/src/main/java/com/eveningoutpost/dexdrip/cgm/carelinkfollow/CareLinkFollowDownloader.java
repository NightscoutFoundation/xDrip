package com.eveningoutpost.dexdrip.cgm.carelinkfollow;

import android.os.PowerManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.client.CareLinkClient;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.client.CountryUtils;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.RecentData;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;

/**
 * CareLink Downloader
 * - download data from CareLink
 * - execute data conversion and update xDrip data
 */
public class CareLinkFollowDownloader {

    private static final String TAG = "CareLinkFollowDL";
    private static final boolean D = false;

    private String carelinkUsername;
    private String carelinkPassword;
    private String carelinkCountry;
    private String carelinkPatient;

    private CareLinkClient careLinkClient;

    private boolean loginDataLooksOkay;

    private static PowerManager.WakeLock wl;

    private String status;

    private int lastResponseCode = 0;

    public String getStatus() {
        return status;
    }

    CareLinkFollowDownloader(String carelinkUsername, String carelinkPassword, String carelinkCountry, String carelinkPatient) {
        this.carelinkUsername = carelinkUsername;
        this.carelinkPassword = carelinkPassword;
        this.carelinkCountry = carelinkCountry;
        this.carelinkPatient = carelinkPatient;
        loginDataLooksOkay = !emptyString(carelinkUsername) && !emptyString(carelinkPassword) && carelinkCountry != null && !emptyString(carelinkCountry);
    }

    public static void resetInstance() {
        UserError.Log.d(TAG, "Instance reset");
        CollectionServiceStarter.restartCollectionServiceBackground();
    }

    public boolean doEverything() {
        msg("Start download");

        if (D) UserError.Log.e(TAG, "doEverything called");
        if (loginDataLooksOkay) {
            try {
                if (getCareLinkClient() != null) {
                    extendWakeLock(30_000);
                    backgroundProcessConnectData();
                } else {
                    UserError.Log.d(TAG, "Cannot get data as ConnectClient is null");
                    return false;
                }
                return true;
            } catch (Exception e) {
                UserError.Log.e(TAG, "Got exception in getData() " + e);
                releaseWakeLock();
                return false;
            }
        } else {
            final String invalid = "Invalid CareLink login data!";
            msg(invalid);
            UserError.Log.e(TAG, invalid);
            if (emptyString(carelinkUsername)) {
                UserError.Log.e(TAG, "CareLink Username empty!");
            }
            if (emptyString(carelinkPassword)) {
                UserError.Log.e(TAG, "CareLink Password empty!");
            }
            if (carelinkCountry == null) {
                UserError.Log.e(TAG, "CareLink Country empty!");
            } else if (!CountryUtils.isSupportedCountry(carelinkCountry)) {
                UserError.Log.e(TAG, "CareLink Country not supported!");
            }
            return false;
        }

    }

    private void msg(final String msg) {
        status = msg != null ? JoH.hourMinuteString() + ": " + msg : null;
        if (msg != null) UserError.Log.d(TAG, "Setting message: " + status);
    }

    public void invalidateSession() {
        this.careLinkClient = null;
    }

    private void backgroundProcessConnectData() {
        Inevitable.task("proc-carelink-follow", 100, this::processConnectData);
        releaseWakeLock(); // handover to inevitable
    }

    // don't call this directly unless you are also handling the wakelock release
    private void processConnectData() {

        RecentData recentData = null;
        CareLinkClient carelinkClient = null;


        //Get client
        carelinkClient = getCareLinkClient();
        //Get RecentData from CareLink client
        if (carelinkClient != null) {

            //Try twice in case of 401 error
            for (int i = 0; i < 2; i++) {

                //Get data
                try {
                    if (JoH.emptyString(this.carelinkPatient))
                        recentData = getCareLinkClient().getRecentData();
                    else
                        recentData = getCareLinkClient().getRecentData(this.carelinkPatient);
                    lastResponseCode = carelinkClient.getLastResponseCode();
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Exception in CareLink data download: " + e);
                }

                //Process data
                if (recentData != null) {
                    UserError.Log.d(TAG, "Success get data!");
                    try {
                        UserError.Log.d(TAG, "Start process data");
                        //Process CareLink data (conversion and update xDrip data)
                        CareLinkDataProcessor.processData(recentData, true);
                        UserError.Log.d(TAG, "ProcessData finished!");
                        //Update Service status
                        CareLinkFollowService.updateBgReceiveDelay();
                        msg(null);
                    } catch (Exception e) {
                        UserError.Log.e(TAG, "Exception in data processing: " + e);
                        msg("Data processing error!");
                    }
                    //Data receive error
                } else {
                    //first 401 error => TRY AGAIN, only debug log
                    if (carelinkClient.getLastResponseCode() == 401 && i == 0) {
                        UserError.Log.d(TAG, "Try get data again due to 401 response code." + getCareLinkClient().getLastErrorMessage());
                        //second 401 error => unauthorized error
                    } else if (carelinkClient.getLastResponseCode() == 401) {
                        UserError.Log.e(TAG, "CareLink login error!  Response code: " + carelinkClient.getLastResponseCode());
                        msg("Login error!");
                        //login error
                    } else if (!getCareLinkClient().getLastLoginSuccess()) {
                        UserError.Log.e(TAG, "CareLink login error!  Response code: " + carelinkClient.getLastResponseCode());
                        UserError.Log.e(TAG, "Error message: " + getCareLinkClient().getLastErrorMessage());
                        msg("Login error!");
                        //other error in download
                    } else {
                        UserError.Log.e(TAG, "CareLink download error! Response code: " + carelinkClient.getLastResponseCode());
                        UserError.Log.e(TAG, "Error message: " + getCareLinkClient().getLastErrorMessage());
                        msg("Data request error!");
                    }
                }

                //Next try only for 401 error and first attempt
                if (!(carelinkClient.getLastResponseCode() == 401 && i == 0))
                    break;

            }

        }

    }


    private CareLinkClient getCareLinkClient() {
        if (careLinkClient == null) {
            try {
                UserError.Log.d(TAG, "Creating CareLinkClient");
                careLinkClient = new CareLinkClient(carelinkUsername, carelinkPassword, carelinkCountry);
            } catch (Exception e) {
                UserError.Log.e(TAG, "Error creating CareLinkClient", e);
            }
        }
        return careLinkClient;
    }


    private static synchronized void extendWakeLock(final long ms) {
        if (wl == null) {
            if (D) UserError.Log.d(TAG, "Creating wakelock");
            wl = JoH.getWakeLock("CLFollow-download", (int) ms);
        } else {
            JoH.releaseWakeLock(wl); // lets not get too messy
            wl.acquire(ms);
            if (D) UserError.Log.d(TAG, "Extending wakelock");
        }
    }

    protected static synchronized void releaseWakeLock() {
        if (D) UserError.Log.d(TAG, "Releasing wakelock");
        JoH.releaseWakeLock(wl);
    }

}