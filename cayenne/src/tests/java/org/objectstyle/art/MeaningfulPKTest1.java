package org.objectstyle.art;

import java.util.List;

public class MeaningfulPKTest1 extends org.objectstyle.cayenne.CayenneDataObject {

    public void setDescr(String descr) {
        writeProperty("descr", descr);
    }
    public String getDescr() {
        return (String)readProperty("descr");
    }
    
    
    public void setPkAttribute(Integer pkAttribute) {
        writeProperty("pkAttribute", pkAttribute);
    }
    public Integer getPkAttribute() {
        return (Integer)readProperty("pkAttribute");
    }
    
    
    public void addToMeaningfulPKDepArray(MeaningfulPKDep obj) {
        addToManyTarget("meaningfulPKDepArray", obj, true);
    }
    public void removeFromMeaningfulPKDepArray(MeaningfulPKDep obj) {
        removeToManyTarget("meaningfulPKDepArray", obj, true);
    }
    public List getMeaningfulPKDepArray() {
        return (List)readProperty("meaningfulPKDepArray");
    }
    
    
}



