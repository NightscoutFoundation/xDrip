package com.eveningoutpost.dexdrip.utils;

import static com.eveningoutpost.dexdrip.utils.UniqueId.clear;
import static com.eveningoutpost.dexdrip.utils.UniqueId.get;
import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import lombok.val;

public class UniqueIdTest extends RobolectricTestWithConfig {

    @Before
    public void before() {
        clear();
    }

    @After
    public void after() {
        clear();
    }

    @Test
    public void getTest() {

        val id = get();
        assertWithMessage("id standard length 1").that(id).hasLength(16);
        assertWithMessage("repeated request gets same 1").that(get()).isEqualTo(id);
        assertWithMessage("repeated request gets same 2").that(get()).isEqualTo(id);

        clear();

        val id2 = get();
        assertWithMessage("is standard length 2").that(id2).hasLength(16);
        assertWithMessage("new id is different").that(get()).isNotEqualTo(id);
        assertWithMessage("new id is same on request").that(get()).isEqualTo(id2);

        val id3 = get(64);
        assertWithMessage("length capped to 32").that(id3).hasLength(32);
        assertWithMessage("long value is superset").that(id3).startsWith(id2);

        val id4 = get(4);
        assertWithMessage("short cap as expected").that(id4).hasLength(4);
        assertWithMessage("short value is subset").that(id3).startsWith(id4);

    }
}