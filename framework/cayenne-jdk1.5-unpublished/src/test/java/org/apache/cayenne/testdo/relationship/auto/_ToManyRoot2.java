package org.apache.cayenne.testdo.relationship.auto;

import java.util.List;

/** Class _ToManyRoot2 was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _ToManyRoot2 extends org.apache.cayenne.CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String DEPS_PROPERTY = "deps";

    public static final String ID_PK_COLUMN = "ID";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void addToDeps(org.apache.cayenne.testdo.relationship.ToManyFkDep obj) {
        addToManyTarget("deps", obj, true);
    }
    public void removeFromDeps(org.apache.cayenne.testdo.relationship.ToManyFkDep obj) {
        removeToManyTarget("deps", obj, true);
    }
    public List getDeps() {
        return (List)readProperty("deps");
    }
    
    
}
