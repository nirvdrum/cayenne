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

package org.objectstyle.cayenne.query;

import org.apache.log4j.*;
import org.objectstyle.art.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.unittest.*;


public class QueryTst extends CayenneTestCase {
	protected Query query;    
    public QueryTst(String name) {
        super(name);
    }
    
    public void setUp() throws java.lang.Exception {
        query = new TstUnqualifiedQuery();
    }
    
    public void testSetRootEntityName() {
    	assertNull(query.getRoot());
    	query.setRoot("SomeEntity");
    	assertSame("SomeEntity", query.getRoot());
    }
    
    public void testSetRootObjEntity() {
    	assertNull(query.getRoot());
    	ObjEntity e= getDomain().getEntityResolver().lookupObjEntity(Artist.class);
    	query.setRoot(e);
    	assertSame(e, query.getRoot());
    }
    
    public void testSetRootClass() {
    	assertNull(query.getRoot());
	   	query.setRoot(Artist.class);
    	assertSame(Artist.class, query.getRoot());
    }

    public void testSetInvalidRoot() {
    	assertNull(query.getRoot());
    	try {
		   	query.setRoot(new Integer(1));
		   	fail("Should not be able to set the root to an Integer");
    	} catch (IllegalArgumentException e) {
    	}
    }

    public void testLoggingLevel() throws Exception {
        Query query = new TstUnqualifiedQuery();
        assertEquals(Query.DEFAULT_LOG_LEVEL, query.getLoggingLevel());
        
        query.setLoggingLevel(Level.ERROR);
        assertSame(Level.ERROR, query.getLoggingLevel());
    }
    
}

