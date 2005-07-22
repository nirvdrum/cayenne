/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.dba;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.dba.db2.DB2Sniffer;
import org.objectstyle.cayenne.dba.hsqldb.HSQLDBSniffer;
import org.objectstyle.cayenne.dba.mysql.MySQLSniffer;
import org.objectstyle.cayenne.dba.openbase.OpenBaseSniffer;
import org.objectstyle.cayenne.dba.oracle.OracleSniffer;
import org.objectstyle.cayenne.dba.postgres.PostgresSniffer;
import org.objectstyle.cayenne.dba.sqlserver.SQLServerSniffer;
import org.objectstyle.cayenne.dba.sybase.SybaseSniffer;

/**
 * Guesses correct DbAdapter for an unknown database. This implementation obtains
 * DatabaseMetaData object from the DataSource and passes it to a chain of conditional
 * factories. The first DbAdapterFactory in the chain that "recognizes" the database would
 * instantiate a DbAdapter that is returned to the user.
 * 
 * @since 1.2
 * @author Andrei Adamchik
 */
// TODO: how can custom adapters be autodetected? I.e. is there a way to plug a
// custom factory into configuration loading process?
public class DbAdapterDetector {

    private static Logger logObj = Logger.getLogger(DbAdapterDetector.class);

    protected List factories;

    public DbAdapterDetector() {
        this.factories = new ArrayList();

        // configure known predicates
        addFactory(new MySQLSniffer());
        addFactory(new PostgresSniffer());
        addFactory(new OracleSniffer());
        addFactory(new SQLServerSniffer());
        addFactory(new HSQLDBSniffer());
        addFactory(new DB2Sniffer());
        addFactory(new SybaseSniffer());
        addFactory(new OpenBaseSniffer());
    }

    /**
     * Removes all configured factories.
     */
    public void clearFactories() {
        this.factories.clear();
    }

    public void addFactory(DbAdapterFactory factory) {
        this.factories.add(factory);
    }

    public DbAdapter adapterForDataSource(DataSource ds) throws SQLException {

        Connection c = ds.getConnection();

        // exclude opening connection from timing autodetection...
        long t0 = System.currentTimeMillis();

        try {
            return adapterForMetaData(c.getMetaData());
        }
        finally {
            try {
                c.close();
            }
            catch (SQLException e) {
                // ignore...
            }

            if (logObj.isInfoEnabled()) {
                long t1 = System.currentTimeMillis();
                logObj.info("auto-detection took " + (t1 - t0) + " ms.");
            }
        }
    }

    /**
     * Returns a default adapter that is used as a failover if autodetection fails.
     */
    protected DbAdapter createDefaultAdapter() {
        return new JdbcAdapter();
    }

    /**
     * Iterates through predicated factories, stopping when the first one returns non-null
     * DbAdapter. If none of the factories match the database, returns default adapter.
     */
    protected DbAdapter adapterForMetaData(DatabaseMetaData md) throws SQLException {

        if (logObj.isInfoEnabled()) {
            logObj.info("DB name: " + md.getDatabaseProductName());
        }

        // match against configured predicated factories

        // iterate in reverse order to allow custom factories to take precedence over the
        // default ones configured in constructor
        for (int i = factories.size() - 1; i >= 0; i--) {
            DbAdapterFactory factory = (DbAdapterFactory) factories.get(i);
            DbAdapter adapter = factory.canHandleDatabase(md);

            if (adapter != null) {
                return adapter;
            }
        }

        logObj.info("Unrecognized database '"
                + md.getDatabaseProductName()
                + "', using default adapter.");

        return createDefaultAdapter();
    }
}
