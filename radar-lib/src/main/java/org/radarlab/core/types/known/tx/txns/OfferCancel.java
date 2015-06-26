package org.radarlab.core.types.known.tx.txns;


import org.radarlab.core.TxObj;
import org.radarlab.core.fields.Field;
import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.tx.Transaction;
import org.radarlab.core.uint.UInt32;

public class OfferCancel extends Transaction {
    public OfferCancel() {
        super(TransactionType.OfferCancel);
    }
    public UInt32 offerSequence() {return get(UInt32.OfferSequence);}
    public void offerSequence(UInt32 val) {put(Field.OfferSequence, val);}
    @Override
    public TxObj analyze(String address){
        init();
        item.setType("offer_cancelled");
        return item;
    }
}
