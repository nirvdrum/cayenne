package org.objectstyle.cayenne;
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

import junit.framework.*;
import java.util.logging.*;
import java.util.*;
import java.io.*;
import org.objectstyle.util.*;
import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.query.*;
import org.objectstyle.cayenne.map.*;
import java.sql.*;
import org.objectstyle.art.*;



public class ObjectIdTst extends TestCase {
    static Logger logObj = Logger.getLogger(ObjectIdTst.class.getName());
    
    public ObjectIdTst(String name) {
        super(name);
    }
    
    
    public void testObjEntityName() throws Exception {
        String nm = "12345";
        ObjectId oid = new ObjectId(nm, null);
        assertEquals(nm, oid.getObjEntityName());       
    }
    
    public void testEquals0() throws Exception {
        String nm = "12345";
        ObjectId oid1 = new ObjectId(nm, null);
        assertEquals(oid1, oid1);       
    }
    
    public void testEquals1() throws Exception {
        String nm = "12345";
        ObjectId oid1 = new ObjectId(nm, null);
        ObjectId oid2 = new ObjectId(nm, null);
        assertEquals(oid1, oid2);       
    }
    
    
    public void testEquals2() throws Exception {
        String nm = "12345";
        HashMap hm = new HashMap();
        ObjectId oid1 = new ObjectId(nm, hm);
        ObjectId oid2 = new ObjectId(nm, hm);
        assertEquals(oid1, oid2);       
    }
    
    
    public void testEquals3() throws Exception {
        String nm = "12345";
        DbAttribute at = new DbAttribute();

        HashMap hm1 = new HashMap();
        hm1.put(at, "123");
        
        HashMap hm2 = new HashMap();
        hm2.put(at, "123");
        
        ObjectId oid1 = new ObjectId(nm, hm1);
        ObjectId oid2 = new ObjectId(nm, hm2);
        assertEquals(oid1, oid2);       
    }
    
    
    public void testIdAsMapKey() throws Exception {
        HashMap map =  new HashMap();
        Object o1 = new Object();
        
        String nm = "12345";
        DbAttribute at = new DbAttribute();

        HashMap hm1 = new HashMap();
        hm1.put(at, "123");
        
        HashMap hm2 = new HashMap();
        hm2.put(at, "123");
        
        ObjectId oid1 = new ObjectId(nm, hm1);
        ObjectId oid2 = new ObjectId(nm, hm2);
        
        map.put(oid1, o1);
        assertSame(o1, map.get(oid2));       
    }
    
    public void testNotEqual1() throws Exception {
        String nm1 = "12345";
        String nm2 = "abcdef";
        
        ObjectId oid1 = new ObjectId(nm1, null);
        ObjectId oid2 = new ObjectId(nm2, null);
        assertTrue(!oid1.equals(oid2));       
    }
    
    public void testNotEqual2() throws Exception {
        String nm = "12345";

        HashMap hm1 = new HashMap();
        DbAttribute at1 = new DbAttribute();
        hm1.put(at1, "123");
        
        HashMap hm2 = new HashMap();
        DbAttribute at2 = new DbAttribute();
        hm2.put(at2, "123");
        
        ObjectId oid1 = new ObjectId(nm, hm1);
        ObjectId oid2 = new ObjectId(nm, hm2);
        assertTrue(!oid1.equals(oid2));       
    }
}
