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
package org.objectstyle.cayenne.access.types;

import java.sql.ResultSet;

import org.objectstyle.cayenne.unittest.CayenneTestCase;


public class ExtendedTypeMapTst extends CayenneTestCase {
    public ExtendedTypeMapTst(String name) {
        super(name);
    }

    public void testRegisterType() throws Exception {
        ExtendedTypeMap map = new ExtendedTypeMap();
        TestExtType tstType = new TestExtType();

        assertSame(map.getDefaultType(), map.getRegisteredType(tstType.getClassName()));

        map.registerType(tstType);
        assertSame(tstType, map.getRegisteredType(tstType.getClassName()));

        map.unregisterType(tstType.getClassName());
        assertSame(map.getDefaultType(), map.getRegisteredType(tstType.getClassName()));
    }

    public void testRegisteredTypeName() throws Exception {
        ExtendedTypeMap map = new TstTypeMap();
        TestExtType tstType = new TestExtType();

        assertNotNull(map.getRegisteredTypeNames());
        assertEquals(0, map.getRegisteredTypeNames().length);

        map.registerType(tstType);

        assertNotNull(map.getRegisteredTypeNames());
        assertEquals(1, map.getRegisteredTypeNames().length);
        assertEquals(tstType.getClassName(), map.getRegisteredTypeNames()[0]);
    }

    class TstTypeMap extends ExtendedTypeMap {
        protected void initDefaultTypes() {
            // noop to avoid any default types
        }
    }

    class TestExtType implements ExtendedType {
        public String getClassName() {
            return "test.test.Test";
        }

        public Object toJdbcObject(Object val, int type) throws Exception {
            return new Object();
        }

        public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
            Object val = rs.getObject(index);
            return (rs.wasNull()) ? null : val;
        }
    }
}

