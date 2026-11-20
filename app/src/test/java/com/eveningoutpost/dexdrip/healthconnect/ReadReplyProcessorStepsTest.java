package com.eveningoutpost.dexdrip.healthconnect;

import static com.google.common.truth.Truth.assertThat;

import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.records.metadata.Metadata;

import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.StepCounter;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowLog;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Steps records from Health Connect in {@link ReadReplyProcessor}
 */
public class ReadReplyProcessorStepsTest extends RobolectricTestWithConfig {

    // One day as Samsung Health writes it: a single record from 00:00 to 23:59:59
    private static final Instant DAY_START = Instant.parse("2026-11-09T00:00:00Z");
    private static final Instant DAY_END = Instant.parse("2026-11-09T23:59:59Z");
    private static final long DAY_MIDPOINT = DAY_START.toEpochMilli() + (DAY_END.toEpochMilli() - DAY_START.toEpochMilli()) / 2;

    @Before
    public void clearRecords() {
        // The database is kept between the tests of this class.
        new Delete().from(StepCounter.class).execute();
        ShadowLog.clear();
    }

    @Test
    public void updatedDailyRecordReplacesTheStoredCount() {
        process(steps(DAY_START, DAY_END, 65)); // first read in the morning
        process(steps(DAY_START, DAY_END, 12804)); // the same record, updated in the evening

        final List<StepCounter> all = allRecords();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).timestamp).isEqualTo(DAY_MIDPOINT);
        assertThat(all.get(0).metric).isEqualTo(12804);
    }

    @Test
    public void lowerCountForTheSameSpanDoesNotReplaceTheStoredCount() {
        // A second source with the same span must not lower or add to the count
        process(steps(DAY_START, DAY_END, 12804));
        process(steps(DAY_START, DAY_END, 300));

        final List<StepCounter> all = allRecords();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).metric).isEqualTo(12804);
        // ... but the ignored count shows up in the log
        assertThat(stepCounterLog()).contains("ignored lower count 300");
    }

    @Test
    public void sameCountAgainIsNotLogged() {
        // Health Connect hands over unchanged records again; they must not fill the log
        process(steps(DAY_START, DAY_END, 12804));
        process(steps(DAY_START, DAY_END, 12804));

        assertThat(allRecords()).hasSize(1);
        assertThat(stepCounterLog()).doesNotContain("ignored lower count");
    }

    @Test
    public void recordsWithDifferentSpansStaySeparate() {
        final Instant start = Instant.parse("2026-11-09T10:00:00Z");
        process(steps(start, start.plusSeconds(60), 40));
        process(steps(start.plusSeconds(60), start.plusSeconds(120), 50));

        final List<StepCounter> all = allRecords();
        assertThat(all).hasSize(2);
        assertThat(all.get(0).metric).isEqualTo(40);
        assertThat(all.get(1).metric).isEqualTo(50);
    }

    private static StepsRecord steps(final Instant start, final Instant end, final long count) {
        return new StepsRecord(start, null, end, null, count, new Metadata());
    }

    private static void process(final StepsRecord record) {
        final DataReply reply = new DataReply();
        reply.stepsRecords = Collections.singletonList(record);
        ReadReplyProcessor.process(reply);
    }

    private static String stepCounterLog() {
        return ShadowLog.getLogsForTag("StepCounter").stream()
                .map(log -> log.msg)
                .collect(Collectors.joining("\n"));
    }

    private static List<StepCounter> allRecords() {
        return new Select().from(StepCounter.class).orderBy("timestamp asc").execute();
    }
}
