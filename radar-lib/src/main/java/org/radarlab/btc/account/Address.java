package org.radarlab.btc.account;


import org.radarlab.core.exception.RadarException;

/**
 *
 * A BitCoin address is fundamentally derived from an elliptic curve public key and a set of network parameters.
 * It has several possible representations:<p>
 *
 * <ol>
 * <li>The raw public key bytes themselves.
 * <li>RIPEMD160 hash of the public key bytes.
 * <li>A base58 encoded "human form" that includes a version and check code, to guard against typos.
 * </ol><p>
 *
 * One may question whether the base58 form is really an improvement over the hash160 form, given
 * they are both very unfriendly for typists. More useful representations might include qrcodes
 * and identicons.<p>
 *
 * Note that an address is specific to a network because the first byte is a discriminator value.
 */
public class Address extends VersionedChecksummedBytes {
    /**
     * Construct an address from parameters and the hash160 form. Example:<p>
     *
     * <pre>new Address(NetworkParameters.prodNet(), Hex.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));</pre>
     */
    public Address(int ver, byte[] hash160) {
        super(ver, hash160);
        if (hash160.length != 20)  // 160 = 8 * 20
            throw new RuntimeException("Addresses are 160-bit hashes, so you must provide 20 bytes");
    }

    /**
     * Construct an address from parameters and the standard "human readable" form. Example:<p>
     *
     * <pre>new Address(NetworkParameters.prodNet(), "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");</pre>
     */
    public Address(int ver, String address) throws RadarException {
        super(address);
        if (version != ver)
            throw new RadarException("Mismatched version number, trying to cross networks? " + version +
                    " vs " + ver);
    }

    /** The (big endian) 20 byte hash that is the core of a BitCoin address. */
    public byte[] getHash160() {
        return bytes;
    }
}
