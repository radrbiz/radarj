package org.radarlab.client.util;

import org.radarlab.core.AccountID;
import org.radarlab.core.Utils;
import org.radarlab.core.hash.B58;

import java.math.BigInteger;

/**
 * Create some magic address, like:
 */
public class CreateMagicRadarAddress {
    public static B58 b58 = B58.getInstance();

    public static void main(String[] v) {
        String startSeed = "sheZXgECjgUDE4EPf7DFf9HZgEwiN";
        byte[] seed_hex = null;
        String prefix = "rada";
        int targetCount = 3;
        if(v.length > 0 && v[0] != null) {
            System.out.println(v[0]);
            if(v[0].startsWith("s") && v[0].length() == B58.LEN_FAMILY_SEED) {
                startSeed = v[0];
                seed_hex = b58.decodeFamilySeed(startSeed);
            } else if(v[0].length() == B58.LEN_FAMILY_SEED_HEX * 2) {
                seed_hex = Convert.hexToBytes(v[0]);
            }
        } else {
            if (seed_hex == null) seed_hex = b58.decodeFamilySeed(startSeed);
        }
        if(v.length > 1 && v[1] != null) prefix = v[1];
        if(v.length > 2 && v[2] != null) targetCount = Integer.parseInt(v[2]);

        magic(seed_hex, prefix, targetCount);
        System.exit(0);
    }

    public static void magic(byte[] seed_hex, String prefix, int targetCount) {
        BigInteger bi = Utils.uBigInt(seed_hex);
        System.out.println("Initial seed_dec is " + bi.longValue());

        long ts0 = System.currentTimeMillis();
        int round = 0, bingo = 0;

        while(true) {
            byte[] b16 = Utils.lowArray(bi.toByteArray(), B58.LEN_FAMILY_SEED_HEX);
            AccountID a = AccountID.fromSeedBytes(b16);
            if(a.address.startsWith(prefix)) {
                long ts1 = System.currentTimeMillis();
                System.out.println("====== Bingo! ==> round:" + round + ", time=" + (ts1 - ts0) + "ms, count=" + bingo);
                System.out.println("Addr=" + a + " seed_hex=" + Convert.bytesToHex(b16) + " seed=" + b58.encodeFamilySeed(b16));
                bingo++;
                if (bingo >= targetCount)
                    break;
            }
            bi = bi.add(BigInteger.ONE);
            round++;
            if(round % 1000 == 0) {
                long ts1 = System.currentTimeMillis();
                System.out.println("  round: " + round + ", cur seed_dec is : " + bi.longValue() + ", time=" + (ts1-ts0) + "ms");
            }
        }//end while

        long ts2 = System.currentTimeMillis();
        System.out.println("Ending...  round: " + round + ", cur seed_dec is : " + bi.longValue() + ", total_time=" + (ts2-ts0) + "ms");
    }
}
