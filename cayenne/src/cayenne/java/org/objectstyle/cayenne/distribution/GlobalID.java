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
package org.objectstyle.cayenne.distribution;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.TempObjectId;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.util.IDUtil;
import org.objectstyle.cayenne.util.Util;

/**
 * A global id for client objects.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// TODO: this implementation is likely to change
public class GlobalID implements Serializable {

    protected String entityName;
    protected Map objectIdKeys;

    protected byte[] key;

    /**
     * Creates a TEMPORARY id.
     */
    // TODO: this may be confusing - there is nothing in constructor that hints that this
    // is a temp id
    public GlobalID(String entityName) {
        this.entityName = entityName;
        this.key = IDUtil.pseudoUniqueByteSequence16();
    }

    /**
     * Creates a portable GlobalID from Cayenne ObjectId.
     */
    // TODO: make it work with Temp IDs
    public GlobalID(EntityResolver resolver, ObjectId serverOID) {
        ObjEntity entity = resolver.lookupObjEntity(serverOID.getObjectClass());
        if (entity == null) {
            throw new CayenneRuntimeException("Unmapped object class:"
                    + serverOID.getObjectClass().getName());
        }

        this.entityName = entity.getName();

        if (serverOID instanceof TempObjectId) {
            key = ((TempObjectId) serverOID).getKey();
        }
        else {
            this.objectIdKeys = serverOID.getIdSnapshot();
        }
    }

    /**
     * Converts client ID to server ID.
     */
    public ObjectId toServerOID(EntityResolver resolver) {
        ObjEntity entity = resolver.lookupObjEntity(this.entityName);

        if (entity == null) {
            throw new CayenneRuntimeException("Unknown entity:" + this.entityName);
        }

        Class serverClass = entity.getJavaClass(Thread
                .currentThread()
                .getContextClassLoader());
        return isTemporary() ? new TempObjectId(serverClass, key) : new ObjectId(
                serverClass,
                objectIdKeys);
    }

    public boolean isTemporary() {
        return key != null;
    }

    public String getEntityName() {
        return entityName;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof GlobalID)) {
            return false;
        }

        GlobalID id = (GlobalID) object;

        if (isTemporary()) {
            return new EqualsBuilder()
                    .append(entityName, entityName)
                    .append(key, id.key)
                    .isEquals();
        }

        if (!Util.nullSafeEquals(entityName, id.entityName)) {
            return false;
        }

        if (id.objectIdKeys == null && objectIdKeys == null) {
            return true;
        }

        if (id.objectIdKeys == null || objectIdKeys == null) {
            return false;
        }

        if (id.objectIdKeys.size() != objectIdKeys.size()) {
            return false;
        }

        EqualsBuilder builder = new EqualsBuilder();
        Iterator entries = objectIdKeys.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();

            Object key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                if (id.objectIdKeys.get(key) != null || !id.objectIdKeys.containsKey(key)) {
                    return false;
                }
            }
            else {
                // takes care of comparing primitive arrays, such as byte[]
                builder.append(value, id.objectIdKeys.get(key));
                if (!builder.isEquals()) {
                    return false;
                }
            }
        }

        return true;
    }

    public int hashCode() {

        // TODO: cache hashCode the way ObjectId does

        HashCodeBuilder builder = new HashCodeBuilder(3, 5);
        builder.append(entityName.hashCode());

        if (key != null) {
            builder.append(key);
        }

        if (objectIdKeys != null) {
            int len = objectIdKeys.size();

            // handle cheap and most common case - single key
            if (len == 1) {
                Iterator entries = objectIdKeys.entrySet().iterator();
                Map.Entry entry = (Map.Entry) entries.next();
                builder.append(entry.getKey()).append(entry.getValue());
            }
            // handle multiple keys - must sort the keys to use with HashCodeBuilder
            else {
                Object[] keys = objectIdKeys.keySet().toArray();
                Arrays.sort(keys);

                for (int i = 0; i < len; i++) {
                    // HashCodeBuilder will take care of processing object if it
                    // happens to be a primitive array such as byte[]

                    // also we don't have to append the key hashcode, its index will
                    // work
                    builder.append(i).append(objectIdKeys.get(keys[i]));
                }
            }
        }

        return builder.toHashCode();
    }

    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);

        builder.append("entityName", entityName);
        builder.append("temporary", isTemporary());

        if (isTemporary()) {
            builder.append("key", key);
        }
        else if (objectIdKeys != null) {
            Iterator it = objectIdKeys.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                builder.append(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return builder.toString();
    }
}
