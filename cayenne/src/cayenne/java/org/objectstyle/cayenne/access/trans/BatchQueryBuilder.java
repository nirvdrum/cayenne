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

package org.objectstyle.cayenne.access.trans;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.query.BatchQuery;

/**
 * Superclass of batch query translators.
 * 
 * @author Andriy Shapochka, Andrei Adamchik
 */

public abstract class BatchQueryBuilder {
    protected DbAdapter adapter;
    protected String trimFunction;

    public BatchQueryBuilder() {
    }

    public BatchQueryBuilder(DbAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Translates BatchQuery into an SQL string formatted to use 
     * in a PreparedStatement.
     */
    public abstract String createSqlString(BatchQuery batch);

    /**
     * Appends the name of the column to the query buffer. Subclasses use
     * this method to append column names in the WHERE clause, i.e. for the 
     * columns that are not being updated.
     */
    protected void appendDbAttribute(
        StringBuffer buf,
        DbAttribute dbAttribute) {

        // TODO: (Andrus) is there a need for trimming binary types?
        boolean trim =
            dbAttribute.getType() == Types.CHAR && trimFunction != null;
        if (trim) {
            buf.append(trimFunction).append('(');
        }

        buf.append(dbAttribute.getName());

        if (trim) {
            buf.append(')');
        }
    }

    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;
    }
    public DbAdapter getAdapter() {
        return adapter;
    }

    public String getTrimFunction() {
        return trimFunction;
    }

    public void setTrimFunction(String string) {
        trimFunction = string;
    }

    /**
     * Binds BatchQuery parameters to the PreparedStatement. 
     */
    public void bindParameters(
        PreparedStatement statement,
        BatchQuery query,
        List dbAttributes)
        throws SQLException, Exception {

        int attributeCount = dbAttributes.size();

        for (int i = 0; i < attributeCount; i++) {
            Object value = query.getObject(i);
            DbAttribute attribute = (DbAttribute) dbAttributes.get(i);
            adapter.bindParameter(
                statement,
                value,
                i + 1,
                attribute.getType(),
                attribute.getPrecision());

        }
    }
}