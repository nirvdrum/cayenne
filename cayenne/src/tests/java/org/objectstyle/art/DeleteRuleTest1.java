package org.objectstyle.art;

import java.util.List;
import org.objectstyle.cayenne.*;

public class DeleteRuleTest1 extends org.objectstyle.cayenne.CayenneDataObject {

    public void setTest2(DeleteRuleTest2 test2) {
        setToOneTarget("test2", test2, true);
    }
    
    public DeleteRuleTest2 getTest2() {
        return (DeleteRuleTest2)readProperty("test2");
    } 
    
    
}



