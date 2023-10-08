package com.eveningoutpost.dexdrip.tidepool;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.google.gson.annotations.Expose;

import java.util.TimeZone;

import static com.eveningoutpost.dexdrip.models.JoH.getTimeZoneOffsetMs;

public class MOpenDatasetRequest extends BaseMessage {

    static final String UPLOAD_TYPE = "continuous";

    @Expose
    public String deviceId;
    @Expose
    public String time = DateUtil.toFormatAsUTC(JoH.tsl());
    @Expose
    public int timezoneOffset = (int) (getTimeZoneOffsetMs() / Constants.MINUTE_IN_MS);
    @Expose
    public String type = "upload";
    //public String byUser;
    @Expose
    public ClientInfo client = new ClientInfo();
    @Expose
    public String computerTime = DateUtil.toFormatNoZone(JoH.tsl());
    @Expose
    public String dataSetType = UPLOAD_TYPE;  // omit for "normal"
    @Expose
    public String[] deviceManufacturers = {DexCollectionType.getBestCollectorHardwareName()};
    @Expose
    public String deviceModel = DexCollectionType.getBestCollectorHardwareName();
    @Expose
    public String[] deviceTags = {"bgm", "cgm", "insulin-pump"};
    @Expose
    public Deduplicator deduplicator = new Deduplicator();
    @Expose
    public String timeProcessing = "none";
    @Expose
    public String timezone = TimeZone.getDefault().getID();
    @Expose
    public String version = BuildConfig.VERSION_NAME;

    class ClientInfo {
        @Expose
        final String name = BuildConfig.APPLICATION_ID;
        @Expose
        final String version = "0.1.0"; // TODO: const it
    }

    class Deduplicator {
        @Expose
        final String name = "org.tidepool.deduplicator.dataset.delete.origin";
    }

    static boolean isNormal() {
        return UPLOAD_TYPE.equals("normal");
    }
}
