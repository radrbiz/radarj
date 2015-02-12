package org.radarlab.core.types.shamap;


import org.radarlab.core.hash.prefixes.Prefix;
import org.radarlab.core.serialized.BytesSink;

abstract public class ShaMapItem<T> {
    abstract void toBytesSink(BytesSink sink);
    public abstract ShaMapItem<T> copy();
    public abstract Prefix hashPrefix();
}
