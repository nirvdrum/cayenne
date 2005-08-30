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
package org.objectstyle.cayenne.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;

/**
 * @since 1.2
 * @author Andrei Adamchik
 */
// TODO: create a generic GraphSerializer, maybe using the same approach as XML
// serialization mechanism only based on Java serialization.
class ClientServerUtils {

    static Object toClientObject(EntityResolver resolver, CayenneDataObject object)
            throws Exception {
        ObjEntity entity = object.getObjEntity();

        // TODO: move class creation to ObjEntity
        if (entity.getClientClassName() == null) {
            throw new CayenneRuntimeException(
                    "No client-side class defined for ObjEntity: " + entity.getName());
        }

        Class clientClass = Class.forName(entity.getClientClassName(), true, Thread
                .currentThread()
                .getContextClassLoader());

        Object clientObject = clientClass.newInstance();

        // copy ID
        if (clientObject instanceof Persistent) {
            ((Persistent) clientObject).setGlobalID(resolver.convertToGlobalID(object.getObjectId()));
        }

        // TODO: implement attribute filtering for client..
        // copy client properties

        Iterator it = entity.getAttributeMap().keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            PropertyUtils.setProperty(clientObject, key, object.readProperty(key));
        }

        return clientObject;
    }

    /**
     * Converts a list of server-side objects to their client counterparts.
     */
    static List toClientObjects(EntityResolver resolver, List dataObjects)
            throws Exception {
        List clientObjects = new ArrayList(dataObjects.size());

        Iterator it = dataObjects.iterator();
        while (it.hasNext()) {
            CayenneDataObject serverObject = (CayenneDataObject) it.next();

            // TODO: toClientObject performs some entity specific lookups that can be
            // cached as local variables when processing the list

            clientObjects.add(toClientObject(resolver, serverObject));
        }

        return clientObjects;
    }

    private ClientServerUtils() {

    }
}