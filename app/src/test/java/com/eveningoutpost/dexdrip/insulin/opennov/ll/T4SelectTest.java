package com.eveningoutpost.dexdrip.insulin.opennov.ll;


import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.HexTestTools;

import org.junit.Test;

public class T4SelectTest extends HexTestTools {

    @Test
    public void encodeTest() {
        assertWithMessage("aselect").that(T4Select.builder().build().aSelect().encode()).isEqualTo(tolerantHexStringToByteArray("00A4040007D276000085010100"));
        assertWithMessage("ccselect").that(T4Select.builder().build().ccSelect().encode()).isEqualTo(tolerantHexStringToByteArray("00A4000C02E103"));
        assertWithMessage("ndefselect").that(T4Select.builder().build().ndefSelect().encode()).isEqualTo(tolerantHexStringToByteArray("00A4000C02E104"));
    }
}