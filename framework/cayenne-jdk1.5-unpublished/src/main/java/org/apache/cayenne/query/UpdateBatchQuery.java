/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * Batched UPDATE query.
 * 
 * @author Andriy Shapochka
 */
public class UpdateBatchQuery extends BatchQuery {

    /**
     * @since 1.2
     */
    protected List objectIds;

    protected List qualifierSnapshots;
    protected List updateSnapshots;

    protected boolean usingOptimisticLocking;

    private List updatedAttributes;
    private List qualifierAttributes;
    private Collection nullQualifierNames;
    private List dbAttributes;

    /**
     * Creates new UpdateBatchQuery.
     * 
     * @param dbEntity Table or view to update.
     * @param qualifierAttributes DbAttributes used in the WHERE clause.
     * @param nullQualifierNames DbAttribute names in the WHERE clause that have null
     *            values.
     * @param updatedAttribute DbAttributes describing updated columns.
     * @param batchCapacity Estimated size of the batch.
     */
    public UpdateBatchQuery(DbEntity dbEntity, List qualifierAttributes,
            List updatedAttribute, Collection nullQualifierNames, int batchCapacity) {

        super(dbEntity);

        this.updatedAttributes = updatedAttribute;
        this.qualifierAttributes = qualifierAttributes;
        this.nullQualifierNames = nullQualifierNames != null
                ? nullQualifierNames
                : Collections.EMPTY_SET;

        qualifierSnapshots = new ArrayList(batchCapacity);
        updateSnapshots = new ArrayList(batchCapacity);
        objectIds = new ArrayList(batchCapacity);

        dbAttributes = new ArrayList(updatedAttributes.size()
                + qualifierAttributes.size());
        dbAttributes.addAll(updatedAttributes);
        dbAttributes.addAll(qualifierAttributes);
    }

    /**
     * Returns true if a given attribute always has a null value in the batch.
     * 
     * @since 1.1
     */
    public boolean isNull(DbAttribute attribute) {
        return nullQualifierNames.contains(attribute.getName());
    }

    /**
     * Returns true if the batch query uses optimistic locking.
     * 
     * @since 1.1
     */
    public boolean isUsingOptimisticLocking() {
        return usingOptimisticLocking;
    }

    /**
     * @since 1.1
     */
    public void setUsingOptimisticLocking(boolean usingOptimisticLocking) {
        this.usingOptimisticLocking = usingOptimisticLocking;
    }

    public Object getValue(int dbAttributeIndex) {
        DbAttribute attribute = (DbAttribute) dbAttributes.get(dbAttributeIndex);

        // take value either from updated values or id's,
        // depending on the index
        Object snapshot = (dbAttributeIndex < updatedAttributes.size()) ? updateSnapshots
                .get(batchIndex) : qualifierSnapshots.get(batchIndex);
        return getValue((Map) snapshot, attribute);
    }

    /**
     * Adds a parameter row to the batch.
     */
    public void add(Map qualifierSnapshot, Map updateSnapshot) {
        add(qualifierSnapshot, updateSnapshot, null);
    }

    /**
     * Adds a parameter row to the batch.
     * 
     * @since 1.2
     */
    public void add(Map qualifierSnapshot, Map updateSnapshot, ObjectId id) {
        qualifierSnapshots.add(qualifierSnapshot);
        updateSnapshots.add(updateSnapshot);
        objectIds.add(id);
    }

    public int size() {
        return qualifierSnapshots.size();
    }

    public List getDbAttributes() {
        return dbAttributes;
    }

    /**
     * @since 1.1
     */
    public List getUpdatedAttributes() {
        return Collections.unmodifiableList(updatedAttributes);
    }

    /**
     * @since 1.1
     */
    public List getQualifierAttributes() {
        return Collections.unmodifiableList(qualifierAttributes);
    }

    /**
     * Returns a snapshot of the current qualifier values.
     * 
     * @since 1.1
     */
    public Map getCurrentQualifier() {
        return (Map) qualifierSnapshots.get(batchIndex);
    }

    /**
     * Returns an ObjectId associated with the current batch iteration. Used internally by
     * Cayenne to match current iteration with a specific object and assign it generated
     * keys.
     * 
     * @since 1.2
     */
    public ObjectId getObjectId() {
        return (ObjectId) objectIds.get(batchIndex);
    }
}