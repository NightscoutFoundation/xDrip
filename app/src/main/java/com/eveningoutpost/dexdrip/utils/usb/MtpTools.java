package com.eveningoutpost.dexdrip.utils.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.mtp.MtpConstants;
import android.mtp.MtpDevice;
import android.mtp.MtpObjectInfo;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.annotation.RequiresApi;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import lombok.Getter;

// jamorham

@RequiresApi(api = Build.VERSION_CODES.N)
public class MtpTools {

    private static final String TAG = "Usbtools-MTP";

    static synchronized MtpDevice openMTP(final UsbDevice device) {

        if (device == null) {
            return null;
        }

        final UsbManager usbManager = (UsbManager) xdrip.getAppContext().getSystemService(Context.USB_SERVICE);

        if (usbManager == null) {
            Log.d(TAG, "usbmanager is null in openMTP");
            return null;
        }

        final MtpDevice mtpDevice = new MtpDevice(device);

        final UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(device);
        try {
            if (!mtpDevice.open(usbDeviceConnection)) {
                return null;
            }
        } catch (Exception e) {
            JoH.static_toast_long("Exception opening USB: " + e);
            return null;
        }

        return mtpDevice;
    }

    private static synchronized int createDocument(final MtpDevice device, final MtpObjectInfo objectInfo,
                                                   final ParcelFileDescriptor source) {

        final MtpObjectInfo sendObjectInfoResult = device.sendObjectInfo(objectInfo);
        if (sendObjectInfoResult == null) {
            Log.e(TAG, "Null sendObjectInfoResult in create document :(");
            return -1;
        }
        Log.d(TAG, "Send object info result: " + sendObjectInfoResult.getName());

        // Association is what passes for a folder within mtp
        if (objectInfo.getFormat() != MtpConstants.FORMAT_ASSOCIATION) {
            if (!device.sendObject(sendObjectInfoResult.getObjectHandle(),
                    sendObjectInfoResult.getCompressedSize(), source)) {
                return -1;
            }
        }
        Log.d(TAG, "Success indicated with handle: " + sendObjectInfoResult.getObjectHandle());
        return sendObjectInfoResult.getObjectHandle();

    }

    public static boolean deleteIfExistsInRoot(final MtpDevice mtpDevice, final int storageId, final String filename) {
        final int handle = existsInRoot(mtpDevice, storageId, filename);
        if (handle != -1) {
            Log.d(TAG, "Deleting: " + filename + " at " + handle);
            return mtpDevice.deleteObject(handle);
        }
        return false;
    }

    public static int existsInTopLevelFolder(final MtpDevice mtpDevice, final int storageId, final String folder, final String filename) {
        final HashMap<String, Integer> folders = getTopLevelFolders(mtpDevice, storageId);
        if (folders == null || folders.get(folder) == null) {
            return -1;
        }
        return existsInFolderHandle(mtpDevice, storageId, filename, folders.get(folder));
    }

    // -1 = not found
    public static int existsInRoot(final MtpDevice mtpDevice, final int storageId, final String filename) {
        return existsInFolderHandle(mtpDevice, storageId, filename, -1);
    }

    // -1 = not found
    public static int existsInFolderHandle(final MtpDevice mtpDevice, final int storageId, final String filename, final int handle) {
        final int[] objectHandles = mtpDevice.getObjectHandles(storageId, 0, handle);
        if (objectHandles == null) {
            return -1;
        }

        if (objectHandles.length > 20 || objectHandles.length < 1) {
            Log.d(TAG, "existsInRoot() Got object handles count: " + objectHandles.length);
        }
        for (int objectHandle : objectHandles) {

            final MtpObjectInfo mtpObjectInfo = mtpDevice.getObjectInfo(objectHandle);
            if (mtpObjectInfo == null) {
                continue;
            }

            if (mtpObjectInfo.getParent() != 0) {
                continue;
            }

            if (mtpObjectInfo.getName().equalsIgnoreCase(filename)) {
                return mtpObjectInfo.getObjectHandle();
            }
        }
        return -1;
    }

    public static HashMap<String, Integer> getTopLevelFolders(final MtpDevice mtpDevice, final int storageId) {
        final int[] objectHandles = mtpDevice.getObjectHandles(storageId, MtpConstants.FORMAT_ASSOCIATION, -1);
        if (objectHandles == null) {
            return null;
        }

        Log.d(TAG, "FoldersInRoot() Got object handles count: " + objectHandles.length);

        final HashMap<String, Integer> results = new HashMap<>();

        for (int objectHandle : objectHandles) {

            final MtpObjectInfo mtpObjectInfo = mtpDevice.getObjectInfo(objectHandle);
            if (mtpObjectInfo == null) {
                continue;
            }
            if (mtpObjectInfo.getParent() != 0) {
                continue;
            }

            if (mtpObjectInfo.getFormat() == MtpConstants.FORMAT_ASSOCIATION) {
                results.put(mtpObjectInfo.getName(), mtpObjectInfo.getObjectHandle());
            }
        }
        return results;
    }

    public static int recreateFile(final String fileName, final byte[] outputBytes, final MtpDevice mtpDevice, final int storage_id, final int parent_id) {
        MtpTools.deleteIfExistsInRoot(mtpDevice, storage_id, fileName);
        return MtpTools.createFile(fileName, outputBytes, mtpDevice, storage_id, parent_id);
    }

    public static int createFile(final String fileName, final byte[] outputBytes, final MtpDevice mtpDevice, final int storage_id, final int parent_id) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "createFile cannot work below Android 7");
            return -1;
        }

        if (outputBytes != null) {
            ParcelFileDescriptor[] pipe = null;
            try {
                pipe = ParcelFileDescriptor.createReliablePipe();

                final FileOutputStream out = new FileOutputStream(pipe[1].getFileDescriptor());
                Inevitable.stackableTask("write-mtp-bytes", 200, () -> {
                    try {
                        Log.d(TAG, "Attempting to write: " + outputBytes.length + " bytes to: " + fileName);
                        out.write(outputBytes);
                        out.flush();

                    } catch (IOException e) {
                        Log.e(TAG, "Got io exception in writing thread");
                    } finally {
                        try {
                            out.close();
                        } catch (IOException e) {
                            Log.e(TAG, "got io exception closing in writing thread");
                        }
                    }
                });
            } catch (NullPointerException | IOException e) {
                Log.e(TAG, "IO exception or null in pipe creation: " + e);
            }

            if (pipe != null) {
                final MtpObjectInfo fileInfo = new MtpObjectInfo.Builder()
                        .setName(fileName)
                        .setFormat(MtpConstants.FORMAT_UNDEFINED)
                        .setStorageId(storage_id)
                        .setParent(parent_id)
                        .setCompressedSize(outputBytes.length).build();
                try {
                    return createDocument(mtpDevice, fileInfo, pipe[0]);
                } finally {
                    try {
                        pipe[1].close();
                    } catch (NullPointerException | IOException e) {
                        Log.d(TAG, "Exception closing pipe 1: " + e);
                    }
                    try {
                        pipe[0].close();
                    } catch (NullPointerException | IOException e) {
                        Log.d(TAG, "Exception closing pipe 0: " + e);
                    }
                }
            }
        } else {
            Log.e(TAG,"Output bytes null");
        }
        return -1;
    }

    public static int createFolder(final String fileName, final MtpDevice mtpDevice, final int storage_id) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "createFolder cannot work below Android 7");
            return -1;
        }

        ParcelFileDescriptor[] pipe = null;
        try {
            pipe = ParcelFileDescriptor.createReliablePipe();
        } catch (NullPointerException | IOException e) {
            Log.e(TAG, "IO exception or null in pipe creation: " + e);
        }

        if (pipe != null) {
            final MtpObjectInfo fileInfo = new MtpObjectInfo.Builder()
                    .setName(fileName)
                    .setFormat(MtpConstants.FORMAT_ASSOCIATION)
                    .setStorageId(storage_id)
                    .setCompressedSize(0).build();
            try {
                return createDocument(mtpDevice, fileInfo, pipe[0]);
            } finally {
                try {
                    pipe[1].close();
                } catch (NullPointerException | IOException e) {
                    Log.e(TAG, "Exception closing pipe 1: " + e);
                }
                try {
                    pipe[0].close();
                } catch (NullPointerException | IOException e) {
                    Log.e(TAG, "Exception closing pipe 0: " + e);
                }
            }
        }

        return -1;
    }

    public static class MtpDeviceHelper {

        @Getter
        final MtpDevice device;

        @Getter
        int[] storageVolumeIds;

        public MtpDeviceHelper(final UsbDevice usbDevice) {
            this.device = openMTP(usbDevice);
            if (this.device != null) {
                try {
                    this.storageVolumeIds = this.device.getStorageIds();
                } catch (Exception e) {
                    Log.e(TAG, "Got exception in MtpDeviceHelper constructor: " + e);
                }
            } else {
                Log.e(TAG, "Mtp device null in MtpDeviceHelper constructor");
            }
        }

        public int getFirstStorageId() {
            try {
                return storageVolumeIds[0];
            } catch (Exception e) {
                return -1;
            }
        }

        public int numberOfStorageIds() {
            try {
                return storageVolumeIds.length;
            } catch (Exception e) {
                return -1;
            }
        }

        public boolean ok() {
            return device != null;
        }

        public String name() {
            try {
                return device.getDeviceInfo().getModel();
            } catch (Exception e) {
                return "<unknown>";
            }
        }

        public String manufacturer() {
            try {
                return device.getDeviceInfo().getManufacturer();
            } catch (Exception e) {
                return "<unknown>";
            }
        }

        public String hash() {
            return CipherUtils.getSHA256(manufacturer());
        }

        public int recreateRootFile(final String filename, final byte[] data) {
            return MtpTools.recreateFile(filename, data, device, getFirstStorageId(), 0);
        }

        public boolean existsInRoot(final String filename) {
            return MtpTools.existsInRoot(device, getFirstStorageId(), filename) != -1;
        }

        public boolean existsInFolder(final String folder, final String filename) {
            return MtpTools.existsInTopLevelFolder(device, getFirstStorageId(), folder, filename) != -1;
        }

        public void close() {
            device.close();
        }
    }

}
