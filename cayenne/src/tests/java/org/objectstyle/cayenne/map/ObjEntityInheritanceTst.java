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
package org.objectstyle.cayenne.map;

import org.objectstyle.cayenne.unittest.CayenneSimpleTestCase;

/**
 * @author Andrei Adamchik
 */
public class ObjEntityInheritanceTst extends CayenneSimpleTestCase {
    protected DataMap map;

    protected ObjEntity entity1;
    protected ObjEntity entity2;
    protected ObjEntity entity3;

    protected ObjAttribute attribute1;
    protected ObjAttribute attribute2;
    protected ObjAttribute attribute3;

    protected ObjRelationship relationship1;
    protected ObjRelationship relationship2;
    protected ObjRelationship relationship3;

    public void setUp() throws Exception {
        map = new DataMap();
        entity1 = new ObjEntity("e1");
        entity2 = new ObjEntity("e2");
        entity3 = new ObjEntity("e3");

        attribute1 = new ObjAttribute("a1");
        attribute2 = new ObjAttribute("a2");
        attribute3 = new ObjAttribute("a3");

        entity1.addAttribute(attribute1);
        entity2.addAttribute(attribute2);
        entity3.addAttribute(attribute3);

        relationship1 = new ObjRelationship("r1");
        relationship2 = new ObjRelationship("r2");
        relationship3 = new ObjRelationship("r3");

        entity1.addRelationship(relationship1);
        entity2.addRelationship(relationship2);
        entity3.addRelationship(relationship3);

        map.addObjEntity(entity1);
        map.addObjEntity(entity2);
        map.addObjEntity(entity3);
    }

    public void testInheritedAttributes() throws Exception {
        assertSame(attribute1, entity1.getAttribute("a1"));
        assertNull(entity1.getAttribute("a2"));

        entity1.setSuperEntityName("e2");
        assertSame(attribute2, entity1.getAttribute("a2"));
        assertNull(entity1.getAttribute("a3"));

        entity2.setSuperEntityName("e3");
        assertSame(attribute3, entity1.getAttribute("a3"));
    }

    public void testInheritedRelationships() throws Exception {
        assertSame(relationship1, entity1.getRelationship("r1"));
        assertNull(entity1.getRelationship("r2"));

        entity1.setSuperEntityName("e2");
        assertSame(relationship2, entity1.getRelationship("r2"));
        assertNull(entity1.getRelationship("r3"));

        entity2.setSuperEntityName("e3");
        assertSame(relationship3, entity1.getRelationship("r3"));
    }
}
