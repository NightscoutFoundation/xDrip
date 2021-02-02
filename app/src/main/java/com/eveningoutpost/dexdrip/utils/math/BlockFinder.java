package com.eveningoutpost.dexdrip.utils.math;


import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import lombok.val;

/**
 * jamorham
 *
 * Used to find random screen positions available for use
 *
 * Probably could do something fancy with Tree but in tests there were < 5 elements so that seemed
 * like overkill.
 *
 */

public class BlockFinder {

    class Block {
        public int top;
        public int bottom;

        public boolean within(int y) {
            return y >= this.top && y <= this.bottom;
        }

        @Override
        public String toString() {
            return top + "->" + bottom + "(" + (bottom - top) + ")";
        }

        public Block set(final int top, final int bottom) {
            if (top < 0 || bottom < 0) return null;
            this.top = top;
            this.bottom = bottom;
            return this;
        }
    }


    final List<Block> segments = new LinkedList<>();

    public Block findOverlappingBlock(final int top, final int bottom) {
        if (bottom <= top) return null;
        for (val segment : segments) {
            if (segment.within(top) || segment.within(bottom)) {
                return segment;
            }
        }
        return null;
    }


    public Block addBlockWithMerge(int top, int bottom) {
        if (top >= bottom || top < 0) return null;
        Block b = findOverlappingBlock(top, bottom);
        if (b == null) {
            b = new Block().set(top, bottom);
            if (b == null) return null;
            segments.add(b);
        } else {
            // merge blocks
            b.bottom = Math.max(b.bottom, bottom);
            b.top = Math.min(b.top, top);
        }
        return b;
    }

    public int findRandomAvailablePositionWithFailSafe(final int height, final int maxHeight) {
        val pos = findRandomAvailablePosition(height, maxHeight);
        if (pos < 0) {
            return new Random().nextInt(maxHeight - height);
        } else {
            return pos;
        }
    }

    // TODO this could be a bit smarter
    public int findRandomAvailablePosition(final int height, final int maxHeight) {

        final int bound = maxHeight - height;
        if (bound < 1) return -2;

        int tries = 200;
        val random = new Random();

        while (tries-- > 0) {
            val pos = random.nextInt(bound);
            if (findOverlappingBlock(pos, pos + height) == null) {
                return pos;
            }
        }
        return -1;
    }


    @Override
    public String toString() {
        val sb = new StringBuilder();
        sb.append("BlockFinder: ");
        for (val segment : segments) {
            sb.append(segment.toString());
            sb.append(" ");
        }
        return sb.toString();
    }

}
