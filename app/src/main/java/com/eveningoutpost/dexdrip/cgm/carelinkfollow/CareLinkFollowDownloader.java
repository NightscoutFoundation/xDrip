package com.eveningoutpost.dexdrip.cgm.carelinkfollow;

import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.client.CareLinkClient;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.client.CountryUtils;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.ConnectDataResult;

import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;

/**
 * Medtronic CareLink Connect Downloader
 *   - download data from CareLink
 *   - execute data conversion and update xDrip data
 */
public class CareLinkFollowDownloader {

    private static final String TAG = "ConnectFollowDL";
    private static final boolean D = false;

    private String carelinkUsername;
    private String carelinkPassword;
    private String carelinkCountry;

    private CareLinkClient careLinkClient;

    private boolean loginDataLooksOkay;

    private static PowerManager.WakeLock wl;

    private String status;

    private long loginBlockedTill = 0;
    private long loginBackoff = Constants.MINUTE_IN_MS;

    public String getStatus(){
        return status;
    }

    CareLinkFollowDownloader(String carelinkUsername, String carelinkPassword, String carelinkCountry) {
        this.carelinkUsername = carelinkUsername;
        this.carelinkPassword = carelinkPassword;
        this.carelinkCountry = carelinkCountry;
        loginDataLooksOkay = !emptyString(carelinkUsername) && !emptyString(carelinkPassword) && carelinkCountry != null && !emptyString(carelinkCountry);
    }

    public static void resetInstance() {
        //retrofit = null;
        //service = null;
        UserError.Log.d(TAG, "Instance reset");
        CollectionServiceStarter.restartCollectionServiceBackground();
    }

    public boolean doEverything( ) {
        msg("Start download");

        if (D) UserError.Log.e(TAG, "doEverything called");
        if (loginDataLooksOkay) {
            if (JoH.tsl() > loginBlockedTill) {
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
                UserError.Log.e(TAG, "Not trying to login due to backoff timer for login failures until: " + JoH.dateTimeText(loginBlockedTill));
                return false;
            }
        } else {
            final String invalid = "Connect login data isn't valid!";
            msg(invalid);
            UserError.Log.e(TAG, invalid);
            if(emptyString(carelinkUsername)){
                UserError.Log.e(TAG, "CareLink Username empty!");
            }
            if(emptyString(carelinkPassword)){
                UserError.Log.e(TAG, "CareLink Password empty!");
            }
            if(carelinkCountry == null){
                UserError.Log.e(TAG, "CareLink Country empty!");
            }else if(!CountryUtils.isSupportedCountry(carelinkCountry)){
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
        Inevitable.task("proc-connect-follow", 100, this::processConnectData);
        releaseWakeLock(); // handover to inevitable
    }

    // don't call this directly unless you are also handling the wakelock release
    private void processConnectData() {

        ConnectDataResult connectDataResult = null;
        CareLinkClient careLinkClient = null;

        loginBackoff = 0;

        //Get client
        careLinkClient = getCareLinkClient();
        //Get ConnectData from CareLink client
        if (careLinkClient != null) {
            connectDataResult = getCareLinkClient().getLast24Hours();

            //Got CareLink data
            if (connectDataResult.success) {
                UserError.Log.d(TAG, "Success get data! Response code: " + connectDataResult.responseCode);
                try {
                    if (connectDataResult.connectData == null) {
                        UserError.Log.d(TAG, "Connect data is null!");
                    } else if (connectDataResult.connectData.sgs == null) {
                        UserError.Log.d(TAG, "SGs is null!");
                    }
                    if (D) UserError.Log.d(TAG, "Start process data");
                    //Process CareLink data (conversion and update xDrip data)
                    CareLinkDataProcessor.processData(connectDataResult.connectData, true);
                    if (D) UserError.Log.d(TAG, "ProcessData finished!");
                    //Update Service status
                    CareLinkFollowService.updateBgReceiveDelay();
                    if (D) UserError.Log.d(TAG, "UpdateBgReceiveDelay finished!");
                    msg(null);
                    if (D) UserError.Log.d(TAG, "SetMessage finished!");
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Got exception for data update" + e);
                }
            //Error during data download
            } else {
                if (!getCareLinkClient().getLastLoginSuccess()) {
                    UserError.Log.e(TAG, "CareLink login error!");
                    loginBackoff += Constants.MINUTE_IN_MS;
                    loginBlockedTill = JoH.tsl() + loginBackoff;
                } else if (!getCareLinkClient().getLastDataSuccess()) {
                    UserError.Log.e(TAG, "CareLink download error! Response code: " + connectDataResult.responseCode);
                    UserError.Log.e(TAG, "Error message: " + getCareLinkClient().getLastErrorMessage());
                    UserError.Log.e(TAG, "Stack trace: " + getCareLinkClient().getLastStackTraceString());
                }
            }

            //Update status message
            if (connectDataResult.success) {
                msg(null);
            }
        }

    }


    private CareLinkClient getCareLinkClient() {
        if (careLinkClient == null) {
            try {
                UserError.Log.d(TAG, "Creating ConnectClient");
                careLinkClient = new CareLinkClient(carelinkUsername, carelinkPassword, carelinkCountry);
            } catch (NullPointerException e) {
                UserError.Log.e(TAG, "Error creating ConnectClient");
            }
        }
        return careLinkClient;
    }


    private static synchronized void extendWakeLock(final long ms) {
        if (wl == null) {
            if (D) UserError.Log.d(TAG,"Creating wakelock");
            wl = JoH.getWakeLock("CLFollow-download", (int) ms);
        } else {
            JoH.releaseWakeLock(wl); // lets not get too messy
            wl.acquire(ms);
            if (D) UserError.Log.d(TAG,"Extending wakelock");
        }
    }

    protected static synchronized void releaseWakeLock() {
        if (D) UserError.Log.d(TAG, "Releasing wakelock");
        JoH.releaseWakeLock(wl);
    }

}