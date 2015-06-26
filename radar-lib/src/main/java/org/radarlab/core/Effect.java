package org.radarlab.core;

/**
 * Created by Andy
 * since 15/1/4.
 */
public class Effect {
    private AmountObj amount;
    private AmountObj balance;
//    private AmountObj balanceVBC;
    private String type;
    private AmountObj takerGets;
    private AmountObj takerPays;

    public AmountObj getAmount() {
        return amount;
    }

    public void setAmount(AmountObj amount) {
        this.amount = amount;
    }

    public AmountObj getBalance() {
        return balance;
    }

    public void setBalance(AmountObj balance) {
        this.balance = balance;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public AmountObj getTakerPays() {
        return takerPays;
    }

    public void setTakerPays(AmountObj takerPays) {
        this.takerPays = takerPays;
    }

    public AmountObj getTakerGets() {
        return takerGets;
    }

    public void setTakerGets(AmountObj takerGets) {
        this.takerGets = takerGets;
    }
}
