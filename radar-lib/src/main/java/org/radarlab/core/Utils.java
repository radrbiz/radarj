package org.radarlab.core;

import org.ripple.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.ripple.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    private static final MessageDigest digest;
    static {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
    }

    /**
     * See {@link Utils#doubleDigest(byte[], int, int)}.
     */
    public static byte[] doubleDigest(byte[] input) {
        return doubleDigest(input, 0, input.length);
    }

    /**
     * Calculates the SHA-256 hash of the given byte range, and then hashes the resulting hash again. This is
     * standard procedure in Bitcoin. The resulting hash is in big endian form.
     */
    public static byte[] doubleDigest(byte[] input, int offset, int length) {
        synchronized (digest) {
            digest.reset();
            digest.update(input, offset, length);
            byte[] first = digest.digest();
            return digest.digest(first);
        }
    }

    public static byte[] halfSha512(byte[] bytes) {
        byte[] hash = new byte[32];
        System.arraycopy(sha512(bytes), 0, hash, 0, 32);
        return hash;
    }

    public static byte[] quarterSha512(byte[] bytes) {
        byte[] hash = new byte[16];
        System.arraycopy(sha512(bytes), 0, hash, 0, 16);
        return hash;
    }

    public static byte[] sha512(byte[] byteArrays) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        messageDigest.update(byteArrays);
        return messageDigest.digest();
    }

    public static byte[] SHA256_RIPEMD160(byte[] input) {
        try {
            byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(input);
            RIPEMD160Digest digest = new RIPEMD160Digest();
            digest.update(sha256, 0, sha256.length);
            byte[] out = new byte[20];
            digest.doFinal(out, 0);
            return out;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    public static String bigHex(BigInteger bn) {
        return Hex.toHexString(bn.toByteArray());
    }

    public static BigInteger uBigInt(byte[] bytes) {
        return new BigInteger(1, bytes);
    }

    /**  get the lowest n bytes from source array */
    public static byte[] lowArray(byte[] src, int n) {
        if(n >= src.length) return src;
        int pos = src.length - n;
        byte[] des = new byte[n];
        System.arraycopy(src, pos, des, 0, n);
        return des;
    }
}
