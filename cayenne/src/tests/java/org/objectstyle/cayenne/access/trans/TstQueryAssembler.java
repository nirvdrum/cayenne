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

package org.objectstyle.cayenne.access.trans;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.query.DeleteQuery;
import org.objectstyle.cayenne.query.InsertQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.UpdateQuery;
import org.objectstyle.cayenne.unittest.CayenneTestResources;

public class TstQueryAssembler extends QueryAssembler {
    private static Logger logObj = Logger.getLogger(TstQueryAssembler.class);

    protected List dbRels = new ArrayList();

    public static TstQueryAssembler assembler(QueryEngine e, int qType) {
        switch (qType) {
            case Query.INSERT_QUERY :
                return new TstQueryAssembler(e, new InsertQuery());
            case Query.DELETE_QUERY :
                return new TstQueryAssembler(e, new DeleteQuery());
            case Query.SELECT_QUERY :
                return new TstQueryAssembler(e, new SelectQuery());
            case Query.UPDATE_QUERY :
                return new TstQueryAssembler(e, new UpdateQuery());
            default :
                throw new RuntimeException("Unknown query type: " + qType);
        }
    }

    public TstQueryAssembler(QueryEngine e, Query q) {
        super.setAdapter(CayenneTestResources.getResources().getSharedNode().getAdapter());
        super.setCon(CayenneTestResources.getResources().getSharedConnection());
        super.setEngine(e);
        super.setQuery(q);
    }

    public void dispose() throws SQLException {
        super.getCon().close();
    }

    public void dbRelationshipAdded(DbRelationship dbRel) {
        dbRels.add(dbRel);
    }

    public String aliasForTable(DbEntity dbEnt) {
        return "ta";
    }

    public boolean supportsTableAliases() {
        return true;
    }

    public String createSqlString() {
        return "SELECT * FROM ARTIST";
    }

    public List getAttributes() {
        return attributes;
    }

    public List getValues() {
        return values;
    }
}