package org.objectstyle.art;

import java.util.List;
import org.objectstyle.cayenne.*;

public class CompoundPkTest extends org.objectstyle.cayenne.CayenneDataObject {

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



