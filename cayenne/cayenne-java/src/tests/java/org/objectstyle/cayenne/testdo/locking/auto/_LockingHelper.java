package org.objectstyle.cayenne.testdo.locking.auto;

/** Class _LockingHelper was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _LockingHelper extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String TO_REL_LOCKING_TEST_PROPERTY = "toRelLockingTest";

    public static final String LOCKING_HELPER_ID_PK_COLUMN = "LOCKING_HELPER_ID";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void setToRelLockingTest(org.objectstyle.cayenne.testdo.locking.RelLockingTest toRelLockingTest) {
        setToOneTarget("toRelLockingTest", toRelLockingTest, true);
    }

    public org.objectstyle.cayenne.testdo.locking.RelLockingTest getToRelLockingTest() {
        return (org.objectstyle.cayenne.testdo.locking.RelLockingTest)readProperty("toRelLockingTest");
    } 
    
    
}