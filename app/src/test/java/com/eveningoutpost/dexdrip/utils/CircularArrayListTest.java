package com.eveningoutpost.dexdrip.utils;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class CircularArrayListTest {

    // -- Capacity and emptiness --

    @Test
    public void newBuffer_isEmpty() {
        CircularArrayList<Integer> buf = new CircularArrayList<>(5);
        assertThat(buf.size()).isEqualTo(0);
        assertThat(buf.capacity()).isEqualTo(5);
    }

    // -- Adding and retrieving elements --

    @Test
    public void addElements_retrievableInOrder() {
        CircularArrayList<String> buf = new CircularArrayList<>(3);
        buf.add("a");
        buf.add("b");
        buf.add("c");
        assertThat(buf.get(0)).isEqualTo("a");
        assertThat(buf.get(1)).isEqualTo("b");
        assertThat(buf.get(2)).isEqualTo("c");
        assertThat(buf.size()).isEqualTo(3);
    }

    // -- Head and tail access --

    @Test
    public void headAndTail_returnFirstAndLastElement() {
        CircularArrayList<Integer> buf = new CircularArrayList<>(5);
        buf.add(10);
        buf.add(20);
        buf.add(30);
        assertThat(buf.head()).isEqualTo(10);
        assertThat(buf.tail()).isEqualTo(30);
    }

    @Test
    public void tail_withOffset_returnsFromEnd() {
        CircularArrayList<Integer> buf = new CircularArrayList<>(5);
        buf.add(10);
        buf.add(20);
        buf.add(30);
        assertThat(buf.tail(0)).isEqualTo(30);
        assertThat(buf.tail(1)).isEqualTo(20);
        assertThat(buf.tail(2)).isEqualTo(10);
    }

    // -- Full buffer without autoEvict throws --

    @Test(expected = IllegalStateException.class)
    public void addBeyondCapacity_withoutAutoEvict_throws() {
        CircularArrayList<Integer> buf = new CircularArrayList<>(2);
        buf.add(1);
        buf.add(2);
        buf.add(3);
    }

    // -- Remove shifts correctly --

    @Test
    public void removeFirst_shiftsRemaining() {
        CircularArrayList<Integer> buf = new CircularArrayList<>(5);
        buf.add(10);
        buf.add(20);
        buf.add(30);
        int removed = buf.remove(0);
        assertThat(removed).isEqualTo(10);
        assertThat(buf.size()).isEqualTo(2);
        assertThat(buf.head()).isEqualTo(20);
    }

    // -- Wraparound behavior --

    @Test
    public void wraparound_afterRemoveAndAdd_worksCorrectly() {
        CircularArrayList<Integer> buf = new CircularArrayList<>(3);
        buf.add(1);
        buf.add(2);
        buf.add(3);
        buf.remove(0);
        buf.add(4);
        assertThat(buf.size()).isEqualTo(3);
        assertThat(buf.get(0)).isEqualTo(2);
        assertThat(buf.get(1)).isEqualTo(3);
        assertThat(buf.get(2)).isEqualTo(4);
    }

    // -- Bounds checking --

    @Test(expected = IndexOutOfBoundsException.class)
    public void getNegativeIndex_throws() {
        CircularArrayList<Integer> buf = new CircularArrayList<>(3);
        buf.add(1);
        buf.get(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getIndexBeyondSize_throws() {
        CircularArrayList<Integer> buf = new CircularArrayList<>(5);
        buf.add(1);
        buf.get(1);
    }

    // -- Set replaces value --

    @Test
    public void set_replacesExistingElement() {
        CircularArrayList<String> buf = new CircularArrayList<>(3);
        buf.add("a");
        buf.add("b");
        buf.set(1, "x");
        assertThat(buf.get(1)).isEqualTo("x");
    }
}
