/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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

import java.io.File;
import java.sql.Connection;
import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.event.EventManager;

/**
 * Superclass of Cayenne test cases. Provides access to shared
 * connection resources.
 * 
 * @author Andrei Adamchik
 */
public abstract class CayenneTestCase extends TestCase {

    static {
        // init resources if needed
        CayenneTestResources.init();
    }

    public static File getDefaultTestResourceDir() {
        return new File(
            new File(new File(new File("build"), "tests"), "deps"),
            "test-resources");
    }

    /**
     * Utility method to strip the time part from the Date.
     */
    public static Date stripTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    protected void setUp() throws Exception {
        // make sure that the right domain is setup as shared, as some tests
        // may overwrite that
        Configuration config = Configuration.getSharedConfiguration();
        if (getDomain() != config.getDomain()) {
            if (config.getDomain() != null) {
                config.removeDomain(config.getDomain().getName());
            }
            config.addDomain(getDomain());
        }
    }

    /**
     * Returns directory that should be used by all test 
     * cases that perform file operations.
     */
    public File getTestDir() {
        return CayenneTestResources.getResources().getTestDir();
    }

    public File getTestResourceDir() {
        return getDefaultTestResourceDir();
    }

    public Connection getConnection() {
        return CayenneTestResources.getResources().getSharedConnection();
    }

    public DataDomain getDomain() {
        return CayenneTestResources.getResources().getSharedDomain();
    }

    public DataNode getNode() {
        return CayenneTestResources.getResources().getSharedNode();
    }

    public DataSourceInfo getFreshConnInfo() throws Exception {
        return CayenneTestResources.getResources().getFreshConnInfo();
    }

    public DataContext createDataContext() {
        return createDataContextWithSharedCache();
    }


    /**
     * Creates a DataContext that uses shared snapshot cache and is based on default test domain.
     */
    public DataContext createDataContextWithSharedCache() {
        // remove listeners for snapshot events
        EventManager.getDefaultManager().removeAllListeners(
            getDomain().getSharedSnapshotCache().getSnapshotEventSubject());

        // clear cache...
        getDomain().getSharedSnapshotCache().clear();
        DataContext context = getDomain().createDataContext(true);

        assertSame(
            getDomain().getSharedSnapshotCache(),
            context.getObjectStore().getDataRowCache());

        return context;
    }

    /**
     * Creates a DataContext that uses local snapshot cache and is based on default test domain.
     */
    public DataContext createDataContextWithLocalCache() {
        DataContext context = getDomain().createDataContext(false);

        assertNotSame(
            getDomain().getSharedSnapshotCache(),
            context.getObjectStore().getDataRowCache());

        return context;
    }

    public CayenneTestDatabaseSetup getDatabaseSetup() {
        return CayenneTestResources.getResources().getSharedDatabaseSetup();
    }

    public DatabaseSetupDelegate getDatabaseSetupDelegate() {
        return CayenneTestResources.getResources().getSharedDatabaseSetup().getDelegate();
    }
}
