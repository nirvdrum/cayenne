package org.objectstyle.art;


public class MeaningfulPKDep extends org.objectstyle.cayenne.CayenneDataObject {

    public void setDescr(String descr) {
        writeProperty("descr", descr);
    }
    public String getDescr() {
        return (String)readProperty("descr");
    }
    
    
    public void setToMeaningfulPK(MeaningfulPKTest1 toMeaningfulPK) {
        setToOneTarget("toMeaningfulPK", toMeaningfulPK, true);
    }
    
    public MeaningfulPKTest1 getToMeaningfulPK() {
        return (MeaningfulPKTest1)readProperty("toMeaningfulPK");
    } 
    
    
}



