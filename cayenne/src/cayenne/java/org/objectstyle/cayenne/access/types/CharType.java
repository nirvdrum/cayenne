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

package org.objectstyle.cayenne.access.types;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.objectstyle.cayenne.CayenneException;

/** 
 * Handles CHAR type for JDBC drivers that don't trim trailing spaces.
 */
public class CharType extends AbstractType {

    private static final int BUF_SIZE = 8 * 1024;

    protected boolean trimmingChars;
    protected boolean usingClobs;

    public CharType(boolean trimingChars, boolean usingClobs) {
        this.trimmingChars = trimingChars;
        this.usingClobs = usingClobs;
    }

    public String getClassName() {
        return String.class.getName();
    }

    /** Return trimmed string. */
    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {

        String val = null;

        // CLOB handling
        if (type == Types.CLOB) {
            val =
                (isUsingClobs())
                    ? readClob(rs.getClob(index))
                    : readCharStream(rs, index);
        }
        else {

            val = rs.getString(index);

            // trim CHAR type
            if (val != null && type == Types.CHAR && isTrimmingChars()) {
                val = val.trim();
            }
        }

        return val;
    }

    /** Return trimmed string. */
    public Object materializeObject(CallableStatement cs, int index, int type)
        throws Exception {

        String val = null;

        // CLOB handling
        if (type == Types.CLOB) {
            if (!isUsingClobs()) {
                throw new CayenneException("Character streams are not supported in stored procedure parameters.");
            }

            val = readClob(cs.getClob(index));
        }
        else {

            val = cs.getString(index);

            // trim CHAR type
            if (val != null && type == Types.CHAR && isTrimmingChars()) {
                val = val.trim();
            }
        }

        return val;
    }

    public void setJdbcObject(
        PreparedStatement st,
        Object val,
        int pos,
        int type,
        int precision)
        throws Exception {

        // if this is a CLOB column, set the value as "String"
        // instead. This should work with most drivers
        if (type == Types.CLOB) {
            st.setString(pos, (String) val);
        }
        else {
            super.setJdbcObject(st, val, pos, type, precision);
        }
    }

    protected String readClob(Clob clob) throws IOException, SQLException {
        if (clob == null) {
            return null;
        }

        // sanity check on size
        if (clob.length() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                "CLOB is too big to be read as String in memory: " + clob.length());
        }

        int size = (int) clob.length();
        if (size == 0) {
            return "";
        }

        int bufSize = (size < BUF_SIZE) ? size : BUF_SIZE;

        Reader in = clob.getCharacterStream();
        return (in != null)
            ? readValueStream(new BufferedReader(in, bufSize), size, bufSize)
            : null;
    }

    protected String readCharStream(ResultSet rs, int index)
        throws IOException, SQLException {
        Reader in = rs.getCharacterStream(index);

        return (in != null) ? readValueStream(in, -1, BUF_SIZE) : null;
    }

    protected String readValueStream(Reader in, int streamSize, int bufSize)
        throws IOException {
        char[] buf = new char[bufSize];
        int read;
        StringWriter out =
            (streamSize > 0) ? new StringWriter(streamSize) : new StringWriter();

        try {
            while ((read = in.read(buf, 0, bufSize)) >= 0) {
                out.write(buf, 0, read);
            }
            return out.toString();
        }
        finally {
            in.close();
        }
    }

    /**
     * Returns <code>true</code> if 'materializeObject' method should trim
     * trailing spaces from the CHAR columns. This addresses an issue with some
     * JDBC drivers (e.g. Oracle), that return Strings for CHAR columsn  padded
     * with spaces.
     */
    public boolean isTrimmingChars() {
        return trimmingChars;
    }

    public void setTrimmingChars(boolean trimingChars) {
        this.trimmingChars = trimingChars;
    }

    public boolean isUsingClobs() {
        return usingClobs;
    }

    public void setUsingClobs(boolean usingClobs) {
        this.usingClobs = usingClobs;
    }
}