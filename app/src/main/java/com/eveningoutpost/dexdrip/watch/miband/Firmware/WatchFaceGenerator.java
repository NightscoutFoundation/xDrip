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
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.DisplayData;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.Header;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.HeaderMiBand4;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.HeaderMiBand5;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.Image;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.Parameter;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.Utils.BgMibandSparklineBuilder;
import com.eveningoutpost.dexdrip.watch.miband.MiBand;
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
import static com.eveningoutpost.dexdrip.utils.FileUtils.getExternalDir;
import static com.eveningoutpost.dexdrip.utils.FileUtils.makeSureDirectoryExists;

public class WatchFaceGenerator {
    private static final boolean d = false;
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

    private static int highColor = 0xffffa0a0;
    private static int lowColor = 0xff8bbbff;
    private static int textColor = Color.WHITE;
    private static int noDataTextSize = 30; //px
    private static int bgValueTextSize = 50; //px
    private static int timeStampTextSize = 18; //px
    private static int statusIoBTextSize = 17; //px
    private static int graphBgColor = 0x0;
    private File wfFile = null;
    private int offset = 0;

    private MiBand.MiBandType miBandType;

    public WatchFaceGenerator(AssetManager assetManager, MiBand.MiBandType miBandType) throws Exception {
        this.assetManager = assetManager;
        this.miBandType = miBandType;
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
            String firmwareFileName = "miband_watchface_parts/xdrip_";
            String imageFileName = "miband_watchface_parts/";
            if (miBandType == MiBand.MiBandType.MI_BAND4) {
                firmwareFileName += "miband4";
                imageFileName += "miband4";
            } else if (miBandType == MiBand.MiBandType.MI_BAND5) {
                firmwareFileName += "miband5";
                imageFileName += "miband5";
            }
            imageFileName += "_main_screen";

            if (!isGraphEnabled) {
                firmwareFileName += "_no_graph";
                imageFileName += "_no_graph";
            }
            if (!MiBandEntry.isUS_DateFormat()) {
                firmwareFileName += "_eu";
            }

            firmwareFileName += ".bin";
            imageFileName += ".png";
            mainImage = assetManager.open(imageFileName);
            fwFileStream = assetManager.open(firmwareFileName);
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
        header = null;
        if (miBandType == MiBand.MiBandType.MI_BAND4) {
            header = new HeaderMiBand4();
        } else if (miBandType == MiBand.MiBandType.MI_BAND5) {
            header = new HeaderMiBand5();
        } else {
            throw new Exception("Not supported device");
        }
        header = header.readFrom(stream);
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

    Bitmap drawBitmap(DisplayData data) {

        Bitmap resultBitmap = mainWatchfaceImage.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultBitmap);

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (offset > height) offset = 0;

        Rect bounds = new Rect();
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);
        paint.setAntiAlias(true);

        if (data.isGraphEnabled()) {
            int graphHeight = 80;
            if (data.isShowTreatment()) graphHeight = 84;
            int finalGraphHeight = graphHeight;
            //draw graph
            drawMutex = true;
            long startTime = System.currentTimeMillis() - Constants.HOUR_IN_MS * data.getGraphHours();
            long endTime = System.currentTimeMillis() + Constants.MINUTE_IN_MS * 30;
            JoH.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BgMibandSparklineBuilder bgGraph = (BgMibandSparklineBuilder) new BgMibandSparklineBuilder(xdrip.getAppContext())
                                .setBgGraphBuilder(new BgGraphBuilder(xdrip.getAppContext(), startTime, endTime))
                                .setStart(startTime)
                                .setEnd(endTime)
                                .setWidthPx(width + 16)
                                .setHeightPx(finalGraphHeight)
                                .setBackgroundColor(graphBgColor)
                                .setTinyDots()
                                .showHighLine()
                                .showLowLine();
                        bgGraph.showTreatmentLine(data.isShowTreatment());
                        graphImage = bgGraph.build();
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
            //draw iob
            if (!data.getIob().isEmpty()) {
                paint.setTextScaleX(1);
                paint.setTextSize(statusIoBTextSize);
                paint.getTextBounds(data.getIob(), 0, data.getIob().length(), bounds);
                int iobTextPosX = 0;//px
                int iobTextPosY = offset + bounds.height();//px
                canvas.drawText(data.getIob(), iobTextPosX, iobTextPosY, paint);
            }
            if (data.isShowTreatment())
                offset = offset + graphHeight - 10;
            else
                offset = offset + graphHeight + 10;
        }

        //draw arrow
        int bgTextPosY = 39 + offset;//px
        int imageRightMargin = 0;//px
        int imageLeftMargin = 5;//px
        int arrowXPos = width - data.getArrowBitmap().getWidth() - imageRightMargin;
        canvas.drawBitmap(data.getArrowBitmap(), arrowXPos, bgTextPosY - data.getArrowBitmap().getHeight(), null);

        //draw bgValueText
        if (data.isHigh()) paint.setColor(highColor);
        if (data.isLow()) paint.setColor(lowColor);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.LEFT);

        paint.setTextScaleX(0.88f);
        paint.setTextSize(bgValueTextSize);
        paint.setStrokeWidth(2);
        String[] bgValuesSplitted = data.getBgValueText().split("[,]");
        String bgtextDigits = data.getBgValueText();
        if (bgtextDigits.length() <= 2 || (bgValuesSplitted.length >= 2 && bgValuesSplitted[0].length() == 1))
            imageLeftMargin += 5;
        int bgTextPosX = 0;
        if (bgValuesSplitted.length >= 2) {
            bgtextDigits = bgValuesSplitted[1];
            paint.setTextSize(bgValueTextSize - 9);
            paint.getTextBounds(bgtextDigits, 0, bgtextDigits.length(), bounds);
            bgTextPosX = arrowXPos - bounds.width() - imageLeftMargin;
            canvas.drawText(bgtextDigits, bgTextPosX, bgTextPosY, paint);

            bgtextDigits = ".";
            paint.setTextSize(bgValueTextSize - 6);
            paint.getTextBounds(bgtextDigits, 0, bgtextDigits.length(), bounds);
            bgTextPosX = bgTextPosX - bounds.width() - 2;
            canvas.drawText(bgtextDigits, bgTextPosX, bgTextPosY, paint); //draw dot

            paint.setTextSize(bgValueTextSize);
            bgtextDigits = bgValuesSplitted[0];
            paint.getTextBounds(bgtextDigits, 0, bgtextDigits.length(), bounds);
            bgTextPosX = bgTextPosX - bounds.width() - 1;
            canvas.drawText(bgtextDigits, bgTextPosX, bgTextPosY, paint);
        } else {
            paint.getTextBounds(bgtextDigits, 0, bgtextDigits.length(), bounds);
            bgTextPosX = arrowXPos - bounds.width() - imageLeftMargin;
            if (bgTextPosX < 0) bgTextPosX = 0;
            canvas.drawText(bgtextDigits, bgTextPosX, bgTextPosY, paint);
        }
        //draw strike line for bg
       if ( data.isStrike_through()){
           canvas.drawLine( bgTextPosX-5, bgTextPosY-bounds.height()/2, arrowXPos-5,bgTextPosY-bounds.height()/2, paint);
       }

        //draw unitized delta
        paint.setTextScaleX(1);
        paint.setColor(textColor);
        int unitsTextPosX = 0;//px
        int unitsTextPosY = bgTextPosY + timeStampTextSize + 1;//px
        paint.setTextSize(timeStampTextSize);
        String delta = data.getUnitized_delta() + " " + data.getTimeStampText();
        paint.getTextBounds(delta, unitsTextPosX, delta.length(), bounds);
        canvas.drawText(delta, width - bounds.width(), unitsTextPosY, paint);

        //draw treatment
        if (data.isGraphEnabled() && data.isShowTreatment()) {
            Treatments treatment = Treatments.last();
            if (treatment != null && treatment.hasContent() && !treatment.noteOnly()) {
                int treatmentTextPosY = unitsTextPosY + timeStampTextSize + 1;//px
                paint.setTextSize(timeStampTextSize);

                String mylabel = "";
                if (treatment.insulin > 0) {
                    if (mylabel.length() > 0)
                        mylabel = mylabel + System.getProperty("line.separator");
                    mylabel = mylabel + (JoH.qs(treatment.insulin, 2) + "u").replace(".0u", "u");
                }
                if (treatment.carbs > 0) {
                    if (mylabel.length() > 0)
                        mylabel = mylabel + System.getProperty("line.separator");
                    mylabel = mylabel + (JoH.qs(treatment.carbs, 1) + "g").replace(".0g", "g");
                }
                if (mylabel.length() > 0) {
                    if (treatment.timestamp > Constants.DAY_IN_MS)
                        mylabel = mylabel + " at " + hourMinuteString(treatment.timestamp);
                    else
                        mylabel = mylabel + " " + JoH.niceTimeScalar(JoH.msSince(treatment.timestamp)) + " ago";
                }

                paint.getTextBounds(mylabel, 0, mylabel.length(), bounds);
                canvas.drawText(mylabel, width - bounds.width(), treatmentTextPosY, paint);
            }
        }
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

    public byte[] genWatchFace(String iob) throws Exception {
        Bitmap mainScreen;
        Bitmap resultImage;
        BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
        if (dg != null) {
            DisplayData.Builder displayDataBuilder = DisplayData.newBuilder(dg, assetManager);
            if (isGraphEnabled) {
                displayDataBuilder.enableGraph(MiBandEntry.getGraphHours());
            }
            displayDataBuilder.setShowTreatment(MiBandEntry.isTreatmentEnabled());
            displayDataBuilder.setIoB(iob);
            mainScreen = drawBitmap(displayDataBuilder.build());
            resultImage = Image.quantinizeImage(mainScreen);
        } else {
            mainScreen = drawNoDataBitmap();
            resultImage = Image.quantinizeImage(mainScreen);
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