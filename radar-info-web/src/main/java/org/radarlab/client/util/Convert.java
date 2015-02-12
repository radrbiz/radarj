package org.radarlab.client.util;

import org.ripple.bouncycastle.util.encoders.Hex;

public class Convert {
    public static String bytesToHex(byte[] bytes) {
        return Hex.toHexString(bytes);
    }
    public static byte[] hexToBytes(String hexString) {
        return Hex.decode(hexString);
    }
}
