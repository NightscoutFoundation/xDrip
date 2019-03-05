package com.eveningoutpost.dexdrip.utils.time;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Created by jamorham on 17/02/2018.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SlidingWindowConstraintTest extends RobolectricTestWithConfig {


    @Test
    public void test_SlidingWindowConstraint1() {

        SlidingWindowConstraint sw = null;

        // text exceptions
        try {
            sw = new SlidingWindowConstraint(0, 0, null);
            assertWithMessage("Does not throw exception too small").fail();
        } catch (RuntimeException e) {
            assertWithMessage("Correctly throws exception too small").that(e.getMessage().contains("too small")).isTrue();
        }
        try {
            sw = new SlidingWindowConstraint(100, 3000, null);
            assertWithMessage("Does not throw exception null").fail();
        } catch (RuntimeException e) {
            assertWithMessage("Correctly throws exception null id").that(e.getMessage().contains("null")).isTrue();
        }

        // test some different maximums
        final double[] maxes = {0.5, 1.0, 5.0, 100, 1000};

        final int steps = 5;
        // test non-persistence generic overload
        for (double max : maxes) {
            String id = max + "U ";
            int count = 0;
            final double inc = max / steps;
            sw = new SlidingWindowConstraint(max, 60 * 60 * 1000, "max_bolus_per_hour");
            assertWithMessage("Instance " + max + "U is not null").that(sw).isNotNull();

            for (int i = 0; i < steps; i++) {
                assertWithMessage(id + "Below threshold " + i).that(sw.acceptable(inc)).isTrue();
                sw.add(inc);
                count++;
            }
            assertWithMessage(id + "Above threshold looped").that(sw.acceptable(inc)).isFalse();
            assertWithMessage(id + "Storage size std").that(sw.testSizeOfStorage()).isEqualTo(count);
        }
    }

    // test json general
    @Test
    public void test_SlidingWindowConstraint_json1() {

        final SlidingWindowConstraint sw = new SlidingWindowConstraint(0.5, 60 * 60 * 1000, "max_bolus_per_hour");
        assertWithMessage("Empty set").that(sw.toJson()).isEqualTo("[]");
        sw.add(0.1, 10000000);
        assertWithMessage("Empty expired set").that(sw.toJson()).isEqualTo("[]");
        final long now = System.currentTimeMillis();
        sw.add(0.7, now);
        assertWithMessage("Single item match").that(sw.toJson()).isEqualTo("[{\"timestamp\":" + now + ",\"value\":0.7}]");
        sw.add(0.7, now);
        assertWithMessage("Single item match").that(sw.toJson()).isEqualTo("[{\"timestamp\":" + now + ",\"value\":0.7},{\"timestamp\":" + now + ",\"value\":0.7}]");

    }

    // test json general
    @Test
    public void test_SlidingWindowConstraint_json2() {

        final SlidingWindowConstraint sw = new SlidingWindowConstraint(0.5, 60 * 60 * 1000, "max_bolus_per_hour");
        assertWithMessage("empty start").that(sw.testSizeOfStorage()).isEqualTo(0);
        sw.fromJson("[{\"timestamp\":1518995142838,\"value\":0.8},{\"timestamp\":1518995142837,\"value\":0.7}]");
        assertWithMessage("Inserted 2 rows").that(sw.testSizeOfStorage()).isEqualTo(2);
        assertWithMessage("input output match").that(sw.toJson()).isEqualTo("[{\"timestamp\":1518995142838,\"value\":0.8},{\"timestamp\":1518995142837,\"value\":0.7}]");
    }

    // test large failure
    @Test
    public void test_SlidingWindowConstraint2() {

        SlidingWindowConstraint sw = null;

        // test excessive usage and storage pruning
        sw = new SlidingWindowConstraint(0.5, 60 * 60 * 1000, "max_bolus_per_hour");
        final long now = System.currentTimeMillis();

        int count = 0;
        for (long i = 60 * 60 * 1000 * 3; i > 0; i = i - (1000 * 60)) {
            sw.add(0.1, now - i);
            count++;
        }
        assertWithMessage("Counter failure 1").that(count).isEqualTo(180);
        assertWithMessage("Correct storage size 1").that(sw.testSizeOfStorage()).isLessThan(62);
        assertWithMessage("Correct storage size 2").that(sw.testSizeOfStorage()).isGreaterThan(58);
        assertWithMessage("Large failure").that(sw.acceptable(0.0)).isFalse();

    }

    // test expected patterns
    @Test
    public void test_SlidingWindowConstraint3() {

        SlidingWindowConstraint sw = null;
        // test expired data
        sw = new SlidingWindowConstraint(0.5, 60 * 60 * 1000, "max_bolus_per_hour");
        final long now = System.currentTimeMillis();
        sw.add(0.1, now - (60 * 1000 * 10));
        assertWithMessage("Mix old/new ok 0").that(sw.acceptable(0.0)).isTrue();
        sw.add(0.1, now - (60 * 1000 * 20));
        assertWithMessage("Mix old/new ok 1").that(sw.acceptable(0.0)).isTrue();
        sw.add(0.1, now - (60 * 1000 * 30));
        assertWithMessage("Mix old/new ok 2").that(sw.acceptable(0.0)).isTrue();
        sw.add(0.1, now - (60 * 1000 * 40));
        assertWithMessage("Mix old/new ok 3").that(sw.acceptable(0.0)).isTrue();
        sw.add(0.1, now - (60 * 1000 * 50));
        assertWithMessage("Mix old/new ok 4").that(sw.acceptable(0.0)).isTrue();
        sw.add(0.1, now - (60 * 1000 * 70));
        assertWithMessage("Mix old/new ok 5").that(sw.acceptable(0.0)).isTrue();
        sw.add(0.1, now - (60 * 1000 * 90));
        assertWithMessage("Mix old/new ok 6").that(sw.acceptable(0.0)).isTrue();
        sw.add(0.01, now - (60 * 1000 * 59)); //  potential race condition @ 60 min - tipping point
        assertWithMessage("Mix old/new excessive").that(sw.acceptable(0.0)).isFalse();
        assertWithMessage("Storage is expected size").that(sw.testSizeOfStorage()).isEqualTo(6);
    }


    @Test
    public void test_SlidingWindowConstraint4() {
        SlidingWindowConstraint sw = null;
        // test persisting data
        sw = new SlidingWindowConstraint(0.5, 60 * 60 * 1000, "max_bolus_per_hour_persist", true);
        assertWithMessage("Initial persistent data is empty").that(sw.testSizeOfStorage()).isEqualTo(0);

        final long now = System.currentTimeMillis();
        sw.add(0.1, now - (60 * 1000 * 10));
        assertWithMessage("Initial persistent data is set").that(sw.testSizeOfStorage()).isEqualTo(1);

        // recreate object
        sw = new SlidingWindowConstraint(0.5, 60 * 60 * 1000, "max_bolus_per_hour_persist", true);
        assertWithMessage("Loaded persistent data is set").that(sw.testSizeOfStorage()).isEqualTo(1);

        sw.add(0.1, now - (60 * 1000 * 20));
        assertWithMessage("Mix old/new ok 1").that(sw.acceptable(0.0)).isTrue();
        sw.add(0.1, now - (60 * 1000 * 30));
        assertWithMessage("Mix old/new ok 2").that(sw.acceptable(0.0)).isTrue();
        sw.add(0.1, now - (60 * 1000 * 40));
        assertWithMessage("Mix old/new ok 3").that(sw.acceptable(0.0)).isTrue();
        sw.add(0.1, now - (60 * 1000 * 50));
        assertWithMessage("Mix old/new ok 4").that(sw.acceptable(0.0)).isTrue();
        sw.add(0.1, now - (60 * 1000 * 70));
        assertWithMessage("Mix old/new ok 5").that(sw.acceptable(0.0)).isTrue();
        sw.add(0.1, now - (60 * 1000 * 90));
        assertWithMessage("Mix old/new ok 6").that(sw.acceptable(0.0)).isTrue();
        sw.add(0.01, now - (60 * 1000 * 59)); //  potential race condition @ 60 min - tipping point
        assertWithMessage("Mix old/new excessive").that(sw.acceptable(0.0)).isFalse();
        assertWithMessage("Storage is expected size").that(sw.testSizeOfStorage()).isEqualTo(6);

        // recreate object
        sw = new SlidingWindowConstraint(0.5, 60 * 60 * 1000, "max_bolus_per_hour_persist", true);
        assertWithMessage("Loaded persistent data is set").that(sw.testSizeOfStorage()).isEqualTo(6);
        assertWithMessage("Mix old/new excessive").that(sw.acceptable(0.0)).isFalse();
    }

    @Test
    public void test_SlidingWindowConstraint5() {
        SlidingWindowConstraint sw = null;
        // test persisting data zero size
        sw = new SlidingWindowConstraint(0.0, 60 * 60 * 1000, "max_bolus_per_hour_persist2", true);
        assertWithMessage("Initial persistent data is empty").that(sw.testSizeOfStorage()).isEqualTo(0);
        assertWithMessage("Zero window max fails").that(sw.acceptable(0.00001)).isFalse();
    }

    // TODO check checkAndAddIfAcceptable
}
