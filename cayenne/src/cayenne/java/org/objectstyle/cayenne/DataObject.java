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

import java.util.Map;

import org.objectstyle.cayenne.access.DataContext;

/** 
 * Defines basic methods for a persistent object in Cayenne.
 * 
 * @author Andrei Adamchik
 */
public interface DataObject extends java.io.Serializable {
    
   /** 
    * Returns a data context this object is registered with, or null
    * if this object has no associated DataContext.
    */
    public DataContext getDataContext();
    
    /** Sets object data context. */
    public void setDataContext(DataContext ctxt);
    
    /** 
     * Returns ObjectId for this data object - piece that
     * uniquely identifies this data object for persistence purposes.
     */
    public ObjectId getObjectId();
    
    /** Sets ObjectId for this data object - piece that uniquely 
     *  identifies this data object for persistence purposes.
     */
    public void setObjectId(ObjectId objectId);
    
    /** Returns current state of this data object.
     *  For valid states look in PersistenceState class.
     */
    public int getPersistenceState();
    
    /** Modifies persistence state of this data object
     *  For valid states look in PersistenceState class.
     */
    public void setPersistenceState(int newState);
    
    
    /** Allows Cayenne framework classes to modify object property values. */
    public void writePropertyDirectly(String propName, Object val);
    
    /** Allows Cayenne framework classes to read object property values. */
    public Object readPropertyDirectly(String propName);
    
    public DataObject readToOneDependentTarget(String relName);

    public void addToManyTarget(String relName, DataObject val, boolean setReverse);

    public void removeToManyTarget(String relName, DataObject val, boolean setReverse);
    
    public void setToOneTarget(String relName, DataObject val, boolean setReverse);
    
    public void setToOneDependentTarget(String relName, DataObject val);

    /** 
     * Returns a snapshot for this object corresponding to the state 
     * of the database when object was last fetched or committed. 
     */ 
    public Map getCommittedSnapshot();
    
    /** Returns a snapshot of object current values. */
    public Map getCurrentSnapshot();
    
    /**
     * Notification method called by DataContext after the object 
     * was read from the database.
     */
    public void fetchFinished();
}
