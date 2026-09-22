package com.eveningoutpost.dexdrip.messages;

import com.squareup.wire.FieldEncoding;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import okio.ByteString;

import static com.google.common.truth.Truth.assertThat;

/**
 * Round trips for the protobuf messages that carry readings from the phone to the watch.
 * <p>
 * These four classes are Wire-generated and encode through okio, so they are the one place an okio
 * change could silently alter a medical data path. Plain JUnit: the encoding is pure Java and needs
 * no Android runtime.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class WireMessageRoundTripTest {

    // ===== BgReadingMessage ======================================================================

    /** A fully populated BgReadingMessage survives encode and decode field for field. */
    @Test
    public void bgReadingMessage_roundTripsEveryField() throws Exception {
        // :: Setup
        BgReadingMessage original = new BgReadingMessage.Builder()
                .timestamp(1_754_000_000_000L)
                .time_since_sensor_started(86_400_000d)
                .raw_data(123.25d)
                .filtered_data(122.75d)
                .age_adjusted_raw_value(121.5d)
                .calibration_flag(true)
                .calculated_value(5.4d)
                .filtered_calculated_value(5.3d)
                .calculated_value_slope(0.0021d)
                .a(1.5d).b(2.5d).c(3.5d)
                .ra(4.5d).rb(5.5d).rc(6.5d)
                .uuid("11111111-2222-3333-4444-555555555555")
                .calibration_uuid("66666666-7777-8888-9999-000000000000")
                .sensor_uuid("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
                .ignoreforstats(false)
                .raw_calculated(120.125d)
                .hide_slope(true)
                .noise("2")
                .build();

        // :: Act
        byte[] encoded = BgReadingMessage.ADAPTER.encode(original);
        BgReadingMessage decoded = BgReadingMessage.ADAPTER.decode(encoded);

        // :: Verify
        assertThat(encoded).isNotEmpty();
        assertThat(decoded).isEqualTo(original);
    }

    /** Fields left unset stay null through a round trip rather than becoming proto defaults. */
    @Test
    public void bgReadingMessage_roundTripsSparseMessage() throws Exception {
        // :: Setup
        BgReadingMessage original = new BgReadingMessage.Builder()
                .timestamp(1_754_000_000_000L)
                .calculated_value(7.1d)
                .build();

        // :: Act
        BgReadingMessage decoded =
                BgReadingMessage.ADAPTER.decode(BgReadingMessage.ADAPTER.encode(original));

        // :: Verify
        assertThat(decoded).isEqualTo(original);
        assertThat(decoded.uuid).isNull();
        assertThat(decoded.noise).isNull();
    }

    // ===== BgReadingMultiMessage =================================================================

    /** A multi-message preserves both the contents and the order of its readings. */
    @Test
    public void bgReadingMultiMessage_roundTripsInOrder() throws Exception {
        // :: Setup
        List<BgReadingMessage> readings = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            readings.add(new BgReadingMessage.Builder()
                    .timestamp(1_754_000_000_000L + i * 300_000L)
                    .calculated_value(5.0d + i)
                    .uuid("uuid-" + i)
                    .build());
        }
        BgReadingMultiMessage original = new BgReadingMultiMessage.Builder()
                .bgreading_message(readings)
                .build();

        // :: Act
        BgReadingMultiMessage decoded = BgReadingMultiMessage.ADAPTER
                .decode(BgReadingMultiMessage.ADAPTER.encode(original));

        // :: Verify
        assertThat(decoded).isEqualTo(original);
        assertThat(decoded.bgreading_message).hasSize(3);
        assertThat(decoded.bgreading_message.get(0).uuid).isEqualTo("uuid-0");
        assertThat(decoded.bgreading_message.get(2).uuid).isEqualTo("uuid-2");
    }

    /** An empty multi-message round trips to an empty list, not to null. */
    @Test
    public void bgReadingMultiMessage_roundTripsEmptyList() throws Exception {
        // :: Setup
        BgReadingMultiMessage original = new BgReadingMultiMessage.Builder()
                .bgreading_message(Collections.<BgReadingMessage>emptyList())
                .build();

        // :: Act
        BgReadingMultiMessage decoded = BgReadingMultiMessage.ADAPTER
                .decode(BgReadingMultiMessage.ADAPTER.encode(original));

        // :: Verify
        assertThat(decoded).isEqualTo(original);
        assertThat(decoded.bgreading_message).isEmpty();
    }

    // ===== BloodTest messages ====================================================================

    /** A fully populated BloodTestMessage survives encode and decode field for field. */
    @Test
    public void bloodTestMessage_roundTripsEveryField() throws Exception {
        // :: Setup
        BloodTestMessage original = new BloodTestMessage.Builder()
                .timestamp(1_754_000_000_000L)
                .mgdl(97.5d)
                .created_timestamp(1_754_000_060_000L)
                .state(4L)
                .source("test-source")
                .uuid("bbbbbbbb-cccc-dddd-eeee-ffffffffffff")
                .build();

        // :: Act
        BloodTestMessage decoded =
                BloodTestMessage.ADAPTER.decode(BloodTestMessage.ADAPTER.encode(original));

        // :: Verify
        assertThat(decoded).isEqualTo(original);
    }

    /** A blood-test multi-message preserves both the contents and the order of its entries. */
    @Test
    public void bloodTestMultiMessage_roundTripsInOrder() throws Exception {
        // :: Setup
        BloodTestMultiMessage original = new BloodTestMultiMessage.Builder()
                .bloodtest_message(Arrays.asList(
                        new BloodTestMessage.Builder()
                                .timestamp(1L).mgdl(90d).uuid("first").build(),
                        new BloodTestMessage.Builder()
                                .timestamp(2L).mgdl(110d).uuid("second").build()))
                .build();

        // :: Act
        BloodTestMultiMessage decoded = BloodTestMultiMessage.ADAPTER
                .decode(BloodTestMultiMessage.ADAPTER.encode(original));

        // :: Verify
        assertThat(decoded).isEqualTo(original);
        assertThat(decoded.bloodtest_message).hasSize(2);
        assertThat(decoded.bloodtest_message.get(0).uuid).isEqualTo("first");
        assertThat(decoded.bloodtest_message.get(1).uuid).isEqualTo("second");
    }

    // ===== Unknown fields ========================================================================

    /** Fields an older reader does not know survive decode and re-encode as bytes, unaltered. */
    @Test
    public void bgReadingMessage_preservesUnknownFields() throws Exception {
        // :: Setup — tags 400 and 401 are not in the .proto, so they arrive as unknown fields
        BgReadingMessage original = new BgReadingMessage.Builder()
                .timestamp(1_754_000_000_000L)
                .calculated_value(6.2d)
                .addUnknownField(400, FieldEncoding.VARINT, 42L)
                .addUnknownField(401, FieldEncoding.LENGTH_DELIMITED,
                        ByteString.encodeUtf8("newer-sender"))
                .build();

        // :: Act
        BgReadingMessage decoded =
                BgReadingMessage.ADAPTER.decode(BgReadingMessage.ADAPTER.encode(original));
        BgReadingMessage reEncoded =
                BgReadingMessage.ADAPTER.decode(BgReadingMessage.ADAPTER.encode(decoded));

        // :: Verify — the guard: without it the assertions below hold on an empty ByteString
        assertThat(original.unknownFields()).isNotEqualTo(ByteString.EMPTY);
        assertThat(decoded).isEqualTo(original);
        assertThat(reEncoded).isEqualTo(original);
    }
}
