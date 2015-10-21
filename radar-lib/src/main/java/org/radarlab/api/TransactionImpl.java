package org.radarlab.api;

import com.google.gson.Gson;
import org.apache.commons.collections4.MapUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.radarlab.client.api.exception.APIException;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements of all kinds of transactions, defined by WebSocket API of Radar.
 *
 * @see "API documents"
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

        if (destinationAmount.currencyString().equals("VRP")) {
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
        String json = RadarWebSocketClient.request(postData);

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
        String jsonStrAsk = RadarWebSocketClient.request(postData);
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
        String jsonStrBids = RadarWebSocketClient.request(postData);
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
     *
     * @param limit (Optional, default varies) Limit the number of transactions to retrieve. The server is not required to honor this value. Cannot be lower than 10 or higher than 400.
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
        String jsonResult = RadarWebSocketClient.request(postData);
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
        if (amount.currencyString().equals("VRP") || amount.currencyString().equals("VBC")) {
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

    public int getClosedLedgerIndex() {
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1);
        params.put("command", "ledger_closed");

        String data = new Gson().toJson(params);
        try {
            String res = RadarWebSocketClient.request(data);
            JSONObject resJson = new JSONObject(res);
            if (resJson.has("status") && resJson.getString("status").equalsIgnoreCase("success")) {
                JSONObject jsonObjectResult = resJson.getJSONObject("result");
                return jsonObjectResult.getInt("ledger_index");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * @param reference
     * @param referee
     * @param account
     * @param seed
     * @param amount
     * @param sequence
     * @return
     * @throws APIException
     */
    public String activateUser(String reference, String referee, String account, String seed, Amount amount, int sequence) throws APIException {
        Integer ledgerIndex = getClosedLedgerIndex() + 20;

        IKeyPair kp = Seed.getKeyPair(seed);
        ActiveAccount activeAccount = new ActiveAccount();
        AccountID referenceAccount = AccountID.fromAddress(reference);
        AccountID refereeAccount = AccountID.fromAddress(referee);
        activeAccount.referee(refereeAccount);
        activeAccount.reference(referenceAccount);
        activeAccount.lastLedgerSequence(new UInt32(ledgerIndex));
        if (amount != null) {
            activeAccount.amount(amount);
        }

        activeAccount.account(AccountID.fromSeedBytes(B58.getInstance().decodeFamilySeed(seed)));

        long fee = 1000;
        if (amount != null) {
            if (amount.currencyString().equals("VRP") || amount.currencyString().equals("VBC")) {
                fee = (long) (amount.multiply(new BigDecimal("1000")).doubleValue());
            }
        }
        fee = Math.max(1000, fee);
        fee = 10000 + fee;

        SignedTransaction sign = new SignedTransaction(activeAccount);
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
        String json = RadarWebSocketClient.request(postData);
        logger.info("make tx result: " + json);
        return json;
    }

    public String getAssetName(String currency, String gateway) {
        if (currency.equals("ASSET")) {
            if ("rpCA4XHCjHtKvPZPDHBqGpkzEWsFQLNXfv".equals(gateway)) {
                return "SGVL";
            } else
                return currency;
        } else {
            return currency;
        }
    }

    public String getAssetName(Amount amount) {
        if (amount != null && "ASSET".equals(amount.currencyString())) {
            if ("rpCA4XHCjHtKvPZPDHBqGpkzEWsFQLNXfv".equals(amount.issuerString())) {
                return "SGVL";
            } else
                return amount.currencyString();
        } else {
            return amount.currencyString();
        }
    }


    public Map<String, Object> generateEffectListFromTxMetaByType(String address, TxObj item, TransactionMeta meta, String type) {
        List<Effect> effects = new ArrayList<>();
        List<Effect> showEffects = new ArrayList<>();
        for (AffectedNode node : meta.affectedNodes()) {
            LedgerEntryType nodeType = node.ledgerEntryType();
            if (nodeType != LedgerEntryType.AssetState && nodeType != LedgerEntryType.AccountRoot && nodeType != LedgerEntryType.Offer && nodeType != LedgerEntryType.RippleState) {
                //not meta info for balance changes and offer changes, continue
                continue;
            }
            STObject field;
            STObject obj;
            STObject preFields = null;
            if (node.isModifiedNode()) {
                obj = (STObject) node.get(Field.ModifiedNode);
                field = (STObject) obj.get(Field.FinalFields);
                preFields = (STObject) obj.get(Field.PreviousFields);
            } else if (node.isCreatedNode()) {
                obj = (STObject) node.get(Field.CreatedNode);
                field = (STObject) obj.get(Field.NewFields);
            } else {
                obj = (STObject) node.get(Field.DeletedNode);
                field = (STObject) obj.get(Field.FinalFields);
                preFields = (STObject) obj.get(Field.PreviousFields);
            }
            if (field == null) {
                continue;
            }
            switch (type) {
                case "failed":
                case "unknown":
                case "account_set":
                case "addreferee":
                case "connecting":
                case "set_regular_key":
                case "cancel_regular_key":
                    if (nodeType == LedgerEntryType.AccountRoot) {
                        parserAccountRoot(field, node, preFields, address, effects, item);
                    }
                    continue;
                case "radaring":
                case "offer_radaring":
                    //RippleState means balance of non-native currency balance changes
                    if (nodeType == LedgerEntryType.RippleState) {
                        parserRippleState(field, node, preFields, address, effects, item);
                    }
                    continue;
                case "sent":
                case "active_add":
                case "active":
                case "active_acc":
                case "received":
                case "active_show":
                case "exchange":
                case "issue":
                    if (nodeType == LedgerEntryType.AccountRoot) {
                        parserAccountRoot(field, node, preFields, address, effects, item);
                    }

                    if (nodeType == LedgerEntryType.RippleState) {
                        parserRippleState(field, node, preFields, address, effects, item);
                    }
                    if (nodeType == LedgerEntryType.AssetState) {
//                        parserAssetState(field, node, preFields, address, effects);
                    }
                    if (nodeType == LedgerEntryType.Offer) {
                        Amount takerGets = (Amount) field.get(Field.TakerGets);
                        Amount takerPays = (Amount) field.get(Field.TakerPays);
                        if (preFields == null) {
                            continue;
                        }
                        Amount preTakerGets = (Amount) preFields.get(Field.TakerGets);
                        Amount preTakerPays = (Amount) preFields.get(Field.TakerPays);
                        //show effects
                        if (address.equals(item.getSender())) {

                            Effect showEffect = new Effect();
                            showEffect.setTakerGets(new AmountObj(preTakerGets.subtract(takerGets).valueText(), getAssetName(preTakerGets), preTakerGets.issuerString()));
                            showEffect.setTakerPays(new AmountObj(preTakerPays.subtract(takerPays).valueText(), getAssetName(preTakerPays), preTakerPays.issuerString()));
                            showEffect.setType("bought");
                            showEffects.add(showEffect);
                        } else {
                            AccountID account = (AccountID) field.get(Field.Account);
                            if (address.equals(account)) {
                                Effect showEffect = new Effect();
                                showEffect.setTakerGets(new AmountObj(preTakerGets.subtract(takerGets).valueText(), getAssetName(preTakerGets), preTakerGets.issuerString()));
                                showEffect.setTakerPays(new AmountObj(preTakerPays.subtract(takerPays).valueText(), getAssetName(preTakerPays), preTakerPays.issuerString()));
                                showEffect.setType("bought");
                                showEffects.add(showEffect);
                            }
                        }
                    }
                    if (nodeType == LedgerEntryType.Asset) {

                    }
                    continue;
                case "dividend":
                    if (node.isModifiedNode()) {
                        if (field != null) {
                            Amount balance = (Amount) field.get(Field.Balance);
                            Amount balanceVBC = (Amount) field.get(Field.BalanceVBC);
                            Effect effect = new Effect();
                            effect.setBalance(new AmountObj(balance.valueText(), "VRP", null));
                            effect.setType("amount");
                            effect.setAmount(item.getAmount());
                            effects.add(effect);
                            Effect effectVBC = new Effect();
                            effectVBC.setBalance(new AmountObj(balanceVBC.valueText(), "VBC", null));
                            effectVBC.setType("amount");
                            effectVBC.setAmount(item.getAmountVBC());
                            effects.add(effectVBC);
                        }
                    }
                    continue;
                case "referee":
                case "connected":
                case "active_referee":
                    continue;
                case "offer_cancelled":
                    if (nodeType == LedgerEntryType.Offer) {
                        parserOffer(field, node, preFields, address, item, showEffects);
                    } else if (node.ledgerEntryType() == LedgerEntryType.AccountRoot && node.isModifiedNode()) {
                        parserAccountRoot(field, node, preFields, address, effects, item);
                    }
                    continue;
                case "offercreate":
                    if (nodeType == LedgerEntryType.Offer) {
                        parserOffer(field, node, preFields, address, item, showEffects);
                    } else if (nodeType == LedgerEntryType.RippleState) {
                        parserRippleState(field, node, preFields, address, effects, item);
                    } else if (nodeType == LedgerEntryType.AccountRoot && node.isModifiedNode()) {
                        parserAccountRoot(field, node, preFields, address, effects, item);
                    } else if (nodeType == LedgerEntryType.AssetState) {
//                        parserAssetState(field, node, preFields, address, effects);
                    }
                    continue;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("effects", effects);
        result.put("show_effects", showEffects);
        result.put("item", item);
        return result;
    }

    private void parserOffer(STObject field, AffectedNode node, STObject preFields, String address, TxObj item, List<Effect> showEffects) {

        Amount takerGets;
        Amount takerPays;
        Amount preTakerGets;
        Amount preTakerPays;
        AccountID account = (AccountID) field.get(Field.Account);
        //account in delete node and fieldsPrev amount is not zero, offer ok.
        if (node.isDeletedNode()) {
            if (preFields != null) {
                takerGets = (Amount) field.get(Field.TakerGets);
                takerPays = (Amount) field.get(Field.TakerPays);
                preTakerGets = (Amount) preFields.get(Field.TakerGets);
                preTakerPays = (Amount) preFields.get(Field.TakerPays);
                if (preTakerPays == null) {
                    preTakerPays = takerPays;
                }
                if (preTakerGets == null) {
                    preTakerGets = takerGets;
                }
                if (account != null && account.address.equals(address)) {
                    if (item.getTakerGets() == null) {
                        item.setTakerPays(new AmountObj(preTakerPays.valueText(), getAssetName(preTakerPays), preTakerPays.issuerString()));
                        item.setTakerGets(new AmountObj(preTakerGets.valueText(), getAssetName(preTakerGets), preTakerGets.issuerString()));
                        item.setOfferStatus("offer_funded");
                    } else {
                        item.setTakerPays(new AmountObj(preTakerPays.add(new BigDecimal(item.getTakerPays().getAmount())).valueText(), getAssetName(preTakerPays), preTakerPays.issuerString()));
                        item.setTakerGets(new AmountObj(preTakerGets.add(new BigDecimal(item.getTakerGets().getAmount())).valueText(), getAssetName(preTakerGets), preTakerGets.issuerString()));
                        item.setOfferStatus("offer_funded");
                    }
                    Effect effect = new Effect();
                    effect.setTakerGets(new AmountObj(preTakerGets.subtract(takerGets).valueText(), getAssetName(takerGets), takerGets.issuerString()));
                    effect.setTakerPays(new AmountObj(preTakerPays.subtract(takerPays).valueText(), getAssetName(takerPays), takerPays.issuerString()));
                    effect.setType("offer_filled");
                    showEffects.add(effect);
                    if (!takerGets.isZero() || !takerPays.isZero()) {
                        effect = new Effect();
                        effect.setTakerGets(new AmountObj(takerGets.valueText(), getAssetName(takerGets), takerGets.issuerString()));
                        effect.setTakerPays(new AmountObj(takerPays.valueText(), getAssetName(takerPays), takerPays.issuerString()));
                        effect.setType("offer_cancelled");
                        showEffects.add(effect);
                    }
                } else if (address.equals(item.getSender())) {
                    //show effects on offer met
                    //sender create an offer, some offers has been filled
//                    takerGets = (Amount) field.get(Field.TakerGets);
//                    takerPays = (Amount) field.get(Field.TakerPays);
//                    preTakerGets = (Amount) preFields.get(Field.TakerGets);
//                    preTakerPays = (Amount) preFields.get(Field.TakerPays);
                    Effect effect = new Effect();
                    effect.setType("offer_filled");
                    if (preTakerPays != null && preTakerGets != null) {
                        if (address.equals(account.address)) {
                            effect.setTakerGets(new AmountObj(preTakerGets.subtract(takerGets).valueText(), getAssetName(takerGets), takerGets.issuerString()));
                            effect.setTakerPays(new AmountObj(preTakerPays.subtract(takerPays).valueText(), getAssetName(takerPays), takerPays.issuerString()));
                        } else {
                            logger.info("preTakerPays:" + preTakerPays + ", takerPays:" + takerPays);
                            effect.setTakerGets(new AmountObj(preTakerPays.subtract(takerPays).valueText(), getAssetName(takerPays), takerPays.issuerString()));
                            effect.setTakerPays(new AmountObj(preTakerGets.subtract(takerGets).valueText(), getAssetName(takerGets), takerGets.issuerString()));
                        }
                    } else {
                        if (address.equals(account.address)) {
                            effect.setTakerGets(new AmountObj(takerGets.valueText(), getAssetName(takerGets), takerGets.issuerString()));
                            effect.setTakerPays(new AmountObj(takerPays.valueText(), getAssetName(takerPays), takerPays.issuerString()));
                        } else {
                            effect.setTakerGets(new AmountObj(takerPays.valueText(), getAssetName(takerPays), takerPays.issuerString()));
                            effect.setTakerPays(new AmountObj(takerGets.valueText(), getAssetName(takerGets), takerGets.issuerString()));
                        }
                    }
                    showEffects.add(effect);
                }
            } else if (field != null) {
                takerGets = (Amount) field.get(Field.TakerGets);
                takerPays = (Amount) field.get(Field.TakerPays);
                if (item.getType().equals("offer_cancelled")) {
                    item.setTakerPays(new AmountObj(takerPays.valueText(), getAssetName(takerPays), takerPays.issuerString()));
                    item.setTakerGets(new AmountObj(takerGets.valueText(), getAssetName(takerGets), takerGets.issuerString()));
                }
                if (account != null && account.address.equals(address)) {

                    Effect effect = new Effect();
                    effect.setType("offer_cancelled");
                    effect.setTakerGets(new AmountObj(takerGets.valueText(), getAssetName(takerGets), takerGets.issuerString()));
                    effect.setTakerPays(new AmountObj(takerPays.valueText(), getAssetName(takerPays), takerPays.issuerString()));
                    showEffects.add(effect);
                }
            }
        } else if (node.isModifiedNode()) {
            if (account != null && account.address.equals(address)) {
                item.setOfferStatus("offer_partially_funded");
                //offer not filled.
                takerGets = (Amount) field.get(Field.TakerGets);
                takerPays = (Amount) field.get(Field.TakerPays);
                if (preFields != null) {
                    preTakerGets = (Amount) preFields.get(Field.TakerGets);
                    preTakerPays = (Amount) preFields.get(Field.TakerPays);
                    if (preTakerGets == null) {
                        preTakerGets = takerGets;
                    }
                    if (preTakerPays == null) {
                        preTakerPays = takerPays;
                    }
                    if (item.getTakerGets() == null) {
                        item.setTakerPays(new AmountObj(preTakerPays.valueText(), getAssetName(takerPays), takerPays.issuerString()));
                        item.setTakerGets(new AmountObj(preTakerGets.valueText(), getAssetName(takerGets), takerGets.issuerString()));
                    } else {
                        item.setTakerPays(new AmountObj(preTakerPays.add(new BigDecimal(item.getTakerPays().getAmount())).valueText(), getAssetName(takerPays), takerPays.issuerString()));
                        item.setTakerGets(new AmountObj(preTakerGets.add(new BigDecimal(item.getTakerGets().getAmount())).valueText(), getAssetName(takerGets), takerGets.issuerString()));
                    }
                    item.setPartiallyPays(new AmountObj(preTakerPays.value().subtract(takerPays.value()).toPlainString(), getAssetName(takerPays), takerPays.issuerString()));
                    item.setPartiallyGets(new AmountObj(preTakerGets.value().subtract(takerGets.value()).toPlainString(), getAssetName(takerGets), takerGets.issuerString()));

                    Effect effect = new Effect();
                    effect.setType("offer_filled");
                    effect.setTakerGets(new AmountObj(preTakerGets.subtract(takerGets).valueText(), getAssetName(takerGets), takerGets.issuerString()));
                    effect.setTakerPays(new AmountObj(preTakerPays.subtract(takerPays).valueText(), getAssetName(takerPays), takerPays.issuerString()));
                    showEffects.add(effect);
                }
                if (takerGets.isZero()) {
                    item.setOfferStatus("offer_funded");
                    return;
                }
                Effect effect = new Effect();
                effect.setType("offer_remained");
                effect.setTakerGets(new AmountObj(takerGets.valueText(), getAssetName(takerGets), takerGets.issuerString()));
                effect.setTakerPays(new AmountObj(takerPays.valueText(), getAssetName(takerPays), takerPays.issuerString()));
                showEffects.add(effect);

            } else if (address.equals(item.getSender())) {
                if (preFields != null) {
                    preTakerGets = (Amount) preFields.get(Field.TakerGets);
                    preTakerPays = (Amount) preFields.get(Field.TakerPays);
                    takerGets = (Amount) field.get(Field.TakerGets);
                    takerPays = (Amount) field.get(Field.TakerPays);

                    Effect effect = new Effect();
                    effect.setType("offer_filled");
                    if (preTakerGets == null) {
                        preTakerGets = takerGets;
                    }
                    if (preTakerPays == null) {
                        preTakerPays = takerPays;
                    }
                    if (takerGets.currencyString().equals(item.getTakerGets().getCurrency())) {
                        effect.setTakerGets(new AmountObj(preTakerGets.subtract(takerGets).valueText(), getAssetName(takerGets), takerGets.issuerString()));
                        effect.setTakerPays(new AmountObj(preTakerPays.subtract(takerPays).valueText(), getAssetName(takerPays), takerPays.issuerString()));
                    } else {
                        effect.setTakerPays(new AmountObj(preTakerGets.subtract(takerGets).valueText(), getAssetName(takerGets), takerGets.issuerString()));
                        effect.setTakerGets(new AmountObj(preTakerPays.subtract(takerPays).valueText(), getAssetName(takerPays), takerPays.issuerString()));
                    }
                    showEffects.add(effect);
                }
            }

        } else if (node.isCreatedNode()) {
            if (field != null) {
                if (account != null && account.address.equals(address)) {
                    takerGets = (Amount) field.get(Field.TakerGets);
                    takerPays = (Amount) field.get(Field.TakerPays);
                    //offer not filled.
                    if (item.getTakerGets() == null) {
                        item.setOfferStatus("offer_create");
                        item.setTakerPays(new AmountObj(takerPays.valueText(), getAssetName(takerPays), takerPays.issuerString()));
                        item.setTakerGets(new AmountObj(takerGets.valueText(), getAssetName(takerGets), takerGets.issuerString()));
                    } else {
                        if (takerGets.value().compareTo(new BigDecimal(item.getTakerGets().getAmount())) == -1) {
                            item.setOfferStatus("offer_partially_funded");
                            Effect effect = new Effect();
                            effect.setType("offer_remained");
                            effect.setTakerGets(new AmountObj(takerGets.valueText(), getAssetName(takerGets), takerGets.issuerString()));
                            effect.setTakerPays(new AmountObj(takerPays.valueText(), getAssetName(takerPays), takerPays.issuerString()));
                            showEffects.add(effect);
                            item.setPartiallyGets(new AmountObj(new BigDecimal(String.valueOf(item.getTakerGets().getAmount())).subtract(takerGets.value()).toPlainString(), getAssetName(takerGets), takerGets.issuerString()));
                            item.setPartiallyPays(new AmountObj(new BigDecimal(String.valueOf(item.getTakerPays().getAmount())).subtract(takerPays.value()).toPlainString(), getAssetName(takerPays), takerPays.issuerString()));
                        } else {
                            item.setOfferStatus("offer_create");
                        }
                    }
                }
            }
        }
    }

    private void parserAssetState(STObject field, AffectedNode node, STObject preFields, String address, List<Effect> effects) {
        //other currency payment, contains highLimit and lowLimit
        //it stands for tx amount's currency's balance changes

        Amount balance = (Amount) field.get(Field.Amount);
        Amount deliveredAmount = field.has(Field.DeliveredAmount) ? (Amount) field.get(Field.DeliveredAmount) : new Amount(new BigDecimal("0"), Currency.ASSET, balance.issuer());
        Amount balanceChange = null;
//        AccountID account = (AccountID) field.get(Field.Account);
        String issuer = balance.issuerString();
        String account = ((AccountID) field.get(Field.Account)).address;
        if (node.isCreatedNode()) {
            balanceChange = balance;
        } else if (preFields != null) {
            Amount preBalance = (Amount) preFields.get(Field.Amount);
            balanceChange = balance.subtract(preBalance);
        }
        if (address.equals(account)) {
            Effect effect = new Effect();
            effect.setAmount(new AmountObj(balanceChange.valueText(), getAssetName(balance.currencyString(), issuer), issuer, account));
            effect.setBalance(new AmountObj(balance.subtract(deliveredAmount).abs().valueText(), getAssetName(balance.currencyString(), issuer), issuer, account));
            effect.setType("asset");
            effects.add(effect);
        }
    }

    /**
     * other currency payment, contains highLimit and lowLimit
     * it stands for tx amount's currency's balance changes
     *
     * @param field
     * @param node
     * @param preFields
     * @param address
     * @param effects
     */
    private void parserRippleState(STObject field, AffectedNode node, STObject preFields, String address, List<Effect> effects, TxObj item) {
        if (item.getType() != null && item.getType().startsWith("active")) {
            return;
        }
        Amount highLimit = (Amount) field.get(Field.HighLimit);
        Amount lowLimit = (Amount) field.get(Field.LowLimit);
        Amount balance = (Amount) field.get(Field.Balance);
        Amount balanceChange = null;
        Amount preBalance = null;
        Amount reserve = (Amount) field.get(Field.Reserve);
        if (node.isCreatedNode()) {
            balanceChange = balance;
            if (reserve != null) {
                balanceChange = balanceChange.add(reserve);
                balance = balance.add(reserve);
            }
        } else if (preFields != null) {
            preBalance = (Amount) preFields.get(Field.Balance);
            Amount preReserve = (Amount) preFields.get(Field.Reserve);
            if (preBalance != null || preReserve != null) {
                if (preBalance != null)
                    balanceChange = balance.subtract(preBalance);
                else {
                    balanceChange = balance.subtract(balance);
                }
                if (reserve != null) {
                    balance = balance.add(reserve);
                    if (preReserve != null) {
                        balanceChange = balanceChange.add(reserve.subtract(preReserve));
                        if (reserve.isZero() && preReserve.isZero()) {
                            logger.debug("tx-malformed" + "tx-reserve" + address + ", " + item.getHash());
                        }
                    }
                } else {
                    if (preReserve != null) {
                        balanceChange = balanceChange.subtract(preReserve);
                    }
                }
            }
        }
        if (highLimit == null || lowLimit == null || balanceChange == null) {
            return;
        }
        if (highLimit.issuerString().equals(address) || lowLimit.issuerString().equals(address)) {
            String issuer = lowLimit.issuerString();
            String account = highLimit.issuerString();
            if (highLimit.value().intValue() == 0 && (balance.doubleValue() > 0 || (preBalance != null && preBalance.doubleValue() > 0))) {
                //highlimit.limit=0, and (balance > 0 or prebalance > 0) shows the issuer is highlimit.issuer
                issuer = highLimit.issuerString();
                account = lowLimit.issuerString();
            }
            Effect effect = new Effect();
            //Account balance change
            if (address.equals(account)) {
                if (highLimit.issuerString().equals(address)) {
                    effect.setAmount(new AmountObj(balanceChange.negate().valueText(), getAssetName(balance.currencyString(), issuer), issuer, account));
                } else if (lowLimit.issuerString().equals(address)) {
                    //paths tx
                    effect.setAmount(new AmountObj(balanceChange.valueText(), getAssetName(balance.currencyString(), issuer), issuer, account));
                }
                effect.setBalance(new AmountObj(balance.abs().valueText(), getAssetName(balance.currencyString(), issuer), issuer, account));
                if (balance.currency().equals(Currency.ASSET)) {
                    effect.setType("asset");
                } else {
                    effect.setType("amount");
                }
                effects.add(effect);
            } else {
                //gateway account, radaring.
                if (lowLimit.issuerString().equals(issuer)) {
                    effect.setAmount(new AmountObj(balanceChange.negate().valueText(), getAssetName(balance.currencyString(), issuer), account));
                    effect.setBalance(new AmountObj(balance.negate().valueText(), getAssetName(balance.currencyString(), issuer), account));
                } else {
                    effect.setAmount(new AmountObj(balanceChange.valueText(), getAssetName(balance.currencyString(), issuer), account));
                    effect.setBalance(new AmountObj(balance.valueText(), getAssetName(balance.currencyString(), issuer), account));
                }
                if (balance.currency().equals(Currency.ASSET)) {
                    effect.setType("asset");
                } else {
                    effect.setType("amount");
                }
                effects.add(effect);
            }
        }
    }

    private void parserAccountRoot(STObject field, AffectedNode node, STObject preFields, String address, List<Effect> effects, TxObj item) {
        AccountID account = (AccountID) field.get(Field.Account);
        if (account == null || !address.equals(account.address)) {
            return;
        }
        //VRP,VBC balance changes
        Amount balance = (Amount) field.get(Field.Balance);
        Amount balanceVBC = (Amount) field.get(Field.BalanceVBC);
        BigDecimal balanceChange = new BigDecimal(0);
        BigDecimal balanceVBCChange = new BigDecimal(0);
        if (node.isCreatedNode()) {
            //new fields
            if (balance != null)
                balanceChange = balance.value();
            if (balanceVBC != null)
                balanceVBCChange = balanceVBC.value();
        } else if (preFields != null) {
            //has prefields
            Amount preBalance = (Amount) preFields.get(Field.Balance);
            Amount preBalanceVBC = (Amount) preFields.get(Field.BalanceVBC);
            if (preBalance != null)
                balanceChange = balance.subtract(preBalance).value();
            if (preBalanceVBC != null)
                balanceVBCChange = balanceVBC.subtract(preBalanceVBC).value();

            if (item.getType().equals("set_regular_key")) {
                AccountID regularKey = (AccountID) preFields.get(Field.RegularKey);
                if (regularKey != null) {
                    item.setRecipient(regularKey.address);
                    item.setType("cancel_regular_key");
                }
            }
        }
        //if vbc is changing, then add to effect
        if (balanceVBCChange.abs().doubleValue() > 0) {
            Effect effect = new Effect();
            effect.setAmount(new AmountObj(balanceVBCChange.toPlainString(), "VBC", "-"));
            effect.setBalance(new AmountObj(balanceVBC.value().toPlainString(), "VBC", "-"));
            effect.setType("amount");
            effects.add(effect);

        }
        if (balanceChange.abs().doubleValue() > 0) {
            //VRP changes, then should know if only fee changes
            //if sender is current user, then add fee effects
            if (item.getSender().equals(address)) {
                if (balanceChange.abs().compareTo(new BigDecimal(item.getFee().getAmount())) == 1) {
                    //not  only fee changes
                    Effect effect = new Effect();
                    balanceChange = balanceChange.add(new BigDecimal(item.getFee().getAmount()));
                    effect.setAmount(new AmountObj(balanceChange.toPlainString(), "VRP", "-"));
                    effect.setBalance(new AmountObj(balance.add(new BigDecimal(item.getFee().getAmount())).valueText(), "VRP", "-"));
                    effect.setType("amount");
                    effects.add(effect);
                }
                Effect effect = new Effect();
                effect.setAmount(new AmountObj(new BigDecimal(item.getFee().getAmount()).negate().toPlainString(), "VRP", "-"));
                effect.setBalance(new AmountObj(balance.valueText(), "VRP", "-"));
                effect.setType("fee");
                effects.add(effect);

            } else {
                //only VRP balance change...
                Effect effect = new Effect();
                effect.setAmount(new AmountObj(balanceChange.toPlainString(), "VRP", "-"));
                effect.setBalance(new AmountObj(balance.valueText(), "VRP", "-"));
                effect.setType("amount");
                effects.add(effect);
            }

        }
    }

    private Map<String, List<AmountObj>> analyzeFeeTakers(TransactionMeta metaObj) {
        Map<String, List<AmountObj>> shares = new HashMap<>();
        if (metaObj.has(Field.FeeShareTakers)) {
            STArray takers = (STArray) metaObj.get(Field.FeeShareTakers);
            for (int j = 0; j < takers.size(); j++) {
                STObject taker = (STObject) takers.get(j).get(Field.FeeShareTaker);
                Amount amount = (Amount) taker.get(Field.Amount);
                AccountID account = (AccountID) taker.get(Field.Account);
                if (shares.containsKey(account.address)) {
                    shares.get(account.address).add(new AmountObj(amount.valueText(), amount.currencyString(), amount.issuerString()));
                } else {
                    ArrayList<AmountObj> amounts = new ArrayList<>();
                    amounts.add(new AmountObj(amount.valueText(), amount.currencyString(), amount.issuerString()));
                    shares.put(account.address, amounts);
                }
            }
        }
        return shares;
    }

    private List<Effect> dealFeeShareEffects(TransactionMeta meta, TxObj item, List<Effect> effects, String address) {
        Map<String, List<AmountObj>> shares = analyzeFeeTakers(meta);
        if (meta.has(Field.FeeShareTakers)) {
            if (item.getType().equals("radaring") || item.getType().equals("offercreate")) {
                List<Effect> newEff = new ArrayList<>();
                for (Effect effect : effects) {
                    // shares contains account
                    if (shares.containsKey(address)) {
                        List<AmountObj> list = shares.get(address);
                        for (int j = 0; j < list.size(); j++) {
                            AmountObj amount = list.get(j);
                            if (new BigDecimal(effect.getAmount().getAmount()).subtract(new BigDecimal(amount.getAmount())).abs().compareTo(new BigDecimal("0.000000001")) == -1  //same amount
                                    && effect.getAmount().getIssuer().equals(amount.getIssuer()) //same issuer
                                    && !effect.getAmount().getIssuer().equals(address)) { //not gateway
                                item.setType("fee_share");
                                newEff.add(effect);
                            }
                        }
                    }
                }
                if (item.getType().equals("fee_share")) {
                    effects = newEff;
                }
            }
        }
        return effects;
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
        String json = RadarWebSocketClient.request(postData);
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
            TxObj item = tx.analyze(address);
            item.setDate(String.valueOf(txObj.getInt("date")));
            item.setHash(tx.get(Field.hash).toHex());
            if (meta.engineResult().asInteger() != 0) {
                item.setType("failed");
            }
            if (!tx.account().address.equals(address) && item.getContact() == null) {
                item.setContact(tx.account().address);
            }

            item = dealAssetName(item);

            Map<String, Object> result = generateEffectListFromTxMetaByType(address, item, meta, item.getType());
            List<Effect> effects = (List<Effect>) result.get("effects");
            if (item.getType().equals("offercreate") && effects.size() > 1 && item.getOfferStatus() == null) {
                item.setOfferStatus("offer_funded");
            }
            if (item.getType().equals("offercreate") && effects.size() == 0) {
                continue;
            }
            item = (TxObj) result.get("item");
            effects = dealFeeShareEffects(meta, item, effects, address);
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

    private TxObj dealAssetName(TxObj item) {
        if (item.getAmount() != null) {
            item.getAmount().setCurrency(getAssetName(item.getAmount().getCurrency(), item.getAmount().getIssuer()));
        }
        if (item.getTakerGets() != null) {
            item.getTakerGets().setCurrency(getAssetName(item.getTakerGets().getCurrency(), item.getTakerGets().getIssuer()));
        }
        if (item.getTakerPays() != null) {
            item.getTakerPays().setCurrency(getAssetName(item.getTakerPays().getCurrency(), item.getTakerPays().getIssuer()));
        }

        return item;
    }

    /**
     * @param offers
     * @param currency1
     * @param currency2
     * @param flag      0--asks(sell)  1--bids(buy)
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
                } else if (isNative(currency1)) {
                    showPrice = new BigDecimal(jo.getString("quality")).multiply(new BigDecimal(1000000));
                } else {
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

    private boolean isNative(String currency) {
        return currency.equalsIgnoreCase("VRP") || currency.equalsIgnoreCase("VBC");
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
        String jsonResult = RadarWebSocketClient.request(postData);
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