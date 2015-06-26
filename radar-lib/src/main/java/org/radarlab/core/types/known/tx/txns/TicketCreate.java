package org.radarlab.core.types.known.tx.txns;


import org.radarlab.core.TxObj;
import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.tx.Transaction;

public class TicketCreate extends Transaction {
    public TicketCreate() {
        super(TransactionType.TicketCreate);
    }

    @Override
    public TxObj analyze(String address){
        init();
        item.setType("ticket_create");
        return item;
    }
}
