package org.radarlab.core.types.known.tx.txns;


import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.tx.Transaction;

public class TicketCreate extends Transaction {
    public TicketCreate() {
        super(TransactionType.TicketCreate);
    }
}
