package org.radarlab.core;

public class AmountObj {
    private Double amount;
    private String currency;
    private String issuer;

    public AmountObj(Double amount, String currency, String issuer){
        this.amount = amount;
        this.currency = currency;
        this.issuer = issuer;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
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
}
