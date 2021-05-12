package com.eveningoutpost.dexdrip.webservices;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by jamorham on 09/01/2018.
 *
 * Create a SSL socket which will present a certificate and use a private key
 * requires a BKS keystore passed in an input stream
 *
 */

class SSLServerSocketHelper {

    static SSLServerSocketFactory makeSSLSocketFactory(InputStream keystoreStream, char[] passphrase) throws IOException {
        try {
            final KeyStore keystore = KeyStore.getInstance("BKS"); // bks should be supported - stream needs to be in this format
            keystore.load(keystoreStream, passphrase);

            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, passphrase);

            return makeSSLSocketFactory(keystore, keyManagerFactory);
        } catch (Exception e) {
            // simplify exception handling
            throw new IOException(e.getMessage());
        }
    }

    private static SSLServerSocketFactory makeSSLSocketFactory(KeyStore loadedKeyStore, KeyManagerFactory loadedKeyFactory) throws IOException {
        try {
            return makeSSLSocketFactory(loadedKeyStore, loadedKeyFactory.getKeyManagers());
        } catch (Exception e) {
            // simplify exception handling
            throw new IOException(e.getMessage());
        }
    }

    private static SSLServerSocketFactory makeSSLSocketFactory(KeyStore loadedKeyStore, KeyManager[] keyManagers) throws IOException {
        final SSLServerSocketFactory factory;
        try {
            final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(loadedKeyStore);

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagerFactory.getTrustManagers(), null);

            factory = sslContext.getServerSocketFactory();
        } catch (Exception e) {
            // simplify exception handling
            throw new IOException(e.getMessage());
        }
        return factory;
    }

}
