package org.objectstyle.cayenne.access;
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

import java.util.logging.Level;

import junit.framework.TestCase;

import org.objectstyle.TestMain;
import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.query.SqlSelectQuery;

public class DataContextExtrasTst extends TestCase {
    protected DataContext ctxt;

    public DataContextExtrasTst(String name) {
        super(name);
    }

    protected void setUp() throws java.lang.Exception {
        ctxt = TestMain.getSharedDomain().createDataContext();
    }

    public void testCreateAndRegisterNewObject() throws Exception {
        Artist a1 = (Artist) ctxt.createAndRegisterNewObject("Artist");
        assertTrue(ctxt.registeredObjects().contains(a1));
        assertTrue(ctxt.newObjects().contains(a1));
    }

    public void testCommitChangesError() throws Exception {
        Artist o1 = new Artist();
        o1.setArtistName("a");
        ctxt.registerNewObject(o1, "Artist");

        // this should cause PK generation exception in commit later
        TestMain.getSharedNode().getAdapter().getPkGenerator().dropAutoPkSupport(
            TestMain.getSharedNode());

        // disable logging for thrown exceptions
        Level oldLevel = DefaultOperationObserver.logObj.getLevel();
        DefaultOperationObserver.logObj.setLevel(Level.SEVERE);
        try {
            ctxt.commitChanges();
            fail("Exception expected but not thrown due to missing PK generation routine.");
        }
        catch (CayenneRuntimeException ex) {
            // exception expected
        }
        finally {
            DefaultOperationObserver.logObj.setLevel(oldLevel);
        }
    }

    /** 
     * Testing behavior of Cayenne when a database exception
     * is thrown in SELECT query.
     */
    public void testSelectException() throws Exception {
        SqlSelectQuery q =
            new SqlSelectQuery("Artist", "SELECT * FROM NON_EXISTENT_TABLE");

        // disable logging for thrown exceptions
        Level oldLevel = DefaultOperationObserver.logObj.getLevel();
        DefaultOperationObserver.logObj.setLevel(Level.SEVERE);
        try {
            ctxt.performQuery(q, new DataContextExtended().getSelectObserver());
            fail("Query was invalid and was supposed to fail.");
        }
        catch (RuntimeException ex) {
            // exception expected
        }
        finally {
            DefaultOperationObserver.logObj.setLevel(oldLevel);
        }
    }

    /** Helper class to get access to DataContext inner classes. */
    class DataContextExtended extends DataContext {
        public OperationObserver getSelectObserver() {
            return new SelectProcessor(Level.FINEST);
        }
    }
}