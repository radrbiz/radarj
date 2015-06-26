package org.radarlab.core.serialized.enums;


import org.radarlab.core.serialized.BinaryParser;
import org.radarlab.core.serialized.BytesSink;
import org.radarlab.core.serialized.SerializedType;
import org.radarlab.core.serialized.TypeTranslator;
import org.ripple.bouncycastle.util.encoders.Hex;

import java.util.TreeMap;

public enum TransactionType implements SerializedType {
    Invalid (-1),
    Payment (0),
    Claim (1), // open
    WalletAdd (2),
    AccountSet (3),
    PasswordFund (4), // open
    SetRegularKey(5),
    NickNameSet (6), // open
    OfferCreate (7),
    OfferCancel (8),
    Contract (9),
    TicketCreate(10),
    TicketCancel(11),
    TrustSet (20),
    EnableAmendment(100),
    SetFee(101),
    AddReferee(182),
    Dividend(181),
    ActiveAccount(183),
    Issue(184);

    public int asInteger() {
        return ord;
    }

    final int ord;
    TransactionType(int i) {
       ord = i;
    }

    static private TreeMap<Integer, TransactionType> byCode = new TreeMap<Integer, TransactionType>();
    static {
        for (Object a : TransactionType.values()) {
            TransactionType f = (TransactionType) a;
            byCode.put(f.ord, f);
        }
    }

    static public TransactionType fromNumber(Number i) {
        return byCode.get(i.intValue());
    }

    // SeralizedType interface
    @Override
    public byte[] toBytes() {
        return new byte[]{(byte) (ord >> 8), (byte) (ord & 0xFF)};
    }
    @Override
    public Object toJSON() {
        return toString();
    }
    @Override
    public String toHex() {
        return Hex.toHexString(toBytes());
    }
    @Override
    public void toBytesSink(BytesSink to) {
        to.add(toBytes());
    }
    public static class Translator extends TypeTranslator<TransactionType> {
        @Override
        public TransactionType fromParser(BinaryParser parser, Integer hint) {
            byte[] read = parser.read(2);
            return fromNumber(((read[0] << 8) | read[1]) & 0x0FF);
        }

        @Override
        public TransactionType fromInteger(int integer) {
            return fromNumber(integer);
        }

        @Override
        public TransactionType fromString(String value) {
            return TransactionType.valueOf(value);
        }
    }
    public static Translator translate = new Translator();

}
