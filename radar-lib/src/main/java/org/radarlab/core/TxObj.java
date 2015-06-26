package org.radarlab.core;

import java.util.List;

/**
 * Created by Andy
 * since 14/12/31.
 */
public class TxObj {
    private String date;
    private String sender;
    private String recipient;
    private AmountObj amount;
    private AmountObj amountVBC;
    private String type;
    private String hash;
    private String contact;
    private AmountObj limitAmount;
    private AmountObj takerGets;
    private AmountObj takerPays;
    private AmountObj partiallyGets;
    private AmountObj partiallyPays;

    private String OfferStatus;

    private List<Effect> effects;
    private List<Effect> showEffects;

    private AmountObj fee;
    private long balance;
    private AmountObj sendMax;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public AmountObj getFee() {
        return fee;
    }

    public void setFee(AmountObj fee) {
        this.fee = fee;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Effect> getEffects() {
        return effects;
    }

    public void setEffects(List<Effect> effects) {
        this.effects = effects;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public void setTakerGets(AmountObj takerGets) {
        this.takerGets = takerGets;
    }

    public void setTakerPays(AmountObj takerPays) {
        this.takerPays = takerPays;
    }

    public AmountObj getAmount() {
        return amount;
    }

    public void setAmount(AmountObj amount) {
        this.amount = amount;
    }

    public AmountObj getAmountVBC() {
        return amountVBC;
    }

    public void setAmountVBC(AmountObj amountVBC) {
        this.amountVBC = amountVBC;
    }

    public AmountObj getTakerGets() {
        return takerGets;
    }

    public AmountObj getTakerPays() {
        return takerPays;
    }

    public AmountObj getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(AmountObj limitAmount) {
        this.limitAmount = limitAmount;
    }

    public String getOfferStatus() {
        return OfferStatus;
    }

    public void setOfferStatus(String offerStatus) {
        OfferStatus = offerStatus;
    }

    public List<Effect> getShowEffects() {
        return showEffects;
    }

    public void setShowEffects(List<Effect> showEffects) {
        this.showEffects = showEffects;
    }

    public AmountObj getPartiallyGets() {
        return partiallyGets;
    }

    public void setPartiallyGets(AmountObj partiallyGets) {
        this.partiallyGets = partiallyGets;
    }

    public AmountObj getPartiallyPays() {
        return partiallyPays;
    }

    public void setPartiallyPays(AmountObj partiallyPays) {
        this.partiallyPays = partiallyPays;
    }

    public AmountObj getSendMax() {
        return sendMax;
    }

    public void setSendMax(AmountObj sendMax) {
        this.sendMax = sendMax;
    }
}
