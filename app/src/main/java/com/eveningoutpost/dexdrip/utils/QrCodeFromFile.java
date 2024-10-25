package com.eveningoutpost.dexdrip.utils;

import android.app.Activity;
import android.content.Intent;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This is a helper class to manage QR code scan from file and
 * return results to the instantiating activity to complement the existing scan from camera function.
 * The scan from file portion reference: https://stackoverflow.com/questions/55427308/scaning-qrcode-from-image-not-from-camera-using-zxing
 */

public class QrCodeFromFile {
    private static final String TAG = QrCodeFromFile.class.getSimpleName();

    private Activity activity;
    private Collection<String> desiredBarcodeFormats;


    public QrCodeFromFile(Activity activity) {
        this.activity = activity;
    }

    public QrCodeFromFile setDesiredBarcodeFormats(Collection<String> desiredBarcodeFormats) {
        this.desiredBarcodeFormats = desiredBarcodeFormats;
        return this;
    }

    public final void initiateFileScan() {

        // TODO Replace startActivityForResult with Androidx Activity Result APIs
        this.activity.startActivityForResult(this.createFileScanIntent(), Constants.ZXING_FILE_REQ_CODE);

    }

    public Intent createFileScanIntent() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setDataAndType( android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");

        return pickIntent;
    }

    private static List<String> list(String... values) {
        return Collections.unmodifiableList(Arrays.asList(values));
    }

    public void scanFile() { // Copied (and slightly modified) from AndroidBarcode.scan()
        actuallyStartScanFile();
    }

    private void actuallyStartScanFile() {
        new QrCodeFromFile(activity)
                .setDesiredBarcodeFormats(list("QR_CODE", "CODE_128"))
                .initiateFileScan();
    }

}
