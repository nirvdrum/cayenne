package org.objectstyle.art;

public class FlattenedTest3 extends org.objectstyle.cayenne.CayenneDataObject {

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void setToFT1(FlattenedTest1 toFT1) {
        setToOneTarget("toFT1", toFT1, true);
    }
    public FlattenedTest1 getToFT1() {
        return (FlattenedTest1)readProperty("toFT1");
    } 
    
    
    public void setToFT2(FlattenedTest2 toFT2) {
        setToOneTarget("toFT2", toFT2, true);
    }
    public FlattenedTest2 getToFT2() {
        return (FlattenedTest2)readProperty("toFT2");
    } 
    
    
}



