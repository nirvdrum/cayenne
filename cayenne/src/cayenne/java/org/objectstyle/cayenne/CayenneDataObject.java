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
package org.objectstyle.cayenne;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * A CayenneDataObject is a default implementation of DataObject interface.
 * It is normally used as a superclass of Cayenne persistent objects. 
 * 
 * @author Andrei Adamchik
 */
public class CayenneDataObject implements DataObject {
    static Logger logObj = Logger.getLogger(CayenneDataObject.class.getName());

    // used for dependent to one relationships
    // to indicate that destination relationship was fetched and is null
    private static final CayenneDataObject nullValue = new CayenneDataObject();

    /** 
     * Returns String label for persistence state. 
     * Used for debugging. 
     */
    public static String persistenceStateString(int persistenceState) {
        switch (persistenceState) {
            case PersistenceState.TRANSIENT :
                return "transient";
            case PersistenceState.NEW :
                return "new";
            case PersistenceState.MODIFIED :
                return "modified";
            case PersistenceState.COMMITTED :
                return "committed";
            case PersistenceState.HOLLOW :
                return "hollow";
            case PersistenceState.DELETED :
                return "deleted";
            default :
                return "unknown";
        }
    }

    protected ObjectId objectId;
    protected transient int persistenceState = PersistenceState.TRANSIENT;
    protected transient DataContext dataContext;
    protected HashMap props = new HashMap();

    /** Returns a data context this object is registered with, or null
     * if this object has no associated DataContext */
    public DataContext getDataContext() {
        return dataContext;
    }

    public void setDataContext(DataContext ctxt) {
        dataContext = ctxt;
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

    public void setPersistenceState(int newState) {
        persistenceState = newState;
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

            if (obj == null) {
                return null;
            } else if (obj instanceof CayenneDataObject) {
                dataObj = (CayenneDataObject) obj;
            } else {
                terminal = true;
            }
        }

        return obj;
    }

    protected Object readProperty(String propName) {
        if (persistenceState == PersistenceState.HOLLOW) {
            dataContext.refetchObject(objectId);
        }

        return readPropertyDirectly(propName);
    }

    public Object readPropertyDirectly(String propName) {
        return props.get(propName);
    }

    protected void writeProperty(String propName, Object val) {
        if (persistenceState == PersistenceState.COMMITTED) {
            persistenceState = PersistenceState.MODIFIED;
        }

        writePropertyDirectly(propName, val);
    }

    public void writePropertyDirectly(String propName, Object val) {
        props.put(propName, val);
    }

    public DataObject readToOneDependentTarget(String relName) {
        Object toOneTarget = readProperty(relName);

        // known to be NULL
        if (toOneTarget == nullValue) {
            return null;
        }

        // known to be NOT NULL
        if (toOneTarget != null) {
            return (DataObject) toOneTarget;
        }

        // need to fetch
		SelectQuery sel = QueryHelper.selectRelationshipObjects(dataContext, this, relName);
        List results = dataContext.performQuery(sel);

        // unexpected
        if (results.size() > 1) {
            throw new CayenneRuntimeException(
                "error retrieving 'to one' target, found " + results.size());
        }

        // null target
        if (results.size() == 0) {
            writePropertyDirectly(relName, nullValue);
            return null;
        }

        // found a valid object

        DataObject dobj = (DataObject) results.get(0);
        writePropertyDirectly(relName, dobj);
        return dobj;
    }

    public void removeToManyTarget(String relName, DataObject val, boolean setReverse) {
		ObjRelationship relationship = this.getRelationshipNamed(relName);
		//Only delete the internal object if we should "setReverse" (or rather, if we aren't not setting the reverse).
		//This kind of doubles up the meaning of that flag, so we may need to add another?
		if (relationship.isFlattened() && setReverse) {
			if (relationship.isReadOnly()) {
				throw new CayenneRuntimeException("Cannot modify (remove from) the read-only relationship " + relName);
			}
			//Handle removing from a flattened relationship
			dataContext.registerFlattenedRelationshipDelete(this, relName, val);
		}
		
		//Now do the rest of the normal handling (regardless of whether it was flattened or not)
        List relList = (List) readProperty(relName);
        relList.remove(val);

        if (val != null && setReverse) {
            unsetReverseRelationship(relName, val);
        }
    }

    public void addToManyTarget(String relName, DataObject val, boolean setReverse) {
		ObjRelationship relationship = this.getRelationshipNamed(relName);
		//Only create the internal object if we should "setReverse" (or rather, if we aren't not setting the reverse).
		//This kind of doubles up the meaning of that flag, so we may need to add another?
		if (relationship.isFlattened() && setReverse) {
			if (relationship.isReadOnly()) {
				throw new CayenneRuntimeException("Cannot modify (add to) the read-only relationship " + relName);
			}
			//Handle adding to a flattened relationship
			dataContext.registerFlattenedRelationshipInsert(this, relName, val);
		}
		
		//Now do the rest of the normal handling (regardless of whether it was flattened or not)
        List relList = (List) readProperty(relName);
        relList.add(val);

        if (val != null && setReverse)
            setReverseRelationship(relName, val);
    }

    public void setToOneDependentTarget(String relName, DataObject val) {
        if (val == null)
            val = nullValue;

        setToOneTarget(relName, val, true);
    }

    public void setToOneTarget(String relName, DataObject val, boolean setReverse) {
        DataObject oldTarget = (DataObject) readPropertyDirectly(relName);
        if (oldTarget == val) {
            return;
        }

        if (setReverse) {
            // unset old reverse relationship
            if (oldTarget != null)
                unsetReverseRelationship(relName, oldTarget);

            // set new reverse relationship
            if (val != null)
                setReverseRelationship(relName, val);
        }

        writeProperty(relName, val);
    }
    
	private ObjRelationship getRelationshipNamed(String relName) {
		return (ObjRelationship) dataContext.getEntityResolver().lookupObjEntity(this.getClass()).getRelationship(relName);
	}

    /** 
     * Initializes reverse relationship from object <code>val</code> 
     * to this object.
     * 
     * @param relName name of relationship from this object 
     * to <code>val</code>. 
     */
    protected void setReverseRelationship(String relName, DataObject val) {
        ObjRelationship rel=(ObjRelationship) dataContext.getEntityResolver().lookupObjEntity(objectId.getObjEntityName()).getRelationship(relName);
        ObjRelationship revRel = rel.getReverseRelationship();
        if (revRel != null) {
            if (revRel.isToMany())
                val.addToManyTarget(revRel.getName(), this, false);
            else
                val.setToOneTarget(revRel.getName(), this, false);
        }
    }

    /** Remove current object from reverse relationship of object <code>val</code> to this object.
      * @param relName name of relationship from this object to <code>val</code>. */
    protected void unsetReverseRelationship(String relName, DataObject val) {
        ObjRelationship rel=(ObjRelationship) dataContext.getEntityResolver().lookupObjEntity(objectId.getObjEntityName()).getRelationship(relName);
        ObjRelationship revRel = rel.getReverseRelationship();
        if (revRel != null) {
            if (revRel.isToMany())
                val.removeToManyTarget(revRel.getName(), this, false);
            else if (revRel.isToDependentEntity())
                val.setToOneTarget(revRel.getName(), nullValue, false);
            else
                val.setToOneTarget(revRel.getName(), null, false);
        }
    }

    public Map getCommittedSnapshot() {
        return dataContext.getObjectStore().getSnapshot(getObjectId());
    }

    public Map getCurrentSnapshot() {
        return dataContext.takeObjectSnapshot(this);
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
            .append(persistenceStateString(persistenceState))
            .append(">}\n");
        return buf;
    }

    protected void appendProperties(StringBuffer buf) {
        buf.append("[");
        Iterator it = props.keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            buf.append('\t').append(key).append(" => ");
            Object val = props.get(key);

            if (val instanceof CayenneDataObject) {
                ((CayenneDataObject) val).toStringBuffer(buf, false);
            } else if (val instanceof List) {
                buf.append('(').append(val.getClass().getName()).append(')');
            } else
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
    public void fetchFinished() {}

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(persistenceState);

        switch (persistenceState) {
            //New, modified or transient - write the whole shebang
            //The other states (committed, hollow, deleted) all need just ObjectId
            case PersistenceState.TRANSIENT :
            case PersistenceState.NEW :
            case PersistenceState.MODIFIED :
                out.writeObject(props);
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
                props = (HashMap) in.readObject();
                break;
            case PersistenceState.COMMITTED :
            case PersistenceState.HOLLOW :
            case PersistenceState.DELETED :
                this.persistenceState = PersistenceState.HOLLOW;
                //props will be populated when required (readProperty called)
                props = new HashMap();
                break;
        }

        this.objectId = (ObjectId) in.readObject();
        // dataContext will be set *IFF* the datacontext it came from is also
        // deserialized.  Setting of datacontext is handled by the datacontext itself
    }
}
