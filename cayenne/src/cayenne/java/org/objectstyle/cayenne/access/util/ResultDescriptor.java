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
package org.objectstyle.cayenne.access.util;

import java.util.List;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;

/**
 * Contains information about the ResultSet used to process fetched rows.
 * 
 * @author Andrei Adamchik
 */
public class ResultDescriptor {

    // indexed data
    protected String[] names;
    protected int[] jdbcTypes;
    protected ExtendedType[] converters;

    protected int resultWidth;
    protected int[] idIndexes;

    // indicates that reindexing is required
    protected boolean dirty;

    // unindexed data
    protected List dbAttributes;
    protected List explicitJavaTypes;
    protected boolean usingDefaultJavaTypes;
    protected DataNode node;
    protected ObjEntity rootEntity;

    public ResultDescriptor(DataNode node) {
        this.node = node;
        this.dirty = true;
    }

    public void setDbAttributes(List dbAttributes) {
        this.dirty = true;
        this.dbAttributes = dbAttributes;
    }

    public void setRootEntity(ObjEntity rootEntity) {
        this.dirty = true;
        this.rootEntity = rootEntity;
    }

    public void useJavaTypes(List javaTypes) {
        this.dirty = true;
        this.explicitJavaTypes = javaTypes;
    }

    public void useDefaultJavaTypes() {
        this.dirty = true;
        this.usingDefaultJavaTypes = true;
        this.explicitJavaTypes = null;
    }

    public void useJavaTypesFromMapping() {
        this.dirty = true;
        this.usingDefaultJavaTypes = false;
        this.explicitJavaTypes = null;
    }

    public synchronized void index() {
        if (!dirty) {
            return;
        }

        // assert validity
        if (dbAttributes == null) {
            throw new IllegalArgumentException("DbAttributes list is null.");
        }

        if (explicitJavaTypes != null
            && explicitJavaTypes.size() != dbAttributes.size()) {
            throw new IllegalArgumentException("DbAttributes and Java type arrays must have the same size.");
        }

        // init various things
        this.resultWidth = dbAttributes.size();

        int idWidth = 0;
        this.names = new String[resultWidth];
        this.jdbcTypes = new int[resultWidth];
        for (int i = 0; i < resultWidth; i++) {
            DbAttribute attr = (DbAttribute) dbAttributes.get(i);

            // set type
            jdbcTypes[i] = attr.getType();

            // check if this is an ID
            if (attr.isPrimaryKey()) {
                idWidth++;
            }

            // figure out name
            String name = null;
            if (rootEntity != null) {
                ObjAttribute objAttr =
                    rootEntity.getAttributeForDbAttribute(attr);
                if (objAttr != null) {
                    name = objAttr.getDbAttributePath();
                }
            }
            
            if(name == null) {
            	name = attr.getName();
            }
            
            names[i] = name;
        }

        this.idIndexes = new int[idWidth];
        for (int i = 0, j = 0; i < resultWidth; i++, j++) {
            DbAttribute attr = (DbAttribute) dbAttributes.get(i);
            jdbcTypes[i] = attr.getType();

            if (attr.isPrimaryKey()) {
                idIndexes[j] = i;
            }
        }

        // initialize type converters
        if (explicitJavaTypes != null) {
            initConvertersFromJavaTypes();
        } else if (usingDefaultJavaTypes) {
            initDefaultConverters();
        } else {
            initConvertersFromMapping();
        }

        this.dirty = false;
    }

    protected void initConvertersFromJavaTypes() {
    }

    protected void initDefaultConverters() {
    }

    protected void initConvertersFromMapping() {
    }

}
