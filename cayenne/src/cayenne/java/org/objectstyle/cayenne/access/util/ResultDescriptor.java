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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.ProcedureParam;

/**
 * Contains information about the ResultSet used to process fetched rows. 
 * ResultDescriptor is initialized by calling various "add*" methods, after that
 * it must be indexed by calling "index".
 * 
 * @author Andrei Adamchik
 */
public class ResultDescriptor {
    private static Logger logObj = Logger.getLogger(ResultDescriptor.class);

    private static final int[] emptyInt = new int[0];

    // indexed data
    protected String[] names;
    protected int[] jdbcTypes;
    protected ExtendedType[] converters;
    protected int[] idIndexes;
    protected int[] outParamIndexes;

    // unindexed data
    protected List dbAttributes = new ArrayList();
    protected List javaTypes = new ArrayList();
    protected ExtendedTypeMap typesMapping;
    protected ObjEntity rootEntity;

    /**
     * Creates and returns a ResultDescritor based on ResultSet metadata.
     */
    public static ResultDescriptor createDescriptor(
        ResultSet resultSet,
        ExtendedTypeMap typeConverters) {
        ResultDescriptor descriptor = new ResultDescriptor(typeConverters);
        try {
            ResultSetMetaData md = resultSet.getMetaData();
            int len = md.getColumnCount();
            if (len == 0) {
                throw new CayenneRuntimeException("No columns in ResultSet.");
            }

            for (int i = 0; i < len; i++) {

                // figure out column name
                int pos = i + 1;
                String name = md.getColumnLabel(pos);
                int sqlType = md.getColumnType(pos);
                int precision = md.getScale(pos);
                int length = md.getColumnDisplaySize(pos);
                if (name == null || name.length() == 0) {
                    name = md.getColumnName(i + 1);

                    if (name == null || name.length() == 0) {
                        name = "column_" + (i + 1);
                    }
                }

                DbAttribute desc = new DbAttribute();
                desc.setName(name);
                desc.setType(md.getColumnType(i + 1));
                
                descriptor.addDbAttribute(desc);
                descriptor.addJavaType(TypesMapping.getJavaBySqlType(sqlType, length, precision));
            }
        } catch (SQLException sqex) {
            throw new CayenneRuntimeException("Error reading metadata.", sqex);
        }

        descriptor.index();
        return descriptor;
    }

    /**
     * Creates and returns a ResultDescriptor for the stored procedure parameters. 
     */
    public static ResultDescriptor createDescriptor(
        Procedure procedure,
        ExtendedTypeMap typeConverters) {
        ResultDescriptor descriptor = new ResultDescriptor(typeConverters);
        Iterator it = procedure.getCallParams().iterator();
        while (it.hasNext()) {
            descriptor.addDbAttribute((DbAttribute) it.next());
        }

        descriptor.index();
        return descriptor;
    }

    public ResultDescriptor(ExtendedTypeMap typesMapping) {
        this(typesMapping, null);
    }

    public ResultDescriptor(
        ExtendedTypeMap typesMapping,
        ObjEntity rootEntity) {
        this.typesMapping = typesMapping;
        this.rootEntity = rootEntity;
    }

    public void addDbAttributes(Collection dbAttributes) {
        this.dbAttributes.addAll(dbAttributes);
    }

    public void addDbAttribute(DbAttribute attr) {
        this.dbAttributes.add(attr);
    }

    public void addJavaTypes(Collection javaTypes) {
        this.javaTypes.addAll(javaTypes);
    }

    public void addJavaType(String javaType) {
        this.javaTypes.add(javaType);
    }

    public void index() {

        // assert validity
        if (javaTypes.size() > 0 && javaTypes.size() != dbAttributes.size()) {
            throw new IllegalArgumentException("DbAttributes and Java type arrays must have the same size.");
        }

        // init various things
        int resultWidth = dbAttributes.size();
        int idWidth = 0;
        int outWidth = 0;
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

            // check if this is a stored procedure OUT parameter
            if (attr instanceof ProcedureParam) {
                if (((ProcedureParam) attr).isOutParam()) {
                    outWidth++;
                }
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

            if (name == null) {
                name = attr.getName();
            }

            names[i] = name;
        }

        if (idWidth == 0) {
            this.idIndexes = emptyInt;
        } else {
            this.idIndexes = new int[idWidth];
            for (int i = 0, j = 0; i < resultWidth; i++) {
                DbAttribute attr = (DbAttribute) dbAttributes.get(i);
                jdbcTypes[i] = attr.getType();

                if (attr.isPrimaryKey()) {
                    idIndexes[j++] = i;
                }
            }
        }

        if (outWidth == 0) {
            this.outParamIndexes = emptyInt;
        } else {
            this.outParamIndexes = new int[outWidth];
            for (int i = 0, j = 0; i < resultWidth; i++) {
                DbAttribute attr = (DbAttribute) dbAttributes.get(i);
                jdbcTypes[i] = attr.getType();

                if (attr instanceof ProcedureParam) {
                    if (((ProcedureParam) attr).isOutParam()) {
                        outParamIndexes[j++] = i;
                    }
                }
            }
        }

        // initialize type converters, must do after everything else,
        // since this may depend on some of the indexed data
        if (javaTypes.size() > 0) {
            initConvertersFromJavaTypes();
        } else if (rootEntity != null) {
            initConvertersFromMapping();
        } else {
            initDefaultConverters();
        }
    }

    protected void initConvertersFromJavaTypes() {
        int resultWidth = dbAttributes.size();
        this.converters = new ExtendedType[resultWidth];

        for (int i = 0; i < resultWidth; i++) {
            converters[i] =
                typesMapping.getRegisteredType((String) javaTypes.get(i));
        }
    }

    protected void initDefaultConverters() {
        logObj.debug(
            "Creating converters using default JDBC->Java type mapping.");

        int resultWidth = dbAttributes.size();
        this.converters = new ExtendedType[resultWidth];

        for (int i = 0; i < resultWidth; i++) {
            String javaType = TypesMapping.getJavaBySqlType(jdbcTypes[i]);
            converters[i] = typesMapping.getRegisteredType(javaType);
        }
    }

    protected void initConvertersFromMapping() {
        logObj.debug("Creating converters using ObjAttributes.");

        // assert that we have all the data
        if (dbAttributes.size() == 0) {
            throw new IllegalArgumentException("DbAttributes list is empty.");
        }

        if (rootEntity == null) {
            throw new IllegalArgumentException("Root ObjEntity is null.");
        }

        int resultWidth = dbAttributes.size();
        this.converters = new ExtendedType[resultWidth];

        for (int i = 0; i < resultWidth; i++) {
            String javaType = null;
            DbAttribute attr = (DbAttribute) dbAttributes.get(i);
            ObjAttribute objAttr = rootEntity.getAttributeForDbAttribute(attr);
            if (objAttr != null) {
                javaType = objAttr.getType();
            } else {
                javaType = TypesMapping.getJavaBySqlType(attr.getType());
            }

            converters[i] = typesMapping.getRegisteredType(javaType);
        }
    }

    public ExtendedType[] getConverters() {
        return converters;
    }

    public int[] getIdIndexes() {
        return idIndexes;
    }

    public int[] getJdbcTypes() {
        return jdbcTypes;
    }

    public String[] getNames() {
        return names;
    }

    /**
     * Returns a count of columns in the result.
     */
    public int getResultWidth() {
        return dbAttributes.size();
    }

    public int[] getOutParamIndexes() {
        return outParamIndexes;
    }
}
