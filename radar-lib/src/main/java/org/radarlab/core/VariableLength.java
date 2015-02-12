
package org.radarlab.core;

import org.radarlab.core.fields.Field;
import org.radarlab.core.fields.TypedFields;
import org.radarlab.core.serialized.BinaryParser;
import org.radarlab.core.serialized.BytesSink;
import org.radarlab.core.serialized.SerializedType;
import org.radarlab.core.serialized.TypeTranslator;
import org.ripple.bouncycastle.util.encoders.Hex;

public class VariableLength implements SerializedType {
    public VariableLength(byte[] bytes) {
        buffer = bytes;
    }

    byte[] buffer;

    @Override
    public Object toJSON() {
        return translate.toJSON(this);
    }

    @Override
    public byte[] toBytes() {
        return buffer;
    }

    @Override
    public String toHex() {
        return translate.toHex(this);
    }

    @Override
    public void toBytesSink(BytesSink to) {
        translate.toBytesSink(this, to);
    }

    public static VariableLength fromBytes(byte[] bytes) {
        return new VariableLength(bytes);
    }

    public static class Translator extends TypeTranslator<VariableLength> {
        @Override
        public VariableLength fromParser(BinaryParser parser, Integer hint) {
            if (hint == null) {
                hint = parser.size() - parser.pos();
            }
            return new VariableLength(parser.read(hint));
        }

        @Override
        public Object toJSON(VariableLength obj) {
            return toString(obj);
        }

        @Override
        public String toString(VariableLength obj) {
            return Hex.toHexString(obj.buffer);
        }

        @Override
        public VariableLength fromString(String value) {
            return new VariableLength(Hex.decode(value));
        }

        @Override
        public void toBytesSink(VariableLength obj, BytesSink to) {
            to.add(obj.buffer);
        }
    }

    static public Translator translate = new Translator();

    public static TypedFields.VariableLengthField variablelengthField(final Field f) {
        return new TypedFields.VariableLengthField() {
            @Override
            public Field getField() {
                return f;
            }
        };
    }

    static public TypedFields.VariableLengthField PublicKey = variablelengthField(Field.PublicKey);
    static public TypedFields.VariableLengthField MessageKey = variablelengthField(Field.MessageKey);
    static public TypedFields.VariableLengthField SigningPubKey = variablelengthField(Field.SigningPubKey);
    static public TypedFields.VariableLengthField TxnSignature = variablelengthField(Field.TxnSignature);
    static public TypedFields.VariableLengthField Generator = variablelengthField(Field.Generator);
    static public TypedFields.VariableLengthField Signature = variablelengthField(Field.Signature);
    static public TypedFields.VariableLengthField Domain = variablelengthField(Field.Domain);
    static public TypedFields.VariableLengthField FundCode = variablelengthField(Field.FundCode);
    static public TypedFields.VariableLengthField RemoveCode = variablelengthField(Field.RemoveCode);
    static public TypedFields.VariableLengthField ExpireCode = variablelengthField(Field.ExpireCode);
    static public TypedFields.VariableLengthField CreateCode = variablelengthField(Field.CreateCode);

    static public TypedFields.VariableLengthField MemoType = variablelengthField(Field.MemoType);
    static public TypedFields.VariableLengthField MemoData = variablelengthField(Field.MemoData);
    static public TypedFields.VariableLengthField MemoFormat = variablelengthField(Field.MemoFormat);
}
