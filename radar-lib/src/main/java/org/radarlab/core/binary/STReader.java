package org.radarlab.core.binary;


import org.radarlab.core.*;
import org.radarlab.core.hash.Hash128;
import org.radarlab.core.hash.Hash160;
import org.radarlab.core.hash.Hash256;
import org.radarlab.core.hash.prefixes.HashPrefix;
import org.radarlab.core.serialized.BinaryParser;
import org.radarlab.core.uint.UInt16;
import org.radarlab.core.uint.UInt32;
import org.radarlab.core.uint.UInt64;
import org.radarlab.core.uint.UInt8;

import java.util.Arrays;
import java.util.Date;

public class STReader {
    protected BinaryParser parser;
    public STReader(BinaryParser parser) {
        this.parser = parser;
    }
    public STReader(String hex) {
        this.parser = new BinaryParser(hex);
    }
    public UInt8 uInt8() {
        return UInt8.translate.fromParser(parser);
    }
    public UInt16 uInt16() {
        return UInt16.translate.fromParser(parser);
    }
    public UInt32 uInt32() {
        return UInt32.translate.fromParser(parser);
    }
    public UInt64 uInt64() {
        return UInt64.translate.fromParser(parser);
    }
    public Hash128 hash128() {
        return Hash128.translate.fromParser(parser);
    }
    public Hash160 hash160() {
        return Hash160.translate.fromParser(parser);
    }
    public Currency currency() {
        return Currency.translate.fromParser(parser);
    }
    public Hash256 hash256() {
        return Hash256.translate.fromParser(parser);
    }
    public Vector256 vector256() {
        return Vector256.translate.fromParser(parser);
    }
    public AccountID accountID() {
        return AccountID.translate.fromParser(parser);
    }
    public VariableLength variableLength() {
        int hint = parser.readVLLength();
        return VariableLength.translate.fromParser(parser, hint);
    }
    public Amount amount() {
        return Amount.translate.fromParser(parser);
    }
    public PathSet pathSet() {
        return PathSet.translate.fromParser(parser);
    }

    public STObject stObject() {
        return STObject.translate.fromParser(parser);
    }
    public STObject vlStObject() {
        return STObject.translate.fromParser(parser, parser.readVLLength());
    }

    public HashPrefix hashPrefix() {
        byte[] read = parser.read(4);
        for (HashPrefix hashPrefix : HashPrefix.values()) {
            if (Arrays.equals(read, hashPrefix.bytes)) {
                return hashPrefix;
            }
        }
        return null;
    }

    public STArray stArray() {
        return STArray.translate.fromParser(parser);
    }
    public Date rippleDate() {
        return RippleDate.fromParser(parser);
    }

    public BinaryParser parser() {
        return parser;
    }
}
