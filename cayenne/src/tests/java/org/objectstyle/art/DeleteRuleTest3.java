package org.objectstyle.art;


public class DeleteRuleTest3 extends org.objectstyle.cayenne.CayenneDataObject {

    public void setToDeleteRuleTest1(DeleteRuleTest1 toDeleteRuleTest1) {
        setToOneTarget("toDeleteRuleTest1", toDeleteRuleTest1, true);
    }
    
    public DeleteRuleTest1 getToDeleteRuleTest1() {
        return (DeleteRuleTest1)readProperty("toDeleteRuleTest1");
    } 
    
    
}



