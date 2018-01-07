package com.eveningoutpost.dexdrip.webservices;

import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;

/**
 * Created by jamorham on 06/01/2018.
 * <p>
 * Provide a webservice on localhost port 17580 respond to incoming requests either for data or
 * to push events in.
 * <p>
 * Designed for watches which support only a http interface
 * <p>
 * base service adapted from android reference documentation
 */

// TODO megastatus for engineering mode

public class XdripWebService implements Runnable {

    private static final String TAG = "xDripWebService";
    private static volatile XdripWebService instance = null;

    /**
     * The port number we listen to
     */
    private final int mPort;

    /**
     * True if the server is running.
     */
    private boolean mIsRunning;

    /**
     * The {@link java.net.ServerSocket} that we listen to.
     */
    private ServerSocket mServerSocket;

    /**
     * WebServer constructor.
     */
    private XdripWebService(int port) {
        mPort = port;
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

    // robustly shut down and erase the instance
    private static synchronized void easyStop() {
        try {
            UserError.Log.d(TAG, "running easyStop()");
            instance.stop();
            instance = null;
        } catch (NullPointerException e) {
            // concurrency issue
        }
    }

    // start up if needed
    private static synchronized void easyStart() {
        if (instance == null) {
            UserError.Log.d(TAG, "easyStart() Starting new instance");
            instance = new XdripWebService(17580);
        }
        instance.startIfNotRunning();
    }

    // start thread if needed
    private void startIfNotRunning() {
        if (!mIsRunning) {
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
        mIsRunning = true;
        new Thread(this).start();
    }

    /**
     * This method stops the web server
     */
    public synchronized void stop() {
        try {
            mIsRunning = false;
            if (null != mServerSocket) {
                mServerSocket.close();
                mServerSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing the server socket.", e);
        }
    }

    public int getPort() {
        return mPort;
    }

    @Override
    public void run() {
        try {
            mServerSocket = new ServerSocket(mPort, 1, InetAddress.getByName("127.0.0.1"));
            while (mIsRunning) {
                final Socket socket = mServerSocket.accept();
                handle(socket);
                socket.close();
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
        final PowerManager.WakeLock wl = JoH.getWakeLock("webservice-handler", 10000);
        BufferedReader reader = null;
        PrintStream output = null;
        try {
            String route = null;

            // Read HTTP headers and parse out the route.
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while (!TextUtils.isEmpty(line = reader.readLine())) {
                if (line.startsWith("GET /")) {
                    int start = line.indexOf('/') + 1;
                    int end = line.indexOf(' ', start);
                    route = URLDecoder.decode(line.substring(start, end), "UTF-8");
                    UserError.Log.d(TAG, "Received request for: " + route);
                    break;
                }
            }

            // Output stream that we send the response to
            output = new PrintStream(socket.getOutputStream());

            // Prepare the content to send.
            if (null == route) {
                writeServerError(output);
                return;
            }

            WebResponse response = null;

            // find a module based on our query string
            if (route.startsWith("pebble")) {
                // support for pebble nightscout watchface emulates /pebble Nightscout endpoint
                response = WebServicePebble.getInstance().request(route);
            } else if (route.startsWith("tasker/")) {
                // forward the request to tasker interface
                response = WebServiceTasker.getInstance().request(route);
            } else if (route.startsWith("sgv.json")) {
                // support for nightscout style sgv.json endpoint
                response = WebServiceSgv.getInstance().request(route);
            } else {
                // error not found
                response = new WebResponse("Path not found: " + route + "\r\n", 404, "text/plain");
            }

            // if we didn't manage to generate a response
            if (response == null) {
                writeServerError(output);
                return;
            }

            byte[] bytes = response.bytes;

            // if the response bytes are null
            if (bytes == null) {
                writeServerError(output);
                return;
            }
            // Send out the content.
            output.println("HTTP/1.0 " + response.resultCode + " OK");
            output.println("Content-Type: " + response.mimeType);
            output.println("Content-Length: " + bytes.length);
            output.println();
            output.write(bytes);
            output.flush();

            UserError.Log.d(TAG, "Sent response: " + bytes.length + " bytes, code: " + response.resultCode + " mimetype: " + response.mimeType);

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
    private void writeServerError(PrintStream output) {
        output.println("HTTP/1.0 500 Internal Server Error");
        output.flush();
        UserError.Log.e(TAG, "Internal server error reply");
    }

}

