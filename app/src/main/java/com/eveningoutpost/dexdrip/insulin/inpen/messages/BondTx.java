package com.eveningoutpost.dexdrip.insulin.inpen.messages;

import android.annotation.SuppressLint;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.insulin.inpen.Constants;

import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// jamorham

public class BondTx extends BaseTx {

    private static final String TAG = "InPenBondTx";

    public BondTx(final float parameter, final byte[] flagBytes) {

        try {
            init(16);
            final byte[] source = data.array();
            new SecureRandom().nextBytes(source);
            final byte[] bytes = new byte[]{(byte) ((int) (parameter * Constants.MULTIPLIER))};
            final AlgorithmParameterSpec parameterSpec = new IvParameterSpec(source);
            final SecretKeySpec secretKeySpec = new SecretKeySpec(format(new ChalTx(flagBytes).getBytes()), "AES");
            @SuppressLint("GetInstance") final Cipher aesInstance = Cipher.getInstance("AES/CBC/NoPadding");
            aesInstance.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec);
            data.put(aesInstance.doFinal(prepare(source, bytes)), 0, 16);
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Got exception making BondTX: " + e);
        }
    }

}
