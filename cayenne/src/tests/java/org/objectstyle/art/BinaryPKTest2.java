package org.objectstyle.art;

public class BinaryPKTest2 extends org.objectstyle.cayenne.CayenneDataObject {

    public void setDetailName(String detailName) {
        writeProperty("detailName", detailName);
    }
    public String getDetailName() {
        return (String)readProperty("detailName");
    }
    
    
    public void setToBinaryPKMaster(BinaryPKTest1 toBinaryPKMaster) {
        setToOneTarget("toBinaryPKMaster", toBinaryPKMaster, true);
    }
    public BinaryPKTest1 getToBinaryPKMaster() {
        return (BinaryPKTest1)readProperty("toBinaryPKMaster");
    } 
    
    
}



