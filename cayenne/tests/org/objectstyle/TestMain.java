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

package org.objectstyle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneTestCase;
import org.objectstyle.cayenne.ConnectionSetup;
import org.objectstyle.cayenne.DatabaseSetup;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conn.PoolDataSource;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.gen.ClassGenerator;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.MapLoader;

/**
 *  Root class of all test cases. When "main" is invoked, 
 *  it will configure database connection and call all package tests.
 *  
 * <p><i>TODO: need refactoring. This class is too big to only hold static
 * methods</i></p>
 *
 *  @author Andrei Adamchik
 */
public class TestMain implements TestConstants {
	static Logger logObj = Logger.getLogger(TestMain.class.getName());

	private static TestResources resources = new TestResources();
	private static boolean noGui;

	public static TestResources getResources() {
		return resources;
	}

	public static boolean noGui() {
		return noGui;
	}

	public static void main(String[] args) {
		// check for "-nogui" flag
		noGui = false;
		boolean xmlDataSource = false;
		if (args != null && args.length > 0) {
			if ("-nogui".equals(args[0]))
				noGui = true;
			else if ("-xml".equals(args[0])) {
				noGui = true;
				xmlDataSource = true;
			}
		}
		// bootstrap ClassLoader
		Configuration.bootstrapSharedConfig(TestMain.class);

		// configure properties
		configureProps();
		
		// check JDK version
		if(CayenneTestCase.hasJSDK14()) {
			logObj.info("JDK 1.4 detected.");
		}
		else {
			logObj.info("No JDK 1.4 detected, assuming JDK1.3.");
		}

		// initialize shared resources
		try {
			DataSourceInfo dsi =
				(xmlDataSource)
					? new ConnectionSetup(false, false).buildConnectionInfo()
					: new ConnectionSetup(true, !noGui).buildConnectionInfo();

			resources.setSharedConnInfo(dsi);
		} catch (Exception ex) {
			logObj.error("Can not load connection info.", ex);
			System.exit(1);
		}

		resources.setSharedDomain(createSharedDomain());
		resources.setSharedDatabaseSetup(createDbSetup());

		// initialize other stuff
		createTestDatabase();
		ClassGenerator.bootstrapVelocity(ClassGenerator.class);

		// run tests
		boolean success = true;
		if (System.getProperty(TestMain.SINGLE_TEST_PROP) != null)
			success =
				ObjectStyleTestRunner.runSingleTestCase(
					System.getProperty(SINGLE_TEST_PROP));
		else
			success = ObjectStyleTestRunner.runTests();

		if (!success) {
			logObj.warn("Some tests have failed.");
			System.exit(1);
		}
	}

	public static DatabaseSetup getSharedDatabaseSetup() {
		return getResources().getSharedDatabaseSetup();
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

	public static DataSourceInfo getFreshConnInfo()
		throws java.lang.Exception {
		return getResources().getFreshConnInfo();
	}

	private static DatabaseSetup createDbSetup() {
		try {
			return new DatabaseSetup(
				resources.getSharedNode().getDataMaps()[0]);
		} catch (Exception ex) {
			logObj.log(Level.ERROR, "Can not create shared DatabaseSetup.", ex);
			System.exit(1);
		}

		return null;
	}

	private static DataDomain createSharedDomain() {
		try {
			// data source
			DataSourceInfo dsi = getFreshConnInfo();
			PoolDataSource poolDS =
				new PoolDataSource(dsi.getJdbcDriver(), dsi.getDataSourceUrl());
			DataSource ds =
				new PoolManager(
					poolDS,
					1,
					2,
					dsi.getUserName(),
					dsi.getPassword());

			// map
			String[] maps = new String[] { TEST_MAP_PATH };
			DataMap map = new MapLoader().loadDataMap(TEST_MAP_PATH);

			// node
			DataNode node = new DataNode("node");
			node.setDataSource(ds);
			String adapterClass = dsi.getAdapterClass();
			if (adapterClass == null)
				adapterClass = DataNode.DEFAULT_ADAPTER_CLASS;
			node.setAdapter(
				(DbAdapter) Class.forName(adapterClass).newInstance());
			node.addDataMap(map);

			// domain
			DataDomain domain = new DataDomain("Shared Domain");
			domain.addNode(node);
			return domain;
		} catch (java.lang.Exception ex) {
			logObj.log(Level.ERROR, "Can not create shared domain.", ex);
			System.exit(1);
		}
		return null;
	}

	private static void createTestDatabase() {
		try {
			DatabaseSetup dbSetup = getSharedDatabaseSetup();
			dbSetup.dropTestTables();
			dbSetup.setupTestTables();
		} catch (java.lang.Exception ex) {
			logObj.log(Level.ERROR, "Error creating test database.", ex);
			System.exit(1);
		}
	}

	private static void configureProps() {
		// load user property overrides
		File propsFile =
			new File(
				System.getProperty("user.home") + File.separator + USER_PROPS);
		if (propsFile.exists()) {
			Properties props = new Properties();

			try {
				FileInputStream in = new FileInputStream(propsFile);
				props.load(in);
				in.close();
			} catch (IOException ioex) {
				logObj.log(Level.ERROR, "Error loading properties.", ioex);
				System.exit(1);
			}

			Properties sysProps = System.getProperties();
			sysProps.putAll(props);
			System.setProperties(sysProps);
		}
		Configuration.configCommonLogging();
	}
}