package org.radarlab.core;

import org.radarlab.core.fields.Field;

/**
 * Created by ac
 * since 15/5/14.
 */
public class ReleaseSchedule extends STArray {
    public void add(ReleasePoint val){
        STObject st = new STObject();
        st.put(Field.ReleasePoint, val);
        super.add(st);
    }
}
