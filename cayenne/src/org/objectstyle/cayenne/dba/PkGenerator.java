package org.objectstyle.cayenne.dba;
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

import java.util.*;
import java.util.logging.Logger;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DefaultOperationObserver;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.query.*;


public class PkGenerator {
    static Logger logObj = Logger.getLogger(PkGenerator.class.getName());

    private static final ObjAttribute[] resultDesc = new ObjAttribute[] {
                new ObjAttribute("nextId", Integer.class.getName(), null)
            };


    class PkInsertProcessor extends DefaultOperationObserver {
        boolean successFlag;

        public void nextCount(Query query, int resultCount) {
            super.nextCount(query, resultCount);
            if(resultCount == 1)
                successFlag = true;
        }

        public void nextQueryException(Query query, Exception ex) {
            super.nextQueryException(query, ex);
            String entityName = (query != null) ? query.getObjEntityName() : null;
            throw new CayenneRuntimeException("Error creating PK for entity '" + entityName + "'.", ex);
        }


        public void nextGlobalException(Exception ex) {
            super.nextGlobalException(ex);
            throw new CayenneRuntimeException("Error creating PK.", ex);
        }
    }

    class PkRetrieveProcessor extends DefaultOperationObserver {
        private boolean successFlag;
        private SqlModifyQuery queryTemplate;
        private Integer nextId;
        private String entName;


        public PkRetrieveProcessor(SqlModifyQuery queryTemplate, String entName) {
            this.queryTemplate = queryTemplate;
            this.entName = entName;
        }


        public boolean useAutoCommit() {
            return false;
        }


        public void nextSnapshots(Query query, List resultObjects) {
            super.nextSnapshots(query, resultObjects);

            // process selected object, issue an update query
            if(resultObjects == null || resultObjects.size() == 0)
                throw new CayenneRuntimeException("Error generating PK : entity not supported: " + entName);
            if(resultObjects.size() > 1)
                throw new CayenneRuntimeException("Error generating PK : too many rows for entity: " + entName);

            Map lastPk = (Map)resultObjects.get(0);
            nextId = (Integer)lastPk.get("nextId");
            if(nextId == null)
                throw new CayenneRuntimeException("Error generating PK : null nextId.");

            // while transaction is still in progress, modify update query that will be executed next

            StringBuffer buf = new StringBuffer();
            buf.append("UPDATE AUTO_PK_SUPPORT SET NEXT_ID = ")
            .append(nextId.intValue() + 1)
            .append(" WHERE TABLE_NAME = '")
            .append(entName)
            .append("' AND NEXT_ID = ")
            .append(nextId);
            queryTemplate.setSqlString(buf.toString());
        }


        public void nextCount(Query query, int resultCount) {
            super.nextCount(query, resultCount);

            if(resultCount != 1)
                throw new CayenneRuntimeException("Error generating PK : update count is wrong: " + resultCount);
        }


        public void transactionCommitted() {
            super.transactionCommitted();
            successFlag = true;
        }

        public void nextQueryException(Query query, Exception ex) {
            super.nextQueryException(query, ex);
            String entityName = (query != null) ? query.getObjEntityName() : null;
            throw new CayenneRuntimeException("Error generating PK for entity '" + entityName + "'.", ex);
        }


        public void nextGlobalException(Exception ex) {
            super.nextGlobalException(ex);
            throw new CayenneRuntimeException("Error generating PK.", ex);
        }
    }


    public PkGenerator() { }


    /** Generate necessary database objects to do primary key generation.
     *  Table used by default is the following:
     *  <pre>
     *  CREATE TABLE AUTO_PK_SUPPORT (
     *   TABLE_NAME           CHAR(100) NOT NULL,
     *  NEXT_ID              INTEGER NOT NULL
     *  );
     *  </pre>
     *
     *  @param dataNode node that provides connection layer for PkGenerator.
     */
    public void createAutoPkSupport(DataNode dataNode) throws Exception {
        // will implement when we have forward engineering code done....
        // ....
    }


    /** <p>Perform necessary database operations to do primary key generation
     *  for a particular DbEntity.
     *  This  requires a prior call to <code>"createAutoPkSupport"<code>
     *  method.</p>
     *
     *  @param dataNode node that provides connection layer for PkGenerator.
     *  @param dbEntity DbEntity that needs an auto PK support
     */
    public void createAutoPkSupportForDbEntity(DataNode dataNode, DbEntity dbEntity) throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("INSERT INTO AUTO_PK_SUPPORT (TABLE_NAME, NEXT_ID) ")
        .append("VALUES ('")
        .append(dbEntity.getName())
        .append("', 1)");

        SqlModifyQuery q = new SqlModifyQuery();
        q.setSqlString(buf.toString());

        PkInsertProcessor resultProcessor = new PkInsertProcessor();
        dataNode.performQuery(q, resultProcessor);

        if(!resultProcessor.successFlag)
            throw new CayenneRuntimeException("Error creating pk support for db entity '" + dbEntity.getName() + "'.");
    }


    /**
     *  <p>Generate new (unique and non-repeating) primary key for specified dbEntity.</p>
     *
     *  <p>This implementation is naive and can have problems with high volume databases,
     *  when multiple applications can use this to get a primary key value. There is a
     *  possiblity that 2 clients will recieve the same value of primary key. So database
     *  specific implementations should be created for cleaner approach (like Oracle
     *  sequences, for example).</p>
     *
     *  @param dbEntity DbEntity that needs an auto PK support
     */
    public Object generatePkForDbEntity(DataNode dataNode, DbEntity dbEntity) throws Exception {
        ArrayList queries = new ArrayList(2);

        StringBuffer b1 = new StringBuffer();
        b1.append("SELECT NEXT_ID FROM AUTO_PK_SUPPORT WHERE TABLE_NAME = '")
        .append(dbEntity.getName())
        .append("'");

        SqlSelectQuery sel = new SqlSelectQuery();
        sel.setSqlString(b1.toString());
        sel.setResultDesc(resultDesc);
        queries.add(sel);


        // create dummy update .. it will be populated with real stuff inside DB transaction
        SqlModifyQuery upd = new SqlModifyQuery();
        queries.add(upd);

        PkRetrieveProcessor pkProcessor = new PkRetrieveProcessor(upd, dbEntity.getName());
        dataNode.performQueries(queries, pkProcessor);

        if(!pkProcessor.successFlag)
            throw new CayenneRuntimeException("Error generating PK.");
        else
            return pkProcessor.nextId;
    }
}
