package org.radarlab.core;


import org.radarlab.core.serialized.enums.LedgerEntryType;
import org.radarlab.core.serialized.enums.TransactionType;
import org.radarlab.core.types.known.sle.LedgerEntry;
import org.radarlab.core.types.known.sle.LedgerHashes;
import org.radarlab.core.types.known.sle.entries.AccountRoot;
import org.radarlab.core.types.known.sle.entries.DirectoryNode;
import org.radarlab.core.types.known.sle.entries.Offer;
import org.radarlab.core.types.known.sle.entries.RippleState;
import org.radarlab.core.types.known.tx.Transaction;
import org.radarlab.core.types.known.tx.result.AffectedNode;
import org.radarlab.core.types.known.tx.result.TransactionMeta;
import org.radarlab.core.types.known.tx.txns.*;
import org.radarlab.core.types.known.tx.txns.Issue;

public class STObjectFormatter {
    public static STObject doFormatted(STObject source) {
        // This would need to go before the test that just checks
        // for ledgerEntryType
        if (AffectedNode.isAffectedNode(source)) {
            return new AffectedNode(source);
        }

        if (TransactionMeta.isTransactionMeta(source)) {
            TransactionMeta meta = new TransactionMeta();
            meta.fields = source.fields;
            return meta;
        }

        LedgerEntryType ledgerEntryType = STObject.ledgerEntryType(source);
        if (ledgerEntryType != null) {
            return ledgerFormatted(source, ledgerEntryType);
        }

        TransactionType transactionType = STObject.transactionType(source);
        if (transactionType != null) {
            return transactionFormatted(source, transactionType);
        }

        return source;
    }

    private static STObject transactionFormatted(STObject source, TransactionType transactionType) {
        STObject constructed = null;
        switch (transactionType) {
            case Invalid:
                break;
            case Payment:
                constructed = new Payment();
                break;
            case Claim:
                break;
            case WalletAdd:
                break;
            case AccountSet:
                constructed = new AccountSet();
                break;
            case PasswordFund:
                break;
            case NickNameSet:
                break;
            case OfferCreate:
                constructed = new OfferCreate();
                break;
            case OfferCancel:
                constructed = new OfferCancel();
                break;
            case Contract:
                break;
            case TicketCreate:
                constructed = new TicketCreate();
                break;
            case TicketCancel:
                constructed = new TicketCancel();
                break;
            case TrustSet:
                constructed = new TrustSet();
                break;
            case EnableAmendment:
                break;
            case SetFee:
                break;
            case AddReferee:
                constructed = new AddReferee();
                break;
            case Dividend:
                constructed = new Dividend();
                break;
            case ActiveAccount:
                constructed = new ActiveAccount();
                break;
            case Issue:
                constructed = new Issue();
                break;
            case SetRegularKey:
                constructed = new SetRegularKey();
                break;
        }
        if (constructed == null) {
            constructed = new Transaction(transactionType);
        }

        constructed.fields = source.fields;
        return constructed;

    }

    private static STObject ledgerFormatted(STObject source, LedgerEntryType ledgerEntryType) {
        STObject constructed = null;
        switch (ledgerEntryType) {
            case Offer:
                constructed = new Offer();
                break;
            case RippleState:
                constructed = new RippleState();
                break;
            case AccountRoot:
                constructed = new AccountRoot();
                break;
            case Invalid:
                break;
            case DirectoryNode:
                constructed = new DirectoryNode();
                break;
            case GeneratorMap:
                break;
            case Contract:
                break;
            case LedgerHashes:
                constructed = new LedgerHashes();
                break;
            case EnabledAmendments:
                break;
            case FeeSettings:
                break;
        }
        if (constructed == null) {
            constructed = new LedgerEntry(ledgerEntryType);
        }
        constructed.fields = source.fields;
        return constructed;
    }
}
