package org.objectstyle.cayenne.map;
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

import java.util.*;

import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionException;


/** Superclass of metadata classes. */
public abstract class Entity {
    public static final String PATH_SEPARATOR = ".";

    protected String name;
    protected HashMap attributes =  new HashMap();
    protected HashMap relationships =  new HashMap();

    /** Returns entity name. */
    public String getName() {
        return name;
    }


    /** Sets entity name. */
    public void setName(String name) {
        this.name = name;
    }


    /** Returns attribute with name <code>attrName</code>.
    * Will return null if no attribute with this name exists in the entity. */
    public Attribute getAttribute(String attrName) {
        return (Attribute)attributes.get(attrName);
    }


    /** Adds new attribute to the entity.
     * Also sets <code>attr</code> entity to be this entity. */
    public void addAttribute(Attribute attr) {
        attributes.put(attr.getName(), attr);

        // set attribute's entity to be "this" entity
        attr.setEntity(this);
    }


    /** Removes an attribute named <code>attrName</code>.*/
    public void removeAttribute(String attrName) {
        attributes.remove(attrName);
    }


    /** Returns relationship with name <code>relName</code>.
    * Will return null if no relationship with this name exists in the entity. */
    public Relationship getRelationship(String relName) {
        return (Relationship)relationships.get(relName);
    }


    /** Adds new relationship to the entity. */
    public void addRelationship(Relationship rel) {
        relationships.put(rel.getName(), rel);

        // set rel's source entity to be "this" entity
        rel.setSourceEntity(this);
    }


    /** Removes a relationship named <code>attrName</code>.*/
    public void removeRelationship(String relName) {
        relationships.remove(relName);
    }


    public Map getRelationshipMap() {
        return Collections.unmodifiableMap(relationships);
    }


    /** Returns a list of Relationship's that exist in this entity. */
    public List getRelationshipList() {
        ArrayList list = new ArrayList();
        Iterator it = relationships.keySet().iterator();
        while(it.hasNext()) {
            list.add(relationships.get(it.next()));
        }

        return list;
    }


    /** Returns the properties of the corresponding data object class */
    public Map getAttributeMap() {
        return Collections.unmodifiableMap(attributes);
    }


    public List getAttributeList() {
        ArrayList list = new ArrayList();
        Iterator it = attributes.keySet().iterator();
        while(it.hasNext()) {
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
    public Iterator resolvePathComponents(Expression objPathExp) throws ExpressionException {
        if(objPathExp.getType() != Expression.OBJ_PATH)
            throw new ExpressionException(
            "Invalid expression type: '" + objPathExp.getType()
            + "' ('" + Expression.OBJ_PATH + "' is expected)."
            );

        return new PathIterator((String)objPathExp.getOperand(0));
    }


    // Used to return an iterator to callers of 'resolvePathComponents'
    final class PathIterator implements Iterator {
        private StringTokenizer toks;
        private Entity currentEnt;

        PathIterator(String path) {
            toks = new StringTokenizer(path, PATH_SEPARATOR);
            currentEnt = Entity.this;
        }


        public boolean hasNext() {
            return toks.hasMoreTokens();
        }


        public Object next() {
            String pathComp = toks.nextToken();

            // see if this is an attribute
            Attribute attr = currentEnt.getAttribute(pathComp);
            if(attr != null) {
                // do a sanity check...
                if(toks.hasMoreTokens())
                    throw new ExpressionException("Attribute must be the last component of the path: '" + pathComp + "'.");

                return attr;
            }

            Relationship rel = currentEnt.getRelationship(pathComp);
            if(rel != null) {
                currentEnt = rel.getTargetEntity();
                return rel;
            }


            throw new ExpressionException("Can't resolve path component: '" + pathComp + "'.");
        }

        public void remove() {
            throw new UnsupportedOperationException("'remove' operation is not supported.");
        }
    }

  HashMap getAttributes() {
    return attributes;
  }

  HashMap getRelationships() {
    return relationships;
  }
}
