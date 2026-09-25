package com.eveningoutpost.dexdrip.plugin;

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

    private static final String VERIFY_KEY = "308201B73082012C06072A8648CE3804013082011F02818100FD7" +
            "F53811D75122952DF4A9C2EECE4E7F611B7523CEF4400C31E3F80B6512669455D402251FB593D8D58FABFC" +
            "5F5BA30F6CB9B556CD7813B801D346FF26660B76B9950A5A49F9FE8047B1022C24FBBA9D7FEB7C61BF83B5" +
            "7E7C6A8A6150F04FB83F6D3C51EC3023554135A169132F675F3AE2B61D72AEFF22203199DD14801C702150" +
            "09760508F15230BCCB292B982A2EB840BF0581CF502818100F7E1A085D69B3DDECBBCAB5C36B857B97994A" +
            "FBBFA3AEA82F9574C0B3D0782675159578EBAD4594FE67107108180B449167123E84C281613B7CF09328CC" +
            "8A6E13C167A8B547C8D28E0A3AE1E2BB3A675916EA37F0BFA213562F1FB627A01243BCCA4F1BEA8519089A" +
            "883DFE15AE59F06928B665E807B552564014C3BFECF492A038184000281806180D810C8071501556C4E3B1" +
            "D8AA842C78F5130B530BB1B9127CDE13760777E437604A5E3308C917B2AB7532857465929633206A4666D6" +
            "EC01FB1663C81C579AAA72B83BC976B735AC50EC52D59E0D02F08F2A75A0C32F86F28517A20DD510B56481" +
            "FEBB298C1F0ED00463DF69B00DF78C197176F68B44B4B3AC3F68B550AE3";

    private static final boolean debug = false;

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
            return signCheck.verify(signature)
                    || debug;
        } catch (Exception e) {
            UserError.Log.e(TAG, "Verification failed due to exception: " + e);
            return debug;
        }
    }
}