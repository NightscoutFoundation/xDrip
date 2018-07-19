package com.eveningoutpost.dexdrip.cgm.medtrum;

import com.eveningoutpost.dexdrip.HexTestTools;

import org.junit.Test;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Crypt.code;
import static com.eveningoutpost.dexdrip.cgm.medtrum.Crypt.schrageRandomInt;
import static com.google.common.truth.Truth.assertWithMessage;

// jamorham

public class CryptTest extends HexTestTools {

    @Test
    public void pseudoRandomTest() {

        // first 50 iterations
        final long[] testSuiteResults = {0, 16807, 282475249, 1622650073, 984943658, 1144108930,
                470211272, 101027544, 1457850878, 1458777923, 2007237709, 823564440, 1115438165,
                1784484492, 74243042, 114807987, 1137522503, 1441282327, 16531729, 823378840, 143542612,
                896544303, 1474833169, 1264817709, 1998097157, 1817129560, 1131570933, 197493099,
                1404280278, 893351816, 1505795335, 1954899097, 1636807826, 563613512, 101929267,
                1580723810, 704877633, 1358580979, 1624379149, 2128236579, 784558821, 530511967,
                2110010672, 1551901393, 1617819336, 1399125485, 156091745, 1356425228, 1899894091,
                585640194};

        long seed = 0;
        for (int i = 0; i < 50; i++) {
            long new_seed = testSuiteResults[i];
            assertWithMessage("pseudo random mismatch").that(schrageRandomInt(seed)).isEqualTo(new_seed);

            if (new_seed == 0) new_seed++;
            seed = new_seed;
        }
    }

    @Test
    public void codeTest() {

        final byte[] referenceResult1 = tolerantHexStringToByteArray("29 88 28 0C 3D BB D4 8B");
        final byte[] referenceResult2 = tolerantHexStringToByteArray("12 34 56 78  06 07 AA FF");
        final byte[] referenceResult3 = tolerantHexStringToByteArray("E7 AD D8 43  F3 9E 24 C4");
        final byte[] workingData = tolerantHexStringToByteArray("12 34 56 78  06 07 AA FF");

        code(workingData, 12345678L);
        assertWithMessage("cipher results enc 1").that(workingData).isEqualTo(referenceResult1);
        code(workingData, 12345678L);
        assertWithMessage("cipher results dec 1").that(workingData).isEqualTo(referenceResult2);
        code(workingData, 0L);
        assertWithMessage("cipher results enc 2").that(workingData).isEqualTo(referenceResult3);

    }

}