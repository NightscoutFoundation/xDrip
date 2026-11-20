package com.eveningoutpost.dexdrip.services;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

public class UiBasedNotificationParserTest extends RobolectricTestWithConfig {

    @Test
    public void parsesEmbeddedMmolValues() {
        final UiBasedCollector collector = new UiBasedCollector();
        collector.lastPackage = "com.sinocare.ican.health.ru";

        assertWithMessage("decimal comma with unit")
                .that(collector.tryExtractNotificationString("4,7 mmol/L"))
                .isEqualTo(85);

        assertWithMessage("value embedded in notification text")
                .that(collector.tryExtractNotificationString("Current glucose: 4.7 mmol/L"))
                .isEqualTo(85);

        assertWithMessage("word joiners and trend arrow")
                .that(collector.tryExtractNotificationString("8,9\u00a0mmol\u2060/\u2060l \u2191"))
                .isEqualTo(160);
    }

    @Test
    public void parsesEmbeddedMgdlValues() {
        final UiBasedCollector collector = new UiBasedCollector();
        collector.lastPackage = "com.sinocare.ican.health.ru";

        assertWithMessage("mg/dL embedded in notification text")
                .that(collector.tryExtractNotificationString("Current glucose: 123 mg/dL"))
                .isEqualTo(123);
    }
}
