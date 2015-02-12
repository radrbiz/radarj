package org.radarlab.core.types.known.sle;


import org.radarlab.core.fields.Field;
import org.radarlab.core.hash.Hash256;
import org.radarlab.core.serialized.enums.LedgerEntryType;
import org.radarlab.core.uint.UInt32;

// this class has a PreviousTxnID and PreviousTxnLgrSeq
abstract public class ThreadedLedgerEntry extends LedgerEntry {
    public ThreadedLedgerEntry(LedgerEntryType type) {
        super(type);
    }
    public UInt32 previousTxnLgrSeq() {return get(UInt32.PreviousTxnLgrSeq);}
    public Hash256 previousTxnID() {return get(Hash256.PreviousTxnID);}
    public void previousTxnLgrSeq(UInt32 val) {put(Field.PreviousTxnLgrSeq, val);}
    public void previousTxnID(Hash256 val) {put(Field.PreviousTxnID, val);}
}
