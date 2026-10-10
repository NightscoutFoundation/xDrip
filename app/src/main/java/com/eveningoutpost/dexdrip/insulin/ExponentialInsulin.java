package com.eveningoutpost.dexdrip.insulin;

import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.google.gson.JsonObject;

import java.util.ArrayList;

// Oref biexponential insulin model, matching the OpenAPS/AAPS implementation.
// Activity curve: (s/τ²) × t × (1 - t/td) × exp(-t/τ)
// IOB curve:      1 - s(1-a) × ((t²/(τ·td·(1-a)) - t/τ - 1) × exp(-t/τ) + 1)
// Both formulas naturally produce a smooth bell curve that reaches zero at exactly DIA.
public class ExponentialInsulin extends Insulin {
    public static Boolean useExponentialModel() {
        return Pref.getBoolean("exponential_model_enabled", false);
    }

    private final double tp; // peak time (minutes)
    private final double td; // total duration / DIA (minutes)
    private final double tau;
    private final double a;
    private final double s;

    // For use with InsulinManager, loaded from insulin_profiles.json.
    // JSON data fields: "peak" (minutes), "duration" (minutes).
    public ExponentialInsulin(String n, String dn, ArrayList<String> ppn, String c, JsonObject curveData) {
        super(n, dn, ppn, c, curveData);
        tp = curveData.get("peak").getAsDouble();
        td = curveData.get("duration").getAsDouble();
        tau = computeTau(tp, td);
        a = 2.0 * tau / td;
        s = 1.0 / (1.0 - a + (1.0 + a) * Math.exp(-td / tau));
        maxEffect = (long) td;
    }

    // For direct use in the legacy (non-MultipleInsulins) path.
    public ExponentialInsulin(double peakMinutes, double diaMinutes) {
        super("oref", "Oref", new ArrayList<String>(), "U100", new JsonObject());
        tp = peakMinutes;
        td = diaMinutes;
        tau = computeTau(tp, td);
        a = 2.0 * tau / td;
        s = 1.0 / (1.0 - a + (1.0 + a) * Math.exp(-td / tau));
        maxEffect = (long) td;
    }

    private static double computeTau(double tp, double td) {
        return tp * (1.0 - tp / td) / (1.0 - 2.0 * tp / td);
    }

    // Returns the activity rate (1/min per unit), integrates to 1 over [0, td].
    @Override
    public double calculateActivity(long t) {
        if (t <= 0 || t >= (long) td) return 0;
        final double tD = t;
        return (s / (tau * tau)) * tD * (1.0 - tD / td) * Math.exp(-tD / tau);
    }

    // Returns the remaining IOB fraction [0, 1].
    @Override
    public double calculateIOB(long t) {
        if (t <= 0) return 1.0;
        if (t >= (long) td) return 0;
        final double tD = t;
        return 1.0 - s * (1.0 - a) * ((tD * tD / (tau * td * (1.0 - a)) - tD / tau - 1.0) * Math.exp(-tD / tau) + 1.0);
    }
}
