package org.radarlab.core.types.ledger;


import org.radarlab.core.RippleDate;
import org.radarlab.core.binary.STReader;
import org.radarlab.core.hash.Hash256;
import org.radarlab.core.serialized.BinaryParser;
import org.radarlab.core.uint.UInt32;
import org.radarlab.core.uint.UInt64;
import org.radarlab.core.uint.UInt8;

import java.util.Date;

public class LedgerHeader {
    public UInt32 version;         // Always 0x4C475200 (LWR) (Secures signed objects)
    public UInt32 sequence;        // Ledger Sequence (0 for genesis ledger)
    public UInt64 totalVRP;        //
    public Hash256 previousLedger;  // The hash of the previous ledger (0 for genesis ledger)
    public Hash256 transactionHash; // The hash of the transaction tree's root node.
    public Hash256 stateHash;       // The hash of the state tree's root node.
    public UInt32 parentCloseTime; // The time the previous ledger closed
    public UInt32 closeTime;       // UTC minute ledger closed encoded as seconds since 1/1/2000 (or 0 for genesis ledger)
    public UInt8   closeResolution; // The resolution (in seconds) of the close time
    public UInt8 closeFlags;      // Flags

    public Date closeDate;

    public static LedgerHeader fromParser(BinaryParser parser) {
        return fromReader(new STReader(parser));
    }
    public static LedgerHeader fromReader(STReader reader) {
        LedgerHeader ledger = new LedgerHeader();

        ledger.version = reader.uInt32();
        ledger.sequence = reader.uInt32();
        ledger.totalVRP = reader.uInt64();
        ledger.previousLedger = reader.hash256();
        ledger.transactionHash= reader.hash256();
        ledger.stateHash = reader.hash256();
        ledger.parentCloseTime = reader.uInt32();
        ledger.closeTime = reader.uInt32();
        ledger.closeResolution = reader.uInt8();
        ledger.closeFlags = reader.uInt8();

        ledger.closeDate = RippleDate.fromSecondsSinceRippleEpoch(ledger.closeTime);

        return ledger;
    }
}
