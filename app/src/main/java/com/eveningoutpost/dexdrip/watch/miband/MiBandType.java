package com.eveningoutpost.dexdrip.watch.miband;

public enum MiBandType {
    MI_BAND2(Const.MIBAND_NAME_2),
    MI_BAND3(Const.MIBAND_NAME_3),
    MI_BAND3_1(Const.MIBAND_NAME_3_1),
    MI_BAND4(Const.MIBAND_NAME_4),
    MI_BAND5(Const.MIBAND_NAME_5),
    AMAZFIT5(Const.AMAZFIT5_NAME),
    AMAZFITGTR(Const.AMAZFITGTR_NAME),
    AMAZFITGTR_LITE(Const.AMAZFITGTR_LITE_NAME),
    UNKNOWN("");

    private final String text;

    /**
     * @param text
     */
    MiBandType(final String text) {
        this.text = text;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return text;
    }

    public static MiBandType fromString(String text) {
        for (MiBandType b : MiBandType.values()) {
            if (b.text.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return UNKNOWN;
    }

    public static boolean supportGraph(MiBandType bandType) {
        return  bandType == MiBandType.MI_BAND4 ||
                bandType == MiBandType.MI_BAND5 ||
                bandType == MiBandType.AMAZFIT5 ||
                bandType == MiBandType.AMAZFITGTR ||
                bandType == MiBandType.AMAZFITGTR_LITE;
    }

    public static boolean supportNightMode(MiBandType bandType) {
        return bandType == MiBandType.MI_BAND3 ||
                bandType == MiBandType.MI_BAND3_1 ||
                bandType == MiBandType.MI_BAND4 ||
                bandType == MiBandType.MI_BAND5 ||
                bandType == MiBandType.AMAZFIT5 ||
                bandType == MiBandType.AMAZFITGTR ||
                bandType == MiBandType.AMAZFITGTR_LITE;
    }
    public static boolean supportPairingKey(MiBandType bandType) {
        return bandType == MiBandType.MI_BAND4 ||
                bandType == MiBandType.MI_BAND5 ||
                bandType == MiBandType.AMAZFIT5 ||
                bandType == MiBandType.AMAZFITGTR ||
                bandType == MiBandType.AMAZFITGTR_LITE;
    }
}