package org.objectstyle;
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

import java.sql.Connection;

import org.objectstyle.cayenne.DatabaseSetup;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.util.Util;

/** Keeps references to all the test resources needed by Test Cases.
  *
  * @author Andrei Adamchik */
public class TestResources implements TestConstants {

    private DataSourceInfo sharedConnInfo;
    private DataDomain sharedDomain;
    private DatabaseSetup sharedDatabaseSetup;

    /** Unchecks connection from the pool. */
    public Connection getSharedConnection() {
        try {
            return getSharedNode().getDataSource().getConnection();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error unchecking connection: " + ex);
        }
    }

    public DataDomain getSharedDomain() {
        return sharedDomain;
    }

    public void setSharedDomain(DataDomain sharedDomain) {
        this.sharedDomain = sharedDomain;
    }

    public DataNode getSharedNode() {
        return sharedDomain.getDataNodes()[0];
    }

    public DataSourceInfo getFreshConnInfo() throws java.lang.Exception {
        return (DataSourceInfo) Util.cloneViaSerialization(sharedConnInfo);
    }

    public void setSharedConnInfo(DataSourceInfo dsi) {
        this.sharedConnInfo = dsi;
    }

    /**
     * Gets the sharedDatabaseSetup.
     * @return Returns a DatabaseSetup
     */
    public DatabaseSetup getSharedDatabaseSetup() {
        return sharedDatabaseSetup;
    }

    /**
     * Sets the sharedDatabaseSetup.
     * @param sharedDatabaseSetup The sharedDatabaseSetup to set
     */
    public void setSharedDatabaseSetup(DatabaseSetup sharedDatabaseSetup) {
        this.sharedDatabaseSetup = sharedDatabaseSetup;
    }

}