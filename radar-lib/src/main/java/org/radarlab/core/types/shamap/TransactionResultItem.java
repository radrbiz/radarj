package org.radarlab.core.types.shamap;


import org.radarlab.core.hash.prefixes.HashPrefix;
import org.radarlab.core.hash.prefixes.Prefix;
import org.radarlab.core.serialized.BinarySerializer;
import org.radarlab.core.serialized.BytesSink;
import org.radarlab.core.types.known.tx.result.TransactionResult;

public class TransactionResultItem extends ShaMapItem<TransactionResult> {
    TransactionResult result;

    public TransactionResultItem(TransactionResult result) {
        this.result = result;
    }

    @Override
    void toBytesSink(BytesSink sink) {
        BinarySerializer write = new BinarySerializer(sink);
        write.addLengthEncoded(result.txn);
        write.addLengthEncoded(result.meta);
    }

    @Override
    public ShaMapItem<TransactionResult> copy() {
        // that's ok right ;) these bad boys are immutable anyway
        return this;
    }

    @Override
    public Prefix hashPrefix() {
        return HashPrefix.txNode;
    }
}
