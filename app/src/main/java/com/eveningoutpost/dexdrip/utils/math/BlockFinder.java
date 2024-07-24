package com.eveningoutpost.dexdrip.utils.math;


import com.eveningoutpost.dexdrip.utilitymodels.Pref;

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
            if (top < 0 || bottom < 0)
            {
                this.top = 0;
                this.bottom = 0;
            } else {
                this.top = top;
                this.bottom = bottom;
            }
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

    // TODO this could be a bit smarter
    public int findRandomAvailablePositionWithFailSafe(final int height, final int maxHeight) {
        boolean useTop, useTopCenter, useCenter, useCenterBottom, useBottom;
        final int sectionSize = maxHeight / 5;

        try {
            useTop = Pref.getBooleanDefaultFalse("aod_use_top");
            useTopCenter = Pref.getBooleanDefaultFalse("aod_use_top_center");
            useCenter = Pref.getBooleanDefaultFalse("aod_use_center");
            useCenterBottom = Pref.getBooleanDefaultFalse("aod_use_center_bottom");
            useBottom = Pref.getBooleanDefaultFalse("aod_use_bottom");
        } catch (NullPointerException e) {
            useTop = useTopCenter = useCenter = useCenterBottom = useBottom = true;
        }

        if (!(useTop || useTopCenter || useCenter || useCenterBottom || useBottom))
        {
            useTop = useTopCenter = useCenter = useCenterBottom = useBottom = true;
        }

        final int bound = maxHeight - height;

        if (bound >= 1) {
            int tries = 200;
            val random = new Random();

            while (tries-- > 0) {
                int pos = random.nextInt(bound);
                if (findOverlappingBlock(pos, pos + height) == null) {
                    if ((pos <= sectionSize && useTop)
                         || (pos >= sectionSize && pos <= 2 * sectionSize && useTopCenter)
                         || (pos >= 2 * sectionSize && pos <= 3 * sectionSize && useCenter)
                         || (pos >= 3 * sectionSize && pos <= 4 * sectionSize && useCenterBottom)
                         || (pos >= 4 * sectionSize && useBottom)) {
                        return pos;
                    }
                }
            }
        }
        // FailSafe
        return new Random().nextInt(bound);
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
