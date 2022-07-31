package com.eveningoutpost.dexdrip.insulin.opennov.ll;


import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.HexTestTools;

import org.junit.Test;

import lombok.val;

public class T4ReadTest extends HexTestTools {

    @Test
    public void encodeForMtuTest() {

        val list = T4Read.builder().offset(2).length(259).build().encodeForMtu(255);

        assertWithMessage("list not null").that(list).isNotNull();
        assertWithMessage("list len").that(list.size()).isEqualTo(2);
        assertWithMessage("list 0").that(list.get(0)).isEqualTo(tolerantHexStringToByteArray("00B00002FF"));
        assertWithMessage("list 1").that(list.get(1)).isEqualTo(tolerantHexStringToByteArray("00B0010104"));

    }
}