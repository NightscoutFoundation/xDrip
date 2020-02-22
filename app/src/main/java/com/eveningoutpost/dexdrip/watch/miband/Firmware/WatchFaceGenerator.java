package com.eveningoutpost.dexdrip.watch.miband.Firmware;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.BgSparklineBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.ColorCache;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.Header;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.Image;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.Parameter;
import com.eveningoutpost.dexdrip.watch.miband.MiBandEntry;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import static com.eveningoutpost.dexdrip.Models.JoH.hourMinuteString;
import static com.eveningoutpost.dexdrip.Models.JoH.threadSleep;
import static com.eveningoutpost.dexdrip.UtilityModels.ColorCache.getCol;
import static com.eveningoutpost.dexdrip.utils.FileUtils.getExternalDir;
import static com.eveningoutpost.dexdrip.utils.FileUtils.makeSureDirectoryExists;

public class WatchFaceGenerator {
    private static final boolean d = true;
    private static final boolean debug = true; //need only for debug to save resulting image and firmware
    private static final String TAG = WatchFaceGenerator.class.getSimpleName();

    private InputStream fwFileStream;
    private boolean isNeedToUseCustomWatchface;
    private boolean isGraphEnabled;

    private Bitmap mainWatchfaceImage;
    private AssetManager assetManager;
    private Header header;
    private Parameter mainParam;
    private ArrayList<Integer> imageOffsets;
    private int parametersTableLength;

    private static Bitmap graphImage;
    private static boolean drawMutex;

    private static int highColor = 0xfff86f69;
    private static int lowColor = 0xff4b95ff;
    private static int textColor = Color.WHITE;
    private static int noDataTextSize = 30; //px
    private static int bgValueTextSize = 45; //px
    private static int timeStampTextSize = 18; //px
    private static int graphBgColor = 0x0;
    private File wfFile = null;
    private int offset = 0;

    public WatchFaceGenerator(AssetManager assetManager) throws Exception {
        this.assetManager = assetManager;
        InputStream mainImage = null;
        isGraphEnabled = MiBandEntry.isGraphEnabled();
        isNeedToUseCustomWatchface = MiBandEntry.isNeedToUseCustomWatchface();
        boolean customFilesFound = false;
        if (isNeedToUseCustomWatchface) {
            final String dir = getExternalDir();
            final File imageFile = new File(dir + "/my_image.png");
            wfFile = new File(dir + "/my_watchface.bin");
            if (imageFile.exists() || wfFile.exists()) {
                customFilesFound = true;
                mainImage = new FileInputStream(imageFile);
                fwFileStream = new FileInputStream(wfFile);
                offset = MiBandEntry.getImageOffset();
            }
        }
        if (!customFilesFound) {
            if (isGraphEnabled) {
                mainImage = assetManager.open("miband_watchface_parts/MainScreen.png");
                fwFileStream = assetManager.open("miband_watchface_parts/xdrip_miband4_main.bin");
            } else {
                mainImage = assetManager.open("miband_watchface_parts/MainScreenNoGraph.png");
                fwFileStream = assetManager.open("miband_watchface_parts/xdrip_miband4_main_no_graph.bin");
            }
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        mainWatchfaceImage = BitmapFactory.decodeStream(mainImage);
        mainImage.close();
        parseWatchfaceFile(fwFileStream);
    }

    private void parseWatchfaceFile(InputStream fwFileStream) throws Exception {
        if (d)
            UserError.Log.d(TAG, "Reading header");
        BufferedInputStream stream = new BufferedInputStream(fwFileStream);
        header = Header.readFrom(stream);
        if (d) {
            UserError.Log.d(TAG, "Header was read:");
            UserError.Log.d(TAG, String.format("Signature: %s, Unknown param: %d, ParametersSize: %d isValid: %s", header.getSignature(), header.getUnknown(), header.getParametersSize(), header.isValid()));
        }
        if (!header.isValid())
            throw new Exception("Wrong watchface format");
        if (d)
            UserError.Log.d(TAG, "Reading parameter offsets...");
        byte[] bytes = new byte[header.getParametersSize()];
        stream.read(bytes, 0, bytes.length);
        InputStream parameterStream = new ByteArrayInputStream(bytes);
        mainParam = Parameter.readFrom(parameterStream, 0);
        if (d)
            UserError.Log.d(TAG, "Parameters descriptor was read");
        parametersTableLength = (int) mainParam.getChildren().get(0).getValue();
        int imagesCount = (int) mainParam.getChildren().get(1).getValue();
        if (d)
            UserError.Log.d(TAG, "parametersTableLength: " + parametersTableLength + ", imagesCount: " + imagesCount);
        bytes = new byte[parametersTableLength];
        stream.read(bytes, 0, bytes.length);
        if (d)
            UserError.Log.d(TAG, "Reading images offsets...");
        bytes = new byte[imagesCount * 4];
        stream.read(bytes, 0, bytes.length);
        ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        imageOffsets = new ArrayList<>();
        while (b.hasRemaining()) {
            imageOffsets.add(b.getInt());
        }
        if (d)
            UserError.Log.d(TAG, "Image offsets were read");
        if (fwFileStream.markSupported())
            fwFileStream.reset();
    }

    Bitmap drawMainBitmapWithGraph(String bgValueText, Bitmap arrowBitmap, String timeStampText, String unitized_delta, boolean strike_through, boolean isHigh, boolean isLow, int graphHours) {
        /*int textHighColor = getCol(ColorCache.X.color_high_values);
        int textLowColor = getCol(ColorCache.X.color_low_values);*/

        Bitmap resultBitmap = mainWatchfaceImage.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultBitmap);

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (offset > height) offset = 0;

        if (isGraphEnabled) {
            int graphHeight = 84;
            //draw graph
            drawMutex = true;
            JoH.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        graphImage = new BgSparklineBuilder(xdrip.getAppContext())
                                .setBgGraphBuilder(new BgGraphBuilder(xdrip.getAppContext()))
                                .setStart(System.currentTimeMillis() - Constants.HOUR_IN_MS * graphHours)
                                .setEnd(System.currentTimeMillis())
                                .setWidthPx(width + 16)
                                .setHeightPx(graphHeight)
                                .setBackgroundColor(graphBgColor)
                                .setTinyDots()
                                .showHighLine()
                                .showLowLine()
                                .build();
                    } catch (Exception e) {
                    } finally {
                        drawMutex = false;
                    }
                }
            });
            while (drawMutex)
                threadSleep(100);
            //strip left and right fields
            Bitmap resizedGraphImage = Bitmap.createBitmap(graphImage, 8, 0, width, graphImage.getHeight());
            canvas.drawBitmap(resizedGraphImage, 0, offset, null);
            offset = offset + graphHeight + 10;
        }

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);
        if (isHigh) paint.setColor(highColor);
        if (isLow) paint.setColor(lowColor);
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextScaleX(0.88f);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(bgValueTextSize);
        paint.setStrikeThruText(strike_through);
        Rect bounds = new Rect();

        //draw arrow
        int bgTextPosY = 34 + offset;//px
        int imageRightMargin = 0;//px
        int imageLeftMargin = 10;//px
        int arrowXPos = width - arrowBitmap.getWidth() - imageRightMargin;
        canvas.drawBitmap(arrowBitmap, arrowXPos, bgTextPosY - arrowBitmap.getHeight(), null);

        //draw bgValueText
        paint.getTextBounds(bgValueText, 0, bgValueText.length(), bounds);
        int bgTextPosX = arrowXPos - bounds.width() - imageLeftMargin;
        if (bgTextPosX < 0) bgTextPosX = 0;
        canvas.drawText(bgValueText, bgTextPosX, bgTextPosY, paint);

        //draw unitized delta
        paint.setTextScaleX(1);
        paint.setColor(textColor);
        int unitsTextPosX = 0;//px
        int unitsTextPosY = 53 + offset;//px
        paint.setTextSize(timeStampTextSize);
        paint.setStrikeThruText(false);
        canvas.drawText(unitized_delta, unitsTextPosX, unitsTextPosY, paint);

        //draw timestamp
        int timeStampTextPosY = unitsTextPosY;//px
        //paint.setColor(textTimestampColor);
        paint.getTextBounds(timeStampText, 0, timeStampText.length(), bounds);
        canvas.drawText(timeStampText, width - bounds.width(), timeStampTextPosY, paint);

        return resultBitmap;
    }

    private Bitmap drawNoDataBitmap() {
        Bitmap resultBitmap = mainWatchfaceImage.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultBitmap);

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(noDataTextSize);
        Rect bounds = new Rect();
        String noDataText = "No Data";
        paint.getTextBounds(noDataText, 0, noDataText.length(), bounds);
        float x = width / 2f - bounds.width() / 2f - bounds.left;

        canvas.drawText(noDataText, x, height - 100, paint); //draw bgValueText
        //draw timestamp
        int timeStampTextPosY = height - 37;//px
        String timeStampText = "at " + hourMinuteString(JoH.tsl());
        paint.setTextSize(timeStampTextSize);
        paint.getTextBounds(timeStampText, 0, timeStampText.length(), bounds);
        canvas.drawText(timeStampText, width - bounds.width(), timeStampTextPosY, paint);

        return resultBitmap;
    }

    public byte[] genWatchFace() throws Exception {
        Bitmap mainScreen;
        Bitmap resultImage;
        int graphHours = MiBandEntry.getGraphHours();
        BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
        if (dg != null) {
            String timeStampText = "";
            if (dg.timestamp > Constants.DAY_IN_MS)
                timeStampText = "at " + hourMinuteString(dg.timestamp);
            else
                timeStampText = JoH.niceTimeScalar(JoH.msSince(dg.timestamp)) + " ago";

            InputStream arrowStream = assetManager.open("miband_watchface_parts/" + dg.delta_name + ".png");
            Bitmap arrowBitmap = BitmapFactory.decodeStream(arrowStream);

            mainScreen = drawMainBitmapWithGraph(dg.unitized.replace(",", "."), arrowBitmap, timeStampText, dg.unitized_delta_no_units, dg.isStale(), dg.isHigh(), dg.isLow(), graphHours);
            resultImage = Image.findApproptiateColorDeph(mainScreen, 2);
        } else {
            mainScreen = drawNoDataBitmap();
            resultImage = Image.findApproptiateColorDeph(mainScreen, 2);
        }

        final String dir = getExternalDir();

        if (d)
            UserError.Log.d(TAG, "Encoding main picture");
        ByteArrayOutputStream imageByteArrayOutput = new ByteArrayOutputStream();
        Image encodedImage = new Image(imageByteArrayOutput);

        encodedImage.write(resultImage);
        if (d)
            UserError.Log.d(TAG, "Encoded image size: " + imageByteArrayOutput.size() + " bytes");

        int imageOffset = header.getHeaderSize() + header.getParametersSize() + parametersTableLength;

        int oldImageSize = imageOffsets.get(1);
        int newImageSize = imageByteArrayOutput.size();
        int newImageOffsetDiff = oldImageSize - newImageSize;
        int imageOffsetTableSize = imageOffsets.size() * 4;

        if (wfFile != null) { //reread file
            fwFileStream.close();
            fwFileStream = new FileInputStream(wfFile);
        }

        BufferedInputStream firmwareReadStream = new BufferedInputStream(fwFileStream, fwFileStream.available());
        ByteArrayOutputStream firmwareWriteStream = new ByteArrayOutputStream(newImageSize +
                (firmwareReadStream.available() - oldImageSize - imageOffset - imageOffsetTableSize));

        if (d) {
            UserError.Log.d(TAG, "Copying original header with params ");
        }

        byte[] bytes = new byte[imageOffset];
        firmwareReadStream.read(bytes, 0, bytes.length);
        firmwareWriteStream.write(bytes, 0, bytes.length);
        if (d) {
            UserError.Log.d(TAG, "Writing modified image offsets");
        }

        Integer oldOffset, newOffset;
        int size = imageOffsets.size();
        for (int i = 1; i < size; i++) {
            oldOffset = imageOffsets.get(i);
            newOffset = oldOffset - newImageOffsetDiff;
            imageOffsets.set(i, newOffset);
        }

        ByteBuffer b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        for (Integer offset : imageOffsets) {
            b.putInt(offset);
            firmwareWriteStream.write(b.array());
            b.rewind();
        }

        if (d) {
            UserError.Log.d(TAG, "Writing main image");
        }
        firmwareWriteStream.write(imageByteArrayOutput.toByteArray());

        if (d) {
            UserError.Log.d(TAG, "Writing rest images from original firmware");
        }
        firmwareReadStream.skip(imageOffsetTableSize + oldImageSize); //skip to the first image
        bytes = new byte[8192];
        int read;
        while ((read = firmwareReadStream.read(bytes)) > 0) {
            firmwareWriteStream.write(bytes, 0, read);
        }
        fwFileStream.close();
        firmwareReadStream.close();

        UserError.Log.d(TAG, "Watchface file ready");

        if (debug) {
            makeSureDirectoryExists(dir);
            try (FileOutputStream out = new FileOutputStream(dir + "/image.png")) {
                resultImage.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (FileOutputStream out = new FileOutputStream(dir + "/watchface.bin")) {
                out.write(firmwareWriteStream.toByteArray());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return firmwareWriteStream.toByteArray();
    }

}