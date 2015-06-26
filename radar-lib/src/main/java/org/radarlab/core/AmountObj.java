package org.radarlab.core;

/**
 * Created by ac
 * since 15/1/22.
 */
public class AmountObj {
    private String amount;
    private String currency;
    private String issuer;
    private String account;

    public AmountObj(String amount, String currency, String issuer){
        this.amount = amount;
        this.currency = currency;
        this.issuer = issuer;
    }

    public AmountObj(String amount, String currency, String issuer, String account){
        this.amount = amount;
        this.currency = currency;
        this.issuer = issuer;
        this.account = account;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
