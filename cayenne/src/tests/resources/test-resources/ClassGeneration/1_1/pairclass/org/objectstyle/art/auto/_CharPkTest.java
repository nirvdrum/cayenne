package org.objectstyle.art.auto;

import java.util.List;

/** Class _CharPkTest was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _CharPkTest extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String OTHER_COL_PROPERTY = "otherCol";
    public static final String PK_COL_PROPERTY = "pkCol";
    public static final String CHAR_FKS_PROPERTY = "charFKs";

    public static final String PK_COL_PK_COLUMN = "PK_COL";

    public void setOtherCol(String otherCol) {
        writeProperty("otherCol", otherCol);
    }
    public String getOtherCol() {
        return (String)readProperty("otherCol");
    }
    
    
    public void setPkCol(String pkCol) {
        writeProperty("pkCol", pkCol);
    }
    public String getPkCol() {
        return (String)readProperty("pkCol");
    }
    
    
    public void addToCharFKs(org.objectstyle.art.CharFkTest obj) {
        addToManyTarget("charFKs", obj, true);
    }
    public void removeFromCharFKs(org.objectstyle.art.CharFkTest obj) {
        removeToManyTarget("charFKs", obj, true);
    }
    public List getCharFKs() {
        return (List)readProperty("charFKs");
    }
    
    
}