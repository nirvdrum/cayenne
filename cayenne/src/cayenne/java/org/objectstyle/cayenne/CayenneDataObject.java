/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group
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
package org.objectstyle.cayenne;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.EntityResolver;
import org.objectstyle.cayenne.access.util.RelationshipFault;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.util.PropertyComparator;

/**
 * A CayenneDataObject is a default implementation of DataObject interface.
 * It is normally used as a superclass of Cayenne persistent objects.
 *
 * @author Andrei Adamchik
 */
public class CayenneDataObject implements DataObject {
    private static Logger logObj = Logger.getLogger(CayenneDataObject.class);

    protected long snapshotVersion = DEFAULT_VERSION;

    protected ObjectId objectId;
    protected transient int persistenceState = PersistenceState.TRANSIENT;
    protected transient DataContext dataContext;
    protected Map values = new HashMap();

    /** Returns a data context this object is registered with, or null
     * if this object has no associated DataContext */
    public DataContext getDataContext() {
        return dataContext;
    }

    public void setDataContext(DataContext dataContext) {
        this.dataContext = dataContext;

        if (dataContext == null) {
            this.persistenceState = PersistenceState.TRANSIENT;
        }
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public void setObjectId(ObjectId objectId) {
        this.objectId = objectId;
    }

    public int getPersistenceState() {
        return persistenceState;
    }

    public void setPersistenceState(int persistenceState) {
        this.persistenceState = persistenceState;

        if (persistenceState == PersistenceState.HOLLOW) {
            values.clear();
        }
    }

    /**
     * Convenience method to read a "nested" property.
     * Dot-separated path is used to traverse object relationships
     * until the final object is found. If a null object found
     * while traversing path, null is returned. If a list is encountered
     * in the middle of the path, CayenneRuntimeException is thrown.
     *
     * <p>Examples:</p>
     * <ul>
     *    <li>Read this object property:<br>
     *    <code>String name = (String)artist.readNestedProperty("name");</code><br><br></li>
     *
     *    <li>Read an object related to this object:<br>
     *    <code>Gallery g = (Gallery)paintingInfo.readNestedProperty("toPainting.toGallery");</code>
     *    <br><br></li>
     *
     *    <li>Read a property of an object related to this object: <br>
     *    <code>String name = (String)painting.readNestedProperty("toArtist.artistName");</code>
     *    <br><br></li>
     *
     *    <li>Read to-many relationship list:<br>
     *    <code>List exhibits = (List)painting.readNestedProperty("toGallery.exhibitArray");</code>
     *    <br><br></li>
     *
     *    <li>Read to-many relationship in the middle of the path <b>(throws exception)</b>:<br>
     *    <code>String name = (String)artist.readNestedProperty("paintingArray.paintingName");</code>
     *   <br><br></li>
     * </ul>
     *
     */
    public Object readNestedProperty(String path) {
        StringTokenizer toks = new StringTokenizer(path, ".");

        Object obj = null;
        CayenneDataObject dataObj = this;
        boolean terminal = false;
        while (toks.hasMoreTokens()) {
            if (terminal) {
                throw new CayenneRuntimeException("Invalid path: " + path);
            }
            String pathComp = toks.nextToken();
            obj = dataObj.readProperty(pathComp);

            // if a null value is returned, 
            // there is still a chance to find a non-persistent property
            // via reflection
            if (obj == null && !values.containsKey(pathComp)) {
                try {
                    obj = PropertyComparator.readProperty(pathComp, dataObj);
                }
                catch (IllegalAccessException e) {
                    throw new CayenneRuntimeException(
                        "Error reading property '" + pathComp + "'.",
                        e);
                }
                catch (InvocationTargetException e) {
                    throw new CayenneRuntimeException(
                        "Error reading property '" + pathComp + "'.",
                        e);
                }
                catch (NoSuchMethodException e) {
                    // ignoring, no such property exists
                }
            }

            if (obj == null) {
                return null;
            }
            else if (obj instanceof CayenneDataObject) {
                dataObj = (CayenneDataObject) obj;
            }
            else {
                terminal = true;
            }
        }

        return obj;
    }

    /**
     * Attempts to initialize object with data from cache or from the database,
     * if this object is a "fault", i.e. not fully resolved.
     * 
     * @since 1.1
     */
    public void resolveFault() {
        if (getPersistenceState() != PersistenceState.HOLLOW || dataContext == null) {
            return;
        }
        
        dataContext.getObjectStore().resolveFault(this);
    }

    protected Object readProperty(String propName) {
        resolveFault();

        Object object = readPropertyDirectly(propName);

        // must resolve faults immediately
        if (object instanceof RelationshipFault) {
            // for now assume we just have to-one faults...
            // after all to-many are represented by ToManyList
            object = ((RelationshipFault) object).resolveToOne();
            writePropertyDirectly(propName, object);
        }

        return object;
    }

    public Object readPropertyDirectly(String propName) {
        return values.get(propName);
    }

    protected void writeProperty(String propName, Object val) {
        resolveFault();

        // 1. retain object snapshot to allow clean changes tracking
        // 2. change object state
        if (persistenceState == PersistenceState.COMMITTED) {
            persistenceState = PersistenceState.MODIFIED;
            dataContext.getObjectStore().retainSnapshot(this);
        }
        // else....
        // other persistence states can't be changed to MODIFIED

        writePropertyDirectly(propName, val);
    }

    public void writePropertyDirectly(String propName, Object val) {
        values.put(propName, val);
    }

    /**
     * @deprecated Since 1.0.1 this method is no longer needed, since "readProperty(String)" 
     * supports to-one dependent targets.
     */
    public DataObject readToOneDependentTarget(String relName) {
        return (DataObject) readProperty(relName);
    }

    public void removeToManyTarget(String relName, DataObject val, boolean setReverse) {
        ObjRelationship relationship = this.getRelationshipNamed(relName);
        //Only delete the internal object if we should "setReverse" (or rather, if we aren't not setting the reverse).
        //This kind of doubles up the meaning of that flag, so we may need to add another?
        if (relationship.isFlattened() && setReverse) {
            if (relationship.isReadOnly()) {
                throw new CayenneRuntimeException(
                    "Cannot modify (remove from) the read-only relationship " + relName);
            }
            //Handle removing from a flattened relationship
            dataContext.registerFlattenedRelationshipDelete(this, relationship, val);
        }

        //Now do the rest of the normal handling (regardless of whether it was flattened or not)
        List relList = (List) readProperty(relName);
        relList.remove(val);
        if (persistenceState == PersistenceState.COMMITTED) {
            persistenceState = PersistenceState.MODIFIED;
        }

        if (val != null && setReverse) {
            unsetReverseRelationship(relName, val);
        }
    }

    public void addToManyTarget(String relName, DataObject val, boolean setReverse) {
        if ((val != null) && (dataContext != val.getDataContext())) {
            throw new CayenneRuntimeException(
                "Cannot add object to relationship "
                    + relName
                    + " because it is in a different DataContext");
        }
        ObjRelationship relationship = this.getRelationshipNamed(relName);
        if (relationship == null) {
            throw new CayenneRuntimeException(
                "Cannot add object to relationship "
                    + relName
                    + " because there is no relationship by that name");
        }
        //Only create the internal object if we should "setReverse" (or rather, if we aren't not setting the reverse).
        //This kind of doubles up the meaning of that flag, so we may need to add another?
        if (relationship.isFlattened() && setReverse) {
            if (relationship.isReadOnly()) {
                throw new CayenneRuntimeException(
                    "Cannot modify (add to) the read-only relationship " + relName);
            }
            //Handle adding to a flattened relationship
            dataContext.registerFlattenedRelationshipInsert(this, relationship, val);
        }

        //Now do the rest of the normal handling (regardless of whether it was flattened or not)
        List relList = (List) readProperty(relName);
        relList.add(val);
        if (persistenceState == PersistenceState.COMMITTED) {
            persistenceState = PersistenceState.MODIFIED;
        }

        if (val != null && setReverse)
            setReverseRelationship(relName, val);
    }

    /**
     * @deprecated Since 1.0.1 this method is no longer needed, since 
     * "setToOneTarget(String, DataObject, boolean)" supports dependent targets 
     * as well.
     */
    public void setToOneDependentTarget(String relName, DataObject val) {
        setToOneTarget(relName, val, true);
    }

    public void setToOneTarget(
        String relationshipName,
        DataObject value,
        boolean setReverse) {
        if ((value != null) && (dataContext != value.getDataContext())) {
            throw new CayenneRuntimeException(
                "Cannot set object as destination of relationship "
                    + relationshipName
                    + " because it is in a different DataContext");
        }

        Object oldTarget = readPropertyDirectly(relationshipName);
        if (oldTarget == value) {
            return;
        }

        ObjRelationship relationship = this.getRelationshipNamed(relationshipName);
        if (relationship.isFlattened()) {
            if (relationship.isReadOnly()) {
                throw new CayenneRuntimeException(
                    "Cannot modify the read-only flattened relationship "
                        + relationshipName);
            }

            // Handle adding to a flattened relationship
            dataContext.registerFlattenedRelationshipInsert(this, relationship, value);
        }

        if (setReverse) {
            // unset old reverse relationship
            if (oldTarget instanceof DataObject) {
                unsetReverseRelationship(relationshipName, (DataObject) oldTarget);
            }

            // set new reverse relationship
            if (value != null) {
                setReverseRelationship(relationshipName, value);
            }
        }

        writeProperty(relationshipName, value);
    }

    private ObjRelationship getRelationshipNamed(String relName) {
        return (ObjRelationship) dataContext
            .getEntityResolver()
            .lookupObjEntity(this)
            .getRelationship(relName);
    }

    /**
     * Initializes reverse relationship from object <code>val</code>
     * to this object.
     *
     * @param relName name of relationship from this object
     * to <code>val</code>.
     */
    protected void setReverseRelationship(String relName, DataObject val) {
        ObjRelationship rel =
            (ObjRelationship) dataContext
                .getEntityResolver()
                .lookupObjEntity(objectId.getObjClass())
                .getRelationship(relName);
        ObjRelationship revRel = rel.getReverseRelationship();
        if (revRel != null) {
            if (revRel.isToMany())
                val.addToManyTarget(revRel.getName(), this, false);
            else
                val.setToOneTarget(revRel.getName(), this, false);
        }
    }

    /** 
     * Removes current object from reverse relationship of object
     * <code>val</code> to this object.
     */
    protected void unsetReverseRelationship(String relName, DataObject val) {
        Class aClass = objectId.getObjClass();
        EntityResolver resolver = dataContext.getEntityResolver();
        ObjEntity entity = resolver.lookupObjEntity(aClass);

        if (entity == null) {
            String className = (aClass != null) ? aClass.getName() : "<null>";
            throw new IllegalStateException(
                "DataObject's class is unmapped: " + className);
        }

        ObjRelationship rel = (ObjRelationship) entity.getRelationship(relName);
        ObjRelationship revRel = rel.getReverseRelationship();
        if (revRel != null) {
            if (revRel.isToMany())
                val.removeToManyTarget(revRel.getName(), this, false);
            else
                val.setToOneTarget(revRel.getName(), null, false);
        }
    }

    /**
     * @deprecated Since 1.1 use 
     * getDataContext().getObjectStore().getSnapshot(this.getObjectId(), getDataContext())
     */
    public Map getCommittedSnapshot() {
        return dataContext.getObjectStore().getSnapshot(getObjectId(), dataContext);
    }

    /**
     * @deprecated Since 1.1 use getDataContext().currentSnapshot(this)
     */
    public Map getCurrentSnapshot() {
        return dataContext.currentSnapshot(this);
    }

    /** A variation of  "toString" method, that may be more efficient in some cases.
     *  For example when printing a list of objects into the same String. */
    public StringBuffer toStringBuffer(StringBuffer buf, boolean fullDesc) {
        // log all properties
        buf.append('{');

        if (fullDesc)
            appendProperties(buf);

        buf
            .append("<oid: ")
            .append(objectId)
            .append("; state: ")
            .append(PersistenceState.persistenceStateName(persistenceState))
            .append(">}\n");
        return buf;
    }

    protected void appendProperties(StringBuffer buf) {
        buf.append("[");
        Iterator it = values.keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            buf.append('\t').append(key).append(" => ");
            Object val = values.get(key);

            if (val instanceof CayenneDataObject) {
                ((CayenneDataObject) val).toStringBuffer(buf, false);
            }
            else if (val instanceof List) {
                buf.append('(').append(val.getClass().getName()).append(')');
            }
            else
                buf.append(val);

            buf.append('\n');
        }

        buf.append("]");
    }

    public String toString() {
        return toStringBuffer(new StringBuffer(), true).toString();
    }

    /**
     * Default implementation does nothing.
     *
     * @see org.objectstyle.cayenne.DataObject#fetchFinished()
     */
    public void fetchFinished() {
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(persistenceState);

        switch (persistenceState) {
            //New, modified or transient or deleted - write the whole shebang
            //The other states (committed, hollow) all need just ObjectId
            case PersistenceState.TRANSIENT :
            case PersistenceState.NEW :
            case PersistenceState.MODIFIED :
            case PersistenceState.DELETED :
                out.writeObject(values);
                break;
        }

        out.writeObject(objectId);
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        this.persistenceState = in.readInt();

        switch (persistenceState) {
            case PersistenceState.TRANSIENT :
            case PersistenceState.NEW :
            case PersistenceState.MODIFIED :
            case PersistenceState.DELETED :
                values = (Map) in.readObject();
                break;
            case PersistenceState.COMMITTED :
            case PersistenceState.HOLLOW :
                this.persistenceState = PersistenceState.HOLLOW;
                //props will be populated when required (readProperty called)
                values = new HashMap();
                break;
        }

        this.objectId = (ObjectId) in.readObject();

        // DataContext will be set *IF* the DataContext it came from is also
        // deserialized.  Setting of DataContext is handled by the DataContext itself
    }

    /**
     * Returns a version of a DataRow snapshot that was used to 
     * create this object.
     * 
     * @since 1.1
     */
    public long getSnapshotVersion() {
        return snapshotVersion;
    }

    /**
     * @since 1.1
     */
    public void setSnapshotVersion(long snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }
}
