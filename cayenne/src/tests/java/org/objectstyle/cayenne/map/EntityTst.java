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
package org.objectstyle.cayenne.map;

import java.util.Iterator;

import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.unittest.CayenneTestCase;


public class EntityTst extends CayenneTestCase {
    protected Entity ent;

    public EntityTst(String name) {
        super(name);
    }
    
    public void setUp() throws Exception {
        // create an anonymous inner Entity subclass, since Entity is abstract
        ent = new Entity() {
            public String getNameToDisplay() {return null;} 
            public String getTypenameToDisplay() {return null;} 
        };
    }
    
    
    public void testName() throws Exception {
        String tstName = "tst_name";
        ent.setName(tstName);
        assertEquals(tstName, ent.getName());
    }
    
    
    public void testAttribute() throws Exception {
        Attribute attr = new Attribute() {
            public String getNameToDisplay() {return null;} 
            public String getTypenameToDisplay() {return null;} 
        };
        attr.setName("tst_name");
        ent.addAttribute(attr);
        assertSame(attr, ent.getAttribute(attr.getName()));
        
        // attribute must have its entity switched to our entity.
        assertSame(ent, attr.getEntity());
        
        // remove attribute
        ent.removeAttribute(attr.getName());
        assertNull(ent.getAttribute(attr.getName()));
    }
    
    
    public void testRelationship() throws Exception {
        Relationship rel = new Relationship() {
            public Entity getTargetEntity() {return null;}             
        };
        rel.setName("tst_name");
        ent.addRelationship(rel);
        assertSame(rel, ent.getRelationship(rel.getName()));
        
        // attribute must have its entity switched to our entity.
        assertSame(ent, rel.getSourceEntity());
        
        // remove attribute
        ent.removeRelationship(rel.getName());
        assertNull(ent.getRelationship(rel.getName()));
    }
    
    
    public void testResolveBadObjPath1() throws Exception {
        // test invalid expression path
        Expression pathExpr = ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        pathExpr.setOperand(0, "invalid.invalid");
        
        // itertator should be returned, but when trying to read 1st component,
        // it should throw an exception....
        ObjEntity galleryEnt = getSharedDomain().getEntityResolver().lookupObjEntity("Gallery");
        Iterator it = galleryEnt.resolvePathComponents(pathExpr);
        assertTrue(it.hasNext());
        
        try {
            it.next();
            fail();
        }
        catch(Exception ex) {
            // exception expected
        }
    }
    
    
    public void testResolveBadObjPath2() throws Exception {
        // test invalid expression type
        Expression badPathExpr = ExpressionFactory.expressionOfType(Expression.IN); 
        badPathExpr.setOperand(0, "a.b.c");
        ObjEntity galleryEnt = getSharedDomain().getEntityResolver().lookupObjEntity("Gallery");
        
        try {
            galleryEnt.resolvePathComponents(badPathExpr);
            fail();
        }
        catch(Exception ex) {
            // exception expected
        }
    }
    
    
    public void testResolveObjPath1() throws Exception {
        Expression pathExpr = ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        pathExpr.setOperand(0, "galleryName");
        
        ObjEntity galleryEnt = getSharedDomain().getEntityResolver().lookupObjEntity("Gallery");
        Iterator it = galleryEnt.resolvePathComponents(pathExpr);
        
        // iterator must contain a single ObjAttribute
        assertNotNull(it);
        assertTrue(it.hasNext());
        ObjAttribute next = (ObjAttribute)it.next();
        assertNotNull(next);
        assertTrue(!it.hasNext());
        assertSame(galleryEnt.getAttribute("galleryName"), next);
    }
}
