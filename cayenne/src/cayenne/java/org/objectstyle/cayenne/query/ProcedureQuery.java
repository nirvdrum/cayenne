/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.query;

import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.QueryBuilder;
import org.objectstyle.cayenne.util.XMLEncoder;
import org.objectstyle.cayenne.util.XMLSerializable;

/**
 * A query based on Procedure. Can be used as a select query, or as a query of an
 * arbitrary complexity, performing data modification, selecting data (possibly with
 * multiple result sets per call), returning values via OUT parameters.
 * 
 * @author Andrei Adamchik
 */
public class ProcedureQuery extends AbstractQuery implements GenericSelectQuery,
        XMLSerializable {

    protected Map params = new HashMap();
    protected int fetchLimit;

    public ProcedureQuery() {
    }

    public ProcedureQuery(Procedure procedure) {
        setRoot(procedure);
    }

    public ProcedureQuery(String procedureName) {
        setRoot(procedureName);
    }

    /**
     * Prints itself as XML to the provided PrintWriter.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<query name=\"");
        encoder.print(getName());
        encoder.print("\" factory=\"");
        encoder.print("org.objectstyle.cayenne.map.ProcedureQueryBuilder");

        encoder.print("\" root=\"");
        encoder.print(QueryBuilder.PROCEDURE_ROOT);

        String rootString = null;

        if (root instanceof String) {
            rootString = root.toString();
        }
        else if (root instanceof Procedure) {
            rootString = ((Procedure) root).getName();
        }

        if (rootString != null) {
            encoder.print("\" root-name=\"");
            encoder.print(rootString);
        }

        encoder.println("\">");

        encoder.indent(1);

        // encode default SQL
        if (fetchLimit > 0) {
            encoder.printProperty(GenericSelectQuery.FETCH_LIMIT_PROPERTY, fetchLimit);
        }

        encoder.indent(-1);
        encoder.println("</query>");
    }

    public Map getParams() {
        return params;
    }

    public void addParam(String name, Object value) {
        params.put(name, value);
    }

    public void removeParam(String name) {
        params.remove(name);
    }

    public void clearParams() {
        params.clear();
    }

    public int getFetchLimit() {
        return fetchLimit;
    }

    /**
     * Always returns zero, since paged queries are currently not supported for stored
     * procedures.
     */
    public int getPageSize() {
        return 0;
    }

    /**
     * Currently always returns <code>true</code>.
     */
    public boolean isFetchingDataRows() {
        return true;
    }

    /**
     * Currently always returns <code>true</code>.
     * 
     * @since 1.1
     */
    public boolean isRefreshingObjects() {
        return true;
    }

    /**
     * Currently always returns false.
     * 
     * @since 1.1
     */
    public boolean isResolvingInherited() {
        return false;
    }

    /**
     * Currently always returns NO_CACHE.
     * 
     * @since 1.1
     */
    public String getCachePolicy() {
        return GenericSelectQuery.NO_CACHE;
    }
}