package org.radarlab.test;

import org.radarlab.btc.Base58;
import org.radarlab.btc.BitUtil;
import org.radarlab.btc.ECKey;
import org.radarlab.btc.account.Address;
import org.radarlab.core.exception.RadarException;
import org.junit.Test;
import org.ripple.bouncycastle.util.encoders.Hex;

import java.security.MessageDigest;

import static org.radarlab.btc.BitUtil.bytesToHexString;

public class TestBTC {

    @Test
    public void testBitCoinAddressFromPub() {
        byte[] pub = Hex.decode("0450863ad64a87ae8a2fe83c1af1a8403cb53f53e486d8511dad8a04887e5b23522cd470243453a299fa9e77237716103abc11a1df38855ed6f2ee187e9c582ba6");
        byte[] priv = null;
        ECKey ecKey = new ECKey(priv, pub);
        System.out.println(bytesToHexString(ecKey.getPubKey()));
        //Step 3,4. SHA256 then  RIPEMD-160
        System.out.println(bytesToHexString(ecKey.getPubKeyHash()));
        //Step 5,6,7,8,9. RIPEMD160(SHA256(input))
        Address addr = ecKey.toAddress(BitUtil.ADDRESS_VERSION);
        //step 10. Base58
        System.out.println(addr.toString());
    }


    @Test
    public void testBtcAddr() throws RadarException {
        BitUtil.DEBUG = false;
        boolean b1 = BitUtil.isBtcAddress("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
        boolean b2 = BitUtil.isBtcAddress("17kzeh4N8g44GFvdDzSf8PjaPfyoD1MndL");
        boolean b3 = BitUtil.isBtcAddress("17kzeh4N8g49GFvdDzSf8PjaPfyoD1Mnd");
        boolean b4 = BitUtil.isBtcAddress("1DkyBEKt5S2GDtv7aQw6rQepAvnsRyHoYM");
        boolean b5 = BitUtil.isBtcAddress("1EpqtDEYwDMxsNDKQbBjRr7uCUsF2U1T4N");
        System.out.println(b1 + ", " + b2 + ", " + b3 + ", " + b4 + ", " + b5);

        //private key in Base58 format
        ECKey ec = ECKey.getECFromPrivStr("QknNc32zSrbHEEpf8UGX7WeFxy4aoYP6X9KgFkxPms1");
        System.out.println("priv in Base58 : " + bytesToHexString(ec.getPrivKeyBytes()));
        System.out.println(ec.toAddress(0).toString());

        //private key in BTC-Qt format
        ec = ECKey.getECFromPrivStr("5Hry2QJML67sWouWHCfSd2r31FSiP7w4TsqzWRCUu6LM5ESv2F9");
        System.out.println("priv in Btc-Qt : " + bytesToHexString(ec.getPrivKeyBytes()));
        System.out.println(ec.toAddress(0).toString());

        //private key in HEX
        ec = ECKey.getECFromPrivStr("0615dd0779606fdd958fb6ef5a608c378577b3f5e8c33fe910ec8d0400cdf044");
        System.out.println("priv in HEX    : " + bytesToHexString(ec.getPrivKeyBytes()));
        System.out.println(ec.toAddress(0).toString());

        //private key in Base64
        ec = ECKey.getECFromPrivStr("BhXdB3lgb92Vj7bvWmCMN4V3s/Xowz/pEOyNBADN8EQ=");
        System.out.println("priv in Base64 : " + bytesToHexString(ec.getPrivKeyBytes()));
        System.out.println(ec.toAddress(0).toString());
        System.out.println( bytesToHexString(ec.getPubKey()) );

        //get a new ECKey,addr
        ECKey ec2 = new ECKey();
        System.out.println("generate new ec: " + ec2.toAddress(Base58.VER_ADDRESS) + "   priv:" + bytesToHexString(ec2.getPrivKeyBytes()));
    }

    @Test
    public void testHash() {
        try {
            byte[] input = "This is a string test".getBytes();
            //byte[] input = "hello".getBytes();

            //1. sha256(i)
            byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(input);
            System.out.println(bytesToHexString(sha256));

            //2. sha256( sha256(i) )
            sha256 = BitUtil.doubleDigest(input);
            System.out.println(bytesToHexString(sha256));

            //3. ripemd( sha256(i) )
            byte[] ripemd = BitUtil.sha256hash160(input);
            System.out.println(bytesToHexString(ripemd));

            //4.1 addr to bytes
            System.out.println(bytesToHexString(Base58.decode("1EpqtDEYwDMxsNDKQbBjRr7uCUsF2U1T4N")));
            //4.2 bytes to addr
            System.out.println( Base58.encode(BitUtil.parseAsHexOrBase58("0097a5fda689533a9cddc55d4e79330d6b87966bf3532f3a3b")) );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
