package com.eveningoutpost.dexdrip.utilitymodels.desertsync;

// jamorham

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouteTools {

    private static final String TAG = RouteTools.class.getSimpleName();

    public static boolean reachable(final String checkIp) {
        try {
            final InetAddress check = ip(checkIp);
            List<InterfaceAddress> addrs = getLikelyInterfaceAddresses();
            boolean anyMatches = false;
            for (InterfaceAddress addr : addrs) {
                anyMatches = inSameNetwork(check, addr.getAddress(), addr.getNetworkPrefixLength());
                if (anyMatches) break;
            }
            return anyMatches;
        } catch (Exception e) {
            if (JoH.quietratelimit("route-tools-error", 60)) {
                UserError.Log.e(TAG, "Exception trying to calculate reachability of: " + checkIp + " " + e);
            }
            return true;
        }
    }

    public static String getBestInterfaceAddress() {
        // TODO how to give bluetooth preference?
        final List<InterfaceAddress> list = getLikelyInterfaceAddresses();
        String best = null;
        for (InterfaceAddress iaddr : list) {
            final InetAddress addr = iaddr.getAddress();
            if (addr.isLoopbackAddress()) continue;
            if (isLocal(addr)) {
                best = addr.getHostAddress();
                break; // grabs first one
            }
        }
        return best;
    }

    static List<InterfaceAddress> getLikelyInterfaceAddresses() {
        final List<InterfaceAddress> results = new ArrayList<>();
        try {
            final List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface face : interfaces) {
                final List<InterfaceAddress> interfaceAddresses = face.getInterfaceAddresses();
                //  for (final InterfaceAddress adx : interfaceAddresses) {
                //      android.util.Log.d("DesertComms", "addr: " + adx.getAddress().getHostAddress() + " " + adx.getNetworkPrefixLength());
                //  }
                results.addAll(interfaceAddresses);
            }
        } catch (Exception ignored) {
            //
        }
        return results;
    }

    public static boolean isLocal(final InetAddress addr) {
        return inSameNetwork(addr, ip("192.168.0.0"), 16)
                || inSameNetwork(addr, ip("172.16.0.0"), 12)
                || inSameNetwork(addr, ip("10.0.0.0"), 8);
        // TODO ipv6 local detect if assigned vs auto
    }

    static boolean inSameNetwork(final InetAddress a, final InetAddress b, final int bits) {
        if (a == null || b == null || bits == 0) return false;
        final byte[] aBytes = a.getAddress();
        final byte[] bBytes = b.getAddress();
        if (aBytes == null || bBytes == null || aBytes.length != bBytes.length) return false;
        final int bytesToCheck = bits >>> 3;
        for (int i = 0; i < bytesToCheck; i++) {
            if (aBytes[i] != bBytes[i]) return false;
        }
        final int remainder = 8 - (bits & 7);
        return remainder == 8 || (aBytes[bytesToCheck] >>> remainder) == (bBytes[bytesToCheck] >>> remainder);
    }


    public static InetAddress ip(final String ip) {
        return com.google.common.net.InetAddresses.forString(ip);
    }

    public static String ip(final InetAddress ip) {
        return ip.getHostAddress().replace("/", "");
    }
}
