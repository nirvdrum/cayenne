package org.objectstyle.cayenne.access.types;
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

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

import org.apache.log4j.*;
import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.dba.*;


/** Handles Java types mapped to JDBC types in JDBC sepcification. */
public class DefaultType implements ExtendedType {
    private static Logger logObj = Logger.getLogger(DefaultType.class);

    private static final Map readMethods = new HashMap();
    private static Method readObjectMethod;

    static {
        try {
            Class rsClass = ResultSet.class;
            Class[] paramTypes = new Class[] {Integer.TYPE};
            readMethods.put(TypesMapping.JAVA_LONG, rsClass.getMethod("getLong", paramTypes));
            readMethods.put(TypesMapping.JAVA_BIGDECIMAL, rsClass.getMethod("getBigDecimal", paramTypes));
            readMethods.put(TypesMapping.JAVA_BOOLEAN, rsClass.getMethod("getBoolean", paramTypes));
            readMethods.put(TypesMapping.JAVA_BYTE, rsClass.getMethod("getByte", paramTypes));
            readMethods.put(TypesMapping.JAVA_BYTES, rsClass.getMethod("getBytes", paramTypes));
            readMethods.put(TypesMapping.JAVA_SQLDATE, rsClass.getMethod("getDate", paramTypes));
            readMethods.put(TypesMapping.JAVA_DOUBLE, rsClass.getMethod("getDouble", paramTypes));
            readMethods.put(TypesMapping.JAVA_FLOAT, rsClass.getMethod("getFloat", paramTypes));
            readMethods.put(TypesMapping.JAVA_INTEGER, rsClass.getMethod("getInt", paramTypes));
            readMethods.put(TypesMapping.JAVA_SHORT, rsClass.getMethod("getShort", paramTypes));
            readMethods.put(TypesMapping.JAVA_STRING, rsClass.getMethod("getString", paramTypes));
            readMethods.put(TypesMapping.JAVA_TIME, rsClass.getMethod("getTime", paramTypes));
            readMethods.put(TypesMapping.JAVA_TIMESTAMP, rsClass.getMethod("getTimestamp", paramTypes));

            readObjectMethod = rsClass.getMethod("getObject", paramTypes);
        } catch(Exception ex) {
            throw new CayenneRuntimeException("Error initializing read methods.", ex);
        }
    }

    /** Returns an Iterator of supported default Java classes (as Strings) */
    public static Iterator defaultTypes() {
        return readMethods.keySet().iterator();
    }

    protected String className;
    protected Method readMethod;
    protected Object[] args = new Object[1];


    /** CreatesDefaultType to read objects from ResultSet
      * using "getObject" method. */
    public DefaultType() {
        className = Object.class.getName();
        readMethod = readObjectMethod;
    }

    public DefaultType(String className) {
        this.className = className;
        readMethod = (Method)readMethods.get(className);

        if(readMethod == null)
            throw new CayenneRuntimeException(
                "Unsupported default class: " + className
                + ". If you want a non-standard class to map to JDBC type,"
                + " you will need to implement ExtendedType interface yourself.");
    }

    public String getClassName() {
        return className;
    }

    public Object toJdbcObject(Object val, int type) throws Exception {
        return val;
    }


    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        args[0] = new Integer(index);
        Object val = readMethod.invoke(rs, args);
        return (rs.wasNull()) ? null : val;
    }
}
