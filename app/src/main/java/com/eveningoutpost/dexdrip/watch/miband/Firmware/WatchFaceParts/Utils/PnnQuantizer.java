package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.Utils;
/* Fast pairwise nearest neighbor based algorithm for multilevel thresholding
Copyright (C) 2004-2016 Mark Tyler and Dmitry Groshev
Copyright (c) 2018-2019 Miller Cy Chan
* error measure; time used is proportional to number of bins squared - WJ */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PnnQuantizer {
    protected final short SHORT_MAX = Short.MAX_VALUE;
    protected final char BYTE_MAX = -Byte.MIN_VALUE + Byte.MAX_VALUE;
    protected boolean hasSemiTransparency = false;
    protected int m_transparentPixelIndex = -1;
    protected int width;
    protected int height;
    protected Bitmap.Config bitmapConfig;
    protected double PR = .2126, PG = .7152, PB = .0722;
    protected int pixels[] = null;
    protected Integer m_transparentColor;
    protected Map<Integer, short[]> closestMap = new HashMap<>();

    public PnnQuantizer(String fname) throws IOException {
        fromBitmap(fname);
    }

    public PnnQuantizer(Bitmap bitmap) throws IOException {
        fromBitmap(bitmap);
    }

    private void fromBitmap(Bitmap bitmap) throws IOException {
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        bitmapConfig = bitmap.getConfig();
    }

    private void fromBitmap(String fname) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeFile(fname);
        fromBitmap(bitmap);
    }

    private static final class Pnnbin {
        double ac = 0, rc = 0, gc = 0, bc = 0, err = 0;
        int cnt = 0;
        int nn, fw, bk, tm, mtm;
    }

    protected int getColorIndex(final int c, boolean hasSemiTransparency, int transparentPixelIndex) {
        if (hasSemiTransparency)
            return (Color.alpha(c) & 0xF0) << 8 | (Color.red(c) & 0xF0) << 4 | (Color.green(c) & 0xF0) | (Color.blue(c) >> 4);
        if (transparentPixelIndex >= 0)
            return (Color.alpha(c) & 0x80) << 8 | (Color.red(c) & 0xF8) << 7 | (Color.green(c) & 0xF8) << 2 | (Color.blue(c) >> 3);
        return (Color.red(c) & 0xF8) << 8 | (Color.green(c) & 0xFC) << 3 | (Color.blue(c) >> 3);
    }

    protected double sqr(double value) {
        return value * value;
    }

    private void find_nn(Pnnbin[] bins, int idx) {
        int nn = 0;
        double err = 1e100;

        Pnnbin bin1 = bins[idx];
        int n1 = bin1.cnt;
        double wa = bin1.ac;
        double wr = bin1.rc;
        double wg = bin1.gc;
        double wb = bin1.bc;
        for (int i = bin1.fw; i != 0; i = bins[i].fw) {
            double nerr = sqr(bins[i].ac - wa) + sqr(bins[i].rc - wr) + sqr(bins[i].gc - wg) + sqr(bins[i].bc - wb);
            double n2 = bins[i].cnt;
            nerr *= (n1 * n2) / (n1 + n2);
            if (nerr >= err)
                continue;
            err = nerr;
            nn = i;
        }
        bin1.err = err;
        bin1.nn = nn;
    }

    private Integer[] pnnquan(final int[] pixels, int nMaxColors, boolean quan_sqrt) {
        Pnnbin[] bins = new Pnnbin[65536];
        int[] heap = new int[65537];
        double err, n1, n2;

        /* Build histogram */
        for (final int pixel : pixels) {
            // !!! Can throw gamma correction in here, but what to do about perceptual
            // !!! nonuniformity then?
            int index = getColorIndex(pixel, hasSemiTransparency, m_transparentPixelIndex);
            if (bins[index] == null)
                bins[index] = new Pnnbin();
            Pnnbin tb = bins[index];
            tb.ac += Color.alpha(pixel);
            tb.rc += Color.red(pixel);
            tb.gc += Color.green(pixel);
            tb.bc += Color.blue(pixel);
            tb.cnt++;
        }

        /* Cluster nonempty bins at one end of array */
        int maxbins = 0;

        for (int i = 0; i < bins.length; ++i) {
            if (bins[i] == null)
                continue;

            double d = 1.0 / (double) bins[i].cnt;
            bins[i].ac *= d;
            bins[i].rc *= d;
            bins[i].gc *= d;
            bins[i].bc *= d;
            if (quan_sqrt)
                bins[i].cnt = (int) Math.sqrt(bins[i].cnt);
            bins[maxbins++] = bins[i];
        }

        for (int i = 0; i < maxbins - 1; i++) {
            bins[i].fw = (i + 1);
            bins[i + 1].bk = i;
        }
        // !!! Already zeroed out by calloc()
        //	bins[0].bk = bins[i].fw = 0;

        int h, l, l2;
        /* Initialize nearest neighbors and build heap of them */
        for (int i = 0; i < maxbins; i++) {
            find_nn(bins, i);
            /* Push slot on heap */
            err = bins[i].err;
            for (l = ++heap[0]; l > 1; l = l2) {
                l2 = l >> 1;
                if (bins[h = heap[l2]].err <= err)
                    break;
                heap[l] = h;
            }
            heap[l] = i;
        }

        /* Merge bins which increase error the least */
        int extbins = maxbins - nMaxColors;
        for (int i = 0; i < extbins; ) {
            Pnnbin tb = null;
            /* Use heap to find which bins to merge */
            for (; ; ) {
                int b1 = heap[1];
                tb = bins[b1]; /* One with least error */
                /* Is stored error up to date? */
                if ((tb.tm >= tb.mtm) && (bins[tb.nn].mtm <= tb.tm))
                    break;
                if (tb.mtm == 0xFFFF) /* Deleted node */
                    b1 = heap[1] = heap[heap[0]--];
                else /* Too old error value */ {
                    find_nn(bins, b1);
                    tb.tm = i;
                }
                /* Push slot down */
                err = bins[b1].err;
                for (l = 1; (l2 = l + l) <= heap[0]; l = l2) {
                    if ((l2 < heap[0]) && (bins[heap[l2]].err > bins[heap[l2 + 1]].err))
                        l2++;
                    if (err <= bins[h = heap[l2]].err)
                        break;
                    heap[l] = h;
                }
                heap[l] = b1;
            }

            /* Do a merge */
            Pnnbin nb = bins[tb.nn];
            n1 = tb.cnt;
            n2 = nb.cnt;
            double d = 1.0 / (n1 + n2);
            tb.ac = d * (n1 * tb.ac + n2 * nb.ac);
            tb.rc = d * (n1 * tb.rc + n2 * nb.rc);
            tb.gc = d * (n1 * tb.gc + n2 * nb.gc);
            tb.bc = d * (n1 * tb.bc + n2 * nb.bc);
            tb.cnt += nb.cnt;
            tb.mtm = ++i;

            /* Unchain deleted bin */
            bins[nb.bk].fw = nb.fw;
            bins[nb.fw].bk = nb.bk;
            nb.mtm = 0xFFFF;
        }

        /* Fill palette */
        List<Integer> palette = new ArrayList<>();
        short k = 0;
        for (int i = 0; ; ++k) {
            int alpha = (int) Math.rint(bins[i].ac);
            palette.add(Color.argb(alpha, (int) Math.rint(bins[i].rc), (int) Math.rint(bins[i].gc), (int) Math.rint(bins[i].bc)));
            if (m_transparentPixelIndex >= 0 && palette.get(k).equals(m_transparentColor))
                Collections.swap(palette, 0, k);

            if ((i = bins[i].fw) == 0)
                break;
        }

        return palette.toArray(new Integer[0]);
    }

    private short nearestColorIndex(final Integer[] palette, final int nMaxColors, final int c) {
        short k = 0;
        double curdist, mindist = SHORT_MAX;
        for (int i = 0; i < nMaxColors; ++i) {
            int c2 = palette[i];

            double adist = Math.abs(Color.alpha(c2) - Color.alpha(c));
            curdist = adist;
            if (curdist > mindist)
                continue;

            double rdist = PR * Math.abs(Color.red(c2) - Color.red(c));
            curdist += rdist;
            if (curdist > mindist)
                continue;

            double gdist = PG * Math.abs(Color.green(c2) - Color.green(c));
            curdist += gdist;
            if (curdist > mindist)
                continue;

            double bdist = PB * Math.abs(Color.blue(c2) - Color.blue(c));
            curdist += bdist;
            if (curdist > mindist)
                continue;

            mindist = curdist;
            k = (short) i;
        }
        return k;
    }

    private short closestColorIndex(final Integer[] palette, final int c) {
        short k = 0;
        short[] closest = new short[5];
        short[] got = closestMap.get(c);
        if (got == null) {
            closest[2] = closest[3] = SHORT_MAX;

            for (; k < palette.length; k++) {
                int c2 = palette[k];

                closest[4] = (short) (Math.abs(Color.alpha(c) - Color.alpha(c2)) + Math.abs(Color.red(c) - Color.red(c2)) + Math.abs(Color.green(c) - Color.green(c2)) + Math.abs(Color.blue(c) - Color.blue(c2)));
                if (closest[4] < closest[2]) {
                    closest[1] = closest[0];
                    closest[3] = closest[2];
                    closest[0] = k;
                    closest[2] = closest[4];
                } else if (closest[4] < closest[3]) {
                    closest[1] = k;
                    closest[3] = closest[4];
                }
            }

            if (closest[3] == SHORT_MAX)
                closest[2] = 0;
        } else
            closest = got;

        Random rand = new Random();
        if (closest[2] == 0 || (rand.nextInt(SHORT_MAX) % (closest[3] + closest[2])) <= closest[3])
            k = closest[0];
        else
            k = closest[1];

        closestMap.put(c, closest);
        return k;
    }

    boolean quantize_image(final int[] pixels, final Integer[] palette, int[] qPixels, final boolean dither) {
        int nMaxColors = palette.length;

        int pixelIndex = 0;
        if (dither) {
            boolean odd_scanline = false;
            short[] row0, row1;
            int a_pix, r_pix, g_pix, b_pix, dir, k;
            final int DJ = 4;
            final int DITHER_MAX = 20;
            final int err_len = (width + 2) * DJ;
            int[] clamp = new int[DJ * 256];
            int[] limtb = new int[512];
            short[] erowerr = new short[err_len];
            short[] orowerr = new short[err_len];
            int[] lookup = new int[65536];

            for (int i = 0; i < 256; i++) {
                clamp[i] = 0;
                clamp[i + 256] = (short) i;
                clamp[i + 512] = BYTE_MAX;
                clamp[i + 768] = BYTE_MAX;

                limtb[i] = -DITHER_MAX;
                limtb[i + 256] = DITHER_MAX;
            }
            for (int i = -DITHER_MAX; i <= DITHER_MAX; i++)
                limtb[i + 256] = i;

            for (short i = 0; i < height; i++) {
                if (odd_scanline) {
                    dir = -1;
                    pixelIndex += (width - 1);
                    row0 = orowerr;
                    row1 = erowerr;
                } else {
                    dir = 1;
                    row0 = erowerr;
                    row1 = orowerr;
                }

                int cursor0 = DJ, cursor1 = width * DJ;
                row1[cursor1] = row1[cursor1 + 1] = row1[cursor1 + 2] = row1[cursor1 + 3] = 0;
                for (short j = 0; j < width; j++) {
                    int c = pixels[pixelIndex];
                    r_pix = clamp[((row0[cursor0] + 0x1008) >> 4) + Color.red(c)];
                    g_pix = clamp[((row0[cursor0 + 1] + 0x1008) >> 4) + Color.green(c)];
                    b_pix = clamp[((row0[cursor0 + 2] + 0x1008) >> 4) + Color.blue(c)];
                    a_pix = clamp[((row0[cursor0 + 3] + 0x1008) >> 4) + Color.alpha(c)];

                    int c1 = Color.argb(a_pix, r_pix, g_pix, b_pix);
                    int offset = getColorIndex(c1, hasSemiTransparency, m_transparentPixelIndex);
                    if (lookup[offset] == 0)
                        lookup[offset] = nearestColorIndex(palette, nMaxColors, c1) + 1;

                    int c2 = qPixels[pixelIndex] = palette[lookup[offset] - 1];

                    r_pix = limtb[r_pix - Color.red(c2) + 256];
                    g_pix = limtb[g_pix - Color.green(c2) + 256];
                    b_pix = limtb[b_pix - Color.blue(c2) + 256];
                    a_pix = limtb[a_pix - Color.alpha(c2) + 256];

                    k = r_pix * 2;
                    row1[cursor1 - DJ] = (short) r_pix;
                    row1[cursor1 + DJ] += (r_pix += k);
                    row1[cursor1] += (r_pix += k);
                    row0[cursor0 + DJ] += (r_pix += k);

                    k = g_pix * 2;
                    row1[cursor1 + 1 - DJ] = (short) g_pix;
                    row1[cursor1 + 1 + DJ] += (g_pix += k);
                    row1[cursor1 + 1] += (g_pix += k);
                    row0[cursor0 + 1 + DJ] += (g_pix += k);

                    k = b_pix * 2;
                    row1[cursor1 + 2 - DJ] = (short) b_pix;
                    row1[cursor1 + 2 + DJ] += (b_pix += k);
                    row1[cursor1 + 2] += (b_pix += k);
                    row0[cursor0 + 2 + DJ] += (b_pix += k);

                    k = a_pix * 2;
                    row1[cursor1 + 3 - DJ] = (short) a_pix;
                    row1[cursor1 + 3 + DJ] += (a_pix += k);
                    row1[cursor1 + 3] += (a_pix += k);
                    row0[cursor0 + 3 + DJ] += (a_pix += k);

                    cursor0 += DJ;
                    cursor1 -= DJ;
                    pixelIndex += dir;
                }
                if ((i % 2) == 1)
                    pixelIndex += (width + 1);

                odd_scanline = !odd_scanline;
            }
            return true;
        }

        if (hasSemiTransparency || nMaxColors < 256) {
            for (int i = 0; i < qPixels.length; i++)
                qPixels[i] = palette[nearestColorIndex(palette, nMaxColors, pixels[i])];
        } else {
            for (int i = 0; i < qPixels.length; i++)
                qPixels[i] = palette[closestColorIndex(palette, pixels[i])];
        }

        return true;
    }

    public Bitmap convert(int nMaxColors, boolean dither) {
        final int[] cPixels = new int[pixels.length];
        for (int i = 0; i < cPixels.length; ++i) {
            int pixel = pixels[i];
            int alfa = (pixel >> 24) & 0xff;
            int r = (pixel >> 16) & 0xff;
            int g = (pixel >> 8) & 0xff;
            int b = (pixel) & 0xff;
            cPixels[i] = Color.argb(alfa, r, g, b);
            if (alfa < BYTE_MAX) {
                hasSemiTransparency = true;
                if (alfa == 0) {
                    m_transparentPixelIndex = i;
                    m_transparentColor = cPixels[i];
                }
            }
        }
        if (nMaxColors > 256) {
            dither = true;
            hasSemiTransparency = false;
        }

        if (hasSemiTransparency || nMaxColors <= 32)
            PR = PG = PB = 1.0;
        boolean quan_sqrt = nMaxColors > BYTE_MAX;
        Integer[] palette = new Integer[nMaxColors];
        if (nMaxColors > 2)
            palette = pnnquan(cPixels, nMaxColors, quan_sqrt);
        else {
            if (hasSemiTransparency) {
                palette[0] = Color.argb(0, 0, 0, 0);
                palette[1] = Color.BLACK;
            } else {
                palette[0] = Color.BLACK;
                palette[1] = Color.WHITE;
            }
        }

        int[] qPixels = new int[cPixels.length];
        quantize_image(cPixels, palette, qPixels, dither);
        if (m_transparentPixelIndex >= 0) {
            int k = qPixels[m_transparentPixelIndex];
            if (nMaxColors > 2)
                palette[k] = m_transparentColor;
            else if (!palette[k].equals(m_transparentColor)) {
                int c1 = palette[0];
                palette[0] = palette[1];
                palette[1] = c1;
            }
        }
        closestMap.clear();
        return Bitmap.createBitmap(qPixels, width, height, bitmapConfig);
    }

}
