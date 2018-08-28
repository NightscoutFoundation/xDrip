package com.eveningoutpost.dexdrip.Models;

import android.os.Bundle;
import android.provider.BaseColumns;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.GcmListenerSvc;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.JamListenerSvc;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.desertsync.DesertComms;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.webservices.XdripWebService;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import lombok.Builder;
import lombok.NoArgsConstructor;

import static com.eveningoutpost.dexdrip.GoogleDriveInterface.getDriveIdentityString;

// created by jamorham 18/08/2018

// not to be confused with dessert sync, yum!


@NoArgsConstructor
@Table(name = "DesertSync", id = BaseColumns._ID)
public class DesertSync extends PlusModel {

    private static boolean patched = false;
    private static final String TAG = DesertSync.class.getSimpleName();
    public static final String NO_DATA_MARKER = "NO DATA";
    private static final String PREF_SENDER_UUID = "DesertSync-sender-uuid";
    private static final int MAX_CATCHUP = 20;
    private static final ReentrantLock sequence_lock = new ReentrantLock();
    private static final boolean d = false;
    private static volatile int duplicateIndicator = 0;
    private static volatile int catchupCounter = 0;
    private static String static_sender = null;
    private static RollCall myRollCall = null;
    private static JamListenerSvc service;

    private static volatile long highestPullTimeStamp = -1;

    private static final String[] schema = {
            "CREATE TABLE DesertSync (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
            "ALTER TABLE DesertSync ADD COLUMN timestamp INTEGER;",
            "ALTER TABLE DesertSync ADD COLUMN topic TEXT;",
            "ALTER TABLE DesertSync ADD COLUMN sender TEXT;",
            "ALTER TABLE DesertSync ADD COLUMN payload TEXT;",
            "ALTER TABLE DesertSync ADD COLUMN processed TEXT;",
            "CREATE UNIQUE INDEX index_DesertSync_timestamp on DesertSync(timestamp);",
            "CREATE INDEX index_DesertSync_payload on DesertSync(payload);",
            "CREATE INDEX index_DesertSync_processed on DesertSync(processed);",
            "CREATE INDEX index_DesertSync_topic on DesertSync(topic);"};

    private static final int MAX_ITEMS = 50;

    public static final String PREF_WEBSERVICE_SECRET = "xdrip_webservice_secret";

    @Expose
    @Column(name = "timestamp", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public long timestamp;

    @Expose
    @Column(name = "topic")
    public String topic;

    @Expose
    @Column(name = "sender")
    public String sender;

    @Expose
    @Column(name = "payload")
    public String payload;

    @Column(name = "processed")
    private String processed;


    @Builder
    private DesertSync(final long timestamp, final String topic, final String sender, final String payload, final boolean processedFlag) {
        this.timestamp = timestamp;
        this.topic = topic;
        this.sender = sender;
        if (processedFlag) {
            this.processed = payload;
        } else {
            this.payload = payload;
        }
    }

    public static List<DesertSync> since(final long position, final String topic) {
        if (topic == null) {
            return new Select()
                    .from(DesertSync.class)
                    .where("timestamp > ?", position)
                    .orderBy("timestamp asc")
                    .limit(MAX_ITEMS)
                    .execute();
        } else {
            return new Select()
                    .from(DesertSync.class)
                    .where("topic = ?", topic)
                    .where("timestamp > ?", position)
                    .orderBy("timestamp asc")
                    .limit(MAX_ITEMS)
                    .execute();
        }
    }

    private boolean alreadyInDatabase(final boolean processedFlag) {
        return new Select()
                .from(DesertSync.class)
                .where("topic = ?", topic)
                .where("processed = ?", processedFlag ? processed : processData())
                .executeSingle() != null;
    }

    private static DesertSync last() {
        return new Select()
                .from(DesertSync.class)
                .where("topic = ?", getTopic())
                .orderBy("timestamp desc")
                .executeSingle();
    }


    public String toS() {
        return JoH.defaultGsonInstance().toJson(this);
    }

    public String getAction() {
        return getPayload(0);
    }

    public String getPayload() {
        return getPayload(1);
    }

    private String processData() {
        if (processed == null) {
            processed = CipherUtils.decryptString(payload);
        }
        return processed;
    }

    private String transmissionPayload() {
        if (payload == null) {
            payload = CipherUtils.compressEncryptString(processed);
        }
        return payload;
    }

    private String getPayload(int section) {
        if (processed == null) return "<null>";
        processData();
        try {
            final String[] ps = processed.split("\\^");
            return ps[section];
        } catch (Exception e) {
            return "<invalid payload>";
        }
    }

    private RemoteMessage getMessage() {
        final HashMap<String, String> map = new HashMap<>();
        map.put("message", "From DesertSync");
        map.put("xfrom", sender);
        map.put("yfrom", getYfrom());
        map.put("datum", getPayload());
        map.put("action", getAction());
        return new RemoteMessage.Builder("internal").setData(map).build();
    }

    // utility methods

    public static String toJson(List<DesertSync> list) {
        return JoH.defaultGsonInstance().toJson(list);
    }

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse("desert_sync_enabled");
    }

    // input / output

    public static void pullAsEnabled() {
        if (Home.get_follower()) {
            if (isEnabled()) {
                // TODO check if no data received? or maybe we don't - should this instead be called from do nothing service??
                DesertComms.pullFromOasis(getTopic(), getHighestPullTimeStamp());
            }
        }
    }

    private synchronized static long getHighestPullTimeStamp() {
        if (highestPullTimeStamp == -1) {
            try {
                highestPullTimeStamp = last().timestamp;
            } catch (NullPointerException e) {
                highestPullTimeStamp = 1;
            }
        }
        return highestPullTimeStamp;
    }

    private static DesertSync createFromBundle(final Bundle data) {
        final String payload = data.getString("payload", data.getString("datum", ""));
        if (payload.length() > 0) {
            return new DesertSync(JoH.tsl(), data.getString("identity", getTopic()), mySender(), data.getString("action") + "^" + payload, true);
        } else {
            UserError.Log.d(TAG, "Invalid bundle");
            return null;
        }
    }

    public static boolean fromGCM(final Bundle data) {
        if (isEnabled()) {
            final DesertSync ds = createFromBundle(data);
            if (ds != null && !ds.alreadyInDatabase(true)) {
                DesertComms.pushToOasis(ds.topic, ds.sender, ds.transmissionPayload());
                ds.save();
                // TODO when we are master we push to recorded clients??
            } else {
                UserError.Log.d(TAG, "Not pushing entry without payload / duplicate");
                return false;
            }
        }
        return true;
    }

    public static boolean fromPush(String topic, String sender, String payload) {
        if (isEnabled()) {

            UserError.Log.d(TAG, String.format("sender: %s, topic: %s, payload: %s", sender, topic, payload));
            if (sender == null || sender.length() != 32 || sender.equals(mySender())) return false;
            if (topic == null || topic.length() != 32) return false;
            if (payload == null || payload.length() == 0) return false;
            // TODO VALIDATE PARAMS

            final DesertSync item = new DesertSync(JoH.tsl(), topic, sender, payload, false);
            processItem(item);
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    private static void processItem(final DesertSync item) {
        if (item != null) {
            if (item.topic != null && item.topic.equals(getTopic())) {
                if (!item.alreadyInDatabase(false)) {
                    UserError.Log.d(TAG, "New item: " + item.payload);
                    item.save();
                    new Thread(() -> onMessageReceived(item.getMessage())).start();
                } else {
                    duplicateIndicator++;
                    UserError.Log.d(TAG, "Duplicate item: " + duplicateIndicator);
                }
            } else {
                UserError.Log.d(TAG, "Invalid topic");
            }
        } else {
            UserError.Log.d(TAG, "processItem NULL");
        }
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    public static void fromPull(final String json) {
        if (!json.startsWith(NO_DATA_MARKER)) {
            try {
                final List<DesertSync> items = JoH.defaultGsonInstance().fromJson(json, new TypeToken<ArrayList<DesertSync>>() {
                }.getType());
                if (items != null) {
                    duplicateIndicator = 0;
                    for (final DesertSync item : items) {
                        if (item.timestamp > highestPullTimeStamp) {
                            highestPullTimeStamp = item.timestamp;
                            Inevitable.task("desert-sync-timestamp", 500, () -> {
                                UserError.Log.d(TAG, "Synced up till: " + JoH.dateTimeText(highestPullTimeStamp));
                            });
                        }
                        processItem(item);
                    }
                    if (items.size() == MAX_ITEMS) {
                        UserError.Log.d(TAG, "Attempting to catch up as all history is duplicates or max size: " + catchupCounter);
                        if (catchupCounter < MAX_CATCHUP) {
                            catchupCounter++;
                            Inevitable.task("Desert catchup", 6000, DesertSync::pullAsEnabled);
                        }
                    } else {
                        catchupCounter = 0;
                    }
                }
            } catch (JsonSyntaxException e) {
                UserError.Log.e(TAG, "fromPull error: " + e + "\n" + json);
            }
        } else {
            UserError.Log.d(TAG, "Web service reported no data matching our query - either we are synced or other mismatch");
        }
    }

    // identity

    private static String getTopic() {
        return getDriveIdentityString();
    }

    public static String mySender() {
        if (static_sender == null) {
            synchronized (DesertSync.class) {
                if (static_sender == null) {
                    String sender = PersistentStore.getString(PREF_SENDER_UUID);
                    UserError.Log.d(TAG, "From store: " + sender);
                    if (sender.length() != 32) {
                        sender = CipherUtils.getRandomHexKey();
                        UserError.Log.d(TAG, "From key: " + sender);
                        PersistentStore.setString(PREF_SENDER_UUID, sender);
                    }
                    static_sender = sender;
                }
            }
        }
        UserError.Log.d(TAG, "Returning sender: " + static_sender);
        return static_sender;
    }

    public static String getMyRollCall() {
        if (myRollCall == null || JoH.msSince(myRollCall.created) > Constants.MINUTE_IN_MS * 15) {
            myRollCall = new RollCall();
        }
        return myRollCall.toS();
    }

    // helpers

    private static JamListenerSvc getInstance() {
        if (service == null) {
            service = new GcmListenerSvc();
            service.setInjectable();
        }
        return service;
    }

    private static void onMessageReceived(final RemoteMessage message) {
        if (sequence_lock.getQueueLength() > 0) {
            UserError.Log.d(TAG, "Sequence lock has: " + sequence_lock.getQueueLength() + " waiting");
        }
        try {
            sequence_lock.tryLock(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //
        } finally {
            getInstance().onMessageReceived(message);
            try {
                sequence_lock.unlock();
            } catch (IllegalMonitorStateException e) {
                //
            }
        }
    }

    private String getYfrom() {
        return xdrip.gs(R.string.gcmtpc) + topic;
    }

    public static void settingsChanged() {
        if (isEnabled()) {
            correctWebServiceSettings();
        }
    }

    private static void correctWebServiceSettings() {
        Pref.setBoolean("xdrip_webservice", true);
        Pref.setBoolean("xdrip_webservice_open", true);
        if (Pref.getString(PREF_WEBSERVICE_SECRET, "").length() == 0) {
            Pref.setString(PREF_WEBSERVICE_SECRET, CipherUtils.getRandomHexKey());
        }
        Inevitable.task("web service changed", 2000, XdripWebService::settingsChanged);
    }

    // maintenance

    // create the table ourselves without worrying about model versioning and downgrading
    public static void updateDB() {
        patched = fixUpTable(schema, patched);
    }

    public static void cleanup() {
        try {
            new Delete()
                    .from(DesertSync.class)
                    .where("timestamp < ?", JoH.tsl() - 86400000L)
                    .execute();
        } catch (Exception e) {
            UserError.Log.d(TAG, "Exception cleaning uploader queue: " + e);
        }
    }

    public static void deleteAll() {
        new Delete()
                .from(DesertSync.class)
                .execute();
    }

}
