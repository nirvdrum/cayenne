/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.tools;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.objectstyle.cayenne.access.DbGenerator;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.MapLoader;
import org.objectstyle.cayenne.util.Util;
import org.xml.sax.InputSource;

/**
 * An Ant Task that is a frontend to Cayenne DbGenerator allowing schema generation from
 * DataMap using Ant.
 * 
 * @author nirvdrum, Andrei Adamchik
 * @since 1.2
 */
public class DbGeneratorTask extends Task {

    protected DbAdapter adapter;
    protected File map;
    protected String driver;
    protected String url;
    protected String userName;
    protected String password;

    // DbGenerator options... setup defaults similar to DbGemerator itself:
    // all DROP set to false, all CREATE - to true
    protected boolean dropTables;
    protected boolean dropPK;
    protected boolean createTables = true;
    protected boolean createPK = true;
    protected boolean createFK = true;

    /**
     * Sets up logging to be in line with the Ant logging system.
     */
    protected void configureLogging() {
        Configuration.setLoggingConfigured(true);
        BasicConfigurator.configure(new AntAppender(this));
    }

    public void execute() {

        configureLogging();
        validateAttributes();

        try {
            // Build up the data source info for the generator.
            DataSourceInfo dsi = new DataSourceInfo();
            dsi.setJdbcDriver(driver);
            dsi.setDataSourceUrl(url);
            dsi.setUserName(userName);
            dsi.setPassword(password);

            // Load the data map and run the db generator.
            DataMap dataMap = loadDataMap();
            DbGenerator generator = new DbGenerator(adapter, dataMap);
            generator.setShouldCreateFKConstraints(createFK);
            generator.setShouldCreatePKSupport(createPK);
            generator.setShouldCreateTables(createTables);
            generator.setShouldDropPKSupport(dropPK);
            generator.setShouldDropTables(dropTables);

            generator.runGenerator(dsi);
        }
        catch (Exception ex) {
            Throwable th = Util.unwindException(ex);

            String message = "Error generating database";

            if (th.getLocalizedMessage() != null) {
                message += ": " + th.getLocalizedMessage();
            }

            super.log(message);
            throw new BuildException(message, th);
        }
    }

    /**
     * Validates atttributes that are not related to internal DefaultClassGenerator.
     * Throws BuildException if attributes are invalid.
     */
    protected void validateAttributes() throws BuildException {
        StringBuffer error = new StringBuffer("");

        if (map == null) {
            error.append("The 'map' attribute must be set.\n");
        }

        if (adapter == null) {
            error.append("The 'adapter' attribute must be set.\n");
        }

        if (driver == null) {
            error.append("The 'driver' attribute must be set.\n");
        }

        if (url == null) {
            error.append("The 'adapter' attribute must be set.\n");
        }

        if (error.length() > 0) {
            throw new BuildException(error.toString());
        }
    }

    /** Loads and returns DataMap based on <code>map</code> attribute. */
    protected DataMap loadDataMap() throws Exception {
        InputSource in = new InputSource(map.getCanonicalPath());
        return new MapLoader().loadDataMap(in);
    }

    public void setCreateFK(boolean createFK) {
        this.createFK = createFK;
    }

    public void setCreatePK(boolean createPK) {
        this.createPK = createPK;
    }

    public void setCreateTables(boolean createTables) {
        this.createTables = createTables;
    }

    public void setDropPK(boolean dropPK) {
        this.dropPK = dropPK;
    }

    public void setDropTables(boolean dropTables) {
        this.dropTables = dropTables;
    }

    /**
     * Sets the map.
     * 
     * @param map The map to set
     */
    public void setMap(File map) {
        this.map = map;
    }

    /**
     * Sets the db adapter.
     * 
     * @param adapter The db adapter to set.
     */
    public void setAdapter(String adapter) {

        if (adapter != null) {
            // Try to create an instance of the DB adapter.
            try {
                Class c = Class.forName(adapter);
                this.adapter = (DbAdapter) c.newInstance();
            }
            catch (Exception e) {
                throw new BuildException("Can't load DbAdapter: " + adapter);
            }
        }
    }

    /**
     * Sets the JDBC driver used to connect to the database server.
     * 
     * @param driver The driver to set.
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * Sets the JDBC URL used to connect to the database server.
     * 
     * @param url The url to set.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets the username used to connect to the database server.
     * 
     * @param username The username to set.
     */
    public void setUserName(String username) {
        this.userName = username;
    }

    /**
     * Sets the password used to connect to the database server.
     * 
     * @param password The password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }
}