package org.objectstyle.art.auto;

import java.util.List;

import org.objectstyle.art.DeleteRuleTest1;
import org.objectstyle.art.DeleteRuleTest3;
import org.objectstyle.cayenne.CayenneDataObject;

/**
 * @author Andrei Adamchik
 */
public class _DeleteRuleTest2 extends CayenneDataObject {

    public void addToDeleteRuleTest3Array(DeleteRuleTest3 obj) {
        addToManyTarget("deleteRuleTest3Array", obj, true);
    }
    public void removeFromDeleteRuleTest3Array(DeleteRuleTest3 obj) {
        removeToManyTarget("deleteRuleTest3Array", obj, true);
    }
    public List getDeleteRuleTest3Array() {
        return (List) readProperty("deleteRuleTest3Array");
    }

    public void addToTest1Array(DeleteRuleTest1 obj) {
        addToManyTarget("test1Array", obj, true);
    }
    public void removeFromTest1Array(DeleteRuleTest1 obj) {
        removeToManyTarget("test1Array", obj, true);
    }
    public List getTest1Array() {
        return (List) readProperty("test1Array");
    }
}
