package org.objectstyle.art;

import java.util.List;

public class BinaryPKTest1 extends org.objectstyle.cayenne.CayenneDataObject {

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void addToBinaryPKDetails(BinaryPKTest2 obj) {
        addToManyTarget("binaryPKDetails", obj, true);
    }
    public void removeFromBinaryPKDetails(BinaryPKTest2 obj) {
        removeToManyTarget("binaryPKDetails", obj, true);
    }
    public List getBinaryPKDetails() {
        return (List)readProperty("binaryPKDetails");
    }
    
    
}



