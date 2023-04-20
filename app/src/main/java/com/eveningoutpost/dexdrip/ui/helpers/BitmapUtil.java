package com.eveningoutpost.dexdrip.ui.helpers;

// jamorham

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.FileUtils;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import lombok.val;

public class BitmapUtil {

    // note: this recycles the source bitmap - TODO make this more generic
    public static Bitmap getTiled(final Bitmap source, final int xSize, final int ySize, final boolean tile, final String backgroundFile) {
        Bitmap tiledBitmap = null;
        if (backgroundFile != null) {
            tiledBitmap = getBackgroundBitmap(xSize, ySize, backgroundFile);
        }
        if (tiledBitmap == null) {
            tiledBitmap = Bitmap.createBitmap(xSize, ySize, Bitmap.Config.ARGB_8888);
        }
        if (source != null) {
            final Canvas canvas = new Canvas(tiledBitmap);
            final BitmapDrawable drawable = new BitmapDrawable(xdrip.getAppContext().getResources(), source);
            drawable.setBounds(0, 0, xSize, ySize);

            if (tile) {
                drawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                drawable.draw(canvas);
            } else {
                double yRatio = JoH.tolerantParseDouble(Pref.getString("numberwall_y_param", ""), 50d) / 100d; // TODO move to method signature
                int yOffset = Math.max(0, (int) ((getScreenDpi() * yRatio * 1.2d) - (getScreenDpi() * 0.30d)));
                final double spacerRatio = JoH.tolerantParseDouble(Pref.getString("numberwall_s_param", ""), 10d) / 100d;
                int xOffset = Math.max(0, (int) ((getScreenDpi() * spacerRatio * 1.2d) - (getScreenDpi() * 0.30d)));

                canvas.drawBitmap(source, xOffset, yOffset, new Paint());
            }
            source.recycle();
        } // returns blank bitmap if source is null
        return tiledBitmap;
    }

    public static Bitmap getBackgroundBitmap(final int xSize, final int ySize, final String backgroundFile) {
        Bitmap tiledBitmap = null;
        FileInputStream inputStream = null;
        try {
            tiledBitmap = BitmapLoader.bitmapFromBundleCache(backgroundFile);
            if (tiledBitmap == null) {
                android.util.Log.d("NumberWall", "Regenerating image");

                final Uri backgroundUri = Uri.parse(backgroundFile);

                Bitmap imageBitmap = null;
                if (backgroundFile.startsWith("content://")) {
                    FileDescriptor fileDescriptor = xdrip.getAppContext().getContentResolver().openFileDescriptor(backgroundUri, "r").getFileDescriptor();
                    imageBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                } else {
                    inputStream = new FileInputStream(backgroundFile);
                    imageBitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                }

                Bitmap rotatedBitmap = imageBitmap;
                final Matrix imageMatrix = new Matrix();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    ExifInterface exif = null;
                    if (backgroundFile.startsWith("content://")) {
                        FileDescriptor fileDescriptor = xdrip.getAppContext().getContentResolver().openFileDescriptor(backgroundUri, "r").getFileDescriptor(); // reset
                        exif = new ExifInterface(fileDescriptor);
                    } else {
                        inputStream = new FileInputStream(backgroundFile);
                        exif = new ExifInterface(inputStream);
                    }
                    int rotation = exifOrientationToDegrees(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
                    android.util.Log.d("NumberWall", "Rotation: " + rotation);

                    if (rotation != 0) {
                        imageMatrix.preRotate(rotation);
                        rotatedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), imageMatrix, true);
                        imageBitmap.recycle();
                    }
                }

                tiledBitmap = getBestCroppedScaled(rotatedBitmap, xSize, ySize);
                BitmapLoader.saveBitmapAsBundle(backgroundFile, Bitmap.createBitmap(tiledBitmap));
            } else {
                tiledBitmap = Bitmap.createBitmap(tiledBitmap); // make a copy
                android.util.Log.d("NumberWall", "cache hit");
            }
        } catch (Exception e) {
            // cannot load bitmap
            android.util.Log.e("NumberWall", "Cannot load bitmap: " + e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    android.util.Log.e("NumberWall", "Error closing input stream", e);
                }
            }
        }
        return tiledBitmap;
    }

    public static String copyBackgroundImage(Uri sourceUri, File destinationFolder) {
        // delete directory with older files
        FileUtils.deleteDirWithFiles(destinationFolder);
        // recreate directory
        destinationFolder.mkdirs();
        String destinationFileName = "background" + new Date().getTime() + ".png"; // unique name to avoid caching
        File destination = new File(destinationFolder, destinationFileName);

        Bitmap sourceImage = getBackgroundBitmap(getScreenWidth(), getScreenHeight(), sourceUri.toString());
        String output = destination.getPath();
        if (sourceImage != null) {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(destination);
                sourceImage.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
            } catch (IOException e) {
                android.util.Log.e("NumberWall", "Error copying background image", e);
                // fallback to original
                output = sourceUri.toString();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        android.util.Log.e("NumberWall", "Error closing output stream", e);
                    }
                }
            }
        }
        return output;
    }


    public static Bitmap getBestCroppedScaled(final Bitmap source, final int width, final int height) {
        if (source == null) return null;
        final int iX = source.getWidth();
        final int iY = source.getHeight();
        final float scaleX = ((float) width) / iX;
        final float scaleY = ((float) height) / iY;
        final float scale = Math.max(scaleX, scaleY);
        final int niX = ((int) (iX * scale)) + 1;
        final int niY = ((int) (iY * scale)) + 1;
        final Bitmap scaledBitmap = Bitmap.createScaledBitmap(source, niX, niY, false);
        source.recycle();
        final int originX = (niX - width) / 2;
        final int originY = (niY - height) / 2;
        final Bitmap croppedBitmap = Bitmap.createBitmap(scaledBitmap, originX, originY, width, height);
        scaledBitmap.recycle();
        return croppedBitmap;
    }

    private static int exifOrientationToDegrees(final int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
        }
        return 0;
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public static int getScreenDpi() {
        return Resources.getSystem().getDisplayMetrics().densityDpi;
    }

    public static float getScreenDensity() {
        return Resources.getSystem().getDisplayMetrics().density;
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        if (bm == null) return null;
        val width = bm.getWidth();
        val height = bm.getHeight();
        val scaleWidth = ((float) newWidth) / width;
        val scaleHeight = ((float) newHeight) / height;
        val matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        val resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

}
