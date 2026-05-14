package com.eveningoutpost.dexdrip.cgm.dex;

import android.app.Activity;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.ui.dialog.GenericConfirmDialog;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.eveningoutpost.dexdrip.models.JoH.runOnUiThread;
import static com.eveningoutpost.dexdrip.xdrip.gs;

import lombok.val;

// JamOrHam

// Helper routines to validate and correct dex transmitter ids

public class TxIdHelper {

    private static final int MAX_TX_ID_LENGTH = 6;

    // g6 regular
    private static final Pattern VALID_SERIAL_NUMBER_PATTERN = Pattern.compile(
            "^[0-9A-HJ-NP-UVWXYZ]{6}$" // TODO improve validation logic for V + Z
    );

    // g6 extended
    private static final Pattern EXTENDED_SERIAL_NUMBER_PATTERN = Pattern.compile(
            "^[0-9A-Z]{6}-[0-9]$"
    );

    // g7 invalid codes
    private static final String[] INVALID_SHORT_PAIRING_CODES = {
            "0006", "0008", "0009", "0060", "0066", "0068", "0069", "0080",
            "0086", "0088", "0089", "0090", "0096", "0098", "0099", "0600",
            "0606", "0608", "0609", "0660", "0666", "0668", "0669", "0680",
            "0686", "0688", "0689", "0696", "0698", "0699", "0800", "0806",
            "0808", "0809", "0860", "0866", "0868", "0869", "0886", "0888",
            "0889", "0890", "0896", "0898", "0899", "0900", "0906", "0908",
            "0909", "0966", "0968", "0969", "0980", "0986", "0988", "0989",
            "0990", "0996", "0998", "0999", "6000", "6006", "6008", "6060",
            "6066", "6068", "6069", "6080", "6086", "6088", "6089", "6090",
            "6096", "6098", "6099", "6600", "6606", "6608", "6609", "6660",
            "6666", "6668", "6669", "6680", "6686", "6688", "6689", "6690",
            "6696", "6698", "6800", "6806", "6808", "6809", "6860", "6866",
            "6868", "6869", "6880", "6886", "6888", "6890", "6896", "6898",
            "6899", "6900", "6906", "6908", "6909", "6960", "6966", "6968",
            "6980", "6986", "6988", "6989", "6990", "6996", "6998", "6999",
            "8000", "8006", "8009", "8060", "8066", "8068", "8069", "8080",
            "8086", "8088", "8089", "8090", "8096", "8098", "8099", "8600",
            "8606", "8608", "8609", "8660", "8666", "8668", "8669", "8680",
            "8686", "8688", "8689", "8690", "8696", "8699", "8800", "8806",
            "8808", "8809", "8860", "8866", "8868", "8869", "8880", "8886",
            "8889", "8890", "8896", "8898", "8899", "8900", "8906", "8908",
            "8909", "8960", "8966", "8969", "8980", "8986", "8988", "8989",
            "8990", "8996", "8998", "8999", "9000", "9008", "9009", "9060",
            "9066", "9068", "9069", "9080", "9086", "9088", "9089", "9090",
            "9096", "9098", "9099", "9600", "9606", "9608", "9609", "9660",
            "9666", "9668", "9669", "9680", "9686", "9688", "9689", "9690",
            "9698", "9699", "9800", "9806", "9808", "9809", "9860", "9866",
            "9868", "9869", "9880", "9888", "9889", "9890", "9896", "9898",
            "9899", "9900", "9906", "9908", "9909", "9960", "9968", "9969",
            "9980", "9986", "9988", "9989", "9990", "9996", "9998", "9999"
    };

    // g6 characters we can substitute
    private static final Map<Character, Character> disallowedCharacters = Map.of(
            'I', '1',
            'O', '0'
            //   'V', 'U' // allowed on some versions - check
         //   'Z', '2' // allowed on some versions 6+
    );

    // return true if the input string is a valid short pairing code
    public static boolean isValidShortPairingCode(final String code) {
        // Basic validation
        if (code == null || code.length() != 4 || !code.matches("\\d{4}")) {
            return false;
        }

        // Check against invalid codes
        for (String invalid : INVALID_SHORT_PAIRING_CODES) {
            if (code.equals(invalid)) {
                return false;
            }
        }

        return true;
    }

   // replace invalid characters in the input string with valid ones based on the map
    static String replaceInvalidChars(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        input = input.toUpperCase(Locale.ENGLISH);

        StringBuilder updated = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (disallowedCharacters.containsKey(ch)) {
                updated.append(disallowedCharacters.get(ch));
            } else {
                updated.append(ch);
            }
        }

        return updated.toString();
    }

    // Find the indexes where characters from the map's keys are found in the input string.
    static List<Integer> findIndexesOfMapKeys(String input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }

        input = input.toUpperCase(Locale.ENGLISH);

        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < input.length(); i++) {
            if (disallowedCharacters.containsKey(input.charAt(i))) {
                indexes.add(i);
            }
        }

        return indexes;
    }


    // validate the g6 transmitter id against the allowed character set and length
    public static boolean isValidSerialNumber(final String serial) {
        if (serial == null) {
            return false;
        }
        return VALID_SERIAL_NUMBER_PATTERN.matcher(serial).matches();
    }

    // check whether this is an g6 extended serial number, eg appended with -1
    public static boolean isExtendSerialNumber(final String serial) {
        if (serial == null) {
            return false;
        }
        return EXTENDED_SERIAL_NUMBER_PATTERN.matcher(serial).matches();
    }

    // fix an extended serial number by removing the suffix
    private static String fixExtendedSerial(String serial) {
        return isExtendSerialNumber(serial)
                ? serial.replaceAll("-\\d$", "")
                : serial;
    }

    // return true if we think we can fix this serial number
    public static boolean canWeFixSerialNumber(final String serial) {
        return isValidSerialNumber(getFixedSerialNumber(serial));
    }

    // get a fixed version of the g6 serial number or return null if invalid length
    public static String getFixedSerialNumber(String serial) {
        serial = fixExtendedSerial(serial);
        if (serial == null || serial.length() != MAX_TX_ID_LENGTH) {
            return null;
        }
        return replaceInvalidChars(serial);
    }

    // get a spannable string with highlights showing either errors or fixes depending on whether useOriginal is set
    private static SpannableString formatSpannableString(final String originalSerial, final boolean useOriginal, final int highlightColor) {
        val fixedSerial = getFixedSerialNumber(originalSerial);
        val spannable = new SpannableString(useOriginal ? originalSerial : fixedSerial);

        val indexes = findIndexesOfMapKeys(originalSerial);
        // for oversized original
        for (int index = MAX_TX_ID_LENGTH; index < spannable.length(); index++) {
            indexes.add(index);
        }
        // for each index, highlight the character
        for (int index : indexes) {
            if (index >= 0 && index < spannable.length()) {
                spannable.setSpan(
                        new ForegroundColorSpan(highlightColor),
                        index, Math.min(index + 1, spannable.length()),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
        return spannable;
    }

    // get the fixed version of a serial number with highlights showing the differences
    public static SpannableString getPatchedSpannableSerial(final String originalSerial) {
        return formatSpannableString(originalSerial, false, 0xFF4CAF50);
    }

    // get the original version of the serial number with highlights showing errors
    public static SpannableString getOriginalSpannableSerial(final String fixedSerial) {
        return formatSpannableString(fixedSerial, true, 0xFFEF4C50);
    }

    // whether an entered transmitter id is okay for g6 or g7
    public static boolean isTransmitterIdOkay(final String txid) {
        return isValidShortPairingCode(txid) || isValidSerialNumber(txid);
    }

    // whether we can use and or fix the supplied transmitter id
    public static boolean isTransmitterIdOkayOrFixable(final String txid) {
        return isTransmitterIdOkay(txid) || canWeFixSerialNumber(txid);
    }

    // use a callback to save the transmitter id after possible dialog interaction and fixing
    public static void handleTransmitterEntry(final String serial, final Activity activity, final Consumer<String> callback) {
        if (isTransmitterIdOkay(serial)) {
            callback.accept(serial);
            return; // no further action needed
        }
        if (canWeFixSerialNumber(serial)) {
            // pop up dialog about wrong but fixable serial
            val ssb = new SpannableStringBuilder();
            ssb.append(activity.getString(R.string.you_entered));
            ssb.append("\n");
            ssb.append(getOriginalSpannableSerial(serial));
            ssb.append("\n\n");
            ssb.append(activity.getString(R.string.fixed_version));
            ssb.append("\n");
            ssb.append(getPatchedSpannableSerial(serial));
            ssb.append("\n\n");
            ssb.append(activity.getString(R.string.click));
            ssb.append(" ");
            ssb.append(activity.getString(R.string.yes)); // needs to be same string used in dialog
            ssb.append(" ");
            ssb.append(activity.getString(R.string.to_use_this_instead));

            GenericConfirmDialog.show(activity, gs(R.string.invalid_transmitter_id), ssb,
                    () -> callback.accept(getFixedSerialNumber(serial)));
        } else {
            // tell user its invalid
            GenericConfirmDialog.inform(activity, gs(R.string.invalid_transmitter_id), activity.getString(R.string.the_transmitter_id_you_have_entered_is_incorrect), null);
        }
    }

    // attach a suitable validator to an EditText
    public static void attachValidator(EditText editText) {
        if (editText == null) return;
        editText.setError(null);
        editText.post(() -> { // move to end of text
            editText.setSelection(editText.getText().length());
        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // delay after keystroke
                Inevitable.task("txid-validator", 50, () -> {
                    val serial = s.toString().trim();
                    if (serial.length() >= 6) {
                        if (!isTransmitterIdOkayOrFixable(serial)) {
                            runOnUiThread(() -> editText.setError(gs(R.string.invalid_transmitter_id)));
                        }
                    }
                });
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

        });
    }

}
