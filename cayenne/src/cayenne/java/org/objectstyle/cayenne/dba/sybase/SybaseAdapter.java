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
package org.objectstyle.cayenne.dba.sybase;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.objectstyle.cayenne.access.types.AbstractType;
import org.objectstyle.cayenne.access.types.ByteArrayType;
import org.objectstyle.cayenne.access.types.CharType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;

/** 
 * DbAdapter implementation for <a href="http://www.sybase.com">Sybase RDBMS</a>.
 *
 * @author Andrei Adamchik
 */
public class SybaseAdapter extends JdbcAdapter {

    /**
     * Installs appropriate ExtendedTypes as converters for passing values
     * between JDBC and Java layers.
     */
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        map.registerType(new CharType(true, false));

        // create specially configured ByteArrayType handler
        map.registerType(new ByteArrayType(true, false));

        // address Sybase driver inability to handle java.lang.Short
        map.registerType(new SybaseShortType());
    }

    /** 
     * Creates and returns a primary key generator. 
     * Overrides superclass implementation to return an
     * instance of SybasePkGenerator.
     */
    protected PkGenerator createPkGenerator() {
        return new SybasePkGenerator();
    }
    /**
     *
     */

    public void bindParameter(
        PreparedStatement statement,
        Object object,
        int pos,
        int sqlType,
        int precision)
        throws SQLException, Exception {

        // Sybase driver doesn't like CLOBs and BLOBs as parameters
        if (object == null) {
            if (sqlType == Types.CLOB) {
                sqlType = Types.VARCHAR;
            }
            else if (sqlType == Types.BLOB) {
                sqlType = Types.VARBINARY;
            }
        }

        super.bindParameter(statement, object, pos, sqlType, precision);
    }

    /**
     * Recasts java.lang.Short to java.lang.Integer when 
     * binding values to PreparedStatement to address Sybase
     * JDBC driver limitations.
     */
    static final class SybaseShortType extends AbstractType {

        public String getClassName() {
            return Short.class.getName();
        }

        public Object materializeObject(ResultSet rs, int index, int type)
            throws Exception {
            short s = rs.getShort(index);
            return (rs.wasNull()) ? null : new Short(s);
        }

        public Object materializeObject(CallableStatement st, int index, int type)
            throws Exception {
            short s = st.getShort(index);
            return (st.wasNull()) ? null : new Short(s);
        }

        public void setJdbcObject(
            PreparedStatement st,
            Object val,
            int pos,
            int type,
            int precision)
            throws Exception {

            if (val instanceof Short) {
                val = new Integer(((Short) val).intValue());
            }
            super.setJdbcObject(st, val, pos, type, precision);
        }
    }
}