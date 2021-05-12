package com.eveningoutpost.dexdrip.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/*
 * Copyright (C) 2012-2015 Jorrit "Chainfire" Jongma
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * Thread utility class continuously reading from an InputStream
 */
public class StreamGobbler extends Thread {
    /**
     * Line callback interface
     */
    public interface OnLineListener {
        /**
         * <p>Line callback</p>
         *
         * <p>This callback should process the line as quickly as possible.
         * Delays in this callback may pause the native process or even
         * result in a deadlock</p>
         *
         * @param line String that was gobbled
         */
        void onLine(String line);
    }

    private String identifier = null;
    private BufferedReader reader = null;
    private List<String> writer = null;
    private OnLineListener listener = null;

    /**
     * <p>StreamGobbler constructor</p>
     *
     * <p>We use this class because STDOUT and STDERR should be read as quickly as
     * possible to prevent a deadlock from occurring, or Process.waitFor() never
     * returning (as the buffer is full, pausing the native process)</p>
     *
     * @param identifier  Name of the stream
     * @param inputStream InputStream to read from
     * @param outputList  {@literal List<String>} to write to, or null
     */
    public StreamGobbler(String identifier, InputStream inputStream, List<String> outputList) {
        this.identifier = identifier;
        reader = new BufferedReader(new InputStreamReader(inputStream));
        writer = outputList;
    }

    /**
     * <p>StreamGobbler constructor</p>
     *
     * <p>We use this class because STDOUT and STDERR should be read as quickly as
     * possible to prevent a deadlock from occurring, or Process.waitFor() never
     * returning (as the buffer is full, pausing the native process)</p>
     *
     * @param identifier     Name of the stream
     * @param inputStream    InputStream to read from
     * @param onLineListener OnLineListener callback
     */
    public StreamGobbler(String identifier, InputStream inputStream, OnLineListener onLineListener) {
        this.identifier = identifier;
        reader = new BufferedReader(new InputStreamReader(inputStream));
        listener = onLineListener;
    }

    @Override
    public void run() {
        // keep reading the InputStream until it ends (or an error occurs)
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                android.util.Log.d(StreamGobbler.class.getSimpleName(),
                        String.format("[%s] %s", identifier, line));
                if (writer != null) writer.add(line);
                if (listener != null) listener.onLine(line);
            }
        } catch (IOException e) {
            // reader probably closed, expected exit condition
        }

        // make sure our stream is closed and resources will be freed
        try {
            reader.close();
        } catch (IOException e) {
            // read already closed
        }
    }
}