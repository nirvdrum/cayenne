package org.objectstyle.art;

public class TinyintTest extends org.objectstyle.cayenne.CayenneDataObject {

    public void setTinyintCol(Byte tinyintCol) {
        writeProperty("tinyintCol", tinyintCol);
    }
    public Byte getTinyintCol() {
        return (Byte)readProperty("tinyintCol");
    }
    
    
}



