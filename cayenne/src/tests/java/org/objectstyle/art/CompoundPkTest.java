package org.objectstyle.art;

import java.util.List;

public class CompoundPkTest extends org.objectstyle.cayenne.CayenneDataObject {

    public void setKey1(String key1) {
        writeProperty("key1", key1);
    }
    public String getKey1() {
        return (String)readProperty("key1");
    }
    
    
    public void setKey2(String key2) {
        writeProperty("key2", key2);
    }
    public String getKey2() {
        return (String)readProperty("key2");
    }
    
    
    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void addToCompoundFkArray(CompoundFkTest obj) {
        addToManyTarget("compoundFkArray", obj, true);
    }
    public void removeFromCompoundFkArray(CompoundFkTest obj) {
        removeToManyTarget("compoundFkArray", obj, true);
    }
    public List getCompoundFkArray() {
        return (List)readProperty("compoundFkArray");
    }
    
    
}



