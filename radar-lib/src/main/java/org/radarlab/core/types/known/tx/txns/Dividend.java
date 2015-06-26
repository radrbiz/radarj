package org.radarlab.core.types.known.tx.txns;

import org.radarlab.core.AccountID;
import org.radarlab.core.AmountObj;
import org.radarlab.core.TxObj;
import org.radarlab.core.fields.Field;
import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.tx.Transaction;
import org.radarlab.core.uint.UInt64;

/**
 * Created by Andy
 * since 15/1/4.
 */
public class Dividend extends Transaction{
    public Dividend() {
        super(TransactionType.Dividend);
    }

    public AccountID destination() {return get(AccountID.Destination);}
    public UInt64 dividendCoins(){return (UInt64)get(Field.DividendCoins);}
    public UInt64 dividendCoinsVBC(){return (UInt64)get(Field.DividendCoinsVBC);}

    @Override
    public TxObj analyze(String address){
        init();
        if (!destination().address.equals(address)) {
            return null;
        }
        item.setRecipient(address);
        item.setType("dividend");
        item.setAmount(new AmountObj(String.valueOf(dividendCoins().doubleValue() / 1000000), "VRP", null));
        item.setAmountVBC(new AmountObj(String.valueOf(dividendCoinsVBC().doubleValue() / 1000000), "VBC", null));

        return item;
    }
}
