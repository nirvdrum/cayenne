package org.objectstyle.art;


public class CompoundFkTest extends org.objectstyle.cayenne.CayenneDataObject {

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void setToCompoundPk(CompoundPkTest toCompoundPk) {
        setToOneTarget("toCompoundPk", toCompoundPk, true);
    }
    
    public CompoundPkTest getToCompoundPk() {
        return (CompoundPkTest)readProperty("toCompoundPk");
    } 
    
    
}



