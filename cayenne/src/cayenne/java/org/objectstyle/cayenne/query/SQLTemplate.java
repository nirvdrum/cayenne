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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;
import org.objectstyle.cayenne.map.DataColumnDescriptor;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;

/**
 * A generic raw SQL query that can be either a DML/DDL or a select. 
 * 
 * <p><strong>Template Script</strong></p>
 * <p>SQLTemplate stores a dynamic template for the SQL query query that supports 
 * parameters and customization using Velocity scripting language. Scripting abilities
 * are required since depending on the parameter list, SQL query text is modified. E.g. 
 * if a value is null, a string "a = ?" must be replaced with "a is null", etc. In 
 * addition SQLTemplate allows switching templates based on a some key. This way a single 
 * query can have multiple "dialects" specific to a given database.</p>
 * 
 * <p><strong>Parameter Sets</strong></p>
 * <p>SQLTemplate supports multiple sets of parameters, so a single query can be executed
 * multiple times with different parameters. "Scrolling" through parameter list is done by 
 * calling {@link #parametersIterator()} method. Returned iterator will return go over the parameter
 * sets, returning a Map on each call to "next()"</p>
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class SQLTemplate extends AbstractQuery implements GenericSelectQuery {

    private static final Transformer nullMapTransformer = new Transformer() {
        public Object transform(Object input) {
            return (input != null) ? input : Collections.EMPTY_MAP;
        }
    };

    protected SelectExecutionProperties selectProperties =
        new SelectExecutionProperties();
    protected DataColumnDescriptor[] resultColumns;
    protected String defaultTemplate;
    protected Map templates;
    protected Map[] parameters;

    /** 
     * Creates an empty SQLTemplate. 
     */
    public SQLTemplate() {
    }

    public SQLTemplate(ObjEntity root) {
        setRoot(root);
    }

    public SQLTemplate(Class rootClass) {
        setRoot(root);
    }

    public SQLTemplate(DbEntity root) {
        setRoot(root);
    }

    public SQLTemplate(String objEntityName) {
        setRoot(objEntityName);
    }

    /**
     * Returns an iterator over parameter sets. Each element returned
     * from the iterator is a java.util.Map.
     */
    public Iterator parametersIterator() {
        return (parameters == null || parameters.length == 0)
            ? IteratorUtils.emptyIterator()
            : IteratorUtils.transformedIterator(
                IteratorUtils.arrayIterator(parameters),
                nullMapTransformer);
    }

    /**
     * Returns a new query built using this query as a prototype 
     * and a new set of parameters.
     */
    public SQLTemplate queryWithParameters(Map parameters) {
        // create a query replica
        SQLTemplate query = new SQLTemplate();

        query.setLoggingLevel(logLevel);
        query.setRoot(root);
        query.setResultColumns(resultColumns);

        selectProperties.copyToProperties(query.selectProperties);
        query.setParameters(parameters);

        return query;
    }

    public int getFetchLimit() {
        return selectProperties.getFetchLimit();
    }

    public void setFetchLimit(int fetchLimit) {
        this.selectProperties.setFetchLimit(fetchLimit);
    }

    public int getPageSize() {
        return selectProperties.getPageSize();
    }

    public void setPageSize(int pageSize) {
        selectProperties.setPageSize(pageSize);
    }

    public void setFetchingDataRows(boolean flag) {
        selectProperties.setFetchingDataRows(flag);
    }

    public boolean isFetchingDataRows() {
        return selectProperties.isFetchingDataRows();
    }

    public boolean isRefreshingObjects() {
        return selectProperties.isRefreshingObjects();
    }

    public void setRefreshingObjects(boolean flag) {
        selectProperties.setRefreshingObjects(flag);
    }

    public boolean isResolvingInherited() {
        return selectProperties.isResolvingInherited();
    }

    public void setResolvingInherited(boolean b) {
        selectProperties.setResolvingInherited(b);
    }

    /**
     * Returns query type as select or unknowns depending on whether
     * result columns are defined.
     */
    public int getQueryType() {
        return resultColumns != null
            && resultColumns.length > 0 ? SELECT_QUERY : UNKNOWN_QUERY;
    }

    public DataColumnDescriptor[] getResultColumns() {
        return resultColumns;
    }

    public void setResultColumns(DataColumnDescriptor[] descriptors) {
        resultColumns = descriptors;
    }

    /**
     * Returns default SQL template for this query.
     */
    public String getDefaultTemplate() {
        return defaultTemplate;
    }

    /**
     * Sets default SQL template for this query.
     */
    public void setDefaultTemplate(String string) {
        defaultTemplate = string;
    }

    /**
     * Returns a template for key, or a default template if a template
     * for key is not found.
     */
    public synchronized String getTemplate(String key) {
        if (templates == null) {
            return defaultTemplate;
        }

        String template = (String) templates.get(key);
        return (template != null) ? template : defaultTemplate;
    }

    public synchronized void setTemplate(String key, String template) {
        if (templates == null) {
            templates = new HashMap();
        }

        templates.put(key, template);
    }

    /**
     * Utility method to get the first set of parameters, since
     * most queries will only have one.
     */
    public Map getParameters() {
        Map map = (parameters != null && parameters.length > 0) ? parameters[0] : null;
        return (map != null) ? map : Collections.EMPTY_MAP;
    }

    /**
     * Utility method to initialize query with only a single set of parameters. 
     * Useful, since most queries will only have one set. Internally calls
     * {@link #setParameters(Map[])}.
     */
    public void setParameters(Map map) {
        setParameters(map != null ? new Map[] { map }
        : null);
    }

    public void setParameters(Map[] parameters) {
        this.parameters = parameters;
    }
}
