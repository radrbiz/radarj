package org.radarlab.core.binary;


import org.radarlab.core.serialized.BinarySerializer;
import org.radarlab.core.serialized.BytesSink;
import org.radarlab.core.serialized.SerializedType;

public class STWriter {
    BytesSink sink;
    BinarySerializer serializer;
    public STWriter(BytesSink bytesSink) {
        serializer = new BinarySerializer(bytesSink);
        sink = bytesSink;
    }
    public void write(SerializedType obj) {
        obj.toBytesSink(sink);
    }
    public void writeVl(SerializedType obj) {
        serializer.addLengthEncoded(obj);
    }
}
