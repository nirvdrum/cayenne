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
package org.objectstyle.cayenne.perform;

import java.util.logging.Logger;

import javax.sql.DataSource;

import org.objectstyle.TestConstants;
import org.objectstyle.cayenne.ConnectionSetup;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.conn.PoolDataSource;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.MapLoaderImpl;
import org.objectstyle.perform.*;

/** Runs performance tests. */
public class PerformMain implements TestConstants {
	static Logger logObj = Logger.getLogger(PerformMain.class.getName());

	public static DataDomain sharedDomain;

	public static void main(String[] args) {
		prepareDomain();
		
		
		PerformanceTestSuite suite = new PerformanceTestSuite();
		suite.addTestPair(
			"org.objectstyle.cayenne.perform.SimpleTest",
			"org.objectstyle.cayenne.perform.SimpleRefTest");
		ResultRenderer renderer = new ResultRenderer();
		new PerformanceTestRunner(renderer).runSuite(suite);
		renderer.showResults();
	}

	public static void prepareDomain() {
		try {
			DataSourceInfo dsi =
				new ConnectionSetup(true, true).buildConnectionInfo();

			PoolDataSource poolDS =
				new PoolDataSource(dsi.getJdbcDriver(), dsi.getDataSourceUrl());

			DataSource ds =
				new PoolManager(
					poolDS,
					dsi.getMinConnections(),
					dsi.getMaxConnections(),
					dsi.getUserName(),
					dsi.getPassword());

			// map
			String[] maps = new String[] { TEST_MAP_PATH };
			DataMap map = new MapLoaderImpl().loadDataMaps(maps)[0];

			// node
			DataNode node = new DataNode("node");
			node.setDataSource(ds);
			String adapterClass = dsi.getAdapterClass();
			if (adapterClass == null)
				adapterClass = DataNode.DEFAULT_ADAPTER_CLASS;
			node.setAdapter(
				(DbAdapter) Class.forName(adapterClass).newInstance());
			node.addDataMap(map);

			// generate pk's
			node.createPkSupportForMapEntities();

			// domain
			sharedDomain = new DataDomain("Shared Domain");
			sharedDomain.addNode(node);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}
}