package org.radarlab.core;

import org.radarlab.btc.BitUtil;
import org.radarlab.core.exception.RadarException;
import org.radarlab.core.hash.B58;
import org.radarlab.core.hash.RFC1751;
import org.radarlab.crypto.ecdsa.IKeyPair;
import org.radarlab.crypto.ecdsa.Seed;
import org.ripple.bouncycastle.util.encoders.Hex;

import java.util.Random;


/**
 * Wallet contains Seed, AccountID, KeyPairs
 *
 * @author Fau
 */
public class Wallet {
    private AccountID accountId;
    private byte[] master_seed_hex; //128 bit
    private String master_seed; //equal to "rippled -q wallet_propose masterpassphrase"
    private IKeyPair keyPair;
    private String passphrase;
    private String master_key;

    /**
     * Create a brand new wallet, random seed
     */
    public Wallet() {
        Random rand = new Random(System.currentTimeMillis());
        long l1 = rand.nextLong();
        master_seed_hex = new byte[16];
        BitUtil.uint64ToByteArrayLE(l1, master_seed_hex, 0);
        long l2 = rand.nextLong();
        BitUtil.uint64ToByteArrayLE(l2, master_seed_hex, 8);
        master_seed = b58.encodeFamilySeed(master_seed_hex);
        keyPair = Seed.createKeyPair(master_seed_hex);
        accountId = AccountID.fromKeyPair(keyPair);
        master_key = RFC1751.key2English(master_seed_hex);
    }

    /**
     * Create a wallet, from a special passphrase
     */
    public Wallet(String passPhrase) {
        passphrase = passPhrase;
        master_seed_hex = Seed.passPhraseToSeedBytes(passphrase);
        master_seed = b58.encodeFamilySeed(master_seed_hex);
        keyPair = Seed.createKeyPair(master_seed_hex);
        accountId = AccountID.fromKeyPair(keyPair);
        master_key = RFC1751.key2English(master_seed_hex);
    }

    public static Wallet fromPrivateKey(String privStr) {
        byte[] hex = Seed.genSeedFromBtcPriv(privStr);
        IKeyPair kp = Seed.createKeyPair(hex);
        Wallet ret = new Wallet();
        ret.accountId = AccountID.fromKeyPair(kp);
        ret.master_seed_hex = hex;
        ret.master_seed = b58.encodeFamilySeed(hex);
        ret.keyPair = kp;
        ret.master_key = RFC1751.key2English(hex);
        return ret;
    }

    public static Wallet fromSeedString(String seed) {
        if (seed == null || seed.length() < B58.LEN_FAMILY_SEED - 2 || !seed.startsWith("s"))
            throw new RadarException("AccountID.fromSeedString() param ERROR: " + seed);
        byte[] b16 = b58.decodeFamilySeed(seed);
        if (b16.length != 16) throw new RadarException("decodeFamilySeed() not 16 bytes: " + Hex.toHexString(b16));
        IKeyPair kp = Seed.createKeyPair(b16);
        Wallet ret = new Wallet();
        ret.accountId = AccountID.fromKeyPair(kp);
        ret.master_seed_hex = b16;
        ret.master_seed = b58.encodeFamilySeed(b16);
        ret.master_key = RFC1751.key2English(b16);
        ret.keyPair = kp;
        return ret;
    }

    public AccountID account() {
        return this.accountId;
    }

    public IKeyPair keyPair() {
        return this.keyPair;
    }

    public String seed() {
        return this.master_seed;
    }

    public byte[] seedHex() {
        return this.master_seed_hex;
    }

    public String passphrase() {
        return this.passphrase;
    }

    public String getMaster_key() {
        return master_key;
    }

    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer(1024);
        ret.append("account_id : ").append(accountId.address)
                .append("\tmaster_seed : ").append(master_seed)
                .append("\tmaster_seed_hex : ").append(Hex.toHexString(master_seed_hex))
                .append("\tmaster_key: ").append(master_key);
        if (keyPair != null) {
            ret.append("\tpubKey=").append(keyPair.pubHex())
                    .append("\tpriKey=").append(keyPair.privHex());
        }
        if (passphrase != null) {
            ret.append("\tpassphrase=").append(passphrase);
        }
        return ret.toString();
    }

    public static B58 b58 = B58.getInstance();

}
