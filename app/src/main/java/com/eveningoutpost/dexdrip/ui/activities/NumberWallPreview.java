package com.eveningoutpost.dexdrip.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.ColorCache;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PrefsViewImpl;
import com.eveningoutpost.dexdrip.utilitymodels.PrefsViewString;
import com.eveningoutpost.dexdrip.utilitymodels.Unitized;
import com.eveningoutpost.dexdrip.adapters.ObservableBackground;
import com.eveningoutpost.dexdrip.databinding.ActivityNumberWallPreviewBinding;
import com.eveningoutpost.dexdrip.ui.LockScreenWallPaper;
import com.eveningoutpost.dexdrip.ui.NumberGraphic;
import com.eveningoutpost.dexdrip.ui.dialog.ColorPreferenceDialog;
import com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil;
import com.eveningoutpost.dexdrip.utils.FileUtils;
import com.eveningoutpost.dexdrip.utils.SdcardImportExport;

import java.io.File;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import static com.eveningoutpost.dexdrip.ui.NumberGraphic.isLockScreenBitmapTiled;
import static com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil.getScreenHeight;
import static com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil.getScreenWidth;

/**
 * jamorham
 * <p>
 * Configuration page for Number Wall feature
 */

public class NumberWallPreview extends AppCompatActivity {

    private static final String TAG = "NumberWallPreview";
    private static final String FOLDER_NAME = "numberWall";
    private static final int LOAD_IMAGE_RESULTS = 35021;
    private static final int ASK_FILE_PERMISSION = 35020;

    private ActivityNumberWallPreviewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNumberWallPreviewBinding.inflate(getLayoutInflater());
        binding.setPrefs(new PrefsViewImpl().setRefresh(() -> {
            try {
                binding.getVm().refreshBitmap();
            } catch (NullPointerException e) {
                //
            }
        }));
        binding.setVm(new ViewModel(this));
        binding.setSprefs(new PrefsViewStringSnapDefaultsRefresh(binding.getVm()));
        setContentView(binding.getRoot());
        JoH.fixActionBar(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOAD_IMAGE_RESULTS && resultCode == RESULT_OK && data != null) {

            final Uri pickedImage = data.getData();
            if (pickedImage != null) {
                File destinationFolder = new File(this.getFilesDir().getAbsolutePath() + File.separator + FOLDER_NAME);
                String path = BitmapUtil.copyBackgroundImage(pickedImage, destinationFolder);
                binding.getSprefs().put(ViewModel.PREF_numberwall_background, path);
                binding.getVm().refreshBitmap();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ASK_FILE_PERMISSION) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                binding.getVm().folderImageButtonClick(); // must be the only functionality which calls for permission
            } else {
                JoH.static_toast_long(this, "Cannot choose file without storage permission");
            }
        }
    }


    // bound view model
    @RequiredArgsConstructor
    public class ViewModel {

        public static final String PREF_numberwall_x_param = "numberwall_x_param";
        public static final String PREF_numberwall_y_param = "numberwall_y_param";
        public static final String PREF_numberwall_s_param = "numberwall_s_param";
        public static final String PREF_numberwall_multi_param = "numberwall_multi_param";
        public static final String PREF_numberwall_background = "numberwall_background";

        private final Activity activity;
        public ObservableBackground background = new ObservableBackground();

        {
            refreshBitmap();
        }

        public void folderImageButtonClick() {
            if (Pref.getString(PREF_numberwall_background, null) == null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    if (SdcardImportExport.checkPermissions(activity, true, ASK_FILE_PERMISSION)) {
                        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("image/*");
                        startActivityForResult(intent, LOAD_IMAGE_RESULTS);
                    } else {
                        if (JoH.ratelimit("need-file-permission", 10)) {
                            //JoH.static_toast_short("Need file permission");
                        }
                    }
                }
            } else {
                binding.getSprefs().put(PREF_numberwall_background, null);
                File backgroundFolder = new File(activity.getFilesDir().getAbsolutePath() + File.separator + FOLDER_NAME);
                FileUtils.deleteDirWithFiles(backgroundFolder);
                refreshBitmap();
            }
        }

        public void paletteImageButtonClick() {
            ColorPreferenceDialog.pick(NumberWallPreview.this, ColorCache.X.color_number_wall.getInternalName(), "Text Color", this::refreshBitmap);
        }

        public boolean paletteImageButtonLongClick(View v) {
            ColorPreferenceDialog.pick(NumberWallPreview.this, ColorCache.X.color_number_wall_shadow.getInternalName(), "Shadow Color", this::refreshBitmap);
            return false;
        }


        // create demo bitmap
        public void refreshBitmap() {
            final BestGlucose.DisplayGlucose dg = new BestGlucose.DisplayGlucose();
            dg.delta_arrow = "\u21C5"; // ⇅
            dg.unitized = Unitized.usingMgDl() ? "123" : "12.3";
            final Bitmap bitmap = BitmapUtil.getTiled(NumberGraphic.getLockScreenBitmap(dg.unitized, dg.delta_arrow, false), getScreenWidth(), getScreenHeight(), isLockScreenBitmapTiled(), Pref.getString(ViewModel.PREF_numberwall_background, null));
            background.setBitmap(bitmap);
            Inevitable.task("refresh-lock-number-wall", 500, LockScreenWallPaper::setIfEnabled);
        }
    }

    // transparent preferences binding with below minimum defaults snaps to default
    @AllArgsConstructor
    public class PrefsViewStringSnapDefaultsRefresh extends PrefsViewString {

        private final ViewModel model;

        @Override
        public String get(Object key) {
            final String original = super.get(key);
            String result = original;
            Integer value = 0;
            try {
                value = Integer.parseInt(result);
            } catch (NumberFormatException e) {
                //
            }
            switch ((String) key) {
                case ViewModel.PREF_numberwall_x_param:
                    if (value < 30) {
                        result = "30";
                    }
                    break;
                case ViewModel.PREF_numberwall_y_param:
                    if (value < 30) {
                        result = "30";
                    }
                    break;
                case ViewModel.PREF_numberwall_s_param:
                    if (value < 10) {
                        result = "10";
                    }
                    break;
            }

            if (!result.equals(original)) {
                UserError.Log.d(TAG, "Snapped: " + key + " " + original + " -> " + result);
                put((String) key, result);
            }
            return result;
        }

        @Override
        public String put(String key, String value) {
            super.put(key, value);
            model.refreshBitmap();
            return value;
        }
    }
}
