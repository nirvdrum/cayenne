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
package org.objectstyle.cayenne;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.objectstyle.cayenne.util.IDUtil;

/**
 * An ObjectId is a globally unique identifier of DataObjects.
 * <p>
 * Each non-transient DataObject has an associated ObjectId. It is a global object
 * identifier and does not depend on the DataContext of a particular object instance.
 * ObjectId conceptually close to a RDBMS primary key idea. Among other things ObjectId is
 * used to ensure object uniqueness within DataContext.
 * </p>
 * 
 * @author Andrei Adamchik
 */
public class ObjectId implements Serializable {

    // Keys: DbAttribute names
    // Values: database values of the corresponding attribute
    protected Map objectIdKeys;
    protected Class objectClass;
    protected byte[] key;

    /**
     * @since 1.2
     */
    protected Map replacementIdMap;

    // TODO: caching hash code may cause issues on deserilaization in a different VM...
    // need custom readObject/writeObject

    // cache hashCode, since ObjectId is immutable
    int hashCode = Integer.MIN_VALUE;

    /**
     * Creates a TEMPORARY ObjectId that should be replaced by a permanent id once a
     * corresponding object is committed.
     */
    public ObjectId(Class objectClass) {
        this(objectClass, IDUtil.pseudoUniqueByteSequence16());
    }

    /**
     * Create a TEMPORARY ObjectId with a binary unique key. This id is "portable" in that
     * it can be used across virtual machines to identify the same object.
     * 
     * @since 1.2
     */
    public ObjectId(Class objectClass, byte[] key) {
        this.objectClass = objectClass;
        this.key = key;
    }

    /**
     * Convenience constructor for entities that have a single Integer as their id.
     */
    public ObjectId(Class objectClass, String keyName, int id) {
        this(objectClass, keyName, new Integer(id));
    }

    /**
     * Convenience constructor for entities that have a single column as their id.
     */
    public ObjectId(Class objectClass, String keyName, Object id) {
        this.objectClass = objectClass;
        this.setIdKeys(Collections.singletonMap(keyName, id));
    }

    /**
     * Creates a new ObjectId.
     */
    public ObjectId(Class objectClass, Map idKeys) {
        this.objectClass = objectClass;
        if (idKeys != null) {
            this.setIdKeys(Collections.unmodifiableMap(idKeys));
        }
        else {
            this.setIdKeys(Collections.EMPTY_MAP);
        }
    }

    /**
     * Returns a binary unique key for this id.
     * 
     * @since 1.2
     */
    public byte[] getKey() {
        return key;
    }

    protected void setIdKeys(Map idKeys) {
        this.objectIdKeys = idKeys;
    }

    public boolean equals(Object object) {
        if (!(object instanceof ObjectId)) {
            return false;
        }

        if (this == object) {
            return true;
        }

        ObjectId id = (ObjectId) object;

        if (isTemporary()) {
            return new EqualsBuilder().append(
                    objectClass.getName(),
                    id.objectClass.getName()).append(key, id.key).isEquals();
        }

        // use the class name because two Objectid's should be equal
        // even if their objClass'es were loaded by different class loaders.
        if (!objectClass.getName().equals(id.objectClass.getName())) {
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

    /**
     * Returns a map of id components. Keys in the map are DbAttribute names, values are
     * database values of corresponding columns.
     */
    public Map getIdSnapshot() {
        if (isTemporary()) {
            return (replacementIdMap == null) ? Collections.EMPTY_MAP : replacementIdMap;
        }

        return objectIdKeys;
    }

    /**
     * Returns a value of id attribute identified by the name of DbAttribute.
     */
    public Object getValueForAttribute(String attrName) {
        return getIdSnapshot().get(attrName);
    }

    /**
     * Returns whether this is a temporary id.
     */
    public boolean isTemporary() {
        return key != null;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(objectClass.getName());
        if (isTemporary()) {
            buf.append(" (temp)");
        }

        buf.append(": ");
        if (objectIdKeys != null) {
            Iterator it = objectIdKeys.keySet().iterator();
            while (it.hasNext()) {
                String nextKey = (String) it.next();
                Object value = objectIdKeys.get(nextKey);
                buf.append(" <").append(nextKey).append(": ").append(value).append('>');
            }
        }
        return buf.toString();
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        if (this.hashCode == Integer.MIN_VALUE) {
            // build and cache hashCode

            HashCodeBuilder builder = new HashCodeBuilder(3, 5);

            // use the class name because two Objectid's should be equal
            // even if their objClass'es were loaded by different class loaders.
            builder.append(objectClass.getName().hashCode());

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

            this.hashCode = builder.toHashCode();
        }

        return this.hashCode;
    }

    /**
     * Returns a Java class of persistent objects identified by this id.
     * 
     * @since 1.2 Renamed from getObjClass().
     */
    public Class getObjectClass() {
        return objectClass;
    }

    /**
     * @deprecated since 1.2 use getObjectClass().
     */
    public Class getObjClass() {
        return getObjectClass();
    }

    /**
     * Returns a replacement ObjectId associated with this id. Replacement ObjectId is
     * either a permananent ObjectId for an uncommitted object or a new id for object
     * whose id depends on its relationships.
     * 
     * @deprecated Since 1.2 replacement id is built by appending to replacementIdMap.
     */
    public ObjectId getReplacementId() {
        return (isReplacementIdAttached()) ? createReplacementId() : null;
    }

    /**
     * Initializes a replacement ObjectId.
     * 
     * @deprecated Since 1.2 replacement id is built by appending to replacementIdMap.
     */
    public void setReplacementId(ObjectId replacementId) {
        if (replacementId == null) {
            replacementIdMap = null;
        }
        else {
            Map map = getReplacementIdMap();
            map.clear();
            map.putAll(replacementId.getIdSnapshot());
        }
    }

    /**
     * Returns non-null mutable map that can be used to append replacement id values. This
     * allows to incrementally build a replacement ObjectId.
     * 
     * @since 1.2
     */
    public Map getReplacementIdMap() {
        if (replacementIdMap == null) {
            replacementIdMap = new HashMap();
        }

        return replacementIdMap;
    }

    /**
     * Creates and returns a replacement ObjectId. No validation of ID is done.
     * 
     * @since 1.2
     */
    public ObjectId createReplacementId() {
        return new ObjectId(getObjectClass(), replacementIdMap);
    }

    /**
     * Returns true if there is full or partial replacement id attached to this id. This
     * method is preferrable to "!getReplacementIdMap().isEmpty()" as it avoids unneeded
     * replacement id map creation.
     * 
     * @since 1.2
     */
    public boolean isReplacementIdAttached() {
        return replacementIdMap != null && !replacementIdMap.isEmpty();
    }
}