package org.radarlab.core.types.known.tx.txns;


import org.radarlab.core.AccountID;
import org.radarlab.core.TxObj;
import org.radarlab.core.fields.Field;
import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.tx.Transaction;

/**
 * Created by AC on 15/8/17.
 */
public class SetRegularKey extends Transaction {
    public SetRegularKey() {
        super(TransactionType.SetRegularKey);
    }

    public AccountID RegularKey() {return get(AccountID.RegularKey);}
    public void RegularKey(AccountID val) {put(Field.RegularKey, val);}


    @Override
    public TxObj analyze(String address){
        init();
        item.setType("set_regular_key");
        if(RegularKey()!=null)
            item.setRecipient(RegularKey().address);
        return item;
    }
}
