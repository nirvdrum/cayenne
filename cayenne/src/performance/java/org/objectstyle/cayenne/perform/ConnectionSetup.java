package org.objectstyle.cayenne.perform;
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

import java.io.InputStream;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.cayenne.conf.ConfigLoader;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DefaultConfiguration;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;
import org.objectstyle.cayenne.conf.RuntimeConfigDelegate;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.util.ResourceLocator;


/** Creates database connection info to run tests.
  * Can build connection info either using an interactive 
  * login procedure or loading this information from
  * the XML file.
  *
  * @author Andrei Adamchik
  */
public class ConnectionSetup  {
    private static Logger logObj = Logger.getLogger(ConnectionSetup.class);

    private boolean interactive;
 
    public ConnectionSetup(boolean interactive) {
        this.interactive = interactive;
    }

    public DataSourceInfo buildConnectionInfo() throws Exception {
        return getInfoFromFile();
    }


    private DataSourceInfo getInfoFromFile() throws Exception {
		DisconnectedFactory factory = new DisconnectedFactory();
        DefaultConfiguration conf = new DefaultConfiguration();
		conf.setOverrideFactory(factory);
        ConfigLoader loader = new ConfigLoader(conf.getLoaderDelegate());
        InputStream in = ResourceLocator.findResourceInFileSystem(Configuration.DOMAIN_FILE);
        if(in == null)
            throw new RuntimeException("Can't find '" + Configuration.DOMAIN_FILE + "'.");

        
        if(!loader.loadDomains(in)) {
            throw new RuntimeException("Error loading configuration.");
        }

        DataSourceInfo dsi = factory.getDriverInfo();            
        DataDomain dom = conf.getDomain();
        dsi.setAdapterClass(dom.getDataNodes()[0].getAdapter().getClass().getName());
        return dsi;
    }


    /** Creates pooled DataSource without actually creating
      * database connections.*/
    class DisconnectedFactory extends DriverDataSourceFactory {
        public DisconnectedFactory() throws Exception {
            super();
        }

        /** Make this method public. */
        public DataSourceInfo getDriverInfo() {
            return super.getDriverInfo();
        }

        public DataSource getDataSource(String location) throws Exception {
            load(location);
            return new PoolManager(getDriverInfo().getJdbcDriver(),
                                   getDriverInfo().getDataSourceUrl(),
                                   0,
                                   0,
                                   getDriverInfo().getUserName(),
                                   getDriverInfo().getPassword());
        }

    }
}
