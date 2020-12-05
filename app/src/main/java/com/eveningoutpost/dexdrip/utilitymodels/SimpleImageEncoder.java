package com.eveningoutpost.dexdrip.utilitymodels;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.TreeMap;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

/**
 * Simple PNG encoder for Pebble and Pebble Time
 *
 * TODO: alpha support is untested
 * TODO: better handling for black and white
 * TODO: threshold detection
 */
public class SimpleImageEncoder {
    private final static String TAG = SimpleImageEncoder.class.getSimpleName();
    int [] palette = getDefaultPalette();

    // Pebble 64-color palette
    public static int [] getDefaultPalette () {
        int [] palette = new int[64];

        for (int i = 0; i < 64; i++) {
            palette[i] = Color.rgb(
                    ((i >> 4) & 0x3) * 85,
                    ((i >> 2) & 0x3) * 85,
                    ((i >> 0) & 0x3) * 85
            );
        }

        return palette;
    }

    public SimpleImageEncoder() {
    }

    //public void setPalette (int [] newPalette) { palette = newPalette; }

    public int [] getPalette () {
        return palette;
    }

    private static int clamp (double value) {
        if (value < 0) return 0;
        else if (value > 255) return 255;
        else return (int) Math.round(value);
    }

    // TODO better handling of black and white
    public void optimizePalette (int [] data, int maxColors, boolean allowTransparent) {
        int [] counts = new int[palette.length];
        boolean hasTransparent = false;

        for (int i = 0; i < data.length; i++) {
            int p = data[i];

            if (Color.alpha(p) == 0 && allowTransparent) {
                hasTransparent = true;
            }

            int index = ((Color.red(p) / 85) << 4)
                    | ((Color.green(p) / 85) << 2)
                    | ((Color.blue(p) / 85) << 0);
            counts[index]++;
        }
        // Quick and dirty histogram
        TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>();
        for (int i = 0; i < counts.length; i++) {
            map.put(counts[i], palette[i]);
        }
        Log.d(TAG, "optimizePalette: map.size() is " + map.size() + ", map.descendingMap().size() is" + map.descendingMap().size());
        int colorCount = 0;
        int [] colors = new int[maxColors];

        if (hasTransparent) {
            colors[colorCount] = Color.argb(0, 255, 255, 255);
            colorCount++;
        }

        // Pick out the top colors
        for (int color : map.descendingMap().values()) {
            colors[colorCount] = color;
            colorCount++;
            Log.d(TAG, "optimizePalette: palette index " + (map.size()-colorCount+1) + " of " + (map.size()-1) +" has r:" + Color.red(color) + ", g:" + Color.green(color) + ", b:" + Color.blue(color));

            if (colorCount >= maxColors) break;
        }

        palette = colors;
    }


    public static double getColorDistance (int color, int pColor) {
        float rd = Color.red(pColor) - Color.red(color);
        float gd = Color.green(pColor) - Color.green(color);
        float bd = Color.blue(pColor) - Color.blue(color);

        return Math.sqrt(0.2126*rd*rd + 0.7152*gd*gd + 0.0722*bd*bd);
        //return Math.sqrt(0.299*rd*rd + 0.587*gd*gd + 0.144*bd*bd);
        //return Math.sqrt(rd*rd + gd*gd + bd*bd);
    }

    public byte getNearestColorIndex (int color) {
        int bestIndex = 0;
        double bestDistance = Double.MAX_VALUE;

        // If the palette contains a transparent pixel in the first slot,
        // use this for fully transparent pixels
        if (Color.alpha(color) == 0 && Color.alpha(palette[0]) == 0) {
            return 0;
        }

        if (palette.length == 2) {
        return 1; // choose any color so long as its black ;)
        } else {
            // This could be optimized
            for (int i = 0; i < palette.length; i++) {
                int pColor = palette[i];

                if (color == pColor) return (byte) i;

                double distance = getColorDistance(pColor, color);
                //double distance = getColorDistance(color, pColor);
                if (distance <= bestDistance) {
                    bestIndex = i;
                    bestDistance = distance;
                }
            }
//            Log.d(TAG, "getNearestColorIndex: selecting index " + bestIndex + " of " + palette.length + ", a distance of " + bestDistance + " for r:" + Color.red(color) + ", g:" + Color.green(color) + ", b:" + Color.blue(color));
        }

        return (byte) bestIndex;
    }

    // Dither image down to the current palette
    public void quantize (int [] pixels, int width) {
        final float[] errors = new float[] { 7f/16f, 3f/16f, 5f/16f, 1f/16f };
        final int[] offsets = new int[] { 1, width - 1, width, width + 1};

        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];
            int nearestColorIndex = this.getNearestColorIndex(color);
            int nearestColor = this.palette[nearestColorIndex];

            int rd = Color.red(color) - Color.red(nearestColor);
            int gd = Color.green(color) - Color.green(nearestColor);
            int bd = Color.blue(color) - Color.blue(nearestColor);

            pixels[i] = nearestColor;

            // Propagate errors
            for (int j = 0; j < errors.length; j++) {
                int offset = offsets[j];
                if (i + offset >= pixels.length) break;
                int neighborPixel = pixels[i + offset];

                int red = clamp(Color.red(neighborPixel) + errors[j] * rd);
                int green = clamp(Color.green(neighborPixel) + errors[j] * gd);
                int blue = clamp(Color.blue(neighborPixel) + errors[j] * bd);

                pixels[i + offset] = Color.rgb(red, green, blue);
            }
        }
    }

    /**
     * Encode an Android bitmap as an indexed PNG using Pebble Time colors.
     * Uses 16 colors for the best balance of quality and size.
     *
     * param: bitmap
     * return: array of bytes in PNG format
     */
/*    public static byte [] encodeBitmapAsPNG (Bitmap bitmap, boolean color) {
        return encodeBitmapAsPNG(bitmap, color, color ? 16 : 2, false);
    }
*/
    /**
     * Encode an Android bitmap as an indexed PNG using Pebble Time colors.
     * param: bitmap
     * param: color Whether the image is color (true) or black-and-white
     * param: numColors  Should be 2, 4, 16, or 64. Using 16 colors is
     *                   typically the best tradeoff. Must be 2 if B&W.
     * param: allowTransparent Allow fully transparent pixels
     * return: Array of bytes in PNG format
     */
    public static byte [] encodeBitmapAsPNG (Bitmap bitmap, boolean color, int numColors, boolean allowTransparent) {
        int bits;

        if (!color && numColors != 2) throw new IllegalArgumentException("must have 2 colors for black and white");

        if (numColors < 2) throw new IllegalArgumentException("minimum 2 colors");
        else if (numColors == 2) bits = 1;
        else if (numColors <= 4) bits = 2;
        else if (numColors <= 16) bits = 4;
        else if (numColors <= 64) bits = 8;
        else throw new IllegalArgumentException("maximum 64 colors");

        SimpleImageEncoder encoder = new SimpleImageEncoder();
        int [] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];

        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        encoder.optimizePalette(pixels, numColors, allowTransparent);

        return encoder.encodeIndexedPNG(pixels, bitmap.getWidth(), bitmap.getHeight(), color, bits);
    }

    public byte [] encodeIndexedPNG (int [] pixels, int width, int height, boolean color, int bits) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        int [] palette = getPalette();

        boolean alpha = Color.alpha(palette[0]) == 0;
        boolean grayscale = !color;
        //Log.d(TAG, "encodeIndexedPNG: color = "+color +", alpha ="+alpha+", grayscale = "+grayscale);

        //ImageInfo imageInfo = new ImageInfo(width, height, bits, alpha, grayscale, color);
        ImageInfo imageInfo = new ImageInfo(width, height, bits, false, grayscale, color);
        PngWriter writer = new PngWriter(bos, imageInfo);
        writer.getPixelsWriter().setDeflaterCompLevel(9);

        if (color) {
            PngChunkPLTE paletteChunk = writer.getMetadata().createPLTEChunk();
            paletteChunk.setNentries(palette.length);

            for (int i = 0; i < palette.length; i++) {
                int c = palette[i];
                paletteChunk.setEntry(i, Color.red(c), Color.green(c), Color.blue(c));
            }
        }

        if (alpha) {
            PngChunkTRNS trnsChunk = writer.getMetadata().createTRNSChunk();
            if (color) {
                trnsChunk.setIndexEntryAsTransparent(0);
            } else {
                trnsChunk.setGray(1);
            }
        }
        else {
            quantize(pixels, imageInfo.cols);
        }
        ImageLineInt line = new ImageLineInt(imageInfo);
        for (int y = 0; y < imageInfo.rows; y++) {
            int [] lineData = line.getScanline();
            for (int x = 0; x < imageInfo.cols; x++) {
                int pixel = pixels[y * imageInfo.cols + x];
                //lineData[x] = getNearestColorIndex(pixel) ^ (x % 2) ^ (y % 2);
                lineData[x] = getNearestColorIndex(pixel);
            }
            writer.writeRow(line);
        }

        writer.end();
        return bos.toByteArray();
    }
}
