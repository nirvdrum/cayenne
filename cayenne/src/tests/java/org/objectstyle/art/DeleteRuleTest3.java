package org.objectstyle.art;


public class DeleteRuleTest3 extends org.objectstyle.cayenne.CayenneDataObject {

    public void setToDeleteRuleTest2(DeleteRuleTest2 toDeleteRuleTest2) {
        setToOneTarget("toDeleteRuleTest2", toDeleteRuleTest2, true);
    }
    
    public DeleteRuleTest2 getToDeleteRuleTest2() {
        return (DeleteRuleTest2)readProperty("toDeleteRuleTest2");
    } 
    
    
}



