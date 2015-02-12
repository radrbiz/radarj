package org.radarlab.crypto.ecdsa;

import org.radarlab.core.Utils;
import org.radarlab.core.exception.RadarException;
import org.radarlab.core.hash.B58;
import org.radarlab.crypto.Sha512;
import org.ripple.bouncycastle.util.encoders.Hex;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

import static org.radarlab.core.Utils.doubleDigest;

public class Seed {
    // See https://wiki.ripple.com/Account_Family
    final byte[] seedBytes;

    public Seed(byte[] seedBytes) {
        this.seedBytes = seedBytes;
    }

    @Override
    public String toString() {
        return B58.getInstance().encodeFamilySeed(seedBytes);
    }

    public byte[] getBytes() {
        return seedBytes;
    }

    public IKeyPair keyPair() {
        return createKeyPair(seedBytes, 0);
    }
    public IKeyPair rootKeyPair() {
        return createKeyPair(seedBytes, -1);
    }

    public IKeyPair keyPair(int account) {
        return createKeyPair(seedBytes, account);
    }

    public static Seed fromBase58(String b58) {
        return new Seed(B58.getInstance().decodeFamilySeed(b58));
    }

    public static Seed fromPassPhrase(String passPhrase) {
        return new Seed(passPhraseToSeedBytes(passPhrase));
    }

    public static byte[] passPhraseToSeedBytes(String phrase) {
        try {
            return new Sha512(phrase.getBytes("utf-8")).finish128();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static IKeyPair createKeyPair(byte[] seedBytes) {
        return createKeyPair(seedBytes, 0);
    }

    public static IKeyPair createKeyPair(byte[] seedBytes, int accountNumber) {
        if(seedBytes.length != 16)
            throw new RadarException("seedBytes MUST be 16 bytes: " + Hex.toHexString(seedBytes));
        BigInteger secret, pub, privateGen;
        // The private generator (aka root private key, master private key)
        privateGen = computePrivateGen(seedBytes);
        byte[] publicGenBytes = computePublicGenerator(privateGen);

        if (accountNumber == -1) {
            // The root keyPair
            return new KeyPair(privateGen, Utils.uBigInt(publicGenBytes));
        }
        else {
            secret = computeSecretKey(privateGen, publicGenBytes, accountNumber);
            pub = computePublicKey(secret);
            return new KeyPair(secret, pub);
        }

    }

    /**
     *
     * @param secretKey secret point on the curve as BigInteger
     * @return corresponding public point
     */
    public static byte[] getPublic(BigInteger secretKey) {
        return SECP256K1.basePointMultipliedBy(secretKey);
    }

    /**
     *
     * @param privateGen secret point on the curve as BigInteger
     * @return the corresponding public key is the public generator
     *         (aka public root key, master public key).
     *         return as byte[] for convenience.
     */
    public static byte[] computePublicGenerator(BigInteger privateGen) {
        return getPublic(privateGen);
    }

    public static BigInteger computePublicKey(BigInteger secret) {
        return Utils.uBigInt(getPublic(secret));
    }

    public static BigInteger computePrivateGen(byte[] seedBytes) {
        byte[] privateGenBytes;
        BigInteger privateGen;
        int i = 0;

        while (true) {
            privateGenBytes = new Sha512().add(seedBytes)
                    .add32(i++)
                    .finish256();
            privateGen = Utils.uBigInt(privateGenBytes);
            if (privateGen.compareTo(SECP256K1.order()) == -1) {
                break;
            }
        }
        return privateGen;
    }

    public static BigInteger computeSecretKey(BigInteger privateGen, byte[] publicGenBytes, int accountNumber) {
        BigInteger secret;
        int i;

        i=0;
        while (true) {
            byte[] secretBytes = new Sha512().add(publicGenBytes)
                    .add32(accountNumber)
                    .add32(i++)
                    .finish256();
            secret = Utils.uBigInt(secretBytes);
            if (secret.compareTo(SECP256K1.order()) == -1) {
                break;
            }
        }

        secret = secret.add(privateGen).mod(SECP256K1.order());
        return secret;
    }
    /*
    public static byte[] passPhraseToSeedBytes(String passPhrase) {
        try {
            return quarterSha512(passPhrase.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static IKeyPair createKeyPair(byte[] seedBytes) {
        if(seedBytes.length != 16)
            throw new RadarException("seedBytes MUST be 16 bytes: " + Convert.bytesToHex(seedBytes));
        BigInteger secret, pub, privateGen, order = SECP256K1.order();
        //System.out.println("Seed.createKeyPair() order=" + order + ", " + order.toString(16));
        byte[] privateGenBytes;
        byte[] publicGenBytes;

        int i = 0, seq = 0;

        while (true) {
            privateGenBytes = hashedIncrement(seedBytes, i++);
            privateGen = Utils.uBigInt(privateGenBytes);
            if (privateGen.compareTo(order) == -1) {
                break;
            }
        }
        publicGenBytes = SECP256K1.basePointMultipliedBy(privateGen);

        i=0;
        while (true) {
            byte[] secretBytes = hashedIncrement(appendIntBytes(publicGenBytes, seq), i++);
            secret = Utils.uBigInt(secretBytes);
            if (secret.compareTo(order) == -1) {
                break;
            }
        }

        secret = secret.add(privateGen).mod(order);
        pub = Utils.uBigInt(SECP256K1.basePointMultipliedBy(secret));

        return new KeyPair(secret, pub);
    }

    public static byte[] hashedIncrement(byte[] bytes, int increment) {
        return halfSha512(appendIntBytes(bytes, increment));
    }

    public static byte[] appendIntBytes(byte[] in, long i) {
        byte[] out = new byte[in.length + 4];

        System.arraycopy(in, 0, out, 0, in.length);

        out[in.length] =     (byte) ((i >>> 24) & 0xFF);
        out[in.length + 1] = (byte) ((i >>> 16) & 0xFF);
        out[in.length + 2] = (byte) ((i >>> 8)  & 0xFF);
        out[in.length + 3] = (byte) ((i)       & 0xFF);

        return out;
    }*/

    public static IKeyPair getKeyPair(byte[] master_seed) {
        return createKeyPair(master_seed);
    }

    public static IKeyPair getKeyPair(String master_seed_str) {
        return getKeyPair(B58.getInstance().decodeFamilySeed(master_seed_str));
    }

    //for BTC convert ...
    public static byte[] genSeedFromBtcPriv(byte[] priv) {
        if(priv.length != 32) throw new RadarException("BTC private key MUST be 32 bytes!");
        byte[] ret = Utils.quarterSha512(Utils.doubleDigest(priv));
        return ret;
    }

    public static byte[] genSeedFromBtcPriv(String privStr) {
        byte[] priv = Hex.decode(privStr);
        return genSeedFromBtcPriv(priv);
    }

}
