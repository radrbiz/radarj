package org.radarlab.core.types.known.tx.txns;

import org.radarlab.core.*;
import org.radarlab.core.fields.Field;
import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.tx.Transaction;

/**
 * Created by ac
 * since 15/5/13.
 */
public class Issue extends Transaction {
    public Issue() {
        super(TransactionType.Issue);
    }

    public Amount amount() {
        return get(Amount.Amount);
    }
    public AccountID destination() {
        return (AccountID) get(Field.Destination);
    }
    public AccountID releaseSchedule() {
        return (AccountID) get(Field.ReleaseSchedule);
    }
    public void amount(Amount val) {put(Field.Amount, val);}
    public void destination(AccountID val) {put(Field.Destination, val);}
    public void releaseSchedule(STArray val) {put(Field.ReleaseSchedule, val);}

    @Override
    public TxObj analyze(String address){
        init();
        item.setAmount(new AmountObj(amount().valueText(), amount().currencyString(), amount().issuerString()));
        item.setSender(account().address);
        item.setRecipient(destination().address);
        item.setType("issue");
        return item;
    }
}
