package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;

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
                paramA = 3100;
                paramB = 3600;
                break;

            case "5917":
                paramA = 3000;
                paramB = 3500;
                break;

            case "5931":
                paramA = 2900;
                paramB = 3400;
                break;

            case "5937":
                paramA = 2800;
                paramB = 3300;
                break;

            case "5951":
                paramA = 3100;
                paramB = 3500;
                break;

            case "5955":
                paramA = 3000;
                paramB = 3400;
                break;

            case "7171":
                paramA = 2700;
                paramB = 3300;
                break;

            case "9117":
                paramA = 2700;
                paramB = 3200;
                break;

            case "9159":
                paramA = 2600;
                paramB = 3200;
                break;

            case "9311":
                paramA = 2600;
                paramB = 3100;
                break;

            case "9371":
                paramA = 2500;
                paramB = 3100;
                break;

            case "9515":
                paramA = 2500;
                paramB = 3000;
                break;

            case "9551":
                paramA = 2400;
                paramB = 3000;
                break;

            case "9577":
                paramA = 2400;
                paramB = 2900;
                break;

            case "9713":
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
