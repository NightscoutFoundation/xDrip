package com.eveningoutpost.dexdrip.insulin.shared;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.PenData;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import java.util.ArrayList;
import java.util.List;

import static com.eveningoutpost.dexdrip.models.JoH.roundDouble;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPenEntry.ID_INPEN;

/**
 * jamorham
 *
 * Detect priming doses from pen data based on preferences specification
 */

public class PrimeDetection {

    private static final String TAG = "PrimeDetection";
    private static final double INVALID_PRIME = -10000d;
    private static final int ACCURACY_PLACES = 2;
    private static final boolean D = false;

    public static synchronized List<List<PenData>> classify() {

        UserError.Log.d(TAG, "Classify called");

        final long now = JoH.tsl();
        final List<PenData> list = PenData.getAllRecordsBetween(now - Constants.DAY_IN_MS * 3, now);

        final List<PenData> primes = new ArrayList<>(list.size());
        final List<PenData> doses = new ArrayList<>(list.size());
        final List<PenData> rewinds = new ArrayList<>();


        String penType = "n/a";
        String penMac = "n/a";

        boolean detect_primes = false;
        double prime_units = INVALID_PRIME;
        long prime_gap_ms = -1;
        boolean orphan_prime_is_prime = true;
        PenData primeCandidate = null;

        for (final PenData pd : list) {

            if (!penType.equalsIgnoreCase(pd.type)) {

                switch (pd.type) {
                    case ID_INPEN:
                        detect_primes = Pref.getBooleanDefaultFalse("inpen_detect_priming");
                        prime_units = Pref.getStringToDouble("inpen_prime_units", INVALID_PRIME);
                        prime_gap_ms = (long) (Pref.getStringToDouble("inpen_prime_minutes", -1) * Constants.MINUTE_IN_MS);
                        orphan_prime_is_prime = Pref.getBoolean("inpen_orphan_primes_ignored", true);
                        break;
                    default:
                        UserError.Log.e(TAG, "Unknown pen type: " + pd.type);
                        prime_units = INVALID_PRIME;
                        prime_gap_ms = -1; // do we need to do this?
                        orphan_prime_is_prime = true;
                        break;
                }

                UserError.Log.d(TAG, "Starting processing of pen type: " + pd.type + " with prime units of: " + prime_units);
                penType = pd.type;
            }

            if (!penMac.equalsIgnoreCase(pd.mac)) {
                UserError.Log.d(TAG, "Starting processing of pen mac: " + pd.mac);
                penMac = pd.mac;
                primeCandidate = null;
            }

            if (pd.units < 0) {
                UserError.Log.d(TAG, "Pen rewind: " + pd.brief());
                rewinds.add(pd);
                continue;
            }

            if (primeCandidate != null) {
                final long timeDifference = pd.timestamp - primeCandidate.timestamp;
                if (timeDifference > 0 && timeDifference <= prime_gap_ms) {
                    UserError.Log.d(TAG, "primeCandidate is suitable prime: " + JoH.niceTimeScalar(timeDifference));
                    primes.add(primeCandidate);
                    primeCandidate = null;
                    doses.add(pd); // store current value as cannot also be prime
                    UserError.Log.d(TAG, "Regular dose following prime: " + pd.brief());
                    continue;
                } else {
                    if (orphan_prime_is_prime) {
                        primes.add(primeCandidate);
                        UserError.Log.d(TAG, "Orphan prime candidate treated as prime: " + primeCandidate.brief());
                    } else {
                        doses.add(primeCandidate);
                        UserError.Log.d(TAG, "Orphan prime candidate treated as dose: " + primeCandidate.brief());
                    }
                }
                primeCandidate = null;
            }

            if (detect_primes && roundDouble(pd.units, ACCURACY_PLACES) == roundDouble(prime_units, ACCURACY_PLACES)) {
                UserError.Log.d(TAG, "Possible Prime dose: " + pd.brief());
                primeCandidate = pd;
            } else {
                if (D) UserError.Log.d(TAG, "Regular dose: " + pd.brief());
                doses.add(pd);
            }

        } // per record

        // final one if last state was to mark a prime candidate
        if (primeCandidate != null) {
            if (JoH.msSince(primeCandidate.timestamp) > prime_gap_ms) {
                if (orphan_prime_is_prime) {
                    primes.add(primeCandidate);
                    UserError.Log.d(TAG, "Trailing Orphan prime candidate treated as prime: " + primeCandidate.brief());
                } else {
                    doses.add(primeCandidate);
                    UserError.Log.d(TAG, "Trailing Orphan prime candidate treated as dose: " + primeCandidate.brief());
                }
            } else {
                // treat as temporal
                UserError.Log.d(TAG, "Temporal prime candidate: " + primeCandidate.brief());
            }
        }

        final List<List<PenData>> results = new ArrayList<>();
        results.add(primes);
        results.add(doses);
        results.add(rewinds);
        return results;
    }

}
