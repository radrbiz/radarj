package org.radarlab.test;

import org.radarlab.core.AccountID;
import org.radarlab.core.Utils;
import org.radarlab.core.Wallet;
import org.radarlab.core.exception.RadarException;
import org.radarlab.core.hash.B58;
import org.radarlab.core.hash.RFC1751;
import org.radarlab.crypto.ecdsa.IKeyPair;
import org.radarlab.crypto.ecdsa.KeyPair;
import org.radarlab.crypto.ecdsa.Seed;
import org.junit.Test;
import org.ripple.bouncycastle.util.encoders.Hex;

import static org.junit.Assert.assertEquals;

public class TestCrypto {

    public static B58 b58 = B58.getInstance();

    @Test
    public void testBase58(){
        byte[] address = Hex.decode("000103996A3BAD918657F86E12A67D693E8FC8A814DA4B958A244B5F14D93E57");
        String addr = b58.encodeToString(address);
        System.out.println(addr);

        addr = "r9af63Nf7bRFajz4DLm4DvxWc2UcTXhLBD";
        byte[] bs  = b58.decode(addr);
        System.out.println(Hex.toHexString(bs));

        byte[] bs2 = b58.decodeChecked(addr, B58.VER_ACCOUNT_ID);
        System.out.println("  " + Hex.toHexString(bs2));
    }

    @Test
    public void testRootSeed() {
        try {

            AccountID rootId =  AccountID.accounts.get("root");
            System.out.println("root address : " + rootId.address + ", " + rootId.toHex() + ", " + rootId.isNativeIssuer() + ", " + rootId.toJSON());

            byte[] seed = Seed.passPhraseToSeedBytes("masterpassphrase");
            System.out.println("master_seed_hex: " + Hex.toHexString(seed));

            System.out.println("master_seed    :" + b58.encodeFamilySeed(seed));
            System.out.println("master_seed dec: " + Hex.toHexString(b58.decodeFamilySeed("snoPBrXtMeMyMHUVTgbuqAfg1SUTb")));

            IKeyPair keyPair = Seed.getKeyPair(seed);
            System.out.println("seed to priv   : " + keyPair.privHex() + ", pub=" + keyPair.pubHex());

            Wallet w = Wallet.fromSeedString("snoPBrXtMeMyMHUVTgbuqAfg1SUTb");
            System.out.println("wallet to priv : " + w.keyPair().privHex() + ", pub=" + w.keyPair().pubHex());

            rootId = new AccountID(Utils.SHA256_RIPEMD160(Hex.decode("0330e7fc9d56bb25d6893ba3f317ae5bcf33b3291bd63db32654a313222f7fd020")));
            System.out.println("pub to address : " + rootId.address);

            System.out.println("account public : " + b58.encodeAccountPublic(keyPair.pubBytes()));
            System.out.println("node public    : " + b58.encodeNodePublic(keyPair.pubBytes()));

            Wallet another = Wallet.fromSeedString("sheZXgECjgUDE4EPf7DFf9jtp7pYr");
            System.out.println("fromSeedString : " + another);

        } catch (RadarException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPrivateKey() {
        String btcPriv = "1acaaedece405b2a958212629e16f2eb46b153eee94cdd350fdeff52795525b7";  // root's private key
        IKeyPair kp = new KeyPair(Hex.decode(btcPriv));
        System.out.println("from priv   : " + kp.privHex());
        System.out.println("priv gen pub: " + kp.pubHex());

        AccountID rootId = new AccountID(Utils.SHA256_RIPEMD160(kp.pubBytes()));
        System.out.println("pub to addr : " + rootId.address);
    }


    @Test
    public void testBTC_Compatible() {
        Wallet w = Wallet.fromPrivateKey("0615dd0779606fdd958fb6ef5a608c378577b3f5e8c33fe910ec8d0400cdf044");
        System.out.println(w);
    }

    @Test
    public void testWallet() {
        Wallet w1 = new Wallet("masterpassphrase");
        System.out.println(w1);


//        for(int i=0; i<10; i++) {
//            Wallet w2 = new Wallet();
//            System.out.println(w2);
//        }
    }

    @Test
    public void testRFC1751(){
        String key = RFC1751.getKeyFromEnglish("AHOY CLAD JUDD NOON MINI CHAD CUBA JAN KANT AMID DEL LETS");
        System.out.println(key);
        assertEquals(key.toUpperCase(), "5BDD10A694F2E36CCAC0CBE28CE2AC49");
    }

    @Test
    public void testNodePub() {
        /*
rippled -q validation_create
{
   "result" : {
      "status" : "success",
      "validation_key" : "BAIT HESS BLAB MID WAGE PRO HANG FIST REEL FOIL ROAM FIST",
      "validation_public_key" : "n9MkWc3kiRaLQfSU2HnkyyzzqRZYMJVGiKoHFmbLPEXWzztMZYXy",
      "validation_seed" : "sp5xMchNAcZBPQ8EfPALsPdjuyUdp"
   }
}
         */
        String b58Str[] = {     "sp5xMchNAcZBPQ8EfPALsPdjuyUdp",
                                "ssDmxiYWcNeMaZCJDK2YpXKG7PPdR",
                                "snfC8R8qPCngKaLSfeVNGSJCPKx9A"};
        String publicKey[] = {  "n9MkWc3kiRaLQfSU2HnkyyzzqRZYMJVGiKoHFmbLPEXWzztMZYXy",
                                "n9Jd5AbYqMiAkuDjQwoPHkU6VcNDbp5cVUqrWBwZrBetaoggnPmv",
                                "n9Mw8nDQZuuj1EHoLNGFfJ6SbE1TbsyPDScn5QrmywUDWbreZHfs"
        };
        int i = 0;
        for(String s : b58Str) {
            Seed seed = Seed.fromBase58(s);
            String np = b58.encodeNodePublic(seed.rootKeyPair().pubBytes());
            System.out.println(np);
            assertEquals(np, publicKey[i++]);
        }

    }

}
