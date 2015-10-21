package org.radarlab.test;

import com.google.gson.Gson;
import org.radarlab.client.ws.RadarWebSocketClient;
import org.radarlab.core.*;
import org.radarlab.core.hash.B58;
import org.radarlab.core.types.known.tx.Transaction;
import org.radarlab.core.types.known.tx.result.TransactionMeta;
import org.radarlab.core.types.known.tx.signed.SignedTransaction;
import org.radarlab.core.uint.UInt32;
import org.radarlab.crypto.ecdsa.IKeyPair;
import org.radarlab.crypto.ecdsa.Seed;
import org.json.JSONObject;
import org.junit.Test;
import org.radarlab.core.types.known.tx.txns.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Test some kinds of Tx, only sign the object, not really send to server .
 */
public class TestTxn {
    final String seed_1 = "snoPBrXtMeMyMHUVTgbuqAfg1SUTb"; // Please change me !!
    final String seed_2 = "snoPBrXtMeMyMHUVTgbuqAfg1SUTb"; // Please change me !!
    AccountID destAddr       = AccountID.fromAddress("rEW7hNCFx5JJXPyfu7YWbHkogko5PTdG5q"); // Please change me !!
    public static String dest2Str   = "rEVEyZNAVpE28TUxzRtVWmVRLnivKCbZnD";                 // Please change me !!
    public static String destCNYStr = "rPYppqDZJ92jhRnwDesttrYm2mEqiGtHq3";                 // Please change me !!

    AccountID dest2          = AccountID.fromAddress(dest2Str);
    AccountID addrCNYGateway = AccountID.fromAddress(destCNYStr);


    /**
     * Sign tx using seed
     * about fee, please check: https://cnwiki.radarlab.org/transaction_fee
     * about seed, please check: https://cnwiki.radarlab.org/ds:account
     * about tx, please check: http://www.radarlab.org/dev/transactions.html
     */
    @Test
    public void testSignTx(){
        System.out.println(System.currentTimeMillis());
        System.out.println(Amount.fromString("1000"));
        String txJson = "{\n" +
                " \"TransactionType\": \"Payment\",\n" +
                " \"Account\": \"account_address_here\",\n" +
                " \"Destination\": \"recipient_address_here\",\n" +
                " \"Amount\": {\n" +
                " \"currency\": \"CNY\",\n" +
                " \"value\": \"0.013\",\n" +
                " \"issuer\": \"rLUti5o23WCkJ4YYqSDU5qywLdUHnFwDw6\"\n" +
                " }\n}";
        JSONObject tx = new JSONObject(txJson);
        STObject txObj = STObject.fromJSONObject(tx);
        Transaction txn = (Transaction) txObj;
        String seed = "seed_here";
        IKeyPair kp = Seed.getKeyPair(seed);
        SignedTransaction signedTransaction = new SignedTransaction(txn);
        signedTransaction.prepare(kp, Amount.fromString("0.001"), new UInt32(28), null);
        System.out.println(signedTransaction.tx_blob);
        System.out.println(signedTransaction.hash.toHex());
    }

    /**
     * about ActivateAccount, please check: http://www.radarlab.org/dev/transactions.html#activeaccount
     */
    @Test
    public void testActivateAccount(){
        String seed = "test_seed_here";
        String reference = "reference_address_here";
        String referee = "referee_address_here";
        Amount amount = new Amount(new BigDecimal(1000).divide(new BigDecimal(1000000)),Currency.VBC,AccountID.VBC_0);

        IKeyPair kp = Seed.getKeyPair(seed);
        ActiveAccount activeAccount = new ActiveAccount();
        AccountID referenceAccount = AccountID.fromAddress(reference);
        AccountID refereeAccount = AccountID.fromAddress(referee);
        activeAccount.referee(refereeAccount);
        activeAccount.reference(referenceAccount);
        if (amount != null) {
            activeAccount.amount(amount);
        }

        activeAccount.account(AccountID.fromSeedBytes(B58.getInstance().decodeFamilySeed(seed)));

        Amounts amounts = new Amounts();

        Entry entry = new Entry();
        entry.Amount(Amount.fromDropString("10000"));
        amounts.add(entry);

        Limits limits = new Limits();
        Entry entryLimit = new Entry();
        entryLimit.LimitAmount(new Amount(new BigDecimal(10000000),Currency.fromString("CNY"),AccountID.fromAddress("rLUti5o23WCkJ4YYqSDU5qywLdUHnFwDw6")));
        entryLimit.Flags(new UInt32(131072));
        limits.add(entryLimit);

        Entry entryLimit2 = new Entry();
        entryLimit2.LimitAmount(new Amount(new BigDecimal(10000000),Currency.fromString("USD"),AccountID.fromAddress("rLUti5o23WCkJ4YYqSDU5qywLdUHnFwDw6")));
        entryLimit2.Flags(new UInt32(131072));
        limits.add(entryLimit2);

        activeAccount.Amounts(amounts);
        activeAccount.Limits(limits);

        long fee = 1000;
        if (amount != null) {
            if (amount.currencyString().equals("VRP") || amount.currencyString().equals("VBC")) {
                fee = (long) (amount.multiply(new BigDecimal("1000")).doubleValue());
            }
        }
        fee = Math.max(1000, fee);
        fee = 10000 + fee;


        SignedTransaction sign = new SignedTransaction(activeAccount);
        sign.prepare(kp, Amount.fromString(String.valueOf(fee)), new UInt32(9), null);

        System.out.println("blob:" + sign.tx_blob);
        System.out.println("hash:" + sign.hash);
        System.out.println("txn_type:" + sign.transactionType());
        System.out.println(activeAccount);
    }

    @Test
    public void testCreatePaymentTxSign() throws Exception {
        IKeyPair kp = Seed.getKeyPair(seed_1);
        Payment txn = new Payment();
        //set the target address of Tx
        txn.destination(destAddr);
        //set the amount 0.01 VRP, note: VRP unit needs * 10^6
        txn.amount(Amount.fromString("10000"));
        //set the sender
        txn.account(AccountID.fromSeedBytes(B58.getInstance().decodeFamilySeed(seed_1)));

        //signing...
        SignedTransaction sign = new SignedTransaction(txn);
        // param 2 is tx fee;   param 3 is account_info.sequence returned
        sign.prepare(kp, Amount.fromString("10"), new UInt32(16), null );
        System.out.println("blob:" + sign.tx_blob);
        System.out.println("hash:" + sign.hash);
        System.out.println("txn_type:" + sign.transactionType());
        System.out.println(txn);
    }

    @Test
    public void testCreatePaymentVBCTxSign(){
        IKeyPair kp = Seed.getKeyPair(seed_1);
        Payment txn = new Payment();
        //set the target address of Tx
        txn.destination(destAddr);
        //set tx fee, VBC unit needs not * 10^6
        Amount vbc = new Amount(new BigDecimal("10000"), Currency.VBC, AccountID.VBC_0);
        txn.amount(vbc);
        //set the sender
        txn.account(AccountID.fromSeedBytes(B58.getInstance().decodeFamilySeed(seed_1)));
        SignedTransaction sign = new SignedTransaction(txn);
        // param 2 is tx fee;   param 3 is account_info.sequence returned
        sign.prepare(kp, Amount.fromString("15"), new UInt32(16), null );
        System.out.println("blob:" + sign.tx_blob);
        System.out.println("hash:" + sign.hash);
        System.out.println("txn_type:" + sign.transactionType());
        System.out.println(txn);
    }

    @Test
    public void testCreateAddRefereeSign() throws Exception {
        IKeyPair kp = Seed.getKeyPair(seed_2);
        AddReferee txn = new AddReferee();
        //set sender, aka invite target
        txn.account(AccountID.fromSeedBytes(B58.getInstance().decodeFamilySeed(seed_2)));
        //set target, aka invite source
        txn.destination(dest2);
        SignedTransaction sign = new SignedTransaction(txn);
        // param 2 is tx fee;   param 3 is account_info.sequence returned
        sign.prepare(kp, Amount.fromString("15"), new UInt32(13), null );
        System.out.println("blob:" + sign.tx_blob);
        System.out.println("hash:" + sign.hash);
        System.out.println("txn_type:" + sign.transactionType());
    }

    @Test
    public void testOfferCreateTxSign(){
        IKeyPair kp = Seed.getKeyPair(seed_2);
        OfferCreate txn = new OfferCreate();
        //set sender
        txn.account(AccountID.fromSeedBytes(B58.getInstance().decodeFamilySeed(seed_2)));
        //other currency, need not * 10^6
        txn.takerGets(new Amount(new BigDecimal("10"), Currency.fromString("DNC"), dest2));
        txn.takerPays(Amount.fromString("20000000"));
        SignedTransaction sign = new SignedTransaction(txn);
        // param 2 is tx fee;   param 3 is account_info.sequence returned
        sign.prepare(kp, Amount.fromString("15"), new UInt32(14), null );
        System.out.println("blob:" + sign.tx_blob);
        System.out.println("hash:" + sign.hash);
        System.out.println("txn_type:" + sign.transactionType());
    }

    @Test
    public void testOfferCancelTxSign(){
        IKeyPair kp = Seed.getKeyPair(seed_2);
        OfferCancel txn = new OfferCancel();
        //set sender
        txn.account(AccountID.fromSeedBytes(B58.getInstance().decodeFamilySeed(seed_2)));
        txn.offerSequence(new UInt32(14));//account_offers.seq
        SignedTransaction sign = new SignedTransaction(txn);
        // param 2 is tx fee;   param 3 is account_info.sequence returned
        sign.prepare(kp, Amount.fromString("15"), new UInt32(15), null );
        System.out.println("blob:" + sign.tx_blob);
        System.out.println("hash:" + sign.hash);
        System.out.println("txn_type:" + sign.transactionType());
    }

    @Test
    public void testTrustSetTxSign(){
        IKeyPair kp = Seed.getKeyPair(seed_2);
        TrustSet txn = new TrustSet();
        //set sender
        txn.account(AccountID.fromSeedBytes(B58.getInstance().decodeFamilySeed(seed_2)));
        //set TrustLimit. Note, Amount unit must be integer.
        Amount limitAmount = new Amount(new BigDecimal("10000"), Currency.fromString("CNY"), addrCNYGateway);
        txn.limitAmount(limitAmount);
        SignedTransaction sign = new SignedTransaction(txn);
        // param 2 is tx fee;   param 3 is account_info.sequence returned
        sign.prepare(kp, Amount.fromString("15"), new UInt32(17), null );
        System.out.println("blob:" + sign.tx_blob);
        System.out.println("hash:" + sign.hash);
        System.out.println("txn_type:" + sign.transactionType());
    }

    @Test
    public void testTransactionJsonParser() throws Exception {
        String json = "{" +
                "  \"Account\": \"raD5qJMAShLeHZXf9wjUmo6vRK4arj9cF3\"," +
                "  \"Fee\": \"10\"," +
                "  \"Flags\": 0," +
                "  \"Sequence\": 103929," +
                "  \"SigningPubKey\": \"028472865AF4CB32AA285834B57576B7290AA8C31B459047DB27E16F418D6A7166\"," +
                "  \"TakerGets\": {" +
                "    \"currency\": \"ILS\"," +
                "    \"issuer\": \"rNPRNzBB92BVpAhhZr4iXDTveCgV5Pofm9\"," +
                "    \"value\": \"1694.768\"" +
                "  }," +
                "  \"TakerPays\": \"98957503520\"," +
                "  \"TransactionType\": \"OfferCreate\"," +
                "  \"TxnSignature\": \"304502202ABE08D5E78D1E74A4C18F2714F64E87B8BD57444AFA5733109EB3C077077520022100DB335EE97386E4C0591CAC024D50E9230D8F171EEB901B5E5E4BD6D1E0AEF98C\"," +
                "  \"hash\": \"232E91912789EA1419679A4AA920C22CFC7C6B601751D6CBE89898C26D7F4394\"," +
                "  \"metaData\": {" +
                "    \"AffectedNodes\": [" +
                "      {" +
                "        \"CreatedNode\": {" +
                "          \"LedgerEntryType\": \"Offer\"," +
                "          \"LedgerIndex\": \"3596CE72C902BAFAAB56CC486ACAF9B4AFC67CF7CADBB81A4AA9CBDC8C5CB1AA\"," +
                "          \"NewFields\": {" +
                "            \"Account\": \"raD5qJMAShLeHZXf9wjUmo6vRK4arj9cF3\"," +
                "            \"BookDirectory\": \"62A3338CAF2E1BEE510FC33DE1863C56948E962CCE173CA55C14BE8A20D7F000\"," +
                "            \"OwnerNode\": \"000000000000000E\"," +
                "            \"Sequence\": 103929," +
                "            \"TakerGets\": {" +
                "              \"currency\": \"ILS\"," +
                "              \"issuer\": \"rNPRNzBB92BVpAhhZr4iXDTveCgV5Pofm9\"," +
                "              \"value\": \"1694.768\"" +
                "            }," +
                "            \"TakerPays\": \"98957503520\"" +
                "          }" +
                "        }" +
                "      }," +
                "      {" +
                "        \"CreatedNode\": {" +
                "          \"LedgerEntryType\": \"DirectoryNode\"," +
                "          \"LedgerIndex\": \"62A3338CAF2E1BEE510FC33DE1863C56948E962CCE173CA55C14BE8A20D7F000\"," +
                "          \"NewFields\": {" +
                "            \"ExchangeRate\": \"5C14BE8A20D7F000\"," +
                "            \"RootIndex\": \"62A3338CAF2E1BEE510FC33DE1863C56948E962CCE173CA55C14BE8A20D7F000\"," +
                "            \"TakerGetsCurrency\": \"000000000000000000000000494C530000000000\"," +
                "            \"TakerGetsIssuer\": \"92D705968936C419CE614BF264B5EEB1CEA47FF4\"" +
                "          }" +
                "        }" +
                "      }," +
                "      {" +
                "        \"ModifiedNode\": {" +
                "          \"FinalFields\": {" +
                "            \"Flags\": 0," +
                "            \"IndexPrevious\": \"0000000000000000\"," +
                "            \"Owner\": \"raD5qJMAShLeHZXf9wjUmo6vRK4arj9cF3\"," +
                "            \"RootIndex\": \"801C5AFB5862D4666D0DF8E5BE1385DC9B421ED09A4269542A07BC0267584B64\"" +
                "          }," +
                "          \"LedgerEntryType\": \"DirectoryNode\"," +
                "          \"LedgerIndex\": \"AB03F8AA02FFA4635E7CE2850416AEC5542910A2B4DBE93C318FEB08375E0DB5\"" +
                "        }" +
                "      }," +
                "      {" +
                "        \"ModifiedNode\": {" +
                "          \"FinalFields\": {" +
                "            \"Account\": \"raD5qJMAShLeHZXf9wjUmo6vRK4arj9cF3\"," +
                "            \"Balance\": \"106861218302\"," +
                "            \"Flags\": 0," +
                "            \"OwnerCount\": 9," +
                "            \"Sequence\": 103930" +
                "          }," +
                "          \"LedgerEntryType\": \"AccountRoot\"," +
                "          \"LedgerIndex\": \"CF23A37E39A571A0F22EC3E97EB0169936B520C3088963F16C5EE4AC59130B1B\"," +
                "          \"PreviousFields\": {" +
                "            \"Balance\": \"106861218312\"," +
                "            \"OwnerCount\": 8," +
                "            \"Sequence\": 103929" +
                "          }," +
                "          \"PreviousTxnID\": \"DE15F43F4A73C4F6CB1C334D9E47BDE84467C0902796BB81D4924885D1C11E6D\"," +
                "          \"PreviousTxnLgrSeq\": 3225338" +
                "        }" +
                "      }" +
                "    ]," +
                "    \"TransactionIndex\": 0," +
                "    \"TransactionResult\": \"tesSUCCESS\"" +
                "  }" +
                "}";

        JSONObject txJson = new JSONObject(json);
        STObject meta = STObject.fromJSONObject((JSONObject) txJson.remove("metaData"));
        TransactionMeta txnMeta = (TransactionMeta) meta;
        System.out.println("affectedNodes:"+txnMeta.affectedNodes());
        System.out.println("TransactionResult:"+txnMeta.engineResult());
        System.out.println("TransactionIndex:"+txnMeta.transactionIndex());
        STObject tx = STObject.fromJSONObject(txJson);
        Transaction txn = (Transaction) tx;
        System.out.println("Account:"+txn.account().address);
        System.out.println("fee:"+txn.fee());
        System.out.println("txType:"+txn.transactionType());
        System.out.println("ledger_sequence::"+txn.lastLedgerSequence());
        System.out.println("AccountTxId:"+txn.accountTxnID());
        System.out.println("flags:"+txn.flags());
        System.out.println("PreviousTxId:"+txn.previousTxnID());
        System.out.println("sequence:"+txn.sequence());
        System.out.println("source tag:"+txn.sourceTag());
        System.out.println("operationLimit:"+txn.operationLimit());
        System.out.println("signature:"+txn.txnSignature().toHex());
        switch (txn.transactionType()){
            case OfferCreate:
                OfferCreate oc = (OfferCreate) txn;
                System.out.println("OfferCreate-takerGets:"+oc.takerGets());
                System.out.println("OfferCreate-takerPays:"+oc.takerPays());
                break;
            case Payment:
                Payment payment = (Payment) txn;
                System.out.println("Payment-amount:"+payment.amount());
                System.out.println("Payment-amount:"+payment.destination());
                System.out.println("Payment-amount:"+payment.sendMax());
                break;
            case OfferCancel:
                OfferCancel ocl = (OfferCancel) txn;
                System.out.println("OfferCancel-offerSequence:"+ocl.offerSequence());
                break;
            case AddReferee:
                AddReferee arf = (AddReferee) txn;
                System.out.println("AddReferee-destAddr:"+arf.destination());
                break;
            case TrustSet:
                TrustSet trustSet = (TrustSet) txn;
                System.out.println("TrustSet-limitAmount:"+trustSet.limitAmount());
                System.out.println("TrustSet-qualityIn:"+trustSet.qualityIn());
                System.out.println("TrustSet-qualityOut:"+trustSet.qualityOut());
                break;
        }
    }

    @Test
    public void testMakeTx(String tx_blob) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 0);
        data.put("command", "submit");
        data.put("tx_blob", tx_blob);
        String postData = new Gson().toJson(data);
        String json = RadarWebSocketClient.request(postData);
        System.out.println("make tx result: " + json);
    }
}
