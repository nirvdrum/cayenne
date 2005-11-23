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
package org.objectstyle.cayenne.dba.openbase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.dba.JdbcPkGenerator;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DerivedDbEntity;

/**
 * @author <a href="mailto:mkienenb@alaska.net">Mike Kienenberger</a>
 * @author Andrei Adamchik
 * @since 1.1
 */
public class OpenBasePkGenerator extends JdbcPkGenerator {

    /**
     * @deprecated Since 1.2 corresponding interface method is unused and deprecated.
     */
    public String generatePkForDbEntityString(DbEntity ent) {
        return newIDString(ent);
    }

    /**
     * Returns a non-repeating primary key for a given entity. Since OpenBase-specific
     * mechanism is used, key caching is disabled. Instead a database operation is
     * performed on every call.
     */
    public Object generatePkForDbEntity(DataNode node, DbEntity entity) throws Exception {
        // check for binary pk
        Object binPK = binaryPK(entity);
        if (binPK != null) {
            return binPK;
        }
        return new Integer(pkFromDatabase(node, entity));
    }

    /**
     * Generates new (unique and non-repeating) primary key for specified DbEntity.
     * Executed SQL looks like this:
     * 
     * <pre>
     * NEWID FOR Table Column
     * </pre>
     * 
     * COLUMN must be marked as UNIQUE in order for this to work properly.
     */
    protected int pkFromDatabase(DataNode node, DbEntity entity) throws Exception {
        String sql = newIDString(entity);
        QueryLogger.logQuery(sql, Collections.EMPTY_LIST);

        Connection con = node.getDataSource().getConnection();
        try {
            Statement st = con.createStatement();
            try {

                ResultSet rs = st.executeQuery(sql);
                try {
                    // Object pk = null;
                    if (!rs.next()) {
                        throw new CayenneRuntimeException(
                                "Error generating pk for DbEntity " + entity.getName());
                    }
                    return rs.getInt(1);
                }
                finally {
                    rs.close();
                }
            }
            finally {
                st.close();
            }
        }
        finally {
            con.close();
        }
    }

    /**
     * Returns SQL string that can generate new (unique and non-repeating) primary key for
     * specified DbEntity. No actual database operations are performed.
     * 
     * @since 1.2
     */
    protected String newIDString(DbEntity ent) {
        if ((null == ent.getPrimaryKey()) || (1 != ent.getPrimaryKey().size())) {
            throw new CayenneRuntimeException("Error generating pk for DbEntity "
                    + ent.getName()
                    + ": pk must be single attribute");
        }
        DbAttribute primaryKeyAttribute = (DbAttribute) ent.getPrimaryKey().get(0);

        StringBuffer buf = new StringBuffer("NEWID FOR ");
        buf.append(ent.getName()).append(' ').append(primaryKeyAttribute.getName());
        return buf.toString();
    }

    public void createAutoPk(DataNode node, List dbEntities) throws Exception {
        // looks like generating a PK on top of an existing one does not
        // result in errors...

        // create needed sequences
        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity entity = (DbEntity) it.next();

            // the caller must take care of giving us the right entities
            // but lets check anyway
            if (!canCreatePK(entity)) {
                continue;
            }

            runUpdate(node, createPKString(entity));
            runUpdate(node, createUniquePKIndexString(entity));
        }
    }

    /**
     * 
     */
    public List createAutoPkStatements(List dbEntities) {
        List list = new ArrayList(2 * dbEntities.size());
        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity entity = (DbEntity) it.next();

            // the caller must take care of giving us the right entities
            // but lets check anyway
            if (!canCreatePK(entity)) {
                continue;
            }

            list.add(createPKString(entity));
            list.add(createUniquePKIndexString(entity));
        }

        return list;
    }

    protected boolean canCreatePK(DbEntity entity) {
        if (entity instanceof DerivedDbEntity) {
            return false;
        }

        List pk = entity.getPrimaryKey();
        if (pk == null || pk.size() == 0) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public void dropAutoPk(DataNode node, List dbEntities) throws Exception {
        // there is no simple way to do that... probably requires
        // editing metadata tables...
        // Good thing is that it doesn't matter, since PK support
        // is attached to the table itself, so if a table is dropped,
        // it will be dropped as well
    }

    /**
     * Returns an empty list, since OpenBase doesn't support this operation.
     */
    public List dropAutoPkStatements(List dbEntities) {
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns a String to create PK support for an entity.
     */
    protected String createPKString(DbEntity entity) {
        List pk = entity.getPrimaryKey();

        if (pk == null || pk.size() == 0) {
            throw new CayenneRuntimeException("Entity '"
                    + entity.getName()
                    + "' has no PK defined.");
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append("CREATE PRIMARY KEY ").append(entity.getName()).append(" (");

        Iterator it = pk.iterator();

        // at this point we know that there is at least on PK column
        DbAttribute firstColumn = (DbAttribute) it.next();
        buffer.append(firstColumn.getName());

        while (it.hasNext()) {
            DbAttribute column = (DbAttribute) it.next();
            buffer.append(", ").append(column.getName());
        }

        buffer.append(")");
        return buffer.toString();
    }

    /**
     * Returns a String to create a unique index on table primary key columns per OpenBase
     * recommendations.
     */
    protected String createUniquePKIndexString(DbEntity entity) {
        List pk = entity.getPrimaryKey();

        if (pk == null || pk.size() == 0) {
            throw new CayenneRuntimeException("Entity '"
                    + entity.getName()
                    + "' has no PK defined.");
        }

        StringBuffer buffer = new StringBuffer();

        // compound PK doesn't work well with UNIQUE index...
        // create a regular one in this case
        buffer.append(pk.size() == 1 ? "CREATE UNIQUE INDEX " : "CREATE INDEX ").append(
                entity.getName()).append(" (");

        Iterator it = pk.iterator();

        // at this point we know that there is at least on PK column
        DbAttribute firstColumn = (DbAttribute) it.next();
        buffer.append(firstColumn.getName());

        while (it.hasNext()) {
            DbAttribute column = (DbAttribute) it.next();
            buffer.append(", ").append(column.getName());
        }
        buffer.append(")");
        return buffer.toString();
    }

    public void reset() {
        // noop
    }

    /**
     * Returns zero, since PK caching is not feasible with OpenBase PK generation
     * mechanism.
     */
    public int getPkCacheSize() {
        return 0;
    }

    public void setPkCacheSize(int pkCacheSize) {
        // noop, no PK caching
    }

}