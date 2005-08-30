package org.objectstyle.cayenne.testdo.mt.auto;

import org.objectstyle.cayenne.PersistentObject;
import org.objectstyle.cayenne.ValueHolder;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1;

/**
 * A generated persistent class mapped as "MtTable2" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public class _ClientMtTable2 extends PersistentObject {

    public static final String GLOBAL_ATTRIBUTE_PROPERTY = "globalAttribute";
    public static final String TABLE1_PROPERTY = "table1";

    protected String globalAttribute;
    protected ValueHolder table1;

    public String getGlobalAttribute() {
        beforePropertyRead("globalAttribute");
        return globalAttribute;
    }
    public void setGlobalAttribute(String globalAttribute) {
        beforePropertyWritten("globalAttribute", globalAttribute);
        this.globalAttribute = globalAttribute;
    }
    
    
    public ClientMtTable1 getTable1() {
        beforePropertyRead("table1");
        return (ClientMtTable1) table1.getValue(ClientMtTable1.class);
    }
    public void setTable1(ClientMtTable1 table1) {
        beforePropertyWritten("table1", table1);
        this.table1.setValue(ClientMtTable1.class, table1);
    }
    
}
