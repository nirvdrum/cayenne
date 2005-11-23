package org.objectstyle.cayenne.testdo.relationship.auto;

/** Class _ClobDetail was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _ClobDetail extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String MASTER_PROPERTY = "master";

    public static final String CLOB_DETAIL_ID_PK_COLUMN = "CLOB_DETAIL_ID";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void setMaster(org.objectstyle.cayenne.testdo.relationship.ClobMaster master) {
        setToOneTarget("master", master, true);
    }

    public org.objectstyle.cayenne.testdo.relationship.ClobMaster getMaster() {
        return (org.objectstyle.cayenne.testdo.relationship.ClobMaster)readProperty("master");
    } 
    
    
}