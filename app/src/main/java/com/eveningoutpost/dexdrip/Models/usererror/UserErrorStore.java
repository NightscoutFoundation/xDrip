package com.eveningoutpost.dexdrip.Models.usererror;


import android.util.Log;

import com.activeandroid.Cache;
import com.activeandroid.query.Delete;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.activeandroid.query.Sqlable;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.utils.validation.Ensure;
import com.google.common.annotations.VisibleForTesting;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import static com.eveningoutpost.dexdrip.Models.usererror.UserEvent.ERROR_HIGH;
import static com.eveningoutpost.dexdrip.Models.usererror.UserEvent.ERROR_LOW;
import static com.eveningoutpost.dexdrip.Models.usererror.UserEvent.ERROR_MEDIUM;
import static com.eveningoutpost.dexdrip.Models.usererror.UserEvent.EVENT_HIGH;
import static com.eveningoutpost.dexdrip.Models.usererror.UserEvent.EVENT_LOW;


@Singleton
public class UserErrorStore {

    private final static UserErrorStore store = new UserErrorStore();

    public static UserErrorStore get() {
        return store;
    }
    public void createError(String errorMsg, String errorDescription) {
        this.createError(ERROR_MEDIUM,errorMsg,errorDescription);
    }
    public void createError(UserEvent e, String errorMsg, String errorDescription) {
        Ensure.notNullAny(e,errorMsg,errorDescription);
        if (e.isEvent()) {
            throw new IllegalArgumentException("An event is not an error");
        }
        new UserError(e,errorMsg,errorDescription);
    }
    public void createEvent(UserEvent e, String eventMsg, String eventDescription) {
        Ensure.notNullAny(e,eventMsg,eventDescription);
        if (e.isError()) {
            throw new IllegalArgumentException("An error is not an event");
        }
        new UserError(e,eventMsg,eventDescription);
    }

    public void createHigh(String shortError, String message) {
        new UserError(ERROR_HIGH, shortError, message);
    }
    public void createMedium(String shortError, String message) {
        new UserError(ERROR_LOW, shortError, message);
    }
    public void createLow(String shortError, String message) {
        new UserError(ERROR_LOW, shortError, message);
    }

    public void createMinor(String shortEvent, String description){
        createEvent(EVENT_LOW, shortEvent,description);
    }

    public void createMajor(String shortEvent, String description){
        createEvent(EVENT_HIGH, shortEvent,description);
    }


    @VisibleForTesting
    protected void cleanup(long timestamp) {
        List<UserError> userErrors = new Select()
                .from(UserError.class)
                .where("timestamp < ?", timestamp)
                .orderBy("timestamp desc")
                .execute();
        if (userErrors != null) {
            Log.d(createCallerTag(),"cleanup UserError size=" + userErrors.size());
        }

        new Cleanup().execute(userErrors);
    }

    private String createCallerTag() {
        StackTraceElement e =  new Exception().getStackTrace()[2];
        return e.getClassName() + ":" + e.getMethodName() + ": line " + e.getLineNumber();
    }




    public List<UserError> doByTimeWhere(final Sqlable query, final long timestamp, final String where) {
        return new From(UserError.class, query).where("timestamp < ?", timestamp)
                .and(where)
                .orderBy("timestamp desc")
                .execute();
    }

    public void cleanup() {
        //
        final long timestamp = JoH.tsl();
        final long day = Constants.DAY_IN_MS;
        doByTimeWhere(new Delete(),timestamp-day, "severity < 3");
        doByTimeWhere(new Delete(),timestamp-(day*3), "severity = 3");
        doByTimeWhere(new Delete(),timestamp-(day*7), "severity > 3");

        Cache.clear();
    }


    public List< UserError> all(@Nullable Sqlable query) {
        if (query == null) {
            query = new Select();
        }
        return new From(UserError.class,query)
                .orderBy("timestamp desc")
                .execute();
    }

    public List< UserError> deletable() {
        final long timestamp = JoH.tsl();
        final long day = Constants.DAY_IN_MS;
        Comparator<? super  UserError> comparator = new Comparator< UserError>() {
            @Override
            public int compare( UserError o1,  UserError o2) {
                return (int)(o1.getTimestamp()-o2.getTimestamp());
            }
        };
        List< UserError> userErrors = doByTimeWhere(new Select(),timestamp-day, "severity < 3");
        userErrors.addAll(doByTimeWhere(new Select(),timestamp-(day*3), "severity = 3"));
        userErrors.addAll(doByTimeWhere(new Select(),timestamp-(day*7), "severity > 3"));

        return userErrors;
    }

    private String createSeverityFilter(Integer[] levels) {
        Ensure.notNullAny((Object)levels);

        StringBuilder levelsString = new StringBuilder();

        for (Integer level : levels) {
            if (levelsString.length()>0) {
                levelsString.append(",");
            }
            levelsString.append(level);
        }
        return levelsString.append(")")
                .insert(0, "severity in (")
                .append(")")
                .toString();
    }
    public List<UserError> bySeverity(Integer[] levels) {

        String severityFilter = createSeverityFilter(levels);
        Log.d("UserError", severityFilter);
        return new Select()
                .from(UserError.class)
                .where(severityFilter)
                .orderBy("timestamp desc")
                .limit(10000)//too many data can kill akp
                .execute();
    }

    public List< UserError> bySeverityNewerThanID(long id, Integer[] levels, int limit) {
        String severityFilter = createSeverityFilter(levels);
        Log.d("UserError", severityFilter);
        return new Select()
                .from(UserError.class)
                .where("_ID > ?", id)
                .where(severityFilter)
                .orderBy("timestamp desc")
                .limit(limit)
                .execute();
    }

    public static List<UserError> newerThanID(long id, int limit) {
        return new Select()
                .from(UserError.class)
                .where("_ID > ?", id)
                .orderBy("timestamp desc")
                .limit(limit)
                .execute();
    }

    public static List< UserError> olderThanID(long id, int limit) {
        return new Select()
                .from(UserError.class)
                .where("_ID < ?", id)
                .orderBy("timestamp desc")
                .limit(limit)
                .execute();
    }

    public  List< UserError> bySeverityOlderThanID(long id, Integer[] levels, int limit) {
        String severityFilter = createSeverityFilter(levels);
        Log.d("UserError", severityFilter);
        return new Select()
                .from(UserError.class)
                .where("_ID < ?", id)
                .where(severityFilter)
                .orderBy("timestamp desc")
                .limit(limit)
                .execute();
    }

    public UserError setMessage(UserError userError, String msg){
        userError.setMessage(msg);
        userError.save();
        return (UserError)userError;
    }
    public UserError setShortError(UserError userError, String error){
        userError.setShortError(error);
        userError.save();
        return userError;
    }

    public void save(UserError userError) {
        userError.save();
    }

    public  UserError getForTimestamp(UserError error) {
        try {
            return new Select()
                    .from(UserError.class)
                    .where("timestamp = ?", error.getTimestamp())
                    .where("shortError = ?", error.getShortError())
                    .where("message = ?", error.getMessage())
                    .executeSingle();
        } catch (Exception e) {
            Log.e(createCallerTag(),"getForTimestamp() Got exception on Select : "+e.toString());
            return null;
        }
    }

}
