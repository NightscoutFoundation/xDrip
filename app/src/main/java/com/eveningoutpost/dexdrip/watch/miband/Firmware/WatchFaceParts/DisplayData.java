package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;

import java.io.IOException;
import java.io.InputStream;

import static com.eveningoutpost.dexdrip.Models.JoH.hourMinuteString;

public class DisplayData {
    String bgValueText;
    Bitmap arrowBitmap;
    String timeStampText;
    String unitized_delta;
    boolean strike_through;
    boolean isHigh;
    boolean isLow;

    boolean isGraphEnabled = false;
    int graphHours = 6;
    boolean showTreatment = false;
    String iob = "";


    public static Builder newBuilder(BestGlucose.DisplayGlucose dg, AssetManager assetManager) throws IOException {
        return new DisplayData().new Builder(dg, assetManager);
    }

    public String getBgValueText() {
        return bgValueText;
    }

    public Bitmap getArrowBitmap() {
        return arrowBitmap;
    }

    public String getTimeStampText() {
        return timeStampText;
    }

    public String getUnitized_delta() {
        return unitized_delta;
    }

    public boolean isStrike_through() {
        return strike_through;
    }

    public boolean isHigh() {
        return isHigh;
    }

    public boolean isLow() {
        return isLow;
    }

    public int getGraphHours() {
        return graphHours;
    }

    public boolean isShowTreatment() {
        return showTreatment;
    }

    public String getIob() {
        return iob;
    }

    public boolean isGraphEnabled() {
        return isGraphEnabled;
    }

    public class Builder {

        private AssetManager assetManager;
        private BestGlucose.DisplayGlucose dg;

        public Builder(BestGlucose.DisplayGlucose dg, AssetManager assetManager) throws IOException {
            this.dg = dg;
            this.assetManager = assetManager;

            if (dg.timestamp > Constants.DAY_IN_MS)
                DisplayData.this.timeStampText = "at " + hourMinuteString(dg.timestamp);
            else
                DisplayData.this.timeStampText = JoH.niceTimeScalar(JoH.msSince(dg.timestamp)) + " ago";

            InputStream arrowStream = assetManager.open("miband_watchface_parts/" + dg.delta_name + ".png");
            DisplayData.this.arrowBitmap = BitmapFactory.decodeStream(arrowStream);
            arrowStream.close();

            DisplayData.this.bgValueText = dg.unitized;
            DisplayData.this.unitized_delta = dg.unitized_delta_no_units;
            DisplayData.this.strike_through = dg.isStale();
            DisplayData.this.isHigh = dg.isHigh();
            DisplayData.this.isLow = dg.isLow();
        }

        public Builder enableGraph(int graphHours) {
            DisplayData.this.isGraphEnabled = true;
            DisplayData.this.graphHours = graphHours;
            return this;
        }

        public Builder setShowTreatment(boolean showTreatment) {
            DisplayData.this.showTreatment = showTreatment;

            return this;
        }

        public Builder setIoB(String iob) {
            String status = ((iob.length() > 0) ? ("IoB: " + iob) : "");
            DisplayData.this.iob = status.replace(",", ".");

            return this;
        }

        public DisplayData build() {
            return DisplayData.this;
        }
    }
}
