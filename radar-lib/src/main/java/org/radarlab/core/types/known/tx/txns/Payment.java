package org.radarlab.core.types.known.tx.txns;


import org.radarlab.core.*;
import org.radarlab.core.fields.Field;
import org.radarlab.core.hash.Hash256;
import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.tx.Transaction;
import org.radarlab.core.uint.UInt32;

public class Payment extends Transaction {
    public Payment() {
        super(TransactionType.Payment);
    }

    public UInt32 destinationTag() {return get(UInt32.DestinationTag);}
    public Hash256 invoiceID() {return get(Hash256.InvoiceID);}
    public Amount amount() {return get(Amount.Amount);}
    public Amount sendMax() {return get(Amount.SendMax);}
    public AccountID destination() {return get(AccountID.Destination);}
    public PathSet paths() {return get(PathSet.Paths);}
    public void destinationTag(UInt32 val) {put(Field.DestinationTag, val);}
    public void invoiceID(Hash256 val) {put(Field.InvoiceID, val);}
    public void amount(Amount val) {put(Field.Amount, val);}
    public void sendMax(Amount val) {put(Field.SendMax, val);}
    public void destination(AccountID val) {put(Field.Destination, val);}
    public void paths(PathSet val) {put(Field.Paths, val);}

    @Override
    public TxObj analyze(String address){
        init();
        item.setRecipient(destination().address);
        if (!destination().address.equals(address))
            item.setContact(destination().address);
        String paymentAmount = amount().valueText();
        AmountObj amount = new AmountObj(paymentAmount, amount().currencyString(), amount().issuerString());
        amount.setCurrency(amount().currencyString());
        item.setAmount(amount);

        if (!address.equals(destination().address) && !address.equals(account().address)) {
            if (paths() != null) {
                for (PathSet.Path path : paths()) {
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
        } else {
            if (address.equals(destination().address) && address.equals(account().address)) {
                item.setType("exchange");
                item.setSendMax(new AmountObj(sendMax().valueText(),
                        sendMax().currencyString(), sendMax().issuerString()));
            } else
                item.setType(destination().address.equals(address) ? "received" : "sent");
        }
        return item;
    }

}
