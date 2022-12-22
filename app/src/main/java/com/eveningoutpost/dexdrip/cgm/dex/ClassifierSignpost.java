package com.eveningoutpost.dexdrip.cgm.dex;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

/**
 * JamOrHam
 */

@RequiredArgsConstructor
public class ClassifierSignpost {
    final public UUID uuid;
    final public String action;
    public BluetoothGattCharacteristic characteristic;
}
