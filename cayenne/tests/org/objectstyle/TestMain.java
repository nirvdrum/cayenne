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

import junit.framework.*;
import java.util.*;
import java.util.logging.*;
import java.io.*;
import java.sql.*;
import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.gui.*;
import org.objectstyle.cayenne.conn.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.util.*;
import org.objectstyle.cayenne.dba.DbAdapter;

/**
 *  Root class of all test cases. When "main" is invoked, 
 *  will configure database connection and invoke all package tests.
 *
 *  @author Andrei Adamchik
 */
public class TestMain implements TestConstants {
    static Logger logObj = Logger.getLogger(TestMain.class.getName());

    private static TestResources resources = new TestResources();


    public static TestResources getResources() {
        return resources;
    }


    public static void main(String[] args) {
        // check for "-nogui" flag
        boolean noGui = false;
        boolean xmlDataSource = false;
        if(args != null && args.length > 0) {
            if("-nogui".equals(args[0]))
                noGui = true;
            else if("-xml".equals(args[0]))
                xmlDataSource = true;
        }

        // configure properties
        configureProps();

        // initialize shared resources
        DataSourceInfo dsi = (xmlDataSource)
                             ? new ConnectionSetup(false, false).buildConnectionInfo()
                             : new ConnectionSetup(true, !noGui).buildConnectionInfo();

        resources.setSharedConnInfo(dsi);
        resources.setSharedConnection(openConnection());
        resources.setSharedDomain(createSharedDomain());


        // initialize other stuff
        createTestDatabase();


        // run tests
        if(System.getProperty(TestMain.SINGLE_TEST_PROP) != null)
            ObjectStyleTestRunner.runSingleTestCase(System.getProperty(SINGLE_TEST_PROP));
        else
            ObjectStyleTestRunner.runTests();
    }


    public static Connection getSharedConnection() {
        return getResources().getSharedConnection();
    }

    public static DataDomain getSharedDomain() {
        return getResources().getSharedDomain();
    }

    public static DataNode getSharedNode() {
        return getResources().getSharedNode();
    }


    public static DataSourceInfo getFreshConnInfo() throws java.lang.Exception {
        return getResources().getFreshConnInfo();
    }



    private static DataDomain createSharedDomain() {
        try {

            // data source
            DataSourceInfo dsi = getFreshConnInfo();
            PoolDataSource poolDS = new PoolDataSource(dsi.getJdbcDriver(), dsi.getDataSourceUrl());
            DataSource ds = new PoolManager(poolDS,
                                            dsi.getMinConnections(),
                                            dsi.getMaxConnections(),
                                            dsi.getUserName(),
                                            dsi.getPassword());

            // map
            String[] maps = new String[] {TEST_MAP_PATH};
            DataMap map = new MapLoaderImpl().loadDataMaps(maps)[0];

            // node
            DataNode node = new DataNode("node");
            node.setDataSource(ds);
            String adapterClass = dsi.getAdapterClass();
            if(adapterClass == null)
                adapterClass = DataNode.DEFAULT_ADAPTER_CLASS;
            node.setAdapter((DbAdapter)Class.forName(adapterClass).newInstance());
            node.addDataMap(map);


            // domain
            DataDomain domain = new DataDomain("Shared Domain");
            domain.addNode(node);
            return domain;

        } catch(java.lang.Exception ex) {
            logObj.log(Level.SEVERE, "Can not create shared domain.", ex);
            System.exit(1);

            // to satisfy a compiler, throw an exception
            // (as if System.exit() is not enough :-))
            throw new RuntimeException("Will never get here.");
        }
    }


    /** If we can not connect to the database, quit the application. */
    private static Connection openConnection() {
        try {
            DataSourceInfo dsi = resources.getFreshConnInfo();
            Driver driver = (Driver)Class.forName(dsi.getJdbcDriver()).newInstance();
            return DriverManager.getConnection(
                       dsi.getDataSourceUrl(),
                       dsi.getUserName(),
                       dsi.getPassword());
        } catch(java.lang.Exception ex) {
            logObj.log(Level.SEVERE, "Can not connect to the database.", ex);
            System.exit(1);
            // to satisfy a compiler, throw an exception
            throw new RuntimeException("Will never get here.");
        }
    }


    private static void createTestDatabase() {
        try {
            DatabaseSetup dbSetup = new DatabaseSetup(getResources().getSharedNode().getDataMaps()[0]);
            dbSetup.dropTestTables();
            dbSetup.setupTestTables();
        } catch(java.lang.Exception ex) {
            logObj.log(Level.SEVERE, "Error creating test database.", ex);
            System.exit(1);
        }
    }


    private static void configureProps() {
        // load user property overrides
        File propsFile = new File(System.getProperty("user.home") + File.separator + USER_PROPS);
        if(propsFile.exists()) {
            Properties props = new Properties();

            try {
                FileInputStream in = new FileInputStream(propsFile);
                props.load(in);
                in.close();
            } catch(IOException ioex) {
                logObj.log(Level.SEVERE, "Error loading properties.", ioex);
                System.exit(1);
            }

            Properties sysProps = System.getProperties();
            sysProps.putAll(props);
            System.setProperties(sysProps);
        }


        File logPropsFile = new File(System.getProperty("user.home") + File.separator + LOGGING_PROPS);
        if(logPropsFile.exists()) {
            try {
                FileInputStream in = new FileInputStream(logPropsFile);
                LogManager.getLogManager().readConfiguration(in);
                in.close();
            } catch(IOException ioex) {
                throw new RuntimeException("Error reading config.", ioex);
            }
        }
    }
}
