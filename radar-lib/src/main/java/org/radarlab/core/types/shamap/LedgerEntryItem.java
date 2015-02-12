package org.radarlab.core.types.shamap;


import org.radarlab.core.STObject;
import org.radarlab.core.hash.prefixes.HashPrefix;
import org.radarlab.core.hash.prefixes.Prefix;
import org.radarlab.core.serialized.BytesSink;
import org.radarlab.core.types.known.sle.LedgerEntry;

public class LedgerEntryItem extends ShaMapItem<LedgerEntry> {
    public LedgerEntryItem(LedgerEntry entry) {
        this.entry = entry;
    }

    public LedgerEntry entry;

    @Override
    void toBytesSink(BytesSink sink) {
        entry.toBytesSink(sink);
    }

    @Override
    public ShaMapItem<LedgerEntry> copy() {
        STObject object = STObject.translate.fromBytes(entry.toBytes());
        LedgerEntry le = (LedgerEntry) object;
        // TODO: what about other auxiliary (non serialized) fields
        le.index(entry.index());
        return new LedgerEntryItem(le);
    }

    @Override
    public Prefix hashPrefix() {
        return HashPrefix.leafNode;
    }
}
