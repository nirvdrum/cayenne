package org.objectstyle.cayenne.map;
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


import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;

import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.CayenneRuntimeException;


/** Utility class that does forward engineering of the database.
  * It can generate database schema using the data map. It is a 
  * counterpart of DbLoader class.
  *
  * @author Andrei Adamchik
 */
public class DbGenerator {

    private Connection con;
    private DbAdapter adapter;


    /** Creates and initializes new DbGenerator. */
    public DbGenerator(Connection con, DbAdapter adapter) {
        this.con = con;
        this.adapter = adapter;
    }


    public DbAdapter getAdapter() {
        return adapter;
    }


    public Connection getCon() {
        return con;
    }


    /** Returns a query that can be used to create database table
      * corresponding to <code>ent</code> parameter. */
    public String createTableQuery(DbEntity ent) {
        StringBuffer buf = new StringBuffer();
        buf.append("CREATE TABLE ")
        .append(ent.getName())
        .append(" (");

        // columns
        Iterator it = ent.getAttributeList().iterator();
        boolean first = true;
        while(it.hasNext()) {
            if(first)
                first = false;
            else
                buf.append(", ");

            DbAttribute at = (DbAttribute)it.next();
            String type = adapter.externalTypesForJdbcType(at.getType())[0];

            buf.append(at.getName())
            .append(' ')
            .append(type);

            int len = at.getMaxLength();
            if(len > 0)
                buf.append('(')
                .append(len)
                .append(')');

            if(at.isMandatory())
                buf.append(" NOT");

            buf.append(" NULL");
        }

        // primary key clause
        Iterator pkit = ent.getPrimaryKey().iterator();
        if(pkit.hasNext()) {
            if(first)
                first = false;
            else
                buf.append(", ");

            buf.append("PRIMARY KEY (");
            boolean firstPk = true;
            while(pkit.hasNext()) {
                if(firstPk)
                    firstPk = false;
                else
                    buf.append(", ");

                DbAttribute at = (DbAttribute)pkit.next();
                buf.append(at.getName());
            }
            buf.append(')');
        }
        buf.append(')');
        return buf.toString();
    }


    /** Returns an array of queries to create foreign key constraints
     * for a particular DbEntity. Throws CayenneRuntimeException, if called
     * for adapter that does not support FK constraints. */
    public List createFkConstraintsQueries(DbEntity dbEnt) {
        if(!adapter.supportsFkConstraints())
            throw new CayenneRuntimeException("FK constraints are not supported by adapter.");
        ArrayList list = new ArrayList();
        Iterator it = dbEnt.getRelationshipList().iterator();
        while(it.hasNext()) {
            DbRelationship rel = (DbRelationship)it.next();
            if(!rel.isToMany() && !rel.isToDependentPK())
                list.add(adapter.createFkConstraint(rel));
        }
        return list;
    }

}
