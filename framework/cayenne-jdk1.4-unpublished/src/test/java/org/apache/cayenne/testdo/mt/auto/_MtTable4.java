package org.apache.cayenne.testdo.mt.auto;

import java.util.List;

/** Class _MtTable4 was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _MtTable4 extends org.apache.cayenne.CayenneDataObject {

    public static final String TABLE5S_PROPERTY = "table5s";

    public static final String ID_PK_COLUMN = "ID";

    public void addToTable5s(org.apache.cayenne.testdo.mt.MtTable5 obj) {
        addToManyTarget("table5s", obj, true);
    }
    public void removeFromTable5s(org.apache.cayenne.testdo.mt.MtTable5 obj) {
        removeToManyTarget("table5s", obj, true);
    }
    public List getTable5s() {
        return (List)readProperty("table5s");
    }
    
    
}