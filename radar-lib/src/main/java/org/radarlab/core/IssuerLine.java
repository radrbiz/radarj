package org.radarlab.core;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class IssuerLine {
    private boolean show = false;
    private double amount;
    private String currency;
    private String issuer;
    private List<JSONObject> lines = new ArrayList<>();



    public List<JSONObject> getLines() {
        return lines;
    }

    public void setLines(List<JSONObject> lines) {
        this.lines = lines;
    }

    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
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
