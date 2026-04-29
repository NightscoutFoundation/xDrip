package com.eveningoutpost.dexdrip.models;

import android.graphics.Color;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.ui.helpers.ColorUtil;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.annotations.Expose;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(name = "UserEvents", id = BaseColumns._ID)
public class UserEvent extends Model {

    private static final String TAG = UserEvent.class.getSimpleName();
    private static boolean patched = false;

    // Built-in event types with all associated metadata in one place
    public enum EventType {
        FOOD(0, R.string.event_food, R.drawable.ic_event_food, "#FF9400"),
        EXERCISE(1, R.string.event_exercise, R.drawable.ic_event_exercise, "#2eb82e"),
        STRESS(2, R.string.event_stress, R.drawable.ic_event_stress, "#FF2929"),
        SLEEP(3, R.string.event_sleep, R.drawable.ic_event_sleep_start, "#6666FF"),
        ILLNESS(4, R.string.event_illness, R.drawable.ic_event_illness, "#FF2929"),
        ALCOHOL(5, R.string.event_alcohol, R.drawable.ic_event_alcohol, "#FFAA00"),
        MEDICATION(6, R.string.event_medication, R.drawable.ic_event_medication, "#00AAAA"),
        CAFFEINE(7, R.string.event_caffeine, R.drawable.ic_event_caffeine, "#8B4513"),
        HYDRATION(8, R.string.event_hydration, R.drawable.ic_event_hydration, "#4499DD"),
        OTHER(9, R.string.event_other, R.drawable.ic_event_other, "#666666");

        public final int code;
        public final int nameRes;
        public final int iconRes;
        private final String colorHex;

        EventType(int code, int nameRes, int iconRes, String colorHex) {
            this.code = code;
            this.nameRes = nameRes;
            this.iconRes = iconRes;
            this.colorHex = colorHex;
        }

        public String getName() {
            return xdrip.getAppContext().getString(nameRes);
        }

        public int getColor() {
            return ColorUtil.blendColor(Color.parseColor(colorHex), Color.TRANSPARENT, 0.2f);
        }

        public int getIcon(String details) {
            if (this == SLEEP && details != null) {
                try {
                    final JSONObject json = new JSONObject(details);
                    final String sleepEvent = json.optString("sleepEvent", "");
                    if (sleepEvent.equals(xdrip.getAppContext().getString(R.string.sleep_event_wakeup))) {
                        return R.drawable.ic_event_sleep_end;
                    }
                } catch (JSONException e) {
                    // fall through
                }
            }
            return iconRes;
        }

        public static EventType fromCode(int code) {
            for (EventType t : values()) {
                if (t.code == code) return t;
            }
            return OTHER;
        }
    }

    // Custom event type support — stored in preferences as comma-separated names
    private static final String CUSTOM_TYPES_PREF = "user_event_custom_types";
    private static final int CUSTOM_TYPE_BASE = 100; // custom types start at code 100
    private static final String CUSTOM_COLOR = "#999999";

    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

    @Expose
    @Column(name = "eventType")
    public int eventType;

    @Expose
    @Column(name = "description")
    public String description;

    @Expose
    @Column(name = "details")
    public String details;

    @Expose
    @Column(name = "uuid", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public String uuid;

    // Convenience getters that resolve type metadata

    public EventType getEventType() {
        return EventType.fromCode(eventType);
    }

    public boolean isCustomType() {
        return eventType >= CUSTOM_TYPE_BASE;
    }

    public String getTypeName() {
        if (isCustomType()) {
            final String[] custom = getCustomTypeNames();
            int idx = eventType - CUSTOM_TYPE_BASE;
            return (idx >= 0 && idx < custom.length) ? custom[idx] : EventType.OTHER.getName();
        }
        return getEventType().getName();
    }

    public int getTypeColor() {
        if (isCustomType()) {
            return ColorUtil.blendColor(Color.parseColor(CUSTOM_COLOR), Color.TRANSPARENT, 0.2f);
        }
        return getEventType().getColor();
    }

    public int getTypeIcon() {
        if (isCustomType()) {
            return R.drawable.ic_event_other;
        }
        return getEventType().getIcon(details);
    }

    public String getSummary() {
        final StringBuilder sb = new StringBuilder(getTypeName());
        final String sub = getDetailsString();
        if (sub != null) {
            sb.append(" (").append(sub).append(")");
        }
        if (description != null && !description.isEmpty()) {
            sb.append(": ").append(description);
        }
        return sb.toString();
    }

    public String getDetailsString() {
        if (details == null || details.isEmpty()) return null;
        try {
            final JSONObject json = new JSONObject(details);
            final StringBuilder sb = new StringBuilder();
            if (json.has("mealSize")) sb.append(json.getString("mealSize"));
            if (json.has("mealType")) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(json.getString("mealType"));
            }
            if (json.has("exerciseType")) sb.append(json.getString("exerciseType"));
            if (json.has("exerciseIntensity")) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(json.getString("exerciseIntensity"));
            }
            if (json.has("sleepQuality")) sb.append(json.getString("sleepQuality"));
            if (json.has("sleepEvent")) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(json.getString("sleepEvent"));
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (JSONException e) {
            return null;
        }
    }

    // Static helper methods for type resolution by code (used by graph/PDF renderers)

    public static String eventTypeName(int type) {
        if (type >= CUSTOM_TYPE_BASE) {
            final String[] custom = getCustomTypeNames();
            int idx = type - CUSTOM_TYPE_BASE;
            return (idx >= 0 && idx < custom.length) ? custom[idx] : EventType.OTHER.getName();
        }
        return EventType.fromCode(type).getName();
    }

    public static int eventTypeColor(int type) {
        if (type >= CUSTOM_TYPE_BASE) {
            return ColorUtil.blendColor(Color.parseColor(CUSTOM_COLOR), Color.TRANSPARENT, 0.2f);
        }
        return EventType.fromCode(type).getColor();
    }

    public static int eventTypeIcon(int type, String details) {
        if (type >= CUSTOM_TYPE_BASE) {
            return R.drawable.ic_event_other;
        }
        return EventType.fromCode(type).getIcon(details);
    }

    // Custom type management

    public static String[] getCustomTypeNames() {
        final String raw = Pref.getString(CUSTOM_TYPES_PREF, "");
        if (raw.isEmpty()) return new String[0];
        return raw.split(",");
    }

    public static void setCustomTypeNames(String[] names) {
        Pref.setString(CUSTOM_TYPES_PREF, String.join(",", names));
    }

    public static List<String> getAllTypeNames() {
        final List<String> names = new ArrayList<>();
        for (EventType t : EventType.values()) {
            names.add(t.getName());
        }
        for (String custom : getCustomTypeNames()) {
            if (!custom.trim().isEmpty()) names.add(custom.trim());
        }
        return names;
    }

    public static int getAllTypeCode(int index) {
        if (index < EventType.values().length) {
            return EventType.values()[index].code;
        }
        return CUSTOM_TYPE_BASE + (index - EventType.values().length);
    }

    // CRUD operations

    public static UserEvent create(int eventType, String description, String details, long timestamp) {
        final UserEvent event = new UserEvent();
        event.eventType = eventType;
        event.description = description;
        event.details = details;
        event.timestamp = timestamp;
        event.uuid = UUID.randomUUID().toString();
        event.save();
        return event;
    }

    public static UserEvent byUUID(String uuid) {
        if (uuid == null) return null;
        fixUpTable();
        try {
            return new Select()
                    .from(UserEvent.class)
                    .where("uuid = ?", uuid)
                    .executeSingle();
        } catch (Exception e) {
            return null;
        }
    }

    public static List<UserEvent> latestForGraph(long start, long end) {
        fixUpTable();
        try {
            return new Select()
                    .from(UserEvent.class)
                    .where("timestamp >= ? AND timestamp <= ?", start, end)
                    .orderBy("timestamp asc")
                    .execute();
        } catch (Exception e) {
            return null;
        }
    }

    public static void update(String uuid, int eventType, String description, String details) {
        final UserEvent event = byUUID(uuid);
        if (event == null) return;
        event.eventType = eventType;
        event.description = description;
        event.details = details;
        event.save();
    }

    public static void deleteByUUID(String uuid) {
        final UserEvent event = byUUID(uuid);
        if (event != null) {
            event.delete();
        }
    }

    // Database migration

    public static void updateDB() {
        fixUpTable();
    }

    private static void fixUpTable() {
        if (patched) return;
        String[] patchup = {
                "CREATE TABLE IF NOT EXISTS UserEvents (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE UserEvents ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE UserEvents ADD COLUMN eventType INTEGER;",
                "ALTER TABLE UserEvents ADD COLUMN description TEXT;",
                "ALTER TABLE UserEvents ADD COLUMN details TEXT;",
                "ALTER TABLE UserEvents ADD COLUMN uuid TEXT;",
                "CREATE INDEX IF NOT EXISTS index_UserEvents_timestamp on UserEvents(timestamp);",
                "CREATE UNIQUE INDEX IF NOT EXISTS index_UserEvents_uuid on UserEvents(uuid);"};

        for (String patch : patchup) {
            try {
                SQLiteUtils.execSql(patch);
            } catch (Exception e) {
                // expected for already-applied patches
            }
        }
        patched = true;
    }
}
