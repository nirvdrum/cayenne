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

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.util.Util;

/** 
 * An ObjectId is a class that uniquely identifies a 
 * persistent object.
 * 
 * <p>Each data object has an id uniquely identifying it. 
 * ObjectId concept corresponds to a primary key concept 
 * in the relational world. Such id is needed to 
 * implement object uniquing and other persistence layer functions. 
 * </p>
 * 
 * @author Andrei Adamchik
 */
public class ObjectId implements Serializable {
	// Keys: DbAttribute names
	// Values: database values of the corresponding attribute
	protected Map objectIdKeys;
	protected Class objectClass;
	
	/**
	 * Convenience constructor for entities that have a 
	 * single Integer as their id.
	 */
	public ObjectId(Class objClass, String keyName, int id) {
		this(objClass, keyName, new Integer(id));
	}

	/**
	 * Convenience constructor for entities that have a 
	 * single column as their id.
	 */
	public ObjectId(Class objClass,  String keyName, Object id) {
		this.objectClass = objClass;
		this.setIdKeys(Collections.singletonMap(keyName, id));
	}

	/**
	 * Creates a new ObjectId.
	 */
	public ObjectId(Class objClass, Map idKeys) {
		this.objectClass = objClass;
		if (idKeys != null) {
			this.setIdKeys(Collections.unmodifiableMap(idKeys));
		}
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
		// use the class name because two Objectid's should be equal
		// even if their objClass'es were loaded by different class loaders.
		return objectClass.getName().equals(id.objectClass.getName()) &&
				Util.nullSafeEquals(id.objectIdKeys, this.objectIdKeys);
	}

	/**
	 * Returns a map of id components. 
	 * Keys in the map are DbAttribute names, values are database values
	 * of corresponding columns.
	 */
	public Map getIdSnapshot() {
		return objectIdKeys;
	}

    /**
     * Returns a value of id attribute identified by the 
     * name of DbAttribute.
     */
	public Object getValueForAttribute(String attrName) {
		return objectIdKeys.get(attrName);
	}

    /**
     * Always returns <code>false</code>.
     */
	public boolean isTemporary() {
		return false;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer(objectClass.getName());
		if (isTemporary())
			buf.append(" (temp)");
		buf.append(": ");
		if (objectIdKeys != null) {
			Iterator it = objectIdKeys.keySet().iterator();
			while (it.hasNext()) {
				String nextKey = (String) it.next();
				Object value = objectIdKeys.get(nextKey);
				buf.append(" <").append(nextKey).append(": ").append(
					value).append(
					'>');
			}
		}
		return buf.toString();
	}
	
    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
    	int mapHash = (objectIdKeys != null) ? objectIdKeys.hashCode() : 0;
		// use the class name because two Objectid's should be equal
		// even if their objClass'es were loaded by different class loaders.
        return objectClass.getName().hashCode() + mapHash;
    }
    
	/**
	 * Returns the class of object that this ObjectId is acting for
	 * @return Class
	 */
	public Class getObjClass() {
		return objectClass;
	}

}
