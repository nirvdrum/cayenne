package org.objectstyle.art;


public class ClobTest extends org.objectstyle.cayenne.CayenneDataObject {

    public void setClobCol(String clobCol) {
        writeProperty("clobCol", clobCol);
    }
    public String getClobCol() {
        return (String)readProperty("clobCol");
    }
    
    
}



