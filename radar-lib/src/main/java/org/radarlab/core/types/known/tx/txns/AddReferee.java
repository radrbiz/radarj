package org.radarlab.core.types.known.tx.txns;

import org.radarlab.core.AccountID;
import org.radarlab.core.Amount;
import org.radarlab.core.TxObj;
import org.radarlab.core.fields.Field;
import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.tx.Transaction;

/**
 * Created by Andy
 * since 14/12/12.
 */
public class AddReferee extends Transaction {
    public AddReferee() {
        super(TransactionType.AddReferee);
    }
    public AccountID destination() {return get(AccountID.Destination);}
    public Amount amount() {return get(Amount.Amount);}
    public void amount(Amount val) {put(Field.Amount, val);}
    public void destination(AccountID val) {put(Field.Destination, val);}
    @Override
    public TxObj analyze(String address){
        init();
        item.setRecipient(destination().address);
        if (!destination().address.equals(address))
            item.setContact(destination().address);
        item.setType(destination().address.equals(address) ? "referee" : "addreferee");
        return item;
    }
}
