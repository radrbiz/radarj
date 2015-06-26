package org.radarlab.core.types.known.tx.txns;


import org.radarlab.core.TxObj;
import org.radarlab.core.hash.Hash256;
import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.tx.Transaction;

public class TicketCancel extends Transaction {
    public TicketCancel() {
        super(TransactionType.TicketCancel);
    }
    public Hash256 ticketID() {
        return get(Hash256.TicketID);
    }
    public void ticketID(Hash256 id) {
        put(Hash256.TicketID, id);
    }
    @Override
    public TxObj analyze(String address){
        init();
        item.setType("ticket_cancel");
        return item;
    }
}
