package org.radarlab.api;

import com.google.gson.Gson;

import org.radarlab.client.ws.RadarWebSocketClient;
import org.radarlab.core.*;
import org.radarlab.core.fields.Field;
import org.radarlab.core.hash.B58;
import org.radarlab.core.serialized.enums.LedgerEntryType;
import org.radarlab.core.types.known.tx.Transaction;
import org.radarlab.core.types.known.tx.result.AffectedNode;
import org.radarlab.core.types.known.tx.result.TransactionMeta;
import org.radarlab.core.types.known.tx.signed.SignedTransaction;
import org.radarlab.core.types.known.tx.txns.*;
import org.radarlab.core.uint.UInt32;
import org.radarlab.crypto.ecdsa.IKeyPair;
import org.radarlab.crypto.ecdsa.Seed;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements of all kinds of transactions, defined by WebSocket API of Radar.
 * @see  "API documents"
 * @see org.radarlab.test.TestWebsocket for usage
 */
public class TransactionImpl {
    private static final Logger logger = Logger.getLogger(TransactionImpl.class);

    /**
     * See: https://ripple.com/build/rippled-apis/#path-find
     */
    public String findPath(String sourceAccount, String destinationAccount, Amount destinationAmount) throws APIException {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 0);
        data.put("command", "ripple_path_find");
        data.put("source_account", sourceAccount);
        data.put("destination_account", destinationAccount);

        if (destinationAmount.currencyString().equals("XRP") || destinationAmount.currencyString().equals("VRP")) {
            data.put("destination_amount", String.valueOf(destinationAmount.value().multiply(new BigDecimal("1000000")).longValue()));
        } else {
            Map<String, Object> destAmount = new HashMap<>();
            destAmount.put("issuer", destinationAccount);
            destAmount.put("value", String.valueOf(destinationAmount.doubleValue()));
            destAmount.put("currency", destinationAmount.currencyString());
            data.put("destination_amount", destAmount);
        }
        Map<String, Object> currency = new HashMap<>();
        currency.put("currency", destinationAmount.currencyString());
//        data.put("source_currencies", Collections.singletonList(currency));
        String postData = new Gson().toJson(data);
        logger.info("request:" + postData);
        String json = RadarWebSocketClient.req(postData);

        return json;
    }

    /**
     * See: https://ripple.com/build/rippled-apis/#book-offers
     */
    public JSONObject bookOffers(String currency1, String issuer1, String currency2, String issuer2, String taker, int limit) throws APIException {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 0);
        data.put("command", "book_offers");
        data.put("limit", limit * 10);
        if (taker == null) {
            data.put("taker", taker);
        }
        List<JSONObject> bids = new ArrayList<>();
        List<JSONObject> asks = new ArrayList<>();
        Map<String, Object> mapTakerGets = new HashMap<>();
        mapTakerGets.put("currency", currency1);
        if (issuer1 != null) {
            mapTakerGets.put("issuer", issuer1);
        }
        Map<String, Object> mapTakerPays = new HashMap<>();
        mapTakerPays.put("currency", currency2);
        if (issuer2 != null) {
            mapTakerPays.put("issuer", issuer2);
        }
        data.put("taker_gets", mapTakerGets);
        data.put("taker_pays", mapTakerPays);

        String postData = new Gson().toJson(data);
        String jsonStrAsk = RadarWebSocketClient.req(postData);
        if (jsonStrAsk != null) {
            JSONObject json = new JSONObject(jsonStrAsk);  //check success
            if (json.getString("status").equalsIgnoreCase("success")) {
                JSONObject jsonResult = json.getJSONObject("result");
                JSONArray jsonArray = jsonResult.getJSONArray("offers");
                asks = formatOfferArray(jsonArray, currency1, currency2, 0, limit);
                ;
            }
        }

        data.put("taker_gets", mapTakerPays);
        data.put("taker_pays", mapTakerGets);

        postData = new Gson().toJson(data);
        String jsonStrBids = RadarWebSocketClient.req(postData);
        if (jsonStrBids != null) {
            JSONObject json = new JSONObject(jsonStrBids);  //check success
            if (json.getString("status").equalsIgnoreCase("success")) {
                JSONObject jsonResult = json.getJSONObject("result");
                JSONArray jsonArray = jsonResult.getJSONArray("offers");
                bids = formatOfferArray(jsonArray, currency1, currency2, 1, limit);
            }
        }

        JSONObject result = new JSONObject();
        result.put("bids", bids.subList(0, limit < bids.size() ? limit : bids.size()));
        result.put("asks", asks.subList(0, limit < asks.size() ? limit : asks.size()));

        return result;
    }

    /**
     * See: https://ripple.com/build/rippled-apis/#submit
     */
    public String makeOffer(String seed, Amount taketGets, Amount takerPays, int sequence) throws APIException {
        IKeyPair kp = Seed.getKeyPair(seed);
        OfferCreate offer = new OfferCreate();
        offer.account(AccountID.fromSeedBytes(B58.getInstance().decodeFamilySeed(seed)));
        offer.takerGets(taketGets);
        offer.takerPays(takerPays);
        String fee = "1000";
        SignedTransaction sign = new SignedTransaction(offer);

        sign.prepare(kp, Amount.fromString(fee), new UInt32(sequence), null);
        String json = makeTx(sign.tx_blob);
        return json;
    }

    /**
     * retrieves a list of offers made by a given account that are outstanding as of a particular ledger version.
     * See: https://ripple.com/build/rippled-apis/#account-offers
     * @param limit   (Optional, default varies) Limit the number of transactions to retrieve. The server is not required to honor this value. Cannot be lower than 10 or higher than 400.
     * @return
     */
    public String accountOffers(String address, int limit, String c1, String issuer1, String c2, String issuer2, String marker) throws APIException {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 0);
        data.put("command", "account_offers");
        data.put("account", address);
        data.put("ledger", "current");
        if (limit >= 10 && limit <= 400) {
            data.put("limit", limit);
        }

        if (marker != null) {
            data.put("marker", marker);
        }
        String postData = new Gson().toJson(data);
        String jsonResult = RadarWebSocketClient.req(postData);
        JSONObject jsonObject = new JSONObject(jsonResult);
        if (jsonObject.getString("status").equalsIgnoreCase("success")) {
            JSONObject result = new JSONObject();
            JSONArray jsonArray = jsonObject.getJSONObject("result").getJSONArray("offers");
            if (jsonObject.getJSONObject("result").has("marker")) {
                String markerReturn = jsonObject.getJSONObject("result").getString("marker");
                result.put("marker", markerReturn);
            }

            result.put("offers", formatAccountOffersAll(jsonArray));
            return result.toString();
            //return jsonArray.toString();
        } else {
            throw new APIException(APIException.ErrorCode.UNKNOWN_ERROR, "unknow error");
        }
    }

    public String offerCancel(String seed, int offerSequence, int sequence) throws APIException {
        IKeyPair kp = Seed.getKeyPair(seed);
        OfferCancel offerCancel = new OfferCancel();
        offerCancel.account(AccountID.fromSeedBytes(B58.getInstance().decodeFamilySeed(seed)));
        offerCancel.offerSequence(new UInt32(offerSequence));
        SignedTransaction sign = new SignedTransaction(offerCancel);
        String fee = "1000";
        sign.prepare(kp, Amount.fromString(fee), new UInt32(sequence), null);
        String json = makeTx(sign.tx_blob);
        return json;
    }

    public String makePaymentTransaction(String seed, String recipient, Amount amount, int sequence, boolean isResolved, JSONArray paths, Amount sendMax) throws APIException {
        IKeyPair kp = Seed.getKeyPair(seed);
        Payment txn = new Payment();
        AccountID destination = AccountID.fromAddress(recipient);
        txn.destination(destination);
        txn.amount(amount);
        txn.account(AccountID.fromSeedBytes(B58.getInstance().decodeFamilySeed(seed)));
        if (paths != null) {
            txn.sendMax(sendMax);
            txn.paths(PathSet.translate.fromJSONArray(paths));
        }
        SignedTransaction sign = new SignedTransaction(txn);
        long fee = 1000;
        if (amount.currencyString().equals("VRP") || amount.currencyString().equals("XRP") || amount.currencyString().equals("VBC")) {
            fee = (long) (amount.doubleValue() * 1000);
        }
        fee = Math.max(1000, fee);
        if (!isResolved) {
            fee = 10000 + fee;
        }
        sign.prepare(kp, Amount.fromString(String.valueOf(fee)), new UInt32(sequence), null);
        String json = makeTx(sign.tx_blob);
        return json;
    }

    public String addReferee(String seed, String refereeAddress, int sequence) throws APIException {
        IKeyPair kp = Seed.getKeyPair(seed);
        AddReferee txn = new AddReferee();
        AccountID destination = AccountID.fromAddress(refereeAddress);
        txn.destination(destination);
        txn.account(AccountID.fromSeedBytes(B58.getInstance().decodeFamilySeed(seed)));
        SignedTransaction sign = new SignedTransaction(txn);
        long fee = 1000;
        sign.prepare(kp, Amount.fromString(String.valueOf(fee)), new UInt32(sequence), null);
        String json = makeTx(sign.tx_blob);
        return json;
    }

    public String trustSet(String seed, String issure, String currency, int sequence, boolean isRemove) throws APIException {
        IKeyPair kp = Seed.getKeyPair(seed);
        TrustSet txn = new TrustSet();
        int trustAmount = 1000000000;
        if (isRemove) {
            trustAmount = 0;
        }
        currency = currency.substring(0, 3);
        Amount limitAmount = new Amount(new BigDecimal(trustAmount), Currency.fromString(currency), AccountID.fromAddress(issure));
        txn.limitAmount(limitAmount);
        txn.account(AccountID.fromSeedBytes(B58.getInstance().decodeFamilySeed(seed)));
        SignedTransaction sign = new SignedTransaction(txn);
        long fee = 1000;
        sign.prepare(kp, Amount.fromString(String.valueOf(fee)), new UInt32(sequence), null);
        String json = makeTx(sign.tx_blob);
        return json;
    }


    /**
     * All methods using this makeTx, will call submit interface of "radard", and blob data needs be signed.
     * See: https://ripple.com/build/rippled-apis/#submit
     */
    public String makeTx(String tx_blob) throws APIException {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 0);
        data.put("command", "submit");
        data.put("tx_blob", tx_blob);
        String postData = new Gson().toJson(data);
        String json = RadarWebSocketClient.req(postData);
        logger.info("make tx result: " + json);
        return json;
    }


    public Map<String, Object> generateEffectListFromTxMetaByType(String address, TxObj item, TransactionMeta meta, String type) {
        List<Effect> effects = new ArrayList<>();
        List<Effect> showEffects = new ArrayList<>();
        for (AffectedNode node : meta.affectedNodes()) {
            switch (type) {
                case "failed":
                case "unknown":
                case "account_set":
                    if (node.isModifiedNode()) {
                        STObject obj = (STObject) node.get(Field.ModifiedNode);
                        STObject ff = (STObject) obj.get(Field.FinalFields);
                        Amount balance = (Amount) ff.get(Field.Balance);
                        Effect feeEffect = new Effect();
                        //if other payments, it stands for fee changes.
                        feeEffect.setAmount(new AmountObj(-item.getFee().getAmount(), "VRP", null));
                        feeEffect.setType("fee");
                        feeEffect.setBalance(new AmountObj(balance.doubleValue(), "VRP", null));
                        effects.add(feeEffect);
                    }
                case "radaring":
                case "offer_radaring":
                    if (node.isModifiedNode()) {
                        STObject obj = (STObject) node.get(Field.ModifiedNode);
                        STObject ff = (STObject) obj.get(Field.FinalFields);
                        if (ff != null) {
                            Amount highLimit = (Amount) ff.get(Field.HighLimit);
                            Amount lowLimit = (Amount) ff.get(Field.LowLimit);
                            Amount balance = (Amount) ff.get(Field.Balance);
                            STObject preFields = (STObject) obj.get(Field.PreviousFields);
                            if (highLimit != null && lowLimit != null) {

                                if (lowLimit.issuerString().equals(address)) {
                                    Effect effect = new Effect();
                                    if (preFields != null) {
                                        Amount preBalance = (Amount) preFields.get(Field.Balance);
                                        if (preBalance != null) {
                                            effect.setAmount(new AmountObj((preBalance.doubleValue() - balance.doubleValue()), balance.currencyString(), highLimit.issuerString()));
                                        }
                                    }
                                    if (effect.getAmount() == null) {
                                        effect.setAmount(item.getAmount());
                                    }
                                    effect.setType("amount");
                                    effect.setBalance(new AmountObj(-balance.doubleValue(), balance.currencyString(), balance.issuerString()));
                                    effects.add(effect);
                                }else{
                                    Effect effect = new Effect();
                                    if (preFields != null) {
                                        Amount preBalance = (Amount) preFields.get(Field.Balance);
                                        if (preBalance != null) {
                                            effect.setAmount(new AmountObj(-(preBalance.doubleValue() - balance.doubleValue()), balance.currencyString(), lowLimit.issuerString()));
                                        }
                                    }
                                    if (effect.getAmount() == null) {
                                        effect.setAmount(item.getAmount());
                                    }
                                    effect.setType("amount");
                                    effect.setBalance(new AmountObj(balance.doubleValue(), balance.currencyString(), balance.issuerString()));
                                    effects.add(effect);
                                }
                            }
                        }
                    }
                    continue;
                case "sent":
                case "received":
                    if (node.isModifiedNode() && (node.ledgerEntryType() == LedgerEntryType.AccountRoot || node.ledgerEntryType() == LedgerEntryType.RippleState)) {
                        STObject obj = (STObject) node.get(Field.ModifiedNode);
                        STObject ff = (STObject) obj.get(Field.FinalFields);
                        STObject prevFields = (STObject) obj.get(Field.PreviousFields);
                        if (ff != null) {
                            Amount highLimit = (Amount) ff.get(Field.HighLimit);
                            Amount lowLimit = (Amount) ff.get(Field.LowLimit);
                            Amount balance = (Amount) ff.get(Field.Balance);
                            AccountID account = (AccountID) ff.get(Field.Account);
                            Amount balanceVBC = (Amount) ff.get(Field.BalanceVBC);
                            //VRP&&VBC payment tx
                            if (account != null) {
                                //when VRP&&VBC payments, all balance change is in single FinalFields object in one ModifiedNode
                                if (account.address.equals(address)) {
                                    //VRP||VBC tx
                                    if (item.getAmount().getCurrency().equals("VRP") || item.getAmount().getCurrency().equals("VBC")) {
                                        boolean haveBalanceEffect = true;
                                        if (prevFields != null) {
                                            Amount preBalance = (Amount) prevFields.get(Field.Balance);
                                            if (preBalance != null) {
                                                if (preBalance.subtract(balance).doubleValue() * 1000000 == item.getFee().getAmount() * 1000000) {
                                                    haveBalanceEffect = false;
                                                }
                                            }
                                        }
                                        //if tx payment currency is payment, and is tx type is sent, then add a fee effect
                                        if ("sent".equals(item.getType())) {

                                            //VRP&VBC tx, balance is VRP balance..
                                            Effect feeEffect = new Effect();

                                            //if VRP payment, then compute balance from meta balance amount.
                                            if (item.getAmount().getCurrency().equals("VRP")) {
                                                if (prevFields != null) {
                                                    Amount preBalance = (Amount) prevFields.get(Field.Balance);
                                                    if (preBalance != null && (preBalance.subtract(balance).doubleValue() * 1000000 == item.getFee().getAmount() * 1000000)) {
                                                        feeEffect.setBalance(new AmountObj(balance.doubleValue(), "VRP", null));
                                                    } else {
                                                        feeEffect.setBalance(new AmountObj(balance.doubleValue() + item.getAmount().getAmount(), "VRP", null));
                                                    }
                                                } else {
                                                    feeEffect.setBalance(new AmountObj(balance.doubleValue() + item.getAmount().getAmount(), "VRP", null));
                                                }
                                            } else {
                                                feeEffect.setBalance(new AmountObj(balance.doubleValue(), "VRP", null));
                                            }
                                            feeEffect.setType("fee");
                                            feeEffect.setAmount(new AmountObj(-item.getFee().getAmount(), "VRP", null));
                                            effects.add(feeEffect);
                                        }
                                        if (haveBalanceEffect) {
                                            Effect effect = new Effect();
                                            if (item.getAmount().getCurrency().equals("VBC")) {
                                                balance = (Amount) ff.get(Field.BalanceVBC);
                                            }
                                            if (item.getAmount().getCurrency().equals("VRP")) {
                                                effect.setBalance(new AmountObj(balance.doubleValue(), "VRP", null));
                                            } else {
                                                effect.setBalance(new AmountObj(balanceVBC.doubleValue(), "VBC", null));
                                            }
                                            effect.setType("amount");
                                            AmountObj amount = item.getAmount();
                                            effect.setAmount(new AmountObj(item.getType().equals("sent") ? -amount.getAmount() : amount.getAmount()
                                                    , amount.getCurrency(), amount.getIssuer()));
                                            effects.add(effect);
                                        }


                                    } else {
                                        Effect feeEffect = new Effect();
                                        //if other payments, it stands for fee changes.
                                        feeEffect.setBalance(new AmountObj(balance.doubleValue(), "VRP", null));
                                        feeEffect.setType("fee");
                                        feeEffect.setAmount(new AmountObj(-item.getFee().getAmount(), "VRP", null));
                                        effects.add(feeEffect);
                                    }
                                }
                            } else {
                                //other currency payment, contains highLimit and lowLimit
                                //it stands for tx amount's currency's balance changes
                                if (highLimit != null && lowLimit != null) {
                                    //balance change abount current account
                                    //highlimit stands for recipient
                                    String issuer = lowLimit.issuerString();
                                    if(highLimit.value().intValue() == 0){
                                        issuer = highLimit.issuerString();
                                    }
                                    if (highLimit.issuerString().equals(address)) {
                                        Effect effect = new Effect();
                                        Amount preBalance = (Amount) prevFields.get(Field.Balance);
                                        if(preBalance != null) {
                                            effect.setAmount(new AmountObj(preBalance.subtract(balance).doubleValue(), preBalance.currencyString(), issuer));
                                            effect.setBalance(new AmountObj(Math.abs(balance.doubleValue()), balance.currencyString(), issuer));
                                            effect.setType("amount");
                                            effects.add(effect);
                                        }
                                    } else if (lowLimit.issuerString().equals(address)) {
                                        Effect effect = new Effect();
                                        //paths tx
                                        Amount preBalance = (Amount) prevFields.get(Field.Balance);
                                        if (preBalance != null) {
                                            effect.setAmount(new AmountObj(-preBalance.subtract(balance).doubleValue(), preBalance.currencyString(), issuer));
                                            effect.setBalance(new AmountObj(Math.abs(balance.doubleValue()), balance.currencyString(), issuer));
                                            effect.setType("amount");
                                            effects.add(effect);
                                        }
                                    }
                                }

                            }
                        }
                    } else if (node.isCreatedNode()) {
                        STObject obj = (STObject) node.get(Field.CreatedNode);
                        STObject nf = (STObject) obj.get(Field.NewFields);
                        AccountID account = (AccountID) nf.get(Field.Account);
                        if (account != null && address.equals(account.address)) {
                            Effect effect = new Effect();
                            Amount amount = (Amount) nf.get(Field.BalanceVBC);
                            Amount balance = (Amount) nf.get(Field.Balance);
                            if (amount != null) {
                                effect.setBalance(new AmountObj(amount.doubleValue(), "VBC", null));
                            }
                            if (balance != null)
                                effect.setBalance(new AmountObj(balance.doubleValue(), "VRP", null));
                            effect.setType("fee");
                            effect.setAmount(item.getAmount());
                            effects.add(effect);
                        }
                    } else if (node.ledgerEntryType() == LedgerEntryType.Offer) {
                        STObject obj = (STObject) node.get(node.getField());
                        STObject ff = (STObject) obj.get(Field.FinalFields);
                        STObject prevFields = (STObject) obj.get(Field.PreviousFields);
                        Amount takerGets = (Amount) ff.get(Field.TakerGets);
                        Amount takerPays = (Amount) ff.get(Field.TakerPays);
                        if (prevFields == null) {
                            continue;
                        }
                        Amount preTakerGets = (Amount) prevFields.get(Field.TakerGets);
                        Amount preTakerPays = (Amount) prevFields.get(Field.TakerPays);
                        //show effects
                        if (address.equals(item.getSender())) {

                            Effect showEffect = new Effect();
                            showEffect.setTakerGets(new AmountObj(preTakerGets.subtract(takerGets).doubleValue(), preTakerGets.currencyString(), preTakerGets.issuerString()));
                            showEffect.setTakerPays(new AmountObj(preTakerPays.subtract(takerPays).doubleValue(), preTakerPays.currencyString(), preTakerPays.issuerString()));
                            showEffect.setType("bought");
                            showEffects.add(showEffect);
                        } else {
                            AccountID account = (AccountID) ff.get(Field.Account);
                            if (address.equals(account)) {
                                Effect showEffect = new Effect();
                                showEffect.setTakerGets(new AmountObj(preTakerGets.subtract(takerGets).doubleValue(), preTakerGets.currencyString(), preTakerGets.issuerString()));
                                showEffect.setTakerPays(new AmountObj(preTakerPays.subtract(takerPays).doubleValue(), preTakerPays.currencyString(), preTakerPays.issuerString()));
                                showEffect.setType("bought");
                                showEffects.add(showEffect);
                            }
                        }
                    }
                    continue;
                case "dividend":
                    if (node.isModifiedNode()) {
                        STObject obj = (STObject) node.get(Field.ModifiedNode);
                        STObject ff = (STObject) obj.get(Field.FinalFields);
                        if (ff != null) {
                            Amount balance = (Amount) ff.get(Field.Balance);
                            Amount balanceVBC = (Amount) ff.get(Field.BalanceVBC);
                            Effect effect = new Effect();
                            effect.setBalance(new AmountObj(balance.doubleValue(), "VRP", null));
                            effect.setType("amount");
                            effect.setAmount(item.getAmount());
                            effects.add(effect);
                            Effect effectVBC = new Effect();
                            effectVBC.setBalance(new AmountObj(balanceVBC.doubleValue(), "VBC", null));
                            effectVBC.setType("amount");
                            effectVBC.setAmount(item.getAmountVBC());
                            effects.add(effectVBC);
                        }
                    }
                    continue;
                case "addreferee":
                case "connecting":
                    if (node.isModifiedNode()) {
                        STObject obj = (STObject) node.get(Field.ModifiedNode);
                        STObject ff = (STObject) obj.get(Field.FinalFields);
                        if (ff != null) {
                            AccountID account = (AccountID) ff.get(Field.Account);
                            if (account != null && account.address.equals(address)) {
                                Amount balance = (Amount) ff.get(Field.Balance);
                                Effect effect = new Effect();
                                effect.setBalance(new AmountObj(balance.doubleValue(), "VRP", null));
                                effect.setType("fee");
                                effect.setAmount(new AmountObj(-item.getFee().getAmount(), "VRP", null));
                                effects.add(effect);
                            }
                        }
                    }
                    continue;
                case "referee":
                case "connected":
                    continue;
                case "offer_cancelled":
                    if (node.ledgerEntryType() == LedgerEntryType.Offer) {
                        if (node.isDeletedNode()) {
                            STObject deleteNode = (STObject) node.get(Field.DeletedNode);
                            STObject ff = (STObject) deleteNode.get(Field.FinalFields);
                            Amount takerGets;
                            Amount takerPays;
                            if (ff != null) {
                                AccountID account = (AccountID) ff.get(Field.Account);
                                takerGets = (Amount) ff.get(Field.TakerGets);
                                takerPays = (Amount) ff.get(Field.TakerPays);
                                if (account != null && account.address.equals(address)) {
                                    item.setTakerPays(new AmountObj(takerPays.doubleValue(), takerPays.currencyString(), takerPays.issuerString()));
                                    item.setTakerGets(new AmountObj(takerGets.doubleValue(), takerGets.currencyString(), takerGets.issuerString()));
                                    item.setOfferStatus("offer_cancelled");
                                    item.setSender(address);
                                }
                            }
                        }
                    } else if (node.ledgerEntryType() == LedgerEntryType.AccountRoot && node.isModifiedNode()) {
                        STObject deleteNode = (STObject) node.get(Field.ModifiedNode);
                        STObject ff = (STObject) deleteNode.get(Field.FinalFields);
                        if (ff != null) {
                            AccountID account = (AccountID) ff.get(Field.Account);
                            if (account != null && account.address.equals(address)) {
                                //trust line balance change
                                Amount balance = (Amount) ff.get(Field.Balance);
                                if (account != null && account.address.equals(address)) {
                                    Effect effect = new Effect();
                                    effect.setType("amount");
                                    effect.setAmount(new AmountObj(-item.getFee().getAmount(), item.getFee().getCurrency(), item.getFee().getIssuer()));
                                    effect.setBalance(new AmountObj(balance.doubleValue(), balance.currencyString(), balance.issuerString()));
                                    effects.add(effect);
                                }
                            }
                        }
                    }
                    continue;
                case "offercreate":
                    if (node.ledgerEntryType() == LedgerEntryType.Offer) {
                        Amount takerGets;
                        Amount takerPays;
                        //account in delete node and fieldsPrev amount is not zero, offer ok.
                        if (node.isDeletedNode()) {
                            STObject deleteNode = (STObject) node.get(Field.DeletedNode);
                            STObject fieldsPrev = (STObject) deleteNode.get(Field.PreviousFields);
                            STObject ff = (STObject) deleteNode.get(Field.FinalFields);
                            if (fieldsPrev != null
                                    && (takerGets = (Amount) fieldsPrev.get(Field.TakerGets)) != null
                                    && !takerGets.isZero()) {
                                if (ff != null) {
                                    AccountID account = (AccountID) ff.get(Field.Account);
                                    if (account != null && account.address.equals(address)) {
                                        takerPays = (Amount) fieldsPrev.get(Field.TakerPays);
                                        if (item.getTakerGets() == null) {
                                            item.setTakerPays(new AmountObj(takerPays.doubleValue(), takerPays.currencyString(), takerPays.issuerString()));
                                            item.setTakerGets(new AmountObj(takerGets.doubleValue(), takerGets.currencyString(), takerGets.issuerString()));
                                            item.setOfferStatus("offer_funded");
                                        }
                                    } else if (address.equals(item.getSender())) {
                                        //show effects on offer met
                                        //sender create an offer, some offers has been filled
                                        takerGets = (Amount) ff.get(Field.TakerGets);
                                        takerPays = (Amount) ff.get(Field.TakerPays);
                                        Amount preTakerGets = (Amount) fieldsPrev.get(Field.TakerGets);
                                        Amount preTakerPays = (Amount) fieldsPrev.get(Field.TakerPays);
                                        Effect effect = new Effect();
                                        effect.setType("offer_filled");
                                        effect.setTakerGets(new AmountObj(preTakerGets.subtract(takerGets).doubleValue(), takerGets.currencyString(), takerGets.issuerString()));
                                        effect.setTakerPays(new AmountObj(preTakerPays.subtract(takerPays).doubleValue(), takerPays.currencyString(), takerPays.issuerString()));
                                        showEffects.add(effect);
                                    }
                                }
                            } else if (ff != null) {
                                AccountID account = (AccountID) ff.get(Field.Account);
                                takerGets = (Amount) ff.get(Field.TakerGets);
                                takerPays = (Amount) ff.get(Field.TakerPays);
                                if (account != null && account.address.equals(address)) {
                                    Effect effect = new Effect();
                                    effect.setType("offer_cancelled");
                                    effect.setTakerGets(new AmountObj(takerGets.doubleValue(), takerGets.currencyString(), takerGets.issuerString()));
                                    effect.setTakerPays(new AmountObj(takerPays.doubleValue(), takerPays.currencyString(), takerPays.issuerString()));
                                    showEffects.add(effect);
                                }
                            }
                        } else if (node.isModifiedNode()) {
                            STObject modifiedNode = (STObject) node.get(Field.ModifiedNode);
                            STObject fieldsPrev = (STObject) modifiedNode.get(Field.PreviousFields);
                            STObject ff = (STObject) modifiedNode.get(Field.FinalFields);
                            if (ff != null) {
                                AccountID account = (AccountID) ff.get(Field.Account);
                                if (account != null && account.address.equals(address)) {
                                    //offer not filled.
                                    item.setOfferStatus("offer_partially_funded");
                                    if (fieldsPrev != null) {
                                        takerGets = (Amount) fieldsPrev.get(Field.TakerGets);
                                        takerPays = (Amount) fieldsPrev.get(Field.TakerPays);
                                        Amount ffgets = (Amount) ff.get(Field.TakerGets);
                                        Amount ffpays = (Amount) ff.get(Field.TakerPays);
                                        if (item.getTakerGets() == null) {
                                            item.setTakerPays(new AmountObj(takerPays.doubleValue(), takerPays.currencyString(), takerPays.issuerString()));
                                            item.setTakerGets(new AmountObj(takerGets.doubleValue(), takerGets.currencyString(), takerGets.issuerString()));
                                        }
                                        item.setPartiallyPays(new AmountObj(takerPays.value().subtract(ffpays.value()).doubleValue(), takerPays.currencyString(), takerPays.issuerString()));
                                        item.setPartiallyGets(new AmountObj(takerGets.value().subtract(ffgets.value()).doubleValue(), takerGets.currencyString(), takerGets.issuerString()));

                                        Effect effect = new Effect();
                                        effect.setType("offer_filled");
                                        effect.setTakerGets(new AmountObj(takerGets.subtract(ffgets).doubleValue(), takerGets.currencyString(), takerGets.issuerString()));
                                        effect.setTakerPays(new AmountObj(takerPays.subtract(ffpays).doubleValue(), takerPays.currencyString(), takerPays.issuerString()));
                                        showEffects.add(effect);
                                    }
                                    takerGets = (Amount) ff.get(Field.TakerGets);
                                    takerPays = (Amount) ff.get(Field.TakerPays);
                                    Effect effect = new Effect();
                                    effect.setType("offer_remained");
                                    effect.setTakerGets(new AmountObj(takerGets.doubleValue(), takerGets.currencyString(), takerGets.issuerString()));
                                    effect.setTakerPays(new AmountObj(takerPays.doubleValue(), takerPays.currencyString(), takerPays.issuerString()));
                                    showEffects.add(effect);

                                }
                            }

                        } else if (node.isCreatedNode()) {
                            STObject createdNode = (STObject) node.get(Field.CreatedNode);
                            STObject nf = (STObject) createdNode.get(Field.NewFields);

                            if (nf != null) {
                                AccountID account = (AccountID) nf.get(Field.Account);
                                if (account != null && account.address.equals(address)) {
                                    takerGets = (Amount) nf.get(Field.TakerGets);
                                    takerPays = (Amount) nf.get(Field.TakerPays);
                                    //offer not filled.
                                    if (item.getTakerGets() == null) {
                                        item.setOfferStatus("offer_create");
                                        item.setTakerPays(new AmountObj(takerPays.doubleValue(), takerPays.currencyString(), takerPays.issuerString()));
                                        item.setTakerGets(new AmountObj(takerGets.doubleValue(), takerGets.currencyString(), takerGets.issuerString()));
                                    } else {
                                        if (takerGets.doubleValue() < item.getTakerGets().getAmount()) {
                                            item.setOfferStatus("offer_partially_funded");
                                            Effect effect = new Effect();
                                            effect.setType("offer_remained");
                                            effect.setTakerGets(new AmountObj(takerGets.doubleValue(), takerGets.currencyString(), takerGets.issuerString()));
                                            effect.setTakerPays(new AmountObj(takerPays.doubleValue(), takerPays.currencyString(), takerPays.issuerString()));
                                            showEffects.add(effect);
                                            item.setPartiallyPays(new AmountObj(new BigDecimal(String.valueOf(item.getTakerGets().getAmount())).subtract(takerPays.value()).doubleValue(), takerPays.currencyString(), takerPays.issuerString()));
                                            item.setPartiallyGets(new AmountObj(new BigDecimal(String.valueOf(item.getTakerPays().getAmount())).subtract(takerPays.value()).doubleValue(), takerGets.currencyString(), takerGets.issuerString()));
                                        } else {
                                            item.setOfferStatus("offer_create");
                                        }
                                    }
                                }
                            }
                        }
                    } else if (node.ledgerEntryType() == LedgerEntryType.RippleState) {
                        if (node.isCreatedNode()) {
                            //offer filled cause an account has some gateway's balance, but trust limit is zero.
                            STObject createdNode = (STObject) node.get(Field.CreatedNode);
                            STObject nf = (STObject) createdNode.get(Field.NewFields);
                            if (nf != null) {
                                Amount highLimit = (Amount) nf.get(Field.HighLimit);
                                Amount lowLimit = (Amount) nf.get(Field.LowLimit);
                                Amount balance = (Amount) nf.get(Field.Balance);
                                if(lowLimit != null && highLimit!=null) {
                                    String issuer = lowLimit.issuerString();
                                    if (highLimit.value().intValue() == 0) {
                                        issuer = highLimit.issuerString();
                                    }
                                    if (highLimit.issuerString().equals(address)) {
                                        Effect effect = new Effect();
                                        effect.setType("amount");
                                        effect.setAmount(new AmountObj(-balance.doubleValue(), balance.currencyString(), issuer));
                                        effect.setBalance(new AmountObj(-balance.doubleValue(), balance.currencyString(), issuer));
                                        effects.add(effect);
                                    } else if (lowLimit.issuerString().equals(address)) {
                                        Effect effect = new Effect();
                                        effect.setType("amount");
                                        effect.setAmount(new AmountObj(balance.doubleValue(), balance.currencyString(), issuer));
                                        effect.setBalance(new AmountObj(balance.doubleValue(), balance.currencyString(), issuer));
                                        effects.add(effect);
                                    }
                                }
                            }

                        } else if (node.isModifiedNode()) {
                            STObject modifyNode = (STObject) node.get(Field.ModifiedNode);
                            STObject ff = (STObject) modifyNode.get(Field.FinalFields);
                            STObject prevFields = (STObject) modifyNode.get(Field.PreviousFields);
                            if (ff != null && prevFields != null) {
                                Amount highLimit = (Amount) ff.get(Field.HighLimit);
                                Amount lowLimit = (Amount) ff.get(Field.LowLimit);
                                Amount preBalance = (Amount) prevFields.get(Field.Balance);
                                if (highLimit != null && lowLimit != null) {
                                    String issuer = lowLimit.issuerString();
                                    if(highLimit.value().intValue() == 0){
                                        issuer = highLimit.issuerString();
                                    }
                                    //trust line balance change
                                    Amount balance = (Amount) ff.get(Field.Balance);
                                    if (highLimit.issuerString().equals(address)) {
                                        Effect effect = new Effect();
                                        effect.setType("amount");
                                        if (preBalance != null) {
                                            effect.setAmount(new AmountObj(-balance.subtract(preBalance).doubleValue(), balance.currencyString(), issuer));
                                        }
                                        effect.setBalance(new AmountObj(-balance.doubleValue(), balance.currencyString(), issuer));
                                        effects.add(effect);
                                    }else if(lowLimit.issuerString().equals(address)){
                                        Effect effect = new Effect();
                                        effect.setType("amount");
                                        if (preBalance != null) {
                                            effect.setAmount(new AmountObj(balance.subtract(preBalance).doubleValue(), balance.currencyString(), issuer));
                                        }
                                        effect.setBalance(new AmountObj(balance.doubleValue(), balance.currencyString(), issuer));
                                        effects.add(effect);
                                    }
                                }
                            }
                        }
                    } else if (node.ledgerEntryType() == LedgerEntryType.AccountRoot && node.isModifiedNode()) {
                        STObject modifiedNode = (STObject) node.get(Field.ModifiedNode);
                        STObject ff = (STObject) modifiedNode.get(Field.FinalFields);
                        STObject prevFields = (STObject) modifiedNode.get(Field.PreviousFields);
                        if (ff != null) {
                            AccountID account = (AccountID) ff.get(Field.Account);
//                            boolean vbcOffer = false;
                            if (account != null && account.address.equals(address)) {

                                //trust line balance change
                                Amount balance = (Amount) ff.get(Field.Balance);
                                Effect effect = new Effect();
                                effect.setType("amount");
                                if (prevFields != null) {
                                    if (item.getSender().equals(address)) {
                                        if (balance.currencyString().equals("XRP")) {
                                            Amount preBalance = (Amount) prevFields.get(Field.Balance);
                                            //not only fee change.
                                            if (((long) preBalance.doubleValue() * 1000000L - (long) item.getFee().getAmount().doubleValue() * 1000000L) != (long) balance.doubleValue() * 1000000) {
                                                effect = new Effect();
                                                effect.setAmount(new AmountObj(-item.getFee().getAmount(), "VRP", null));
                                                effect.setBalance(new AmountObj(preBalance.doubleValue() - item.getFee().getAmount(), "VRP", null));
                                                effect.setType("fee");
                                                effects.add(effect);
                                            } else {
                                                //only fee
                                                effect = new Effect();
                                                effect.setAmount(new AmountObj(-item.getFee().getAmount(), "VRP", null));
                                                effect.setBalance(new AmountObj(balance.doubleValue(), "VRP", null));
                                                effect.setType("fee");
                                                effects.add(effect);
                                            }
                                        }
                                    }
                                    effect = new Effect();
                                    if (prevFields.get(Field.BalanceVBC) != null) {
                                        //vbc offer
//                                        vbcOffer = true;
                                        Amount balanceVBC = (Amount) ff.get(Field.BalanceVBC);
                                        Amount preBalanceVBC = (Amount) prevFields.get(Field.BalanceVBC);
                                        effect.setAmount(new AmountObj(balanceVBC.subtract(preBalanceVBC).doubleValue(), "VBC", null));
                                        effect.setBalance(new AmountObj(balanceVBC.doubleValue(), "VBC", null));
                                        effects.add(effect);
                                    } else if (prevFields.get(Field.Balance) != null) {
                                        Amount preBalance = (Amount) prevFields.get(Field.Balance);
                                        effect.setBalance(new AmountObj(balance.doubleValue(), balance.currencyString(), balance.issuerString()));
                                        if (item.getSender().equals(address) &&
                                                balance.currencyString().equals("XRP")) {
                                            if (Math.abs(preBalance.subtract(balance).doubleValue()) > item.getFee().getAmount()) {
                                                effect.setAmount(new AmountObj(balance.subtract(preBalance).doubleValue() + item.getFee().getAmount(), balance.currencyString(), balance.issuerString()));
                                                effects.add(effect);
                                            }
                                        } else {
                                            effect.setAmount(new AmountObj(balance.subtract(preBalance).doubleValue(), balance.currencyString(), balance.issuerString()));
                                            effects.add(effect);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    continue;
            }
        }
//        if (type.equals("offercreate")) {
//            for (Effect effect : effects) {
//                if (effect.getAmount() == null) {
//                    if ("offer_create".equals(item.getOfferStatus())) {
//                        //set fee
//                        effect.setAmount(new AmountObj(-item.getFee().getAmount(), item.getFee().getCurrency(), item.getFee().getIssuer()));
//                        effect.setType("fee");
//                    } else if (item.getTakerGets() != null && effect.getBalance().getCurrency().equals(item.getTakerGets().getCurrency())) {
//                        if ("offer_partially_funded".equals(item.getOfferStatus()) && effect.getAmount() == null) {
//                            effect.setAmount(new AmountObj(-item.getPartiallyGets().getAmount(), item.getPartiallyGets().getCurrency(), item.getPartiallyGets().getIssuer()));
//                        } else {
//                            effect.setAmount(new AmountObj(-item.getTakerGets().getAmount(), item.getTakerGets().getCurrency(), item.getTakerGets().getIssuer()));
//                        }
//                    } else if (item.getTakerPays() != null && effect.getBalance().getCurrency().equals(item.getTakerPays().getCurrency())) {
//                        if ("offer_partially_funded".equals(item.getOfferStatus())) {
//                            effect.setAmount(item.getPartiallyPays());
//                        } else {
//                            effect.setAmount(item.getTakerPays());
//                        }
//                    }
//                }
//            }
//        }
        Map<String, Object> result = new HashMap<>();
        result.put("effects", effects);
        result.put("show_effects", showEffects);
        result.put("item", item);
        return result;
    }


    /**
     * @param address
     * @return
     */
    public String getAccountTx(String address, Map<String, Object> marker) throws APIException {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 0);
        data.put("command", "account_tx");
        data.put("account", address);
        data.put("limit", 20);
        data.put("binary", false);
        data.put("ledger_index_min", -1);
        if (MapUtils.isNotEmpty(marker)) {
            data.put("marker", marker);
        }
        String postData = new Gson().toJson(data);
        String json = RadarWebSocketClient.req(postData);
        System.out.println("tx:" + json);
        JSONObject jsonObject = new JSONObject(json);
        if (jsonObject.getJSONObject("result").has("marker")) {
            marker = new HashMap<>();
            marker.put("ledger", jsonObject.getJSONObject("result").getJSONObject("marker").getInt("ledger"));
            marker.put("seq", jsonObject.getJSONObject("result").getJSONObject("marker").getInt("seq"));
        }
        JSONArray txs = jsonObject.getJSONObject("result").getJSONArray("transactions");

        List<TxObj> resultList = new ArrayList<>();
        for (int i = 0; i < txs.length(); i++) {
            JSONObject txObj = txs.getJSONObject(i).getJSONObject("tx");
            JSONObject metaObj = txs.getJSONObject(i).getJSONObject("meta");
            Transaction tx = (Transaction) Transaction.fromJSONObject(txObj);
            TransactionMeta meta = (TransactionMeta) TransactionMeta.fromJSONObject(metaObj);
            TxObj item = new TxObj();
//            RippleDate date = RippleDate.fromSecondsSinceRippleEpoch(txObj.getInt("date"));
//            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            item.setDate(String.valueOf(txObj.getInt("date")));
            item.setSender(tx.account().address);
            if (!tx.account().address.equals(address))
                item.setContact(tx.account().address);
            //if is tx maker, then set fee obj;
            AmountObj fee = new AmountObj(tx.fee().doubleValue(), "VRP", null);
            item.setFee(fee);
            item.setHash(tx.get(Field.hash).toHex());
            //if tx result is not success, then set type to failed;
            if (meta.engineResult().asInteger() != 0) {
                item.setType("failed");
            }else{
                if (tx instanceof Payment) {
                    Payment payment = (Payment) tx;
                    item.setRecipient(payment.destination().address);
                    if (!((Payment) tx).destination().address.equals(address))
                        item.setContact(payment.destination().address);
                    Double paymentAmount = payment.amount().doubleValue();
                    AmountObj amount = new AmountObj(paymentAmount, payment.amount().currencyString().replace("XRP", "VRP"), payment.amount().issuerString());
                    item.setAmount(amount);

                    if (!address.equals(payment.destination().address) && !address.equals(tx.account().address)) {
                        if (payment.paths() != null) {
                            for (PathSet.Path path : payment.paths()) {
                                for (PathSet.Hop hop : path) {
                                    if (hop.account != null && address.equals(hop.account.address)) {
                                        item.setType("radaring");
                                        break;
                                    }
                                }
                            }
                            if (item.getType() == null) {
                                item.setType("offercreate");
                            }
                        } else
                            item.setType("radaring");
                    } else
                        item.setType(((Payment) tx).destination().address.equals(address) ? "received" : "sent");

                }
                if (tx instanceof Dividend) {
                    if (!((Dividend) tx).destination().address.equals(address)) {
                        continue;
                    }
                    Dividend dividend = (Dividend) tx;
                    item.setRecipient(address);
                    item.setType("dividend");
                    item.setAmount(new AmountObj(dividend.dividendCoins().doubleValue() / 1000000, "VRP", null));
                    item.setAmountVBC(new AmountObj(dividend.dividendCoinsVBC().doubleValue() / 1000000, "VBC", null));
                }
                if (tx instanceof AddReferee) {
                    AddReferee addReferee = (AddReferee) tx;
                    item.setRecipient(addReferee.destination().address);
                    if (!addReferee.destination().address.equals(address))
                        item.setContact(addReferee.destination().address);
                    item.setType(addReferee.destination().address.equals(address) ? "referee" : "addreferee");
                }
                if (tx instanceof TrustSet) {
                    TrustSet trustSet = (TrustSet) tx;
                    Amount limit = trustSet.limitAmount();
                    AmountObj limitAmount = new AmountObj(limit.doubleValue(), limit.currencyString(), limit.issuerString());
                    item.setLimitAmount(limitAmount);
                    item.setRecipient(limit.issuer().address);
                    if (!limit.issuer().address.equals(address))
                        item.setContact(limit.issuer().address);
                    item.setType(limit.issuer().address.equals(address) ? "connected" : "connecting");
                }
                if (tx instanceof OfferCreate) {
                    OfferCreate offerCreate = (OfferCreate) tx;
                    if (item.getSender().equals(address)) {
                        item.setSender(address);
                    }
                    if (address.equals(offerCreate.takerGets().issuerString()) || address.equals(offerCreate.takerPays().issuerString())) {
                        item.setType("offer_radaring");
                    } else
                        item.setType("offercreate");
                    if (item.getSender().equals(address) || item.getType().equals("offer_radaring")) {
                        AmountObj takerGets = new AmountObj(offerCreate.takerGets().doubleValue(), offerCreate.takerGets().currencyString(), offerCreate.takerGets().issuerString());
                        AmountObj takerPays = new AmountObj(offerCreate.takerPays().doubleValue(), offerCreate.takerPays().currencyString(), offerCreate.takerPays().issuerString());
                        item.setTakerGets(takerGets);
                        item.setTakerPays(takerPays);
                    }

                }
                if (tx instanceof OfferCancel) {
                    item.setType("offer_cancelled");
                }
                if (tx instanceof AccountSet){
                    item.setType("account_set");
                }
                if (StringUtils.isBlank(item.getType())) {
                    item.setType("unknown");
                }
            }
            Map<String, Object> result = generateEffectListFromTxMetaByType(address, item, meta, item.getType());
            List<Effect> effects = (List<Effect>) result.get("effects");
            if (item.getType().equals("offercreate") && effects.size() > 1 && item.getOfferStatus() == null) {
                item.setOfferStatus("offer_funded");
            }
            item = (TxObj) result.get("item");
            item.setEffects(effects);
            item.setShowEffects((List<Effect>) result.get("show_effects"));
            resultList.add(item);
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("account", address);
        resultMap.put("transactions", resultList);
        if (MapUtils.isNotEmpty(marker)) {
            resultMap.put("marker", marker);
        }
        return new Gson().toJson(resultMap);
    }

    /**
     * @param offers
     * @param currency1
     * @param currency2
     * @param flag     0--asks(sell)  1--bids(buy)
     * @return
     */
    private List<JSONObject> formatOfferArray(JSONArray offers, String currency1, String currency2, int flag, int limit) {
        List<JSONObject> result = new ArrayList<>();
        int len = offers.length();
        BigDecimal showSum = new BigDecimal(0);

        BigDecimal prePrice = new BigDecimal(0);

        if (flag == 1) {
            JSONObject jsonObjectOfferPre = new JSONObject();
            for (int i = 0; i < len; i++) {
                if (result.size() >= limit) {
                    break;
                }
                JSONObject jo = offers.getJSONObject(i);
                Map<String, BigDecimal> takerValue = getOfferValue(jo);
                showSum = showSum.add(takerValue.get("takerPays"));
                BigDecimal showPrice = new BigDecimal(0);
                if (takerValue.get("takerPays").compareTo(new BigDecimal(0)) != 0) {
                    showPrice = takerValue.get("takerGets").divide(takerValue.get("takerPays"), 6, RoundingMode.HALF_UP);
                }

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("Account", jo.getString("Account"));
                jsonObject.put("showSum", showSum);
                jsonObject.put("showPrice", showPrice);
                jsonObject.put("showTakerPays", takerValue.get("takerPays"));

                if (i == 0) {
                    jsonObjectOfferPre = jsonObject;
                    prePrice = showPrice;
                } else {
                    if (prePrice.compareTo(showPrice) != 0) {
                        if (prePrice.compareTo(new BigDecimal(0)) != 0) {
                            result.add(jsonObjectOfferPre);
                        }
                        jsonObjectOfferPre = jsonObject;
                        prePrice = showPrice;
                    } else {
                        jsonObjectOfferPre.put("showSum", new BigDecimal(jsonObjectOfferPre.get("showSum").toString()).add(takerValue.get("takerPays")));
                        jsonObjectOfferPre.put("showTakerPays", new BigDecimal(jsonObjectOfferPre.get("showTakerPays").toString()).add(takerValue.get("takerPays")));
                    }
                }
                //result.add(jsonObject);
            }
            if (prePrice.compareTo(new BigDecimal(0)) != 0) {
                result.add(jsonObjectOfferPre);
            }
        } else {
            prePrice = new BigDecimal(0);
            JSONObject jsonObjectOfferPre = new JSONObject();
            for (int i = 0; i < len; i++) {
                if (result.size() > limit) {
                    break;
                }
                JSONObject jo = offers.getJSONObject(i);
                Map<String, BigDecimal> takerValue = getOfferValue(jo);
                showSum = showSum.add(takerValue.get("takerGets"));
                BigDecimal showPrice;
                if (isNative(currency2)) {
                    showPrice = new BigDecimal(jo.getString("quality")).divide(new BigDecimal(1000000));
                } else if(isNative(currency1)) {
                    showPrice = new BigDecimal(jo.getString("quality")).multiply(new BigDecimal(1000000));
                }else{
                    showPrice = new BigDecimal(jo.getString("quality"));
                }
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("Account", jo.getString("Account"));
                jsonObject.put("showSum", showSum.toPlainString());
                jsonObject.put("showPrice", showPrice.toPlainString());
                jsonObject.put("showTakerGets", takerValue.get("takerGets"));


                if (i == 0) {
                    jsonObjectOfferPre = jsonObject;
                    prePrice = showPrice;
                } else {
                    if (prePrice.compareTo(showPrice) != 0) {
                        if (prePrice.compareTo(new BigDecimal(0)) != 0) {
                            result.add(jsonObjectOfferPre);
                        }
                        jsonObjectOfferPre = jsonObject;
                        prePrice = showPrice;
                    } else {
                        jsonObjectOfferPre.put("showSum", new BigDecimal(jsonObjectOfferPre.get("showSum").toString()).add(takerValue.get("takerGets")));
                        jsonObjectOfferPre.put("showTakerGets", new BigDecimal(jsonObjectOfferPre.get("showTakerGets").toString()).add(takerValue.get("takerGets")));
                    }
                }
            }
            if (prePrice.compareTo(new BigDecimal(0)) != 0) {
                result.add(jsonObjectOfferPre);
            }
        }
        return result;
    }

    private boolean isNative(String currency){
        return currency.equalsIgnoreCase("VRP") || currency.equalsIgnoreCase("XRP") || currency.equalsIgnoreCase("VBC");
    }

    private Map<String, BigDecimal> getOfferValue(JSONObject jo) {
        Map<String, BigDecimal> result = new HashMap<>();

        String taketPaysStr = jo.get("TakerPays").toString();
        if (taketPaysStr.indexOf("currency") > 0) {
            if (taketPaysStr.contains("VBC")) {
                result.put("takerPays", new BigDecimal(jo.getJSONObject("TakerPays").getString("value")).divide(new BigDecimal(1000000), 6, RoundingMode.HALF_UP));
            } else {
                result.put("takerPays", new BigDecimal(jo.getJSONObject("TakerPays").getString("value")));
            }
        } else {
            result.put("takerPays", new BigDecimal(jo.getString("TakerPays")).divide(new BigDecimal(1000000), 6, RoundingMode.HALF_UP));
        }

        String takeGetsStr = jo.get("TakerGets").toString();
        if (takeGetsStr.indexOf("currency") > 0) {
            if (takeGetsStr.contains("VBC")) {
                result.put("takerGets", new BigDecimal(jo.getJSONObject("TakerGets").getString("value")).divide(new BigDecimal(1000000), 6, RoundingMode.HALF_UP));
            } else {
                result.put("takerGets", new BigDecimal(jo.getJSONObject("TakerGets").getString("value")));
            }

        } else {
            result.put("takerGets", new BigDecimal(jo.getString("TakerGets")).divide(new BigDecimal(1000000), 6, RoundingMode.HALF_UP));
        }

        return result;
    }

    private Map getAccountOffers(String address, int limit, String c1, String issuer1, String c2, String issuer2, String marker) throws APIException {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 0);
        data.put("command", "account_offers");
        data.put("account", address);
        data.put("ledger", "current");
        if (limit >= 10 && limit <= 400) {
            data.put("limit", limit);
        }

        if (marker != null) {
            data.put("marker", marker);
        }

        Map<String, Object> result = new HashMap<>();
        String postData = new Gson().toJson(data);
        String jsonResult = RadarWebSocketClient.req(postData);
        JSONObject jsonObject = new JSONObject(jsonResult);
        if (jsonObject.getString("status").equalsIgnoreCase("success")) {
            String markerReturn = null;
            if (jsonObject.getJSONObject("result").has("marker")) {
                markerReturn = jsonObject.getJSONObject("result").getString("marker");
                result.put("maker", markerReturn);
            }
            JSONArray jsonArray = jsonObject.getJSONObject("result").getJSONArray("offers");
            result.put("offers", formatAccountOffers(c1, issuer1, c2, issuer2, jsonArray));

        } else {
            throw new APIException(APIException.ErrorCode.UNKNOWN_ERROR, "unknown error");
        }
        return result;

    }


    private List<JSONObject> formatAccountOffersAll(JSONArray jsonArray) {

        List<JSONObject> list = new ArrayList<>();
        int len = jsonArray.length();
        for (int i = 0; i < len; i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String takerGetsStr = jsonObject.get("taker_gets").toString();
            String takerPaysStr = jsonObject.get("taker_pays").toString();

            BigDecimal getValue;
            BigDecimal payValue;
            BigDecimal price;
            String priceUnit = "VRP";

            JSONObject jsonObjectTmp = new JSONObject();
            JSONObject priceJson = new JSONObject();

            if (takerGetsStr.contains("currency")) {
                getValue = new BigDecimal(jsonObject.getJSONObject("taker_gets").getString("value"));
                if (jsonObject.getJSONObject("taker_gets").getString("currency").equalsIgnoreCase("VBC")) {   //VBC
                    getValue = getValue.divide(new BigDecimal(1000000), 6, RoundingMode.HALF_UP);
                    jsonObject.getJSONObject("taker_gets").put("value", getValue + "");
                }
                priceUnit = jsonObject.getJSONObject("taker_gets").getString("currency");
            } else { //VRP
                getValue = new BigDecimal(jsonObject.getString("taker_gets")).divide(new BigDecimal(1000000), 6, RoundingMode.HALF_UP);
                jsonObjectTmp.put("currency", "VRP");
                jsonObjectTmp.put("value", getValue + "");
                jsonObject.put("taker_gets", jsonObjectTmp);
            }


            if (takerPaysStr.contains("currency")) {
                payValue = new BigDecimal(jsonObject.getJSONObject("taker_pays").getString("value"));
                if (jsonObject.getJSONObject("taker_pays").getString("currency").equalsIgnoreCase("VBC")) {
                    payValue = payValue.divide(new BigDecimal(1000000), 6, RoundingMode.HALF_UP);
                    jsonObject.getJSONObject("taker_pays").put("value", payValue + "");
                }
            } else { //VRP
                payValue = new BigDecimal(jsonObject.getString("taker_pays")).divide(new BigDecimal(1000000), 6, RoundingMode.HALF_UP);
                jsonObjectTmp.put("currency", "VRP");
                jsonObjectTmp.put("value", payValue + "");
                jsonObject.put("taker_pays", jsonObjectTmp);
            }


            if (payValue.compareTo(new BigDecimal(0)) == 0) {
                continue;
            }

            price = getValue.divide(payValue, 6, RoundingMode.HALF_UP);
            priceJson.put("value", price + "");
            priceJson.put("currency", priceUnit);
            jsonObject.put("price", priceJson);

            jsonObject.put("type", "buy"); //

            list.add(jsonObject);
        }
        return list;
    }


    private List<JSONObject> formatAccountOffers(String c1, String issuer1, String c2, String issuer2, JSONArray
            jsonArray) {
        List<JSONObject> list = new ArrayList<>();
        int len = jsonArray.length();
        for (int i = 0; i < len; i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String takerGets = jsonObject.get("taker_gets").toString();
            String takerPays = jsonObject.get("taker_pays").toString();
            if (issuer1 != null) {
                if (!takerGets.contains(issuer1) && !takerPays.contains(issuer1)) {
                    continue;
                }
            }
            if (issuer2 != null) {
                if (!takerGets.contains(issuer2) && !takerPays.contains(issuer2)) {
                    continue;
                }
            }

            BigDecimal amountPays;
            BigDecimal amountGets;
            JSONObject jsonObjectTmp = new JSONObject();
            String price;
            if (issuer1 == null || issuer2 == null) {  //have origin currency
                if (takerGets.contains("currency")) {
                    amountPays = new BigDecimal(jsonObject.getString("taker_pays"));
                    BigDecimal amount = amountPays.divide(new BigDecimal(1000000), 6, RoundingMode.HALF_UP);
                    JSONObject jsonTakerGets = jsonObject.getJSONObject("taker_gets");
                    amountGets = new BigDecimal(jsonTakerGets.getString("value"));
                    if (jsonTakerGets.getString("currency").equalsIgnoreCase("VBC")) {
                        amountGets = amountGets.divide(new BigDecimal(1000000), 6, RoundingMode.HALF_UP);
                        jsonObject.getJSONObject("taker_gets").put("value", amountGets + "");
//                        jsonObjectTmpVBC.put("currency", "VBC");
//                        jsonObjectTmpVBC.put("value", amountGets+"");
//                        jsonObject.put("taker_gets", jsonObjectTmpVBC);
                    }

                    if (amountGets.compareTo(new BigDecimal(0)) == 0 || amount.compareTo(new BigDecimal(0)) == 0) {
                        continue;
                    }

                    if (takerGets.contains(c1)) {
                        jsonObject.put("type", "sell");
                        JSONObject priceJson = new JSONObject();
                        priceJson.put("value", amount.divide(amountGets, 6, RoundingMode.HALF_UP) + "");
                        priceJson.put("currency", "VRP");
                        jsonObject.put("price", priceJson);
                    } else {
                        jsonObject.put("type", "buy");
                        JSONObject priceJson = new JSONObject();
                        priceJson.put("value", amountGets.divide(amount, 6, RoundingMode.HALF_UP) + "");
                        priceJson.put("currency", jsonTakerGets.getString("currency"));
                        jsonObject.put("price", priceJson);

                    }
                    jsonObjectTmp.put("currency", "VRP");
                    jsonObjectTmp.put("value", amount + "");
                    jsonObject.put("taker_pays", jsonObjectTmp);

                } else {
                    amountGets = new BigDecimal(jsonObject.getString("taker_gets"));
                    BigDecimal amount = amountGets.divide(new BigDecimal(1000000), 6, RoundingMode.HALF_UP);
                    JSONObject jsonTakerPays = jsonObject.getJSONObject("taker_pays");
                    amountPays = new BigDecimal(jsonTakerPays.getString("value"));

                    if (jsonTakerPays.getString("currency").equalsIgnoreCase("VBC")) {
                        amountPays = amountPays.divide(new BigDecimal(1000000), 6, RoundingMode.HALF_UP);
                        jsonObject.getJSONObject("taker_pays").put("value", amountPays + "");
                    }

                    if (amount.compareTo(new BigDecimal(0)) == 0 || amountPays.compareTo(new BigDecimal(0)) == 0) {
                        continue;
                    }

                    if (takerPays.contains(c1)) {
                        jsonObject.put("type", "buy");
                        JSONObject priceJson = new JSONObject();
                        priceJson.put("value", amount.divide(amountPays, 6, RoundingMode.HALF_UP) + "");
                        priceJson.put("currency", "VRP");
                        jsonObject.put("price", priceJson);
                    } else {
                        jsonObject.put("type", "sell");
                        JSONObject priceJson = new JSONObject();
                        priceJson.put("value", amountPays.divide(amount, 6, RoundingMode.HALF_UP) + "");
                        priceJson.put("currency", jsonTakerPays.getString("currency"));
                        jsonObject.put("price", priceJson);

                    }

                    jsonObjectTmp.put("currency", "VRP");
                    jsonObjectTmp.put("value", amount + "");
                    jsonObject.put("taker_gets", jsonObjectTmp);
                }
                list.add(jsonObject);
            } else {
                JSONObject jsonTakerPays = jsonObject.getJSONObject("taker_pays");
                JSONObject jsonTakerGets = jsonObject.getJSONObject("taker_gets");
                BigDecimal getValue = new BigDecimal(jsonTakerGets.getString("value"));
                BigDecimal payValue = new BigDecimal(jsonTakerPays.getString("value"));

                if (jsonTakerGets.getString("currency").equalsIgnoreCase("VBC")) {
                    getValue = getValue.divide(new BigDecimal(1000000), 6, RoundingMode.HALF_UP);
                    jsonObject.getJSONObject("taker_gets").put("value", getValue + "");
                }

                if (jsonTakerPays.getString("currency").equalsIgnoreCase("VBC")) {
                    payValue = payValue.divide(new BigDecimal(1000000), 6, RoundingMode.HALF_UP);
                    jsonObject.getJSONObject("taker_pays").put("value", payValue + "");
                }

                if (payValue.compareTo(new BigDecimal(0)) == 0 || getValue.compareTo(new BigDecimal(0)) == 0) {
                    continue;
                }

                if (takerGets.contains(issuer1)) {
                    JSONObject priceJson = new JSONObject();
                    priceJson.put("value", payValue.divide(getValue, 6, RoundingMode.HALF_UP) + "");
                    priceJson.put("currency", c2);
                    jsonObject.put("price", priceJson);

                    jsonObject.put("type", "sell");
                } else {
                    JSONObject priceJson = new JSONObject();
                    priceJson.put("value", getValue.divide(payValue, 6, RoundingMode.HALF_UP) + "");
                    priceJson.put("currency", c2);
                    jsonObject.put("price", priceJson);
                    jsonObject.put("type", "buy");
                }
                list.add(jsonObject);
            }

        }
        return list;
    }


    private String commaAndRound(BigDecimal value) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(6);
        return nf.format(value.setScale(6, BigDecimal.ROUND_HALF_UP));
    }


}