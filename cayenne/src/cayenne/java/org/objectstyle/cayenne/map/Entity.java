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
package org.objectstyle.cayenne.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionException;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.util.CayenneMap;

/** 
 * An Entity is an abstract descriptor for an entity mapping concept.
 * Entity can represent either a descriptor of database table or
 * a persistent object. 
 * 
 * @author Andrei Adamchik 
 */
public abstract class Entity extends MapObject {
    private static Logger logObj = Logger.getLogger(Entity.class);

    public static final String PATH_SEPARATOR = ".";

    protected CayenneMap attributes = new CayenneMap(this);
    protected CayenneMap relationships = new CayenneMap(this);
    protected CayenneMap queries = new CayenneMap(this);

    /**
     * @return parent DataMap of this entity.
     */
    public DataMap getDataMap() {
        return (DataMap) getParent();
    }

    /**
     * Sets parent DataMap of this entity.
     */
    public void setDataMap(DataMap dataMap) {
        setParent(dataMap);
    }

    /**
     * Returns a named query associated with this entity.
     */
    public SelectQuery getQuery(String queryName) {
        return (SelectQuery) queries.get(queryName);
    }

    /**
     * Creates a named association of a SelectQuery with this entity. Throws
     * IllegalArgumentException if query root can not be resolved to this
     * entity.
     */
    public void addQuery(String queryName, SelectQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Attempt to insert null query.");
        }

        if (queryName == null) {
            throw new IllegalArgumentException("Attempt to insert query with null name.");
        }

        // check if this is the right query
        validateQueryRoot(query);
        queries.put(queryName, query);
    }
    
    /**
     * Removes a named query from this Entity.
     */
    public void removeQuery(String queryName) {
    	queries.remove(queryName);
    }
    
    public void clearQueries() {
        queries.clear();
    }

    /**
     * Helper method that checks that a Query belongs to this entity by
     * validating query root object. 
     * 
     * @throws IllegalArgumentException if query does not belong to this entity.
     */
    protected abstract void validateQueryRoot(Query query)
        throws IllegalArgumentException;

    /** 
     * Returns attribute with name <code>attrName</code>.
     * Will return null if no attribute with this name exists. 
     */
    public Attribute getAttribute(String attrName) {
        return (Attribute) attributes.get(attrName);
    }

    /** 
     * Adds new attribute to the entity. If attribute has no name,
     * IllegalArgumentException is thrown.
     * 
     * Also sets <code>attr</code>'s entity to be this entity. 
     */
    public void addAttribute(Attribute attr) {
        if (attr.getName() == null) {
            throw new IllegalArgumentException("Attempt to insert unnamed attribute.");
        }

        attributes.put(attr.getName(), attr);
    }

    /** Removes an attribute named <code>attrName</code>.*/
    public void removeAttribute(String attrName) {
        attributes.remove(attrName);
    }

    public void clearAttributes() {
        attributes.clear();
    }

    /** 
     * Returns relationship with name <code>relName</code>.
     * Will return null if no relationship with this name 
     * exists in the entity. 
     */
    public Relationship getRelationship(String relName) {
        return (Relationship) relationships.get(relName);
    }

    /** Adds new relationship to the entity. */
    public void addRelationship(Relationship rel) {
        relationships.put(rel.getName(), rel);
    }

    /** Removes a relationship named <code>attrName</code>.*/
    public void removeRelationship(String relName) {
        relationships.remove(relName);
    }

    public void clearRelationships() {
        relationships.clear();
    }

    public Map getRelationshipMap() {
        return Collections.unmodifiableMap(relationships);
    }

    /** Returns a list of Relationship's that exist in this entity. */
    public List getRelationshipList() {
        List list = new ArrayList();
        Iterator it = relationships.keySet().iterator();
        while (it.hasNext()) {
            list.add(relationships.get(it.next()));
        }

        return list;
    }

    /** Returns entity attributes as an unmodifiable map. */
    public Map getAttributeMap() {
        return Collections.unmodifiableMap(attributes);
    }

    /** Returns entity attributes as a list. */
    public List getAttributeList() {
        List list = new ArrayList();
        Iterator it = attributes.keySet().iterator();
        while (it.hasNext()) {
            list.add(attributes.get(it.next()));
        }

        return list;
    }

    /** 
     * Processes expression <code>objPathExp</code> and returns an Iterator
     * of path components that contains a sequence of Attributes and Relationships.
     * Note that if path is invalid and can not be resolved from this entity,
     * this method will still return an Iterator, but an attempt to read the first
     * invalid path component will result in ExpressionException.
     *
     * @see org.objectstyle.cayenne.exp.Expression#OBJ_PATH for definition of OBJ_PATH.
     *
     * @throws org.objectstyle.cayenne.exp.ExpressionException Exception is thrown if
     * <code>objPathExp</code> is not of type OBJ_PATH
     */
    public Iterator resolvePathComponents(Expression pathExp)
        throws ExpressionException {
        if (pathExp.getType() != Expression.OBJ_PATH
            && pathExp.getType() != Expression.DB_PATH) {
            StringBuffer msg = new StringBuffer();
            msg
                .append("Invalid expression type: '")
                .append(pathExp.getType())
                .append("' ('")
                .append(Expression.OBJ_PATH)
                .append(" or ")
                .append(Expression.DB_PATH)
                .append("' is expected).");

            throw new ExpressionException(msg.toString());
        }

        return new PathIterator(this, (String) pathExp.getOperand(0));
    }

    // Used to return an iterator to callers of 'resolvePathComponents'
    protected final class PathIterator implements Iterator {
        private StringTokenizer toks;
        private Entity currentEnt;

        PathIterator(Entity ent, String path) {
            this.toks = new StringTokenizer(path, PATH_SEPARATOR);
            this.currentEnt = ent;
        }

        public boolean hasNext() {
            return toks.hasMoreTokens();
        }

        public Object next() {
            String pathComp = toks.nextToken();

            // see if this is an attribute
            Attribute attr = currentEnt.getAttribute(pathComp);
            if (attr != null) {
                // do a sanity check...
                if (toks.hasMoreTokens()) {
                    throw new ExpressionException(
                        "Attribute must be the last component of the path: '"
                            + pathComp
                            + "'.");
                }

                return attr;
            }

            Relationship rel = currentEnt.getRelationship(pathComp);
            if (rel != null) {
                currentEnt = rel.getTargetEntity();
                return rel;
            }

            // build error message
            StringBuffer buf = new StringBuffer();
            buf
                .append("Can't resolve path component: [")
                .append(currentEnt.getName())
                .append('.')
                .append(pathComp)
                .append("].");
            throw new ExpressionException(buf.toString());
        }

        public void remove() {
            throw new UnsupportedOperationException("'remove' operation is not supported.");
        }
    }
}
