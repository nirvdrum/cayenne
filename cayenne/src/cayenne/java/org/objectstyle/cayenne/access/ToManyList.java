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

package org.objectstyle.cayenne.access;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.objectstyle.cayenne.ObjectId;

/**
 * Special List implementation to hold "to many" relationship data.
 *
 * <p>To retain guaranteed immutable reference to the list, 
 * application might use "List.toArray()" method.</p>
 * 
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 * 
 * @author Andrei Adamchik
 */
public class ToManyList implements List, Serializable {
    private ObjectId srcObjectId;
    private String relName;
    private List objectList;
    private ToManyListDataSource listDataSource;
    
    /** Creates ToManyList. */
    public ToManyList(ToManyListDataSource listDataSource, ObjectId srcObjectId, String relName) {
        this.listDataSource = listDataSource;
        this.srcObjectId = srcObjectId;
        this.relName = relName;
    }
    
    public ToManyListDataSource getListDataSource() {
        return listDataSource;
    }
    
    public ObjectId getSrcObjectId() {
        return srcObjectId;
    }
    
    public String getRelName() {
        return relName;
    }
    
    public boolean needsFetch() {
        return objectList == null;
    }
    
    /**
     * Will force refresh on the next access.
     */
    public void invalidateObjectList() {
		setObjectList(null);
    }
    
    public void setObjectList(List objectList) {
        this.objectList = objectList;
    }
    
    private List getObjectList() {
        if(needsFetch()) {
            listDataSource.updateListData(this);
        }
        
        return objectList;
    }
    
    
    public boolean add(Object obj) {
        return getObjectList().add(obj);
    }
    
    
    public void add (int index, Object element) {
        getObjectList().add(index, element);
    }
    
    
    public boolean addAll(Collection c) {
        return getObjectList().addAll(c);
    }
    
    
    public boolean addAll(int index, Collection c) {
        return getObjectList().addAll(index, c);
    }
    
    
    public void clear() {
        getObjectList().clear();
    }
    
    
    public boolean contains(Object o) {
        return getObjectList().contains(o);
    }
    
    
    public boolean containsAll(Collection c) {
        return getObjectList().containsAll(c);
    }
    
    
    public boolean equals(Object o) {
        if(o == null)
            return false;
        
        if(o.getClass() != ToManyList.class)
            return false;
        
        return getObjectList().equals(((ToManyList)o).getObjectList());
    }
    
    
    public int hashCode() {
        return 15 + getObjectList().hashCode();
    }
    
    
    public Object get(int index) {
        return getObjectList().get(index);
    }
    
    
    public int indexOf(Object o) {
        return getObjectList().indexOf(o);
    }
    
    
    public boolean isEmpty() {
        return getObjectList().isEmpty();
    }
    
    
    public Iterator iterator() {
        return getObjectList().iterator();
    }
    
    
    public int lastIndexOf(Object o) {
        return getObjectList().lastIndexOf(o);
    }
    
    
    public ListIterator listIterator() {
        return getObjectList().listIterator();
    }
    
    
    public ListIterator listIterator(int index) {
        return getObjectList().listIterator(index);
    }
    
    
    public Object remove(int index) {
        return getObjectList().remove(index);
    }
    
    
    public boolean remove(Object o) {
        return getObjectList().remove(o);
    }
    
    
    public boolean removeAll(Collection c) {
        return getObjectList().removeAll(c);
    }
    
    
    public boolean retainAll(Collection c) {
        return getObjectList().retainAll(c);
    }
    
    
    public Object set(int index, Object element) {
        return getObjectList().set(index, element);
    }
    
    
    public int size() {
        return getObjectList().size();
    }
    
    
    public List subList(int fromIndex, int toIndex) {
        return getObjectList().subList(fromIndex, toIndex);
    }
    
    
    public Object[] toArray() {
        return getObjectList().toArray();
    }
    
    
    public Object[] toArray(Object[] a) {
        return getObjectList().toArray(a);
    }
}
