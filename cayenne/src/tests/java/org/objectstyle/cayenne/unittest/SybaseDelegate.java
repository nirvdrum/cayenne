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
package org.objectstyle.cayenne.unittest;

import java.sql.Connection;

import org.objectstyle.cayenne.access.DataContextStoredProcTst;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;

/**
 * @author Andrei Adamchik
 */
public class SybaseDelegate extends DatabaseSetupDelegate {

    /**
     * Constructor for SybaseDelegate.
     * @param adapter
     */
    public SybaseDelegate(DbAdapter adapter) {
        super(adapter);
    }

    public boolean supportsStoredProcedures() {
        return true;
    }

    public void createdTables(Connection con, DataMap map) throws Exception {
        createSelectSP(con, DataContextStoredProcTst.SELECT_STORED_PROCEDURE);
        createUpdateSP(con, DataContextStoredProcTst.UPDATE_STORED_PROCEDURE);
    }

    public void willDropTables(Connection con, DataMap map) throws Exception {
        dropSP(con, DataContextStoredProcTst.SELECT_STORED_PROCEDURE);
        dropSP(con, DataContextStoredProcTst.UPDATE_STORED_PROCEDURE);
    }

    private void createSelectSP(Connection con, String name) throws Exception {
        StringBuffer buf = new StringBuffer();
        buf
            .append(" CREATE PROCEDURE ")
            .append(name)
            .append(" @aName VARCHAR(255), @paintingPrice INT AS")
            .append(" BEGIN")
        //
        .append(" BEGIN TRANSACTION")
        .append(" UPDATE PAINTING SET ESTIMATED_PRICE = ESTIMATED_PRICE * 2")
        .append(" WHERE ESTIMATED_PRICE < @paintingPrice")
        .append(" COMMIT")
        //
        .append(" SELECT DISTINCT A.ARTIST_ID, A.DATE_OF_BIRTH, A.ARTIST_NAME")
        .append(" FROM ARTIST A, PAINTING P")
        .append(" WHERE A.ARTIST_ID = P.ARTIST_ID AND A.ARTIST_NAME = @aName ")
        .append(" AND P.ESTIMATED_PRICE > @paintingPrice")
        //
        .append(" END");

        executeDDL(con, buf.toString());
    }

    private void createUpdateSP(Connection con, String name) throws Exception {
        StringBuffer buf = new StringBuffer();
        buf
            .append(" CREATE PROCEDURE ")
            .append(name)
            .append(" @aName VARCHAR(255), @paintingPrice INT AS")
            .append(" BEGIN")
        //
        .append(" BEGIN TRANSACTION")
        .append(" UPDATE PAINTING SET ESTIMATED_PRICE = ESTIMATED_PRICE * 2")
        .append(" WHERE ESTIMATED_PRICE < @paintingPrice")
        .append(" COMMIT")
        //
        .append(" END");

        executeDDL(con, buf.toString());
    }

    private void dropSP(Connection con, String name) throws Exception {
        String sql =
            "if exists (SELECT * FROM sysobjects WHERE name = '"
                + name
                + "') BEGIN DROP PROCEDURE "
                + name
                + " END";

        executeDDL(con, sql);
    }
}
