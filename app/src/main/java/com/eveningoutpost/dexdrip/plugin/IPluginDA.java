package com.eveningoutpost.dexdrip.plugin;

/**
 * JamOrHam
 *
 * Simple plugin data exchange interface
 */

public interface IPluginDA {

    byte[][] aNext();

    byte[][] bNext();

    byte[][] cNext();

    void amConnected();

    boolean bondNow(final byte[] data);

    boolean receivedResponse(final byte[] data);

    boolean receivedResponse2(final byte[] data);

    boolean receivedResponse3(final byte[] data);

    boolean receivedData(final byte[] data);

    boolean receivedData2(final byte[] data);

    boolean receivedData3(final byte[] data);

    byte[] getPersistence(final int channel);

    boolean setPersistence(final int channel, final byte[] data);

    String getStatus();

    String getName();

}
