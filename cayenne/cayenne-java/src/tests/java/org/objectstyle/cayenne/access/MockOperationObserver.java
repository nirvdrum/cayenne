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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.query.Query;

/**
 * Helper class to process test queries results.
 */
public class MockOperationObserver implements OperationObserver {

    protected Map resultRows = new HashMap();
    protected Map resultCounts = new HashMap();
    protected Map resultBatch = new HashMap();

    public List rowsForQuery(Query q) {
        return (List) resultRows.get(q);
    }

    public int countForQuery(Query q) {
        Integer count = (Integer) resultCounts.get(q);
        return (count != null) ? count.intValue() : -1;
    }

    public int[] countsForQuery(Query q) {
        return (int[]) resultBatch.get(q);
    }

    public void nextCount(Query query, int resultCount) {
        resultCounts.put(query, new Integer(resultCount));
    }

    public void nextDataRows(Query query, List dataRows) {
        resultRows.put(query, dataRows);
    }

    public void nextBatchCount(Query query, int[] resultCount) {
        resultBatch.put(query, resultCount);
    }

    public void nextGlobalException(Exception ex) {
        throw new CayenneRuntimeException(ex);
    }

    public void nextQueryException(Query query, Exception ex) {
        throw new CayenneRuntimeException(ex);
    }

    public void nextDataRows(Query q, ResultIterator it) {
    }

    public void nextGeneratedDataRows(Query query, ResultIterator keysIterator) {
    }

    /**
     * @deprecated since 1.2
     */
    public Level getLoggingLevel() {
        return null;
    }

    public boolean isIteratedResult() {
        return false;
    }
}