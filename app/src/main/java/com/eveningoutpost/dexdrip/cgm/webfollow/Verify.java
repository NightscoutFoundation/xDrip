package com.eveningoutpost.dexdrip.cgm.webfollow;

import static com.eveningoutpost.dexdrip.models.JoH.hexStringToByteArray;

import com.eveningoutpost.dexdrip.models.UserError;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

import lombok.val;

/**
 * JamOrHam
 */

public class Verify {

    private static final String VERIFY_KEY = "308201B63082012B06072A8648CE3804013082011E028181008CC" +
            "4FFA85B0F74EEFBBC8120E15DFCA98EE0B5AF77A41A2FD6DBDEC47B062DB1A3113EA0FCCDD0FA55B430349" +
            "CB9960C009CFE750314CD663CF5BD3712EB2175689D178E2791E3DE2209DB78F91E7EE9B8A55EC174AC693" +
            "DFAC55B7FF174C21362E5ACDDD7DDF1E1025611AA867DCA012FCD2CDE20B745CB5AE014B7E06F876F02150" +
            "0BAD5DFB8F4C0BC817BA9FCC7B5D4BCC5E9B9A91B028180334A5D853B3B7BFAA69274369E21351E30EA93B" +
            "21FFEA2875DA8891A035C6B8B5E78DF4A7F892F5C0A5491359F82F61863A07582569115D27CAE812CABA70" +
            "58E30E20E91892D5A9C9CB3897AEDD2B06CB074870416FF0792325E303827290A5EA60CB0F045D7C3F71C7" +
            "9BFB8EFB3DEBF0301B36D7E8A7946513E5816EF8305640381840002818044081F98AD4BB93DCBBC1CC6C6F" +
            "3574F0CE9D45758471DCE6C76151885B8E9FF2FEDF50A976DA3347A300A0F6E4B5C58A41C327A5E7A316C9" +
            "4B94EC076028ABD5D8C2903BC312EE0D3090193A07C48EC3D7B465E4F637BD3A045051EFE5450EC7CDB181" +
            "1FC2AD6853612742A1AEDC3943CE34842EBE4926911902AF09C60312B";

    private static final String TAG = Verify.class.getSimpleName();

    private static PublicKey getPublicKey() {
        try {
            return KeyFactory.getInstance("DSA")
                    .generatePublic(new X509EncodedKeySpec(hexStringToByteArray(VERIFY_KEY)));
        } catch (Exception e) {
            UserError.Log.e(TAG, "Failed to get public key: " + e);
            return null;
        }
    }

    public static boolean verify(final byte[] data, final byte[] signature) {
        try {
            val signCheck = Signature.getInstance("SHA256withDSA");
            signCheck.initVerify(getPublicKey());
            signCheck.update(data);
            return signCheck.verify(signature);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Verification failed due to exception: " + e);
            return false;
        }
    }
}
