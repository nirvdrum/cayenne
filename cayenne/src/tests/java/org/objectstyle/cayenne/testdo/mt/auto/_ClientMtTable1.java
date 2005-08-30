package org.objectstyle.cayenne.testdo.mt.auto;

import java.util.List;

import org.objectstyle.cayenne.PersistentObject;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable2;

/**
 * A generated persistent class mapped as "MtTable1" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public class _ClientMtTable1 extends PersistentObject {

    public static final String GLOBAL_ATTRIBUTE1_PROPERTY = "globalAttribute1";
    public static final String SERVER_ATTRIBUTE1_PROPERTY = "serverAttribute1";
    public static final String TABLE2ARRAY_PROPERTY = "table2Array";

    protected String globalAttribute1;
    protected String serverAttribute1;
    protected List table2Array;

    public String getGlobalAttribute1() {
        beforePropertyRead("globalAttribute1");
        return globalAttribute1;
    }
    public void setGlobalAttribute1(String globalAttribute1) {
        beforePropertyWritten("globalAttribute1", globalAttribute1);
        this.globalAttribute1 = globalAttribute1;
    }
    
    
    public String getServerAttribute1() {
        beforePropertyRead("serverAttribute1");
        return serverAttribute1;
    }
    public void setServerAttribute1(String serverAttribute1) {
        beforePropertyWritten("serverAttribute1", serverAttribute1);
        this.serverAttribute1 = serverAttribute1;
    }
    
    
    public List getTable2Array() {
        beforePropertyRead("table2Array");
        return table2Array;
    }
    public void addToTable2Array(ClientMtTable2 object) {
        beforePropertyWritten("table2Array", object);
        this.table2Array.add(object);
    }
    public void removeFromTable2Array(ClientMtTable2 object) {
        beforePropertyWritten("table2Array", object);
        this.table2Array.remove(object);
    }
    
}
