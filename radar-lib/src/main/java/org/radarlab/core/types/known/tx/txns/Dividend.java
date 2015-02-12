package org.radarlab.core.types.known.tx.txns;

import org.radarlab.core.AccountID;
import org.radarlab.core.fields.Field;
import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.tx.Transaction;
import org.radarlab.core.uint.UInt64;

/**
 * Dividend data class
 */
public class Dividend extends Transaction{
    public Dividend() {
        super(TransactionType.Dividend);
    }

    public AccountID destination() {return get(AccountID.Destination);}
    public UInt64 dividendCoins(){return (UInt64)get(Field.DividendCoins);}
    public UInt64 dividendCoinsVBC(){return (UInt64)get(Field.DividendCoinsVBC);}
}
