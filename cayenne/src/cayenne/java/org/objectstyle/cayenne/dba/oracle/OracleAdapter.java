/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002 The ObjectStyle Group
 * and individual authors of the software.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne"
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */

package org.objectstyle.cayenne.dba.oracle;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.BatchInterpreter;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationSorter;
import org.objectstyle.cayenne.access.trans.DeleteBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.InsertBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.UpdateBatchQueryBuilder;
import org.objectstyle.cayenne.access.types.CharType;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;

/** 
 * DbAdapter implementation for <a href="http://www.oracle.com">Oracle
 * RDBMS</a>. Sample <a target="_top" 
 * href="../../../../../../../developer.html#unit">connection 
 * settings</a> to use with Oracle adapter are shown below:
 * 
<pre>
test-oracle.cayenne.adapter = org.objectstyle.cayenne.dba.oracle.OracleAdapter
test-oracle.jdbc.username = test
test-oracle.jdbc.password = secret
test-oracle.jdbc.url = jdbc:oracle:thin:@192.168.0.20:1521:ora1 
test-oracle.jdbc.driver = oracle.jdbc.driver.OracleDriver
</pre>
 */
public class OracleAdapter extends JdbcAdapter {
    private static Logger logObj = Logger.getLogger(OracleAdapter.class);

    public static final String ORACLE_FLOAT = "FLOAT";
    public static final String ORACLE_BLOB = "BLOB";
    public static final String ORACLE_CLOB = "CLOB";

    protected Map sorters = new HashMap();

    public OracleAdapter() {
        super();
        extendedTypes.registerType(new CharType(true));
        qualifierFactory.setTranslatorClass(
            org
                .objectstyle
                .cayenne
                .dba
                .oracle
                .OracleQualifierTranslator
                .class
                .getName());
    }

    /**
     * Creates and returns a primary key generator.
     * Overrides superclass implementation to return an
     * instance of OraclePkGenerator.
     */
    protected PkGenerator createPkGenerator() {
        return new OraclePkGenerator();
    }

    public OperationSorter getOpSorter(DataNode node) {
        synchronized (sorters) {
            OperationSorter sorter = (OperationSorter) sorters.get(node);
            if (sorter == null) {
                sorter = new OperationSorter(node, node.getDataMapsAsList());
                sorters.put(node, sorter);
            }
            return sorter;
        }
    }

    /**
     * Returns a query string to drop a table corresponding
     * to <code>ent</code> DbEntity. Changes superclass behavior
     * to drop all related foreign key constraints.
     */
    public String dropTable(DbEntity ent) {
        return "DROP TABLE "
            + ent.getFullyQualifiedName()
            + " CASCADE CONSTRAINTS";
    }

    /**
     * Fixes some reverse engineering problems. Namely if a columns
     * is created as DECIMAL and has non-positive precision it is
     * converted to INTEGER.
     */
    public DbAttribute buildAttribute(
        String name,
        String typeName,
        int type,
        int size,
        int precision,
        boolean allowNulls) {

        DbAttribute attr =
            super.buildAttribute(
                name,
                typeName,
                type,
                size,
                precision,
                allowNulls);

        if (type == Types.DECIMAL && precision <= 0) {
            attr.setType(Types.INTEGER);
            attr.setPrecision(-1);
        } else if (type == Types.OTHER) {
            // in this case we need to guess the attribute type 
            // based on its string value
            if (ORACLE_FLOAT.equals(typeName)) {
                attr.setType(Types.FLOAT);
            } else if (ORACLE_BLOB.equals(typeName)) {
                attr.setType(Types.BLOB);
            } else if (ORACLE_CLOB.equals(typeName)) {
                attr.setType(Types.CLOB);
            }
        }

        return attr;
    }

    /** Returns Oracle-specific classes for SELECT queries. */
    protected Class queryTranslatorClass(Query q) {
        if (q instanceof SelectQuery) {
            return OracleSelectTranslator.class;
        } else {
            return super.queryTranslatorClass(q);
        }
    }

    public BatchInterpreter getInsertBatchInterpreter() {
        if (insertBatchInterpreter == null) {
            insertBatchInterpreter = new OracleBatchInterpreter();
            insertBatchInterpreter.setAdapter(this);
            insertBatchInterpreter.setQueryBuilder(
                new InsertBatchQueryBuilder(this));
        }
        return insertBatchInterpreter;
    }

    public BatchInterpreter getDeleteBatchInterpreter() {
        if (deleteBatchInterpreter == null) {
            deleteBatchInterpreter = new OracleBatchInterpreter();
            deleteBatchInterpreter.setAdapter(this);
            deleteBatchInterpreter.setQueryBuilder(
                new DeleteBatchQueryBuilder(this));
        }
        return deleteBatchInterpreter;
    }

    public BatchInterpreter getUpdateBatchInterpreter() {
        if (updateBatchInterpreter == null) {
            updateBatchInterpreter = new OracleBatchInterpreter();
            updateBatchInterpreter.setAdapter(this);
            updateBatchInterpreter.setQueryBuilder(
                new UpdateBatchQueryBuilder(this));
        }
        return updateBatchInterpreter;
    }
}