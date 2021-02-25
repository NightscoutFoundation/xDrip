package com.eveningoutpost.dexdrip.utils.math;

import com.google.common.collect.Range;

import org.junit.Test;

import lombok.val;

import static com.google.common.truth.Truth.assertWithMessage;


public class BlockFinderTest {

    @Test
    public void addBlockWithMergeTest() {
        val b = new BlockFinder();
        assertWithMessage("invalid bottom/top 0").that(b.addBlockWithMerge(-100, 90)).isNull();
        assertWithMessage("first block").that(b.addBlockWithMerge(10, 100)).isInstanceOf(BlockFinder.Block.class);
        assertWithMessage("invalid bottom/top 1").that(b.addBlockWithMerge(0, 0)).isNull();
        assertWithMessage("invalid bottom/top 2").that(b.addBlockWithMerge(100, 90)).isNull();
        assertWithMessage("invalid bottom/top 3").that(b.addBlockWithMerge(-100, 90)).isNull();
        assertWithMessage("Block 2 ok").that(b.addBlockWithMerge(50, 150)).isNotNull();
        assertWithMessage("Block 3 ok").that(b.addBlockWithMerge(700, 900)).isNotNull();
        assertWithMessage("Block 2 match a").that(b.findOverlappingBlock(150, 160).top).isEqualTo(10);
        assertWithMessage("Block 2 match b").that(b.findOverlappingBlock(150, 160).bottom).isEqualTo(150);
        assertWithMessage("Block 3 match invalid").that(b.findOverlappingBlock(750, 650)).isNull();
        assertWithMessage("Block 3 match a").that(b.findOverlappingBlock(650, 760).top).isEqualTo(700);
        assertWithMessage("Block 3 match b").that(b.findOverlappingBlock(650, 760).bottom).isEqualTo(900);
        assertWithMessage("block merge test").that(b.toString().trim()).isEqualTo("BlockFinder: 10->150(140) 700->900(200)");
    }

    @Test
    public void findRandomAvailablePositionTest() {
        val b = new BlockFinder();
        b.addBlockWithMerge(10, 100);
        b.addBlockWithMerge(50, 150);
        assertWithMessage("invalid bound test").that(b.findRandomAvailablePosition(200, 50)).isEqualTo(-2);
        assertWithMessage("impossible fit").that(b.findRandomAvailablePosition(100, 200)).isEqualTo(-1);
        for (int i = 0; i < 50; i++) {
            assertWithMessage("impossible fit failsafe " + i).that(b.findRandomAvailablePositionWithFailSafe(100, 200)).isIn(Range.closed(0, 100));
            assertWithMessage("example fit " + i).that(b.findRandomAvailablePosition(50, 2000)).isIn(Range.closed(50, 2000 - 50));
        }
    }
}