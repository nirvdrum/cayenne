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
package org.objectstyle.cayenne;

import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.cayenne.conf.ConnectionProperties;
import org.objectstyle.cayenne.conn.PoolDataSource;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.MapLoader;
import org.objectstyle.cayenne.util.Util;

/**
 * Initializes connections for Cayenne unit tests.
 * 
 * @author Andrei Adamchik
 */
public class CayenneTestResources {
    static Logger logObj = Logger.getLogger(CayenneTestResources.class);

    public static final String TEST_MAP_PATH = "test-resources/testmap.xml";

    protected DataSourceInfo sharedConnInfo;
    protected DataDomain sharedDomain;
    protected CayenneTestDatabaseSetup sharedDatabaseSetup;

    public CayenneTestResources(String connectionKey) {
        sharedConnInfo =
            ConnectionProperties.getInstance().getConnectionInfo(connectionKey);

        if (sharedConnInfo != null) {
            createSharedDomain();
            createDbSetup();
            createTestDatabase();
        }
    }

    /** Unchecks connection from the pool. */
    public Connection getSharedConnection() {
        try {
            return getSharedNode().getDataSource().getConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error unchecking connection: " + ex);
        }
    }

    public DataDomain getSharedDomain() {
        return sharedDomain;
    }

    public DataNode getSharedNode() {
        return sharedDomain.getDataNodes()[0];
    }

    public DataSourceInfo getFreshConnInfo() throws Exception {
        return (sharedConnInfo != null)
            ? (DataSourceInfo) Util.cloneViaSerialization(sharedConnInfo)
            : null;
    }

    /**
     * Gets the sharedDatabaseSetup.
     * @return Returns a DatabaseSetup
     */
    public CayenneTestDatabaseSetup getSharedDatabaseSetup() {
        return sharedDatabaseSetup;
    }

    protected void createDbSetup() {
        try {
            sharedDatabaseSetup = new CayenneTestDatabaseSetup(this, getSharedNode().getDataMaps()[0]);
        } catch (Exception ex) {
            logObj.error("Can not create shared DatabaseSetup.", ex);
            throw new CayenneRuntimeException("Can not create shared DatabaseSetup.", ex);
        }
    }

    protected void createSharedDomain() {
        try {
            // data source
            DataSourceInfo dsi = getFreshConnInfo();
            PoolDataSource poolDS =
                new PoolDataSource(dsi.getJdbcDriver(), dsi.getDataSourceUrl());
            DataSource ds =
                new PoolManager(poolDS, 1, 1, dsi.getUserName(), dsi.getPassword());

            // map
            String[] maps = new String[] { TEST_MAP_PATH };
            DataMap map = new MapLoader().loadDataMap(TEST_MAP_PATH);

            // node
            DataNode node = new DataNode("node");
            node.setDataSource(ds);
            Class adapterClass = DataNode.DEFAULT_ADAPTER_CLASS;

            if (dsi.getAdapterClass() != null) {
                adapterClass = Class.forName(dsi.getAdapterClass());
            }

            node.setAdapter((DbAdapter) adapterClass.newInstance());
            node.addDataMap(map);

            // domain
            sharedDomain = new DataDomain("Shared Domain");
            sharedDomain.addNode(node);
        } catch (Exception ex) {
            logObj.error("Can not create shared domain.", ex);
            throw new CayenneRuntimeException("Error creating test database.", ex);
        }
    }

    protected void createTestDatabase() {
        try {
            CayenneTestDatabaseSetup dbSetup = getSharedDatabaseSetup();
            dbSetup.dropTestTables();
            dbSetup.setupTestTables();
        } catch (Exception ex) {
            logObj.error("Error creating test database.", ex);
            throw new CayenneRuntimeException("Error creating test database.", ex);
        }
    }
}
