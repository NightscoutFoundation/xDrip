package com.eveningoutpost.dexdrip.G5Model;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

import java.util.LinkedList;

import lombok.val;

// jamorham

public class RawScalingTest {

    @Test
    public void scaleTest() {
        assertWithMessage("G5 1").that((int) RawScaling.scale(10000, RawScaling.DType.G5, false)).isEqualTo(10000);

        assertWithMessage("G6v1 1").that((int) RawScaling.scale(294, RawScaling.DType.G6v1, false)).isEqualTo(9996);

        assertWithMessage("G6v2 1").that((int) RawScaling.scale(1168582904, RawScaling.DType.G6v2, false)).isEqualTo(187219);
    }

    @Test
    public void scale1Test() {
        val raw = new LinkedList<Integer>();
        raw.add(0x4583aa08);
        raw.add(0x4583b210);
        raw.add(0x4583b6f0);
        raw.add(0x45c90c00);
        raw.add(0x45b93ac8);
        raw.add(0x45b941c0);
        raw.add(0x4662ec58);
        raw.add(0x4662f124);
        raw.add(0x4662e19c);

        val filtered = new LinkedList<Integer>();
        filtered.add(0xc25499c3);       // error state
        filtered.add(0x4550a1ac);
        filtered.add(0x458c0249);
        filtered.add(0xc25499c3);       // error state
        filtered.add(0x459134bf);
        filtered.add(0x45c46edf);
        filtered.add(0x465b0404);
        filtered.add(0x467070dd);
        filtered.add(0x46754541);

        val sb = new StringBuilder();
        for (int i = 0; i < raw.size(); i++) {
            sb
                    .append("or: ")
                    .append(raw.get(i))
                    .append(" of: ")
                    .append(filtered.get(i))
                    .append("   X: ")
                    .append(RawScaling.scale(raw.get(i), RawScaling.DType.G6v2, false))
                    .append(" Y: ")
                    .append(RawScaling.scale(filtered.get(i), RawScaling.DType.G6v2, false))
                    .append("\n");
        }
        assertWithMessage("matches test data set").that(sb.toString()).isEqualTo(
                "or: 1166256648 of: -1034643005   X: 147463.890625 Y: -1860.2554931640625\n" +
                        "or: 1166258704 of: 1162912172   X: 147499.03125 Y: 116833.65625\n" +
                        "or: 1166259952 of: 1166803529   X: 147520.34375 Y: 156810.0\n" +
                        "or: 1170803712 of: -1034643005   X: 225172.5 Y: -1860.2554931640625\n" +
                        "or: 1169767112 of: 1167144127   X: 207457.171875 Y: 162630.765625\n" +
                        "or: 1169768896 of: 1170501343   X: 207487.65625 Y: 220005.0625\n" +
                        "or: 1180888152 of: 1180369924   X: 508308.0 Y: 490595.125\n" +
                        "or: 1180889380 of: 1181774045   X: 508349.96875 Y: 538587.5625\n" +
                        "or: 1180885404 of: 1182090561   X: 508214.09375 Y: 549406.0\n");
    }
}