package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static com.eveningoutpost.dexdrip.watch.miband.Firmware.FirmwareOperations.fromUint16;

public class Image {
    private static final boolean d = true;
    private static final String TAG = WatchFaceGenerator.class.getSimpleName();

    private static byte[] Signature = {(byte) 'B', (byte) 'M', (byte) 'd', 0};
    private ArrayList<Integer> palette;

    private ByteArrayOutputStream writer;

    private int bitsPerPixel;
    private int height;

    private Bitmap image;
    private int paletteColors;
    private int rowLengthInBytes;
    private int transparency;

    public ArrayList<Integer> getPalette() {
        return palette;
    }

    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

    public int getHeight() {
        return height;
    }

    public Bitmap getImage() {
        return image;
    }

    public int getPaletteColors() {
        return paletteColors;
    }

    public int getRowLengthInBytes() {
        return rowLengthInBytes;
    }

    public int getWidth() {
        return width;
    }

    private int width;

    public Image(ByteArrayOutputStream stream) {
        writer = stream;
        palette = new ArrayList<>();
    }

    public Image(ByteArrayOutputStream stream, Bitmap image) {
        this(stream);
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
    }

    public void write(Bitmap image) throws Exception {
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();

        extractPalette();

        if (palette.size() > 256)
            throw new Exception("Too many colors for palette mode, stopping execution");
        if (bitsPerPixel == 3) bitsPerPixel = 4;
        if (bitsPerPixel == 0) bitsPerPixel = 1;

        rowLengthInBytes = (int) Math.ceil((width * bitsPerPixel / 8.0));

        writer.write(Signature);

        writeHeader();
        writePalette();
        writeImage();
    }

    public static Bitmap findApproptiateColorDeph(Bitmap src, int initialBitOffest) {
        Bitmap bmOut = null;
        for (; initialBitOffest < 64; initialBitOffest++) {
            bmOut = decreaseColorDepth(src, initialBitOffest);
            ByteArrayOutputStream imageByteArrayOutput = new ByteArrayOutputStream();
            Image encodedImage = new Image(imageByteArrayOutput, bmOut);
            encodedImage.extractPalette();
            if ((encodedImage.bitsPerPixel == 8 || encodedImage.bitsPerPixel == 4 || encodedImage.bitsPerPixel == 2  ) && encodedImage.palette.size() <= 255) break;
        }
        return bmOut;
    }

    public static Bitmap decreaseColorDepth(Bitmap src, int bitOffset) {
        // get image size
        int width = src.getWidth();
        int height = src.getHeight();
        // create output bitmap
        Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
        // color information
        int A, R, G, B;
        int pixel;
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);

                // round-off color offset
                R = ((R + (bitOffset / 2)) - ((R + (bitOffset / 2)) % bitOffset) - 1);
                if (R < 0) {
                    R = 0;
                }
                G = ((G + (bitOffset / 2)) - ((G + (bitOffset / 2)) % bitOffset) - 1);
                if (G < 0) {
                    G = 0;
                }
                B = ((B + (bitOffset / 2)) - ((B + (bitOffset / 2)) % bitOffset) - 1);
                if (B < 0) {
                    B = 0;
                }
                bmOut.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }
        return bmOut;
    }


    private void extractPalette() {
        if (d)
            UserError.Log.e(TAG, "Extracting palette");

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = image.getPixel(x, y);
                if (palette.contains(color)) continue;

                if (Color.alpha(color) < 0x80 && transparency == 0) {
                    palette.add(0, color);
                    transparency = 1;
                } else {
                    palette.add(color);
                }
            }
        }
        //TODO transparent colors should be first?
        Collections.sort(palette);
        paletteColors = palette.size();
        bitsPerPixel = (int) Math.ceil(Math.log(paletteColors) / Math.log(2));
        if (d)
            UserError.Log.e(TAG, String.format("Extracted: %d palette colors, bitsPerPixel:%d", paletteColors, bitsPerPixel));
    }

    private void writeHeader() throws IOException {
        if (d) {
            UserError.Log.e(TAG, "Writing image header");
            UserError.Log.e(TAG, String.format("Width: %d, Height: %d, RowLength: %d", width, height, rowLengthInBytes));
            UserError.Log.e(TAG, String.format("BPP: %d, PaletteColors: %d, Transaparency: %d", bitsPerPixel, paletteColors, transparency));
        }
        writer.write(fromUint16(width));
        writer.write(fromUint16(height));
        writer.write(fromUint16(rowLengthInBytes));
        writer.write(fromUint16(bitsPerPixel));
        writer.write(fromUint16(paletteColors));
        writer.write(fromUint16(transparency));
    }

    private void writePalette() throws IOException {
        if (d)
            UserError.Log.e(TAG, "Writing palette");
        for (int color : palette) {
            writer.write(Color.red(color));
            writer.write(Color.green(color));
            writer.write(Color.blue(color));
            writer.write((byte) 0); // always 0 maybe padding
        }
    }

    private void writeImage() throws IOException {
        if (d)
            UserError.Log.e(TAG, "Writing image");

        HashMap<Integer, Integer> paletteHash = new HashMap<>();
        int i = 0;
        for (int color : palette) {
            paletteHash.put(color, i);
            i++;
        }

        for (int y = 0; y < height; y++) {
            ByteArrayOutputStream memoryStream = new ByteArrayOutputStream(rowLengthInBytes);
            WatchfaceBitWriter bitWriter = new WatchfaceBitWriter(memoryStream);
            Integer paletteIndex;
            for (int x = 0; x < width; x++) {
                int color = image.getPixel(x, y);
                if (Color.alpha(color) < 0x80 && transparency == 1) {
                    bitWriter.writeBits(0, bitsPerPixel);
                } else {
                    paletteIndex = paletteHash.get(color);
                    bitWriter.writeBits(paletteIndex, bitsPerPixel);
                }
            }
            bitWriter.flush();
            writer.write(memoryStream.toByteArray());
        }
    }
}
