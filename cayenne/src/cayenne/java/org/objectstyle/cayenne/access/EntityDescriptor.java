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
package org.objectstyle.cayenne.access;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.Transformer;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;

/**
 * A utility class that provides a set of transformers to build snapshots from
 * DataObjects. One important feature of EntityDescriptor is the ability to provide
 * deferred snapshots that contain factories for unresolved keys.
 * 
 * @author Andrei Adamchik
 * @since 1.2
 */
class EntityDescriptor {

    DataNode node;
    ObjEntity entity;
    DbEntity dbEntity;
    Map attributeTransformers;

    EntityDescriptor(DataNode node, ObjEntity entity) {
        this.node = node;
        this.entity = entity;
        this.dbEntity = entity.getDbEntity();
        initTransformers();
    }

    private void initTransformers() {

        Collection attributes = dbEntity.getAttributes();
        this.attributeTransformers = new HashMap((int) (attributes.size() * 4.00 / 3.00));

        // init transformers for attributes coming from object...

        Iterator it = entity.getAttributes().iterator();
        while (it.hasNext()) {
            // ObjAttribute attribute = (ObjAttribute) it.next();

        }
    }

    /**
     * Creates an object snapshot replacing unknown values with
     * org.apache.commons.collections.Factory instances that allow to defer value
     * resolution.
     */
    Map deferredSnapshot(DataObject object) {
        Map snapshot = new HashMap((int) (attributeTransformers.size() * 4.00 / 3.00));
        Iterator it = attributeTransformers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            snapshot.put(entry.getKey(), ((Transformer) entry.getValue())
                    .transform(object));
        }

        return snapshot;
    }

    Object deferredSnapshotValue(DataObject object, String dbAttributeName) {
        Transformer transformer = (Transformer) attributeTransformers
                .get(dbAttributeName);

        if (transformer == null) {
            throw new CayenneRuntimeException("Invalid DbAttribute: " + dbAttributeName);
        }

        return transformer.transform(object);
    }

    // ==========================================================
    // Transformers
    // ==========================================================

    abstract class DataObjectTransformer implements Transformer {

        public Object transform(Object input) {
            if (input instanceof DataObject) {
                return (DataObject) transformDataObject((DataObject) input);
            }

            throw new CayenneRuntimeException("Invalid input: " + input);
        }

        protected abstract Object transformDataObject(DataObject object);
    }

    // passes object to all transformers in sequence until one evaluates to non-null
    final class NullPredicateTransformer extends DataObjectTransformer {

        String attributeName;
        DataObjectTransformer[] transformers;

        NullPredicateTransformer(DataObjectTransformer[] transformers,
                String attributeName) {
            this.transformers = transformers;
            this.attributeName = attributeName;
        }

        protected Object transformDataObject(DataObject object) {
            for (int i = 0; i < transformers.length; i++) {
                Object value = transformers[i].transformDataObject(object);
                if (value != null) {
                    return value;
                }
            }

            throw new CayenneRuntimeException("Problem getting snapshot value for "
                    + object.getClass()
                    + ", attribute name: "
                    + attributeName);
        }
    }

    final class ObjectPropertyTransformer extends DataObjectTransformer {

        String property;

        ObjectPropertyTransformer(String property) {
            this.property = property;
        }

        protected Object transformDataObject(DataObject object) {
            return object.readPropertyDirectly(property);
        }
    }

    final class PropagatedValueTransformer extends DataObjectTransformer {

        String masterProperty;
        String masterIDKey;
        boolean deferred;
        boolean required;

        PropagatedValueTransformer(String masterProperty, String masterIDKey,
                boolean required, boolean deferred) {
            this.masterIDKey = masterIDKey;
            this.masterProperty = masterProperty;
            this.required = required;
            this.deferred = deferred;
        }

        protected Object transformDataObject(DataObject object) {

            DataObject targetDo = (DataObject) object
                    .readPropertyDirectly(masterProperty);

            if (targetDo == null) {
                throw new CayenneRuntimeException(
                        "Problem extracting master PK value - null master object for "
                                + object.getClass()
                                + "."
                                + masterProperty);
            }

            ObjectId targetKey = targetDo.getObjectId();

            if (deferred) {
                return new DeferredIDValueFactory(targetKey, masterIDKey);
            }

            Object value = targetKey.getValueForAttribute(masterIDKey);

            if (required && value == null) {
                throw new CayenneRuntimeException(
                        "Problem extracting master PK value - null master PK "
                                + object.getClass()
                                + "."
                                + masterProperty);
            }

            return value;
        }
    }

    final class CayenneGeneratedPKTransformer extends DataObjectTransformer {

        PkGenerator generator;

        CayenneGeneratedPKTransformer(PkGenerator generator) {
            this.generator = generator;
        }

        protected Object transformDataObject(DataObject object) {
            try {
                return generator.generatePkForDbEntity(node, dbEntity);
            }
            catch (Exception ex) {
                throw new CayenneRuntimeException("Error generating PK: "
                        + ex.getMessage(), ex);
            }
        }
    }

    // ==========================================================
    // Factories [used for deferred value creation]
    // ==========================================================

    final static class DeferredIDValueFactory implements Factory {

        ObjectId objectID;
        String idKey;

        DeferredIDValueFactory(ObjectId objectID, String idKey) {
            this.objectID = objectID;
            this.idKey = idKey;
        }

        public Object create() {
            if (!objectID.isReplacementIdAttached()) {
                throw new CayenneRuntimeException("Replacement value for key '"
                        + idKey
                        + "' is not available in ObjectId "
                        + objectID);
            }

            Map replacementId = objectID.getReplacementIdMap();
            Object value = replacementId.get(idKey);
            if (value == null) {
                throw new CayenneRuntimeException("Replacement value for key '"
                        + idKey
                        + "' is not available in ObjectId "
                        + objectID);
            }

            return value;
        }
    }
}