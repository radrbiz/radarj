package org.radarlab.core.hash;

import org.radarlab.core.Utils;
import org.radarlab.core.exception.RadarException;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;


public class B58 {
    public static final int VER_ACCOUNT_ID        = 0;
    public static final int VER_FAMILY_SEED       = 33;
    public static final int VER_NONE              = 1;
    public static final int VER_NODE_PUBLIC       = 28;
    public static final int VER_NODE_PRIVATE      = 32;
    public static final int VER_ACCOUNT_PUBLIC    = 35;
    public static final int VER_ACCOUNT_PRIVATE   = 34;
    public static final int VER_FAMILY_GENERATOR  = 41;

    public static final int LEN_ADDRESS           = 34;
    public static final int LEN_FAMILY_SEED       = 29;
    public static final int LEN_FAMILY_SEED_HEX   = 16;
    public static final int LEN_PRIVATE_KEY       = 32;
    public static final int LEN_PUBLIC_KEY        = 33;

    public static final String DEFAULT_ALPHABET = "rpshnaf39wBUDNEGHJKLM4PQRST7VWXYZ2bcdeCg65jkm8oFqi1tuvAxyz";


    private static final B58 instance = new B58();
    private int[] mIndexes;
    private char[] mAlphabet;


    public static B58 getInstance() {
        return instance;
    }

    public B58() {
        setAlphabet(DEFAULT_ALPHABET);
        buildIndexes();
    }

    private void setAlphabet(String alphabet) {
        mAlphabet = alphabet.toCharArray();
    }

    private void buildIndexes() {
        mIndexes = new int[128];

        for (int i = 0; i < mIndexes.length; i++) {
            mIndexes[i] = -1;
        }
        for (int i = 0; i < mAlphabet.length; i++) {
            mIndexes[mAlphabet[i]] = i;
        }
    }

    public String encodeToStringChecked(byte[] input, int version) {
        try {
            return new String(encodeToBytesChecked(input, version), "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    public byte[] encodeToBytesChecked(byte[] input, int version) {
        byte[] buffer = new byte[input.length + 1];
        buffer[0] = (byte) version;
        System.arraycopy(input, 0, buffer, 1, input.length);
        byte[] checkSum = copyOfRange(Utils.doubleDigest(buffer), 0, 4);
        byte[] output = new byte[buffer.length + checkSum.length];
        System.arraycopy(buffer, 0, output, 0, buffer.length);
        System.arraycopy(checkSum, 0, output, buffer.length, checkSum.length);
        return encodeToBytes(output);
    }

    public String encodeToString(byte[] input) {
        byte[] output = encodeToBytes(input);
        try {
            return new String(output, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    /**
     * Encodes the given bytes in base58. No checksum is appended.
     */
    public byte[] encodeToBytes(byte[] input) {
        if (input.length == 0) {
            return new byte[0];
        }
        input = copyOfRange(input, 0, input.length);
        // Count leading zeroes.
        int zeroCount = 0;
        while (zeroCount < input.length && input[zeroCount] == 0) {
            ++zeroCount;
        }
        // The actual encoding.
        byte[] temp = new byte[input.length * 2];
        int j = temp.length;

        int startAt = zeroCount;
        while (startAt < input.length) {
            byte mod = divmod58(input, startAt);
            if (input[startAt] == 0) {
                ++startAt;
            }
            temp[--j] = (byte) mAlphabet[mod];
        }

        // Strip extra '1' if there are some after decoding.
        while (j < temp.length && temp[j] == mAlphabet[0]) {
            ++j;
        }
        // Add as many leading '1' as there were leading zeros.
        while (--zeroCount >= 0) {
            temp[--j] = (byte) mAlphabet[0];
        }

        byte[] output;
        output = copyOfRange(temp, j, temp.length);
        return output;
    }

    public byte[] decode(String input) throws RadarException {
        if (input.length() == 0) {
            return new byte[0];
        }
        byte[] input58 = new byte[input.length()];
        // Transform the String to a base58 byte sequence
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);

            int digit58 = -1;
            if (c >= 0 && c < 128) {
                digit58 = mIndexes[c];
            }
            if (digit58 < 0) {
                throw new RadarException("Illegal character " + c + " at " + i);
            }

            input58[i] = (byte) digit58;
        }
        // Count leading zeroes
        int zeroCount = 0;
        while (zeroCount < input58.length && input58[zeroCount] == 0) {
            ++zeroCount;
        }
        // The encoding
        byte[] temp = new byte[input.length()];
        int j = temp.length;

        int startAt = zeroCount;
        while (startAt < input58.length) {
            byte mod = divmod256(input58, startAt);
            if (input58[startAt] == 0) {
                ++startAt;
            }

            temp[--j] = mod;
        }
        // Do no add extra leading zeroes, move j to first non null byte.
        while (j < temp.length && temp[j] == 0) {
            ++j;
        }

        return copyOfRange(temp, j - zeroCount, temp.length);
    }

    public BigInteger decodeToBigInteger(String input) throws RadarException {
        return new BigInteger(1, decode(input));

    }

    /**
     * Uses the checksum in the last 4 bytes of the decoded data to verify the rest are correct. The checksum is
     * removed from the returned data.
     *
     * @throws org.radarlab.core.exception.RadarException if the input is not baseFields 58 or the checksum does not validate.
     */
    public byte[] decodeChecked(String input, int version) throws RadarException {
        byte buffer[] = decode(input);
        if (buffer.length < 4)
            throw new RadarException("Input too short");
        byte actualVersion = buffer[0];
        if (actualVersion != version) {
            throw new RadarException("Bro, version is wrong yo:" + input + " ver=" + actualVersion + " need=" + version);
        }

        byte[] toHash = copyOfRange(buffer, 0, buffer.length - 4);
        byte[] hashed = copyOfRange(Utils.doubleDigest(toHash), 0, 4);
        byte[] checksum = copyOfRange(buffer, buffer.length - 4, buffer.length);

        if (!Arrays.equals(checksum, hashed))
            throw new RadarException("Checksum does not validate");

        return copyOfRange(buffer, 1, buffer.length - 4);
    }


    /** family seed */
    public byte[] decodeFamilySeed(String seed) throws RadarException {
        return decodeChecked(seed, VER_FAMILY_SEED);
    }

    public String encodeFamilySeed(byte[] bytes) {
        return encodeToStringChecked(bytes, VER_FAMILY_SEED);
    }

    /** Addrress */
    public byte[] decodeAddress(String address) throws RadarException {
        return decodeChecked(address, VER_ACCOUNT_ID);
    }

    public String encodeAddress(byte[] bytes) {
        return encodeToStringChecked(bytes, VER_ACCOUNT_ID);
    }

    /** Node public (by Fau) */
    public byte[] decodeNodePublic(String np) throws RadarException {
        return decodeChecked(np, VER_NODE_PUBLIC);
    }

    public String encodeNodePublic(byte[] bytes) {
        return encodeToStringChecked(bytes, VER_NODE_PUBLIC);
    }

    /** Account public (by Fau) */
    public byte[] decodeAccountPublic(String ap) throws RadarException {
        return decodeChecked(ap, VER_ACCOUNT_PUBLIC);
    }

    public String encodeAccountPublic(byte[] bytes) {
        return encodeToStringChecked(bytes, VER_ACCOUNT_PUBLIC);
    }


    //--------------------------  private section   --------------------------
    //
    // number -> number / 58, returns number % 58
    //
    private byte divmod58(byte[] number, int startAt) {
        int remainder = 0;
        for (int i = startAt; i < number.length; i++) {
            int digit256 = (int) number[i] & 0xFF;
            int temp = remainder * 256 + digit256;

            number[i] = (byte) (temp / 58);

            remainder = temp % 58;
        }

        return (byte) remainder;
    }

    //
    // number -> number / 256, returns number % 256
    //
    private byte divmod256(byte[] number58, int startAt) {
        int remainder = 0;
        for (int i = startAt; i < number58.length; i++) {
            int digit58 = (int) number58[i] & 0xFF;
            int temp = remainder * 58 + digit58;

            number58[i] = (byte) (temp / 256);

            remainder = temp % 256;
        }

        return (byte) remainder;
    }

    private byte[] copyOfRange(byte[] source, int from, int to) {
        byte[] range = new byte[to - from];
        System.arraycopy(source, from, range, 0, range.length);

        return range;
    }
}
