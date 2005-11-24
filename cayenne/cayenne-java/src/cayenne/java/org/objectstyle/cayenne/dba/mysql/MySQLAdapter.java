/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.dba.mysql;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.types.ByteArrayType;
import org.objectstyle.cayenne.access.types.CharType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLAction;

/**
 * DbAdapter implementation for <a href="http://www.mysql.com">MySQL RDBMS</a>. Sample <a
 * target="_top" href="../../../../../../../developerguide/unit-tests.html">connection
 * settings</a> to use with MySQL are shown below:
 * 
 * <pre>
 *                    test-mysql.cayenne.adapter = org.objectstyle.cayenne.dba.mysql.MySQLAdapter
 *                    test-mysql.jdbc.username = test
 *                    test-mysql.jdbc.password = secret
 *                    test-mysql.jdbc.url = jdbc:mysql://serverhostname/cayenne
 *                    test-mysql.jdbc.driver = com.mysql.jdbc.Driver
 * </pre>
 * 
 * @author Andrei Adamchik
 */
public class MySQLAdapter extends JdbcAdapter {

    public MySQLAdapter() {
        // init defaults
        this.setSupportsFkConstraints(false);
        this.setSupportsUniqueConstraints(true);
        this.setSupportsGeneratedKeys(true);
    }

    /**
     * Uses special action builder to create the right action.
     * 
     * @since 1.2
     */
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new MySQLActionBuilder(this, node
                .getEntityResolver()));
    }

    public String dropTable(DbEntity entity) {
        return "DROP TABLE IF EXISTS " + entity.getFullyQualifiedName();
    }

    /**
     * Installs appropriate ExtendedTypes used as converters for passing values between
     * JDBC and Java layers.
     */
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // must handle CLOBs as strings, otherwise there
        // are problems with NULL clobs that are treated
        // as empty strings... somehow this doesn't happen
        // for BLOBs (ConnectorJ v. 3.0.9)
        map.registerType(new CharType(false, false));
        map.registerType(new ByteArrayType(false, false));
    }

    public DbAttribute buildAttribute(
            String name,
            String typeName,
            int type,
            int size,
            int precision,
            boolean allowNulls) {

        // all LOB types are returned by the driver as OTHER... must remap them manually
        // (at least on MySQL 3.23)
        if (type == Types.OTHER) {
            if ("longblob".equalsIgnoreCase(typeName)) {
                type = Types.BLOB;
            }
            else if ("mediumblob".equalsIgnoreCase(typeName)) {
                type = Types.BLOB;
            }
            else if ("blob".equalsIgnoreCase(typeName)) {
                type = Types.BLOB;
            }
            else if ("tinyblob".equalsIgnoreCase(typeName)) {
                type = Types.VARBINARY;
            }
            else if ("longtext".equalsIgnoreCase(typeName)) {
                type = Types.CLOB;
            }
            else if ("mediumtext".equalsIgnoreCase(typeName)) {
                type = Types.CLOB;
            }
            else if ("text".equalsIgnoreCase(typeName)) {
                type = Types.CLOB;
            }
            else if ("tinytext".equalsIgnoreCase(typeName)) {
                type = Types.VARCHAR;
            }
        }

        return super.buildAttribute(name, typeName, type, size, precision, allowNulls);
    }

    /** Throws an exception, since FK constraints are not supported by MySQL. */
    public String createFkConstraint(DbRelationship rel) {
        throw new CayenneRuntimeException("FK constraints are not supported.");
    }

    /**
     * Returns null, since views are not yet supported in MySQL. Views are available on
     * newer versions of MySQL.
     */
    public String tableTypeForView() {
        return null;
    }

    /**
     * Creates and returns a primary key generator. Overrides superclass implementation to
     * return an instance of MySQLPkGenerator that does the correct table locking.
     */
    protected PkGenerator createPkGenerator() {
        return new MySQLPkGenerator();
    }

    /**
     * Customizes table creation procedure to put generated columns first in the PK
     * definition, preventing a crash on InnoDB tables.
     * 
     * @since 1.2
     */
    // See CAY-358 for details of the problem
    protected void createTableAppendPKClause(StringBuffer sqlBuffer, DbEntity entity) {

        List pkList = new ArrayList(entity.getPrimaryKey());
        Collections.sort(pkList, new PKComparator());

        // must move generated to the front...

        Iterator pkit = pkList.iterator();
        if (pkit.hasNext()) {

            sqlBuffer.append(", PRIMARY KEY (");
            boolean firstPk = true;
            while (pkit.hasNext()) {
                if (firstPk)
                    firstPk = false;
                else
                    sqlBuffer.append(", ");

                DbAttribute at = (DbAttribute) pkit.next();
                sqlBuffer.append(at.getName());
            }
            sqlBuffer.append(')');
        }
    };

    protected void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        super.createTableAppendColumn(sqlBuffer, column);

        if (column.isGenerated()) {
            sqlBuffer.append(" AUTO_INCREMENT");
        }
    }

    final class PKComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            DbAttribute a1 = (DbAttribute) o1;
            DbAttribute a2 = (DbAttribute) o2;
            if (a1.isGenerated() != a2.isGenerated()) {
                return a1.isGenerated() ? -1 : 1;
            }
            else {
                return a1.getName().compareTo(a2.getName());
            }
        }
    }
}
