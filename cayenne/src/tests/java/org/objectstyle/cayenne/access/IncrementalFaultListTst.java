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
package org.objectstyle.cayenne.access;

import java.util.Iterator;

import org.objectstyle.TestMain;
import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.CayenneTestCase;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * @author Andrei Adamchik
 */
public class IncrementalFaultListTst extends CayenneTestCase {
	protected IncrementalFaultList list;
	protected GenericSelectQuery query;

    /**
     * Constructor for IncrementalFaultListTst.
     * @param name
     */
    public IncrementalFaultListTst(String name) {
        super(name);
    }
    
    
    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        TestMain.getSharedDatabaseSetup().cleanTableData();
        new DataContextTst("Helper").populateTables();
        
        SelectQuery q = new SelectQuery("Artist");
        q.setPageSize(5);
        query = q;
        list = new IncrementalFaultList(super.createDataContext(), query);
    }


    public void testSize() throws Exception {
    	assertEquals(DataContextTst.artistCount, list.size());
    }
    
    public void testPageIndex() throws Exception {
    	assertEquals(0, list.pageIndex(0));
    	assertEquals(0, list.pageIndex(1));
    	assertEquals(1, list.pageIndex(5));
    	assertEquals(13, list.pageIndex(69));
    }
    
    public void testPagesRead1() throws Exception {
    	assertTrue(!list.isFullyResolved());
    	assertEquals(1, list.getPagesRead());
    	
    	list.readUpToPage(1);
    	assertEquals(1, list.getPagesRead());
    	
    	list.readUpToObject(5);
    	assertEquals(1, list.getPagesRead());
    	
    	list.readUpToObject(6);
    	assertEquals(2, list.getPagesRead());
    } 
    
    public void testGet() throws Exception {
    	Artist a = (Artist)list.get(6);
    	
    	assertNotNull(a);
    	assertEquals(2, list.getPagesRead());
    	assertTrue(a != list.get(7));
    	assertEquals(2, list.getPagesRead());
    } 
    
    public void testRemove1() throws Exception {
    	// remove from faulted
    	list.readAll();
    	try {
    		list.remove(2);
    		fail("Remove shouldn't be supported.");
    	}
    	catch(UnsupportedOperationException ex) {
    		//exception expected
    	}
    }
    
    public void testRemove2() throws Exception {
    	// remove from unfaulted
    	try {
    		list.remove(2);
    		fail("Remove shouldn't be supported.");
    	}
    	catch(UnsupportedOperationException ex) {
    		//exception expected
    	}
    }
    
    public void testIterator() throws Exception {
    	Iterator it = list.iterator();
    	assertNotNull(it);
    	
    	assertTrue(!list.isFullyResolved());
    	
    	assertTrue(it.hasNext());
    	assertNotNull(it.next());
    	assertTrue(!list.isFullyResolved());
    }
}

