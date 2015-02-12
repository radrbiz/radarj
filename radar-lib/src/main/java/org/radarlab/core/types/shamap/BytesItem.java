package org.radarlab.core.types.shamap;


import org.radarlab.core.hash.prefixes.Prefix;
import org.radarlab.core.serialized.BytesSink;

public class BytesItem extends ShaMapItem<byte[]> {
    private byte[] item;

    public BytesItem(byte[] item) {
        this.item = item;
    }

    @Override
    void toBytesSink(BytesSink sink) {
        sink.add(item);
    }

    @Override
    public ShaMapItem<byte[]> copy() {
        return this;
    }

    @Override
    public Prefix hashPrefix() {
        return new Prefix() {
            @Override
            public byte[] bytes() {
                return new byte[0];
            }
        };
    }
}
