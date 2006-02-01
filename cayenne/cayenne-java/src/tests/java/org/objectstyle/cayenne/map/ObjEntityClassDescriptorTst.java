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
package org.objectstyle.cayenne.map;

import org.objectstyle.cayenne.property.ArcProperty;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.MockClassDescriptor;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.testdo.mt.MtTable1;
import org.objectstyle.cayenne.testdo.mt.MtTable2;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;

public class ObjEntityClassDescriptorTst extends CayenneTestCase {

    /**
     * Configures multi-tier stack as we want to access descriptors in different tiers...
     */
    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testServerDescriptorCaching() {
        ObjEntity entity = getServerEntity("MtTable1");
        entity.setClassDescriptor(null);

        ClassDescriptor descriptor = entity.getClassDescriptor();
        assertNotNull(descriptor);
        assertSame(descriptor, entity.getClassDescriptor());
        entity.setClassDescriptor(null);

        ClassDescriptor descriptor1 = entity.getClassDescriptor();
        assertNotNull(descriptor1);
        assertNotSame(descriptor, descriptor1);
    }

    public void testServerDescriptorFactory() {
        ObjEntity entity = getServerEntity("MtTable1");
        entity.setClassDescriptor(null);

        MockClassDescriptor mockDescriptor = new MockClassDescriptor();
        entity.getDataMap().setDescriptorFactory(
                new MockClassDescriptorFactory(mockDescriptor));

        try {
            ClassDescriptor descriptor = entity.getClassDescriptor();
            assertNotNull(descriptor);
            assertSame(mockDescriptor, descriptor);
        }
        finally {
            entity.getDataMap().setDescriptorFactory(null);
        }
    }

    public void testClientDescriptorCaching() {
        ObjEntity entity = getClientEntity("MtTable1");
        entity.setClassDescriptor(null);

        ClassDescriptor descriptor = entity.getClassDescriptor();
        assertNotNull(descriptor);
        assertSame(descriptor, entity.getClassDescriptor());
        entity.setClassDescriptor(null);

        ClassDescriptor descriptor1 = entity.getClassDescriptor();
        assertNotNull(descriptor1);
        assertNotSame(descriptor, descriptor1);
    }

    public void testArcProperties() {
        ObjEntity entity = getServerEntity("MtTable1");
        entity.setClassDescriptor(null);

        ObjEntity entity2 = getServerEntity("MtTable2");
        entity2.setClassDescriptor(null);

        ClassDescriptor descriptor = entity.getClassDescriptor();
        assertNotNull(descriptor);

        Property p = descriptor.getProperty(MtTable1.TABLE2ARRAY_PROPERTY);
        assertTrue(p instanceof ArcProperty);

        ClassDescriptor target = ((ArcProperty) p).getTargetDescriptor();
        assertNotNull(target);
        assertSame(entity2.getClassDescriptor(), target);
        assertEquals(MtTable2.TABLE1_PROPERTY, ((ArcProperty) p).getReversePropertyName());
    }

    protected ObjEntity getServerEntity(String name) {
        return getDomain().getEntityResolver().getObjEntity(name);
    }

    protected ObjEntity getClientEntity(String name) {
        return getDomain().getEntityResolver().getClientEntityResolver().getObjEntity(
                name);
    }
}
