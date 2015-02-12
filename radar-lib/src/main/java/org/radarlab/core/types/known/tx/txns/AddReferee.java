package org.radarlab.core.types.known.tx.txns;

import org.radarlab.core.AccountID;
import org.radarlab.core.Amount;
import org.radarlab.core.fields.Field;
import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.tx.Transaction;

public class AddReferee extends Transaction {
    public AddReferee() {
        super(TransactionType.AddReferee);
    }
    public AccountID destination() {return get(AccountID.Destination);}
    public Amount amount() {return get(Amount.Amount);}
    public void amount(Amount val) {put(Field.Amount, val);}
    public void destination(AccountID val) {put(Field.Destination, val);}
}
