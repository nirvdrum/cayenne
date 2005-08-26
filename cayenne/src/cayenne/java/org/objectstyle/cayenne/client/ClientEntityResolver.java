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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.map.ObjEntity;

/**
 * Provides a view of the mapping information to the client applications. Entities are
 * indexed by persistent object class and by entity name.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ClientEntityResolver implements Serializable {

    protected Map entitiesByClassName;
    protected Map entitiesByName;

    /**
     * Creates a client entity resolver initializing it with collection of ObjEntities.
     */
    public ClientEntityResolver(Collection entities) {
        this.entitiesByClassName = new HashMap();
        this.entitiesByName = new HashMap();

        index(entities);
    }

    /**
     * Indexes a collection of entities for faster lookup.
     */
    protected void index(Collection entities) {
        entitiesByClassName.clear();
        entitiesByName.clear();

        Iterator it = entities.iterator();
        while (it.hasNext()) {
            ObjEntity entity = (ObjEntity) it.next();

            if (entity.getName() == null) {
                throw new CayenneClientException("Invalid entity, name is null: "
                        + entity);
            }

            if (entity.getClassName() == null) {
                throw new CayenneClientException(
                        "Invalid entity, Java class name is null: " + entity);
            }

            entitiesByName.put(entity.getName(), entity);
            entitiesByClassName.put(entity.getClassName(), entity);
        }
    }

    /**
     * Returns a guaranteed non-null client ObjEntity for a given entity name. If entity
     * name is not mapped, its superclass is checked, all the way to the base class. If no
     * mapping is found in the hierarchy, a CayenneClientException is thrown.
     * 
     * @throws CayenneClientException if a class is not mapped.
     */
    public ObjEntity lookupEntity(String entityName) {
        if (entityName == null) {
            throw new CayenneClientException("Null entityName.");
        }

        ObjEntity entity = (ObjEntity) entitiesByName.get(entityName);

        if (entity == null) {
            throw new CayenneClientException("Unmapped entity: " + entityName);
        }

        return entity;
    }

    /**
     * Returns a guaranteed non-null client ObjEntity for a given object class. If class
     * is not mapped, its superclass is checked, all the way to the base class. If no
     * mapping is found in the hierarchy, a CayenneClientException is thrown.
     * 
     * @throws CayenneClientException if a class is not mapped.
     */
    public ObjEntity lookupEntity(Class objectClass) {
        if (objectClass == null) {
            throw new CayenneClientException("Null object class.");
        }

        ObjEntity entity = doLookupEntity(objectClass);

        if (entity == null) {
            throw new CayenneClientException("Unmapped class hierarchy: " + objectClass);
        }
        return entity;
    }

    /**
     * A recursive method to lookup entity by class. For internal use only.
     */
    protected ObjEntity doLookupEntity(Class objectClass) {
        if (objectClass == null) {
            return null;
        }

        // do lookup by class name. This is a more reliable key than class itself due to
        // hierachical class loaders
        ObjEntity entity = (ObjEntity) entitiesByClassName.get(objectClass.getName());
        return (entity != null) ? entity : doLookupEntity(objectClass.getSuperclass());
    }
}
