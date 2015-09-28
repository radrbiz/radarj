package org.radarlab.core.types.known.tx.txns;

import org.radarlab.core.AccountID;
import org.radarlab.core.Amount;
import org.radarlab.core.STArray;
import org.radarlab.core.TxObj;
import org.radarlab.core.fields.Field;
import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.tx.Transaction;

/**
 * Created by ac
 * since 15/5/7.
 */
public class ActiveAccount extends Transaction {
    public ActiveAccount() {
        super(TransactionType.ActiveAccount);
    }
    public Amount amount() {
        return get(Amount.Amount);
    }
    public AccountID referee() {
        return (AccountID) get(Field.Referee);
    }
    public AccountID reference() {
        return (AccountID) get(Field.Reference);
    }
    public void amount(Amount val) {put(Field.Amount, val);}
    public void referee(AccountID val) {put(Field.Referee, val);}
    public void reference(AccountID val) {put(Field.Reference, val);}

    public void Amounts(STArray val){put(Field.Amounts,val);}
    public void Limits(STArray val){put(Field.Limits,val);}

    @Override
    public TxObj analyze(String address){
        init();
        item.setRecipient(reference().address);
        //not reference
        if(!reference().address.equals(address)){
            //referee==sender
            if(referee().address.equals(item.getSender())) {
                item.setType("active_show");
            }
            //referee!=sender && referee==address
            else if(referee().address.equals(address)) {
                item.setType("active_referee");
            }
            //referee!=sender && sender==address
            else {
                item.setType("active_add");
                item.setContact(referee().address);
            }
        }
        //reference
        else{
            if(referee().address.equals(item.getSender())){
                item.setType("active_acc");
            }else {
                item.setType("active");
                item.setContact(referee().address);
            }
        }

        return item;
    }
}
