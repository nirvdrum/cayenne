/*
 * ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0
 * 
 * Copyright (c) 2002-2003 The ObjectStyle Group and individual authors of the
 * software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 1.
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. The end-user documentation
 * included with the redistribution, if any, must include the following
 * acknowlegement: "This product includes software developed by the ObjectStyle
 * Group (http://objectstyle.org/)." Alternately, this acknowlegement may
 * appear in the software itself, if and wherever such third-party
 * acknowlegements normally appear. 4. The names "ObjectStyle Group" and
 * "Cayenne" must not be used to endorse or promote products derived from this
 * software without prior written permission. For written permission, please
 * contact andrus@objectstyle.org. 5. Products derived from this software may
 * not be called "ObjectStyle" nor may "ObjectStyle" appear in their names
 * without prior written permission of the ObjectStyle Group.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * OBJECTSTYLE GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the ObjectStyle Group. For more information on the ObjectStyle
 * Group, please see <http://objectstyle.org/> .
 *  
 */
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.List;

import org.objectstyle.art.Gallery;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unittest.MultiContextTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextDelegateTst extends MultiContextTestCase {
    protected Gallery gallery;

    protected void setUp() throws Exception {
        super.setUp();

        // prepare a single gallery record
        gallery = (Gallery) context.createAndRegisterNewObject("Gallery");
        gallery.setGalleryName("version1");
        context.commitChanges();
    }

    public void testSnapshotChangedInDataRow() throws Exception {
        // prepare a second context
        DataContext altContext = mirrorDataContext(context);

        // prepare delegates
        TestDelegate delegate = new TestDelegate();
        context.setDelegate(delegate);
        TestDelegate altDelegate = new TestDelegate();
        altContext.setDelegate(altDelegate);

        Gallery altGallery =
            (Gallery) altContext.getObjectStore().getObject(gallery.getObjectId());
        assertNotNull(altGallery);

        // update
        gallery.setGalleryName("version2");
        context.commitChanges();
        assertNull(
            "Delegate shouldn't have been notified, since we are using the same snapshot.",
            delegate.getChangedSnapshot());

        // test behavior on commit when snapshot has changed underneath
        altGallery.setGalleryName("version3");
        altContext.commitChanges();
		assertNotNull(
			"Delegate should have been notified, since we are using a different snapshot.",
			altDelegate.getChangedSnapshot());
    }

    public void testWillPerformSelect1() throws Exception {
        TestDelegate delegate = new TestDelegate();
        context.setDelegate(delegate);

        // test that delegate is consulted before select
        SelectQuery query = new SelectQuery(Gallery.class);
        List results = context.performQuery(query);

        assertTrue(
            "Delegate is not notified of a query being run.",
            delegate.containsQuery(query));
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    public void testWillPerformSelect2() throws Exception {
        TestDelegate delegate = new TestDelegate();
        context.setDelegate(delegate);

        // test that delegate can block a query
        delegate.setBlockQueries(true);

        SelectQuery query = new SelectQuery(Gallery.class);
        List results = context.performQuery(query);

        assertTrue(
            "Delegate is not notified of a query being run.",
            delegate.containsQuery(query));

        assertNotNull(results);

        // blocked
        assertEquals("Delegate couldn't block the query.", 0, results.size());
    }

    class TestDelegate implements DataContextDelegate {
        protected DataRow changedSnapshot;
        protected List queries = new ArrayList();
        protected boolean blockQueries;

        public void snapshotChangedInDataRowStore(
            DataObject object,
            DataRow snapshotInStore) {
            this.changedSnapshot = snapshotInStore;
        }

        public GenericSelectQuery willPerformSelect(
            DataContext context,
            GenericSelectQuery query) {

            queries.add(query);
            return (blockQueries) ? null : query;
        }

        public DataRow getChangedSnapshot() {
            return changedSnapshot;
        }

        public void setBlockQueries(boolean flag) {
            this.blockQueries = flag;
        }

        public boolean containsQuery(GenericSelectQuery query) {
            return queries.contains(query);
        }
    }
}
