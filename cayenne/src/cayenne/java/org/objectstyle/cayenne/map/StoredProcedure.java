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
import java.util.List;

/**
 * A mapping descriptor for the database stored procedure.
 * 
 * @author Andrei Adamchik
 */
public class StoredProcedure {
    protected String name;
    protected String schema;
    protected boolean returningRows;
    protected List params = new ArrayList();
    protected List resultAttrs = new ArrayList();

    /**
     * Default constructor for StoredProcedure.
     */
    public StoredProcedure() {
        super();
    }

    /**
     * Creates an instance of StoredProcedure with the specified name and select
     * behaviour.
     */
    public StoredProcedure(String schema, String name, boolean returningRows) {
        this.schema = schema;
        this.name = name;
        this.returningRows = returningRows;
    }

    /**
     * Returns StoredProcedure's name. This is also a database name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the StoredProcedure.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
      * Returns the schema.
      * @return String
      */
    public String getSchema() {
        return schema;
    }

    /**
     * Sets the schema.
     * @param schema The schema to set
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Returns <code>true</code> if the StoredProcedure is expected to return a
     * ResultSet, <code>false</code> otherwise.
     */
    public boolean isReturningRows() {
        return returningRows;
    }

    /**
     * Sets whether the StoredProcedure returns a ResultSet.
     */
    public void setReturningRows(boolean returningRows) {
        this.returningRows = returningRows;
    }

    /**
     * Adds a DbAttribute describing returned ResultSet.
     */
    public void addResultAttr(DbAttribute attr) {
        if (attr == null) {
            throw new IllegalArgumentException("Attempt to add a null DbAttribute.");
        }

        resultAttrs.add(attr);
    }

    public void removeResultAttr(DbAttribute attr) {
        resultAttrs.remove(attr);
    }

    public List getResultAttrs() {
        return resultAttrs;
    }

    public void clearResultAttrs() {
        resultAttrs.clear();
    }

    /**
     * Creates and adds a StoredProcedureParam to the list of parameters.
     */
    public void addParam(String name, int type) {
        addParam(new StoredProcedureParam(name, type)); 
    }
    
    /**
      * Adds a StoredProcedureParam to the list of parameters.
      */
    public void addParam(StoredProcedureParam param) {
        if (param == null) {
            throw new IllegalArgumentException("Attempt to add a null StoredProcedureParam.");
        }

        params.add(param);
        param.setStoredProcedure(this);
    }

    public void removeParam(StoredProcedureParam param) {
        params.remove(param);
    }

    public List getParams() {
        return params;
    }

    public void clearParams() {
        params.clear();
    }
}
