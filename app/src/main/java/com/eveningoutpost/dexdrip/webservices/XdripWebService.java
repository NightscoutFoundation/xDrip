package com.eveningoutpost.dexdrip.webservices;

import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.dagger.Singleton;
import com.eveningoutpost.dexdrip.utils.TriState;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Locale;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;


/**
 * Created by jamorham on 06/01/2018.
 * <p>
 * Provides a webservice on localhost port 17580 respond to incoming requests either for data or
 * to push events in.
 * <p>
 * Also provides a https webservice on localhost port 17581 which presents the key / certificate
 * contained in the raw resource localhost_cert.bks - More work is likely needed for a chain
 * which passes validation but this provides some structure for that or similar services.
 * <p>
 * <p>
 * Designed for watches which support only a http interface
 * <p>
 * base service adapted from android reference documentation
 */

// TODO megastatus for engineering mode

public class XdripWebService implements Runnable {

    private static final String TAG = "xDripWebService";
    private static final int MAX_RUNNING_THREADS = 15;
    private static volatile XdripWebService instance = null;
    private static volatile XdripWebService ssl_instance = null;

    private final int listenPort;
    private final boolean useSSL;
    private final AtomicInteger thread_count = new AtomicInteger();

    private boolean isRunning;
    private ServerSocket mServerSocket;

    private DateTimeFormatter rfc7231formatter;

    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            rfc7231formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O", Locale.ENGLISH);
        }
    }

    /**
     * WebServer constructor.
     */
    private XdripWebService(int port, boolean use_ssl) {
        this.listenPort = port;
        this.useSSL = use_ssl;
    }

    // start the service if needed, shut it down if not
    public static void immortality() {
        if (Pref.getBooleanDefaultFalse("xdrip_webservice")) {
            easyStart();
        } else {
            if (instance != null) {
                easyStop();
            }
        }
    }

    // cause a restart if enabled to refresh configuration settings
    public static void settingsChanged() {
        easyStop();
        immortality();
    }

    // robustly shut down and erase the instance
    private static synchronized void easyStop() {
        try {
            UserError.Log.d(TAG, "running easyStop()");
            instance.stop();
            instance = null;
            ssl_instance.stop();
            ssl_instance = null;
        } catch (NullPointerException e) {
            // concurrency issue
        }
    }

    // start up if needed
    private static synchronized void easyStart() {
        if (instance == null) {
            UserError.Log.d(TAG, "easyStart() Starting new instance");
            instance = new XdripWebService(17580, false);
            ssl_instance = new XdripWebService(17581, true);
        }
        instance.startIfNotRunning();
        ssl_instance.startIfNotRunning();
    }

    // start thread if needed
    private void startIfNotRunning() {
        if (!isRunning) {
            UserError.Log.d(TAG, "Not running so starting");
            start();
        } else {
            // UserError.Log.d(TAG, "Already running");
        }
    }

    /**
     * This method starts the web server listening to the specified port.
     */
    public void start() {
        isRunning = true;
        new Thread(this).start();
    }

    /**
     * This method stops the web server
     */
    public synchronized void stop() {
        try {
            isRunning = false;
            if (null != mServerSocket) {
                mServerSocket.close();
                mServerSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing the server socket.", e);
        }
    }

    public int getPort() {
        return listenPort;
    }


    @Override
    public void run() {
        try {
            final boolean open_service = Pref.getBooleanDefaultFalse("xdrip_webservice_open");
            if (useSSL) {
                // SSL type
                UserError.Log.d(TAG, "Attempting to initialize SSL");
                final SSLServerSocketFactory ssocketFactory = SSLServerSocketHelper.makeSSLSocketFactory(
                        new BufferedInputStream(xdrip.getAppContext().getResources().openRawResource(R.raw.localhost_cert)),
                        "password".toCharArray());
                mServerSocket = ssocketFactory.createServerSocket(listenPort, 1, open_service ? null : InetAddress.getByName("127.0.0.1"));

            } else {
                // Non-SSL type
                mServerSocket = new ServerSocket(listenPort, 1, open_service ? null : InetAddress.getByName("127.0.0.1"));
            }
            while (isRunning) {
                final Socket socket = mServerSocket.accept();
                final int runningThreads = thread_count.get();
                if (runningThreads < MAX_RUNNING_THREADS) {
                    Log.d(TAG, "Running threads: " + runningThreads);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            thread_count.incrementAndGet();
                            try {
                                handle(socket);
                                socket.close();
                            } catch (SocketException e) {
                                // ignore
                            } catch (IOException e) {
                                Log.e(TAG, "Web server thread error.", e);
                            } finally {
                                thread_count.decrementAndGet();
                            }
                        }
                    }).start();
                } else {
                    if (JoH.ratelimit("webservice-thread-overheat", 60)) {
                        UserError.Log.wtf(TAG, "Web service jammed with too many connections > " + runningThreads);
                    }
                    socket.close();
                }

            }
        } catch (SocketException e) {
            // The server was stopped; ignore.
        } catch (IOException e) {
            Log.e(TAG, "Web server error.", e);
        }
    }

    /**
     * Respond to a request from a client.
     *
     * @param socket The client socket.
     * @throws IOException
     */
    private void handle(Socket socket) throws IOException {
        final PowerManager.WakeLock wl = JoH.getWakeLock("webservice-handler", 20000);
        BufferedReader reader = null;
        PrintStream output = null;
        try {
            socket.setSoTimeout((int) (Constants.SECOND_IN_MS * 10));
            try {
                if (socket instanceof SSLSocket) {
                    // if ssl
                    UserError.Log.d(TAG, "Attempting SSL handshake");
                    final SSLSocket sslSocket = (SSLSocket) socket;

                    sslSocket.startHandshake();
                    final SSLSession sslSession = sslSocket.getSession();

                    UserError.Log.d(TAG, "SSLSession :");
                    UserError.Log.d(TAG, "\tProtocol : " + sslSession.getProtocol());
                    UserError.Log.d(TAG, "\tCipher suite : " + sslSession.getCipherSuite());
                }
            } catch (SSLHandshakeException e) {
                UserError.Log.e(TAG, "SSL ERROR: " + e.toString());
                return;
            } catch (Exception e) {
                UserError.Log.e(TAG, "SSL unknown error: " + e);
                return;
            }

            // get the set password if any
            final String secret = Pref.getStringDefaultBlank("xdrip_webservice_secret");
            final String hashedSecret = hashPassword(secret);

            final boolean authNeeded = hashedSecret != null && !socket.getInetAddress().isLoopbackAddress();
            final TriState secretCheckResult = new TriState();


            String route = null;

            // Read HTTP headers and parse out the route.
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;

            boolean headersOnly = false;
            int lineCount = 0;
            while (!TextUtils.isEmpty(line = reader.readLine()) && lineCount < 50) {

                if (line.startsWith("GET /") || line.startsWith("HEAD /")) {
                    int start = line.indexOf('/') + 1;
                    int end = line.indexOf(' ', start);
                    if (start < line.length()) {
                        route = line.substring(start, end);
                        UserError.Log.d(TAG, "Received request for: " + route);
                        //if (hashedSecret == null) break; // we can't optimize as we always need to look for api-secret even if server doesn't use it
                    }

                    headersOnly = line.startsWith("HEAD /");
                } else if (line.toLowerCase().startsWith(("api-secret"))) {
                    final String requestSecret[] = line.split(": ");
                    if (requestSecret.length < 2) continue;
                    secretCheckResult.set(hashedSecret != null && hashedSecret.equalsIgnoreCase(requestSecret[1]));
                    break; // last and only header checked and will appear after GET request
                }
                lineCount++;
            }

            // Output stream that we send the response to
            output = new PrintStream(socket.getOutputStream());

            // Prepare the content to send.
            if (null == route) {
                writeServerError(output);
                return;
            }

            final WebResponse response;

            if (secretCheckResult.isFalse() || (authNeeded && !secretCheckResult.isTrue())) {
                final String failureMessage = "Authentication failed - check api-secret\n"
                        + "\n" + (authNeeded ? "secret is required " : "secret is not required")
                        + "\n" + secretCheckResult.trinary("no secret supplied", "supplied secret matches", "supplied secret doesn't match")
                        + "\n" + "Your address: " + socket.getInetAddress().toString()
                        + "\n\n";
                if (JoH.ratelimit("web-auth-failure", 10)) {
                    UserError.Log.e(TAG, failureMessage);
                }
                response = new WebResponse(failureMessage, 403, "text/plain");
                JoH.threadSleep(1000 + (300 * thread_count.get()));
            } else {
                response = ((RouteFinder) Singleton.get("RouteFinder")).handleRoute(route, socket.getInetAddress());
            }

            // if we didn't manage to generate a response
            if (response == null) {
                writeServerError(output);
                return;
            }

            // if the response bytes are null
            if (response.bytes == null) {
                writeServerError(output);
                return;
            }
            // Send out the content.
            output.println("HTTP/1.0 " + response.resultCode + " " + response.getResultDesc());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                output.println("Date: " + rfc7231formatter.format(ZonedDateTime.now(ZoneOffset.UTC)));
            }
            output.println("Access-Control-Allow-Origin: *");
            output.println("Content-Type: " + response.mimeType);
            output.println("Content-Length: " + response.bytes.length);
            output.println();
            if (!headersOnly) {
                output.write(response.bytes);
            }
            output.flush();

            UserError.Log.d(TAG, "Sent response: " + response.bytes.length + " bytes, code: " + response.resultCode + " mimetype: " + response.mimeType);

        } catch (SocketTimeoutException e) {
            UserError.Log.d(TAG, "Got socket timeout: " + e);
        } catch (NullPointerException e) {
            UserError.Log.wtf(TAG, "Got null pointer exception: " + e);

        } finally {
            if (output != null) {
                output.close();
            }
            if (reader != null) {
                reader.close();
            }
            JoH.releaseWakeLock(wl);
        }
    }


    /**
     * Writes a server error response (HTTP/1.0 500) to the given output stream.
     *
     * @param output The output stream.
     */
    private void writeServerError(final PrintStream output) {
        output.println("HTTP/1.0 500 Internal Server Error");
        output.flush();
        UserError.Log.e(TAG, "Internal server error reply");
    }

    public static String hashPassword(final String secret) {
        return secret.isEmpty() ? null : Hashing.sha1().hashBytes(secret.getBytes(Charsets.UTF_8)).toString();
    }
}

