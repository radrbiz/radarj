package org.radarlab.core;


import org.radarlab.core.fields.Field;
import org.radarlab.core.uint.UInt32;

public class Entry extends STObject{

    public void Amount(Amount val){
        super.put(Field.Amount,val);
    }

    public void LimitAmount(Amount val){
        super.put(Field.LimitAmount,val);
    }

    public void Flags(UInt32 val){
        super.put(Field.Flags,val);
    }
}
