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
package org.objectstyle.cayenne.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;

/**
 * Batched UPDATE query.
 * 
 * @author Andriy Shapochka
 */

public class UpdateBatchQuery extends BatchQuery {
    private List dataObjectIds;
    private List updates;
    private List dbAttributes;
    private List updatedDbAttributes;
    private List idDbAttributes;
    private Iterator idIterator = IteratorUtils.EMPTY_ITERATOR;
    private Map currentId = Collections.EMPTY_MAP;
    private Iterator updateIterator = IteratorUtils.EMPTY_ITERATOR;
    private Map currentUpdate = Collections.EMPTY_MAP;

    public UpdateBatchQuery(
        DbEntity objectEntity,
        List updatedDbAttributeNames,
        int batchCapacity) {
        super(objectEntity);
        this.updatedDbAttributes =
            new ArrayList(updatedDbAttributeNames.size());
        Map attrMap = getDbEntity().getAttributeMap();
        for (Iterator i = updatedDbAttributeNames.iterator(); i.hasNext();) {
            Object name = i.next();
            updatedDbAttributes.add(attrMap.get(name));
        }
        dataObjectIds = new ArrayList(batchCapacity);
        updates = new ArrayList(batchCapacity);
        prepareMetadata();
    }

    public void reset() {
        idIterator = dataObjectIds.iterator();
        currentId = Collections.EMPTY_MAP;
        updateIterator = updates.iterator();
        currentUpdate = Collections.EMPTY_MAP;
    }

    public boolean next() {
        if (!idIterator.hasNext())
            return false;
        currentId = (Map) idIterator.next();
        currentId = (currentId != null ? currentId : Collections.EMPTY_MAP);
        currentUpdate = (Map) updateIterator.next();
        currentUpdate =
            (currentUpdate != null ? currentUpdate : Collections.EMPTY_MAP);
        return true;
    }

    public Object getObject(int dbAttributeIndex) {
        DbAttribute attribute =
            (DbAttribute) dbAttributes.get(dbAttributeIndex);
        String name = attribute.getName();

        // take value either from updated values or id's,
        // depending on the index
        return (dbAttributeIndex < updatedDbAttributes.size())
            ? currentUpdate.get(name)
            : currentId.get(name);
    }

    public void add(Map dataObjectId, Map updateSnapshot) {
        dataObjectIds.add(dataObjectId);
        updates.add(updateSnapshot);
    }

    public int size() {
        return dataObjectIds.size();
    }

    public List getDbAttributes() {
        return Collections.unmodifiableList(dbAttributes);
    }

    public List getIdDbAttributes() {
        return Collections.unmodifiableList(idDbAttributes);
    }

    public List getUpdatedDbAttributes() {
        return Collections.unmodifiableList(updatedDbAttributes);
    }

    private void prepareMetadata() {
        idDbAttributes = getDbEntity().getPrimaryKey();
        dbAttributes =
            new ArrayList(updatedDbAttributes.size() + idDbAttributes.size());
        dbAttributes.addAll(updatedDbAttributes);
        dbAttributes.addAll(idDbAttributes);
    }

    public int getQueryType() {
        return Query.UPDATE_BATCH_QUERY;
    }
}
