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
package org.objectstyle.cayenne.access.types;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.objectstyle.cayenne.CayenneException;

/**
 * @author Andrei Adamchik
 */
public class ByteArrayType extends AbstractType {

    private static final int BUF_SIZE = 8 * 1024;

    protected boolean trimmingBytes;
    protected boolean usingBlobs;

    /**
     * Strips null bytes from the byte array, returning a potentially smaller
     * array that contains no trailing zero bytes.
     */
    public static byte[] trimBytes(byte[] bytes) {
        int bytesToTrim = 0;
        for (int i = bytes.length - 1; i >= 0; i--) {
            if (bytes[i] != 0) {
                bytesToTrim = bytes.length - 1 - i;
                break;
            }
        }

        if (bytesToTrim == 0) {
            return bytes;
        }

        byte[] dest = new byte[bytes.length - bytesToTrim];
        System.arraycopy(bytes, 0, dest, 0, dest.length);
        return dest;
    }

    public ByteArrayType(boolean trimmingBytes, boolean usingBlobs) {
        this.usingBlobs = usingBlobs;
        this.trimmingBytes = trimmingBytes;
    }

    public String getClassName() {
        return "byte[]";
    }

    public Object materializeObject(ResultSet rs, int index, int type)
        throws Exception {

        byte[] bytes = null;

        if (type == Types.BLOB) {
            bytes =
                (isUsingBlobs())
                    ? readBlob(rs.getBlob(index))
                    : readBinaryStream(rs, index);
        } else {
            bytes = rs.getBytes(index);

            // trim BINARY type
            if (bytes != null && type == Types.BINARY && isTrimmingBytes()) {
                bytes = trimBytes(bytes);
            }
        }

        return bytes;
    }

    public Object materializeObject(CallableStatement cs, int index, int type)
        throws Exception {

        byte[] bytes = null;

        if (type == Types.BLOB) {
            if (!isUsingBlobs()) {
                throw new CayenneException("Binary streams are not supported in stored procedure parameters.");
            }
            bytes = readBlob(cs.getBlob(index));
        } else {
            
            bytes = cs.getBytes(index);

            // trim BINARY type
            if (bytes != null && type == Types.BINARY && isTrimmingBytes()) {
                bytes = trimBytes(bytes);
            }
        }

        return bytes;
    }

    public void setJdbcObject(
        PreparedStatement st,
        Object val,
        int pos,
        int type,
        int precision)
        throws Exception {

        // if this is a BLOB column, set the value as "bytes"
        // instead. This should work with most drivers
        if (type == Types.BLOB) {
            st.setBytes(pos, (byte[]) val);
        } else {
            super.setJdbcObject(st, val, pos, type, precision);
        }
    }

    protected byte[] readBlob(Blob blob) throws IOException, SQLException {

        // sanity check on size
        if (blob.length() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                "BLOB is too big to be read as byte[] in memory: "
                    + blob.length());
        }

        int size = (int) blob.length();
        int bufSize = (size < BUF_SIZE) ? size : BUF_SIZE;
        InputStream in =
            new BufferedInputStream(blob.getBinaryStream(), bufSize);

        return readValueStream(in, size, bufSize);
    }

    protected byte[] readBinaryStream(ResultSet rs, int index)
        throws IOException, SQLException {
        return readValueStream(rs.getBinaryStream(index), -1, BUF_SIZE);
    }

    protected byte[] readValueStream(
        InputStream in,
        int streamSize,
        int bufSize)
        throws IOException {

        byte[] buf = new byte[bufSize];
        int read;
        ByteArrayOutputStream out =
            (streamSize > 0)
                ? new ByteArrayOutputStream(streamSize)
                : new ByteArrayOutputStream();

        try {
            while ((read = in.read(buf, 0, bufSize)) >= 0) {
                out.write(buf, 0, read);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Returns <code>true</code> if byte columns are handled as BLOBs
     * internally.
     */
    public boolean isUsingBlobs() {
        return usingBlobs;
    }

    public void setUsingBlobs(boolean usingBlobs) {
        this.usingBlobs = usingBlobs;
    }

    public boolean isTrimmingBytes() {
        return trimmingBytes;
    }

    public void setTrimmingBytes(boolean trimingBytes) {
        this.trimmingBytes = trimingBytes;
    }
}
