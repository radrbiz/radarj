package org.radarlab.core.types.known.tx.txns;


import org.radarlab.core.Amount;
import org.radarlab.core.AmountObj;
import org.radarlab.core.TxObj;
import org.radarlab.core.fields.Field;
import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.tx.Transaction;
import org.radarlab.core.uint.UInt32;

public class OfferCreate extends Transaction {
    public OfferCreate() {
        super(TransactionType.OfferCreate);
    }
    public UInt32 expiration() {return get(UInt32.Expiration);}
    public UInt32 offerSequence() {return get(UInt32.OfferSequence);}
    public Amount takerPays() {return get(Amount.TakerPays);}
    public Amount takerGets() {return get(Amount.TakerGets);}
    public void expiration(UInt32 val) {put(Field.Expiration, val);}
    public void offerSequence(UInt32 val) {put(Field.OfferSequence, val);}
    public void takerPays(Amount val) {put(Field.TakerPays, val);}
    public void takerGets(Amount val) {put(Field.TakerGets, val);}

    @Override
    public TxObj analyze(String address){
        init();
        //if tx result is not success, then set type to failed;
        if (item.getSender().equals(address)) {
            item.setSender(address);
        }
        if (address.equals(takerGets().issuerString()) || address.equals(takerPays().issuerString())) {
            item.setType("offer_radaring");
        } else
            item.setType("offercreate");
        if (item.getSender().equals(address) || item.getType().equals("offer_radaring")) {
            AmountObj takerGets = new AmountObj(takerGets().valueText(), takerGets().currencyString(), takerGets().issuerString());
            AmountObj takerPays = new AmountObj(takerPays().valueText(), takerPays().currencyString(), takerPays().issuerString());
            item.setTakerGets(takerGets);
            item.setTakerPays(takerPays);
        }

        return item;
    }
}
