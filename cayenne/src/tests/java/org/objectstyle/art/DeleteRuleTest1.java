package org.objectstyle.art;

import org.objectstyle.cayenne.CayenneDataObject;


public class DeleteRuleTest1 extends CayenneDataObject {

    public void setTest2(DeleteRuleTest2 test2) {
        setToOneTarget("test2", test2, true);
    }
    
    public DeleteRuleTest2 getTest2() {
        return (DeleteRuleTest2)readProperty("test2");
    } 
    
    
}



