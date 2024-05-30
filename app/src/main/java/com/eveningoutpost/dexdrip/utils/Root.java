package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.WholeHouse;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// jamorham

public class Root {

    private static final String TAG = "RootTools";
    public static Boolean gotRoot;

    public static boolean gotRoot() {
        if (gotRoot == null) {
            gotRoot = WholeHouse.isRpi(); // TODO improve this detection
        }
        return gotRoot;
    }

    public static List<String> exec(final String... command) {
        if (gotRoot()) {
            return runCommandList(command, false);
        } else {
            return null;
        }
    }

    private static List<String> runCommandList(final String[] commands, final boolean includeStdError) {

        final String shell = "su";

        final List<String> res = Collections.synchronizedList(new ArrayList<String>());

        try {
            final Process process = Runtime.getRuntime().exec(shell);
            final DataOutputStream dataOutputStream = new DataOutputStream(process.getOutputStream());
            final StreamGobbler inputStream = new StreamGobbler(TAG + " " + shell + "-", process.getInputStream(), res);
            final StreamGobbler errorStream = new StreamGobbler(TAG + " " + shell + "E", process.getErrorStream(), includeStdError ? res : null);

            inputStream.start();
            errorStream.start();

            for (final String command : commands) {
                dataOutputStream.write(bytesNewLine(command));
                dataOutputStream.flush();
            }
            dataOutputStream.write(bytesNewLine("exit"));
            dataOutputStream.flush();

            process.waitFor();

            UserError.Log.d(TAG, "Process exited code: " + process.exitValue());
            try {
                dataOutputStream.close();
            } catch (IOException e) {
                //
            }
            inputStream.join();
            errorStream.join();
            process.destroy();

        } catch (InterruptedException e) {
            UserError.Log.d(TAG, "Interrupted exception: " + e);
        } catch (IOException e) {
            UserError.Log.d(TAG, "IO exception: " + e);
            return null;
        }
        return res;
    }

    private static byte[] bytesNewLine(final String str) throws UnsupportedEncodingException {
        return (str + "\n").getBytes("UTF-8");
    }
}
