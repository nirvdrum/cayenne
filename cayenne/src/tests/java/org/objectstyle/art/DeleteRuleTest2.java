package org.objectstyle.art;

import java.util.List;

public class DeleteRuleTest2 extends org.objectstyle.cayenne.CayenneDataObject {

    public void addToTest1Array(DeleteRuleTest1 obj) {
        addToManyTarget("test1Array", obj, true);
    }
    public void removeFromTest1Array(DeleteRuleTest1 obj) {
        removeToManyTarget("test1Array", obj, true);
    }
    public List getTest1Array() {
        return (List)readProperty("test1Array");
    }
    
    
}



