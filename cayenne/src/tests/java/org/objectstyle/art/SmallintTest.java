package org.objectstyle.art;

public class SmallintTest extends org.objectstyle.cayenne.CayenneDataObject {

    public void setSmallintCol(Short smallintCol) {
        writeProperty("smallintCol", smallintCol);
    }
    public Short getSmallintCol() {
        return (Short)readProperty("smallintCol");
    }
    
    
}



