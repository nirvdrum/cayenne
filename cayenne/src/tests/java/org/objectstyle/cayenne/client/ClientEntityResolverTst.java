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
package org.objectstyle.cayenne.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.distribution.HessianConnector;
import org.objectstyle.cayenne.map.ObjEntity;

public class ClientEntityResolverTst extends TestCase {

    public void testSerializabilityWithHessian() throws Exception {
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName("java.lang.String");
        ClientEntityResolver resolver = new ClientEntityResolver(Collections
                .singleton(entity));

        // simple case
        Object c1 = HessianConnector.cloneViaHessianSerialization(resolver);

        assertNotNull(c1);
        assertTrue(c1 instanceof ClientEntityResolver);
        ClientEntityResolver cr1 = (ClientEntityResolver) c1;

        assertNotSame(resolver, cr1);
        assertEquals(1, cr1.getEntityNames().size());
        assertTrue(cr1.getEntityNames().contains(entity.getName()));

        // with descriptors resolved...
        assertNotNull(entity.getClassDescriptor());

        ClientEntityResolver cr2 = (ClientEntityResolver) HessianConnector
                .cloneViaHessianSerialization(resolver);
        assertNotNull(cr2);
        assertEquals(1, cr2.getEntityNames().size());
        assertTrue(cr2.getEntityNames().contains(entity.getName()));
        assertNotNull(cr2.entityForName(entity.getName()).getClassDescriptor());
    }

    public void testConstructor() {
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName("java.lang.String");
        Collection entities = Collections.singleton(entity);

        ClientEntityResolver resolver = new ClientEntityResolver(entities);

        assertSame(entity, resolver.entityForName(entity.getName()));

        Collection names = resolver.getEntityNames();
        assertEquals(1, names.size());
        assertTrue(names.contains(entity.getName()));
    }

    public void testDataMapInjection() {
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName("java.lang.String");
        Collection entities = Collections.singleton(entity);

        assertNull(entity.getDataMap());
        new ClientEntityResolver(entities);
        assertNotNull(entity.getDataMap());
    }

    public void testInheritance() {
        ObjEntity superEntity = new ObjEntity("super_entity");
        superEntity.setClassName("java.lang.Object");

        ObjEntity subEntity = new ObjEntity("sub_entity");
        subEntity.setClassName("java.lang.String");

        subEntity.setSuperEntityName(superEntity.getName());

        try {
            subEntity.getSuperEntity();
            fail("hmm... superentity can't possibly be resolved at this point.");
        }
        catch (CayenneRuntimeException e) {
            // expected
        }

        new ClientEntityResolver(Arrays.asList(new Object[] {
                superEntity, subEntity
        }));

        // after registration with resolver super entity should resolve just fine
        assertSame(superEntity, subEntity.getSuperEntity());
    }
}
