package com.eveningoutpost.dexdrip.models;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import java.util.Date;

public class Libre2RawValueTest extends RobolectricTestWithConfig {

    @Test
    public void jsonParsingWorks() {
        Libre2RawValue value = new Libre2RawValue();
        value.serial = "Hello World";
        value.timestamp = new Date().getTime();
        value.glucose = 123.456;

        String json = value.toJSON();

        Libre2RawValue new_value = Libre2RawValue.fromJSON(json);

        assertThat(value.serial).isEqualTo(new_value.serial);
        assertThat(value.timestamp).isEqualTo(new_value.timestamp);
        assertThat(value.glucose).isEqualTo(new_value.glucose);
    }
}

