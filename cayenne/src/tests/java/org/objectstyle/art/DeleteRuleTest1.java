package org.objectstyle.art;

import java.util.List;

public class DeleteRuleTest1 extends org.objectstyle.cayenne.CayenneDataObject {

    public void addToDeleteRuleTest3Array(DeleteRuleTest3 obj) {
        addToManyTarget("deleteRuleTest3Array", obj, true);
    }
    public void removeFromDeleteRuleTest3Array(DeleteRuleTest3 obj) {
        removeToManyTarget("deleteRuleTest3Array", obj, true);
    }
    public List getDeleteRuleTest3Array() {
        return (List)readProperty("deleteRuleTest3Array");
    }
    
    
    public void setTest2(DeleteRuleTest2 test2) {
        setToOneTarget("test2", test2, true);
    }
    
    public DeleteRuleTest2 getTest2() {
        return (DeleteRuleTest2)readProperty("test2");
    } 
    
    
}



