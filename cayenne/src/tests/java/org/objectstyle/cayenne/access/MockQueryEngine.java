/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.access;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.access.Transaction;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.query.Query;

/**
 * A query engine used for unit testing. Returns canned results instead of doing the
 * actual query.
 * 
 * @author Andrei Adamchik
 */
public class MockQueryEngine implements QueryEngine {

    // mockup the actual results
    protected Map results = new HashMap();
    protected EntityResolver entityResolver;
    protected int runCount;

    public MockQueryEngine() {
    }

    public MockQueryEngine(QueryEngine engine) {
        this(engine.getEntityResolver());
    }

    public MockQueryEngine(EntityResolver resolver) {
        this.entityResolver = resolver;
    }

    public void reset() {
        runCount = 0;
        results.clear();
    }

    public int getRunCount() {
        return runCount;
    }

    public void addExpectedResult(Query query, List result) {
        results.put(query, result);
    }

    public void performQueries(
            Collection queries,
            OperationObserver resultConsumer,
            Transaction transaction) {
        initWithPresetResults(queries, resultConsumer);
    }

    public void performQueries(Collection queries, OperationObserver resultConsumer) {
        initWithPresetResults(queries, resultConsumer);
    }

    private void initWithPresetResults(
            Collection queries,
            OperationObserver resultConsumer) {

        runCount++;

        // stick preset results to the consumer
        Iterator it = queries.iterator();
        while (it.hasNext()) {
            Query query = (Query) it.next();
            resultConsumer.nextDataRows(query, (List) results.get(query));
        }
    }

    public DataNode lookupDataNode(DataMap dataMap) {
        return null;
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public Collection getDataMaps() {
        return (entityResolver != null)
                ? entityResolver.getDataMaps()
                : Collections.EMPTY_LIST;
    }
}