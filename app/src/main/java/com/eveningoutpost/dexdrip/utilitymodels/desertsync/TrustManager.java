package com.eveningoutpost.dexdrip.utilitymodels.desertsync;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utils.CipherUtils;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

public class TrustManager {

    private static final String TAG = "DesertTrustManager";
    private static String CERTIFICATE_PIN = "41e3ef040bc7befa7bcf482303ce8faedc801b41ba348f5531c41e8d9b713d18"; // localhost_cert

    // allow any certificate chain at all - no verification
    private static final X509TrustManager[] naiveTrustManager = new X509TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }
    };

    static X509TrustManager getNaiveTrustManager() {
        return naiveTrustManager[0];
    }

    static SSLSocketFactory getSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, naiveTrustManager, new java.security.SecureRandom());
        return sslContext.getSocketFactory();
    }

    // verify talking to something that looks like an xDrip+ instance
    static HostnameVerifier getXdripHostVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(final String hostname, final SSLSession session) {
                try {
                    final String peerCertificateHash = CipherUtils.getSHA256(session.getPeerCertificateChain()[0].getEncoded());
                    if (CERTIFICATE_PIN.equals(peerCertificateHash)) {
                        return true;
                    } else {
                        UserError.Log.e(TAG, "Remote https certificate doesn't match! " + peerCertificateHash);
                        return false;
                    }

                } catch (Exception e) {
                    UserError.Log.e(TAG, "Unable to verify host: " + e);
                    return false;
                }
            }
        };
    }

}
