package org.objectstyle.art;

public class CharPkTest extends org.objectstyle.cayenne.CayenneDataObject {

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
    
    
}



