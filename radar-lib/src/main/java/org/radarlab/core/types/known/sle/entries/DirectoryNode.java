package org.radarlab.core.types.known.sle.entries;


import org.radarlab.core.AccountID;
import org.radarlab.core.Currency;
import org.radarlab.core.Vector256;
import org.radarlab.core.fields.Field;
import org.radarlab.core.hash.Hash160;
import org.radarlab.core.hash.Hash256;
import org.radarlab.core.hash.Index;
import org.radarlab.core.serialized.enums.LedgerEntryType;
import org.radarlab.core.types.known.sle.LedgerEntry;
import org.radarlab.core.uint.UInt64;

public class DirectoryNode extends LedgerEntry {
    public DirectoryNode() {
        super(LedgerEntryType.DirectoryNode);
    }

    public UInt64 indexNext() {return get(UInt64.IndexNext);}
    public UInt64 indexPrevious() {return get(UInt64.IndexPrevious);}
    public UInt64 exchangeRate() {return get(UInt64.ExchangeRate);}
    public Hash256 rootIndex() {return get(Hash256.RootIndex);}
    public AccountID owner() {return get(AccountID.Owner);}
    public Hash160 takerPaysCurrency() {return get(Hash160.TakerPaysCurrency);}
    public Hash160 takerPaysIssuer() {return get(Hash160.TakerPaysIssuer);}
    public Hash160 takerGetsCurrency() {return get(Hash160.TakerGetsCurrency);}
    public Hash160 takerGetsIssuer() {return get(Hash160.TakerGetsIssuer);}
    public Vector256 indexes() {return get(Vector256.Indexes);}
    public void indexNext(UInt64 val) {put(Field.IndexNext, val);}
    public void indexPrevious(UInt64 val) {put(Field.IndexPrevious, val);}
    public void exchangeRate(UInt64 val) {put(Field.ExchangeRate, val);}
    public void rootIndex(Hash256 val) {put(Field.RootIndex, val);}
    public void owner(AccountID val) {put(Field.Owner, val);}
    public void takerPaysCurrency(Hash160 val) {put(Field.TakerPaysCurrency, val);}
    public void takerPaysIssuer(Hash160 val) {put(Field.TakerPaysIssuer, val);}
    public void takerGetsCurrency(Hash160 val) {put(Field.TakerGetsCurrency, val);}
    public void takerGetsIssuer(Hash160 val) {put(Field.TakerGetsIssuer, val);}
    public void indexes(Vector256 val) {put(Field.Indexes, val);}

    public Hash256 nextIndex() {
        return Index.directoryNode(rootIndex(), indexNext());
    }
    public Hash256 prevIndex() {
        return Index.directoryNode(rootIndex(), indexPrevious());
    }

    public boolean hasPreviousIndex() {
        return indexPrevious() != null;
    }

    public boolean hasNextIndex() {
        return indexNext() != null;
    }

    public boolean isRootIndex() {
        return rootIndex().equals(index());
    }

    public void setExchangeDefaults() {
        if (takerGetsCurrency() == null) {
            takerGetsCurrency(Currency.VRP);
            takerGetsIssuer(AccountID.VRP_ISSUER);
        } else if (takerPaysCurrency() == null) {
            takerPaysCurrency(Currency.VRP);
            takerPaysIssuer(AccountID.VRP_ISSUER);
        }
    }

    @Override
    public void setDefaults() {
        super.setDefaults();
        if (exchangeRate() != null) {
            setExchangeDefaults();
        }
        if (indexes() == null) {
            indexes(new Vector256());
        }
    }
}
