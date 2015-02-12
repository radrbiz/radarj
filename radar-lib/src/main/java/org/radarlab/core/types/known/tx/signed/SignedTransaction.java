package org.radarlab.core.types.known.tx.signed;


import org.radarlab.core.Amount;
import org.radarlab.core.VariableLength;
import org.radarlab.core.hash.HalfSha512;
import org.radarlab.core.hash.Hash256;
import org.radarlab.core.hash.prefixes.HashPrefix;
import org.radarlab.core.serialized.BytesList;
import org.radarlab.core.serialized.MultiSink;
import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.tx.Transaction;
import org.radarlab.core.uint.UInt32;
import org.radarlab.crypto.ecdsa.IKeyPair;

public class SignedTransaction {
    public Transaction txn;
    public Hash256 hash;
    public Hash256 signingHash;
    public Hash256 previousSigningHash;
    public String  tx_blob;

    public SignedTransaction(Transaction txn){
        this.txn = txn;
    }

    public void prepare(IKeyPair keyPair, Amount fee, UInt32 Sequence, UInt32 lastLedgerSequence) {
        VariableLength pubKey = new VariableLength(keyPair.pubBytes());

        // This won't always be specified
        if (lastLedgerSequence != null) {
            txn.put(UInt32.LastLedgerSequence, lastLedgerSequence);
        }
        txn.put(UInt32.Sequence, Sequence);
        txn.put(Amount.Fee, fee);
        txn.put(VariableLength.SigningPubKey, pubKey);

        if (Transaction.CANONICAL_FLAG_DEPLOYED) {
            txn.setCanonicalSignatureFlag();
        }

        signingHash = txn.signingHash();
        if (previousSigningHash != null && signingHash.equals(previousSigningHash)) {
            return;
        }
        try {
            VariableLength signature = new VariableLength(keyPair.sign(signingHash.bytes()));
            txn.put(VariableLength.TxnSignature, signature);

            BytesList blob = new BytesList();
            HalfSha512 id = HalfSha512.prefixed256(HashPrefix.transactionID);

            txn.toBytesSink(new MultiSink(blob, id));
            tx_blob = blob.bytesHex();
            hash = id.finish();
        } catch (Exception e) {
            // electric paranoia
            previousSigningHash = null;
            throw new RuntimeException(e);
        } /*else {*/
            previousSigningHash = signingHash;
        // }
    }

    public TransactionType transactionType() {
        return txn.transactionType();
    }
}
