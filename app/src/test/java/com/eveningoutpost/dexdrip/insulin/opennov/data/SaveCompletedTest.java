package com.eveningoutpost.dexdrip.insulin.opennov.data;

import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.google.common.truth.Truth.assertWithMessage;

import android.util.SparseArray;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import lombok.val;

public class SaveCompletedTest extends RobolectricTestWithConfig {

    private final boolean d = false;

    @After
    public void cleanup() {
        Treatments.delete_all();
    }

    private List<Treatments> getCacheSpec() {
        Treatments.delete_all();
        final List<Treatments> cache = new LinkedList<>();
        val tx = tsl();
        for (int i = 0; i < 10; i++) {
            val treatment = Treatments.create(0d, 1d, tx + i, UUID.randomUUID().toString());
            treatment.notes = "test";
            cache.add(treatment);
        }

        // sort timestamps ascending
        Collections.sort(cache, (o1, o2) -> Long.valueOf(o1.timestamp).compareTo(Long.valueOf(o2.timestamp)));

        return cache;
    }

    private void verify(SparseArray<Boolean> answers, List<Treatments> cache, double doseThreshold, long timeThreshold) {
        for (int i = 0; i < cache.size(); i++) {
            Treatments dose = cache.get(i);
            if (SaveCompleted.isPrimingDose(cache, dose, doseThreshold, timeThreshold)) {
                if (d)
                    System.out.println("Removed priming dose @ " + JoH.dateTimeText(dose.timestamp) + " " + dose.timestamp + " " + dose.notes);
                dose.insulin = 0;
                dose.notes = tsl() + dose.notes;
                assertWithMessage("removal match" + i).that(answers.get(i, false)).isTrue();
            } else {
                if (d)
                    System.out.println("Didn't remove dose @ " + JoH.dateTimeText(dose.timestamp) + " " + dose.timestamp + " " + dose.notes);
                assertWithMessage("non removal match " + i).that(answers.get(i, false)).isFalse();
            }
        }
    }

    @Test
    public void isPrimingDoseTest1() {
        val answers = new SparseArray<Boolean>();
        answers.put(0, true);
        answers.put(3, true);
        answers.put(6, true);
        verify(answers, getCacheSpec(), 2, 3);
    }

    @Test
    public void isPrimingDoseTest2() {
        val answers = new SparseArray<Boolean>();
        answers.put(0, true);
        verify(answers, getCacheSpec(), 2, 30);
    }

}