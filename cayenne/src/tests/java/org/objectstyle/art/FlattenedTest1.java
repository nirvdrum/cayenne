package org.objectstyle.art;

import java.util.List;

public class FlattenedTest1 extends org.objectstyle.cayenne.CayenneDataObject {

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void addToFt3Array(FlattenedTest3 obj) {
        addToManyTarget("ft3Array", obj, true);
    }
    public void removeFromFt3Array(FlattenedTest3 obj) {
        removeToManyTarget("ft3Array", obj, true);
    }
    public List getFt3Array() {
        return (List)readProperty("ft3Array");
    }
    
    
}



