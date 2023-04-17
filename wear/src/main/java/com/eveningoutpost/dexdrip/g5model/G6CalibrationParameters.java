package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

import lombok.Getter;

public class G6CalibrationParameters {

    public static final String PREF_CURRENT_CODE = "G6-Current-Sensor-Code";

    @Getter
    private final String code;
    @Getter
    private final int paramA;
    @Getter
    private final int paramB;


    public G6CalibrationParameters(String code) {

        this.code = code;

        switch (code) {

            // special null code
            case "0000":
                paramA = 1;
                paramB = 0;
                break;

            case "5915":
            case "9759":
                paramA = 3100;
                paramB = 3600;
                break;

            case "5917":
            case "9357":
                paramA = 3000;
                paramB = 3500;
                break;

            case "5931":
            case "9137":
                paramA = 2900;
                paramB = 3400;
                break;

            case "5937":
            case "7197":
                paramA = 2800;
                paramB = 3300;
                break;

            case "5951":
            case "9517":
                paramA = 3100;
                paramB = 3500;
                break;

            case "5955":
            case "9179":
                paramA = 3000;
                paramB = 3400;
                break;

            case "7171":
            case "7539":
                paramA = 2700;
                paramB = 3300;
                break;

            case "9117":
            case "7135":
                paramA = 2700;
                paramB = 3200;
                break;

            case "9159":
            case "5397":
                paramA = 2600;
                paramB = 3200;
                break;

            case "9311":
            case "5391":
                paramA = 2600;
                paramB = 3100;
                break;

            case "9371":
            case "5375":
                paramA = 2500;
                paramB = 3100;
                break;

            case "9515":
            case "5795":
                paramA = 2500;
                paramB = 3000;
                break;

            case "9551":
            case "5317":
                paramA = 2400;
                paramB = 3000;
                break;

            case "9577":
            case "5177":
                paramA = 2400;
                paramB = 2900;
                break;

            case "9713":
            case "5171":
                paramA = 2300;
                paramB = 2900;
                break;

            default:
                paramA = -1;
                paramB = -1;
        }

    }

    public boolean isValid() {
        return paramA > 0;
    }

    public boolean isNullCode() {
        return isValid() && paramB == 0;
    }

    public static boolean checkCode(String code) {
        return new G6CalibrationParameters(code).isValid();
    }

    public static String getCurrentSensorCode() {
        final String code = PersistentStore.getString(PREF_CURRENT_CODE);
        return code.equals("") ? null : code;
    }

    public static void setCurrentSensorCode(String code) {
        if (checkCode(code)) {
            PersistentStore.setString(PREF_CURRENT_CODE, code);
        } else {
            PersistentStore.setString(PREF_CURRENT_CODE, "");
            throw new RuntimeException("Invalid sensor code: " + code);
        }
    }


}
