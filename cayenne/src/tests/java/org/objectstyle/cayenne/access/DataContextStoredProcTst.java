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

package org.objectstyle.cayenne.access;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.access.util.DefaultOperationObserver;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextStoredProcTst extends CayenneTestCase {
    public static final String UPDATE_STORED_PROCEDURE = "cayenne_tst_upd_proc";
    public static final String SELECT_STORED_PROCEDURE = "cayenne_tst_select_proc";
    public static final String OUT_STORED_PROCEDURE = "cayenne_tst_out_proc";

    protected DataContext ctxt;

    public void testUpdate() throws Exception {
        // Don't run this on MySQL
        if (!getDatabaseSetupDelegate().supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database  
        createArtist(1000.0);

        ProcedureQuery q = new ProcedureQuery(UPDATE_STORED_PROCEDURE);
        q.addParam("paintingPrice", new Integer(3000));
        DefaultOperationObserver observer = new DefaultOperationObserver();

        // since stored procedure commits its stuff, we must use an explicit 
        // non-committing transaction
        Transaction.externalTransaction(null).performQueries(
            ctxt,
            Collections.singletonList(q),
            observer);
            
        // check that price have doubled
        SelectQuery select = new SelectQuery(Artist.class);
        select.addPrefetch("paintingArray");
        select.setLoggingLevel(Level.WARN);
        List artists = ctxt.performQuery(select);
        assertEquals(1, artists.size());

        Artist a = (Artist) artists.get(0);
        Painting p = (Painting) a.getPaintingArray().get(0);
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    public void testSelect1() throws Exception {
        // Don't run this on MySQL
        if (!getDatabaseSetupDelegate().supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);

        ProcedureQuery q = new ProcedureQuery(SELECT_STORED_PROCEDURE);
        q.addParam("aName", "An Artist");
        q.addParam("paintingPrice", new Integer(3000));
        List artists = null;

        // since stored procedure commits its stuff, we must use an explicit 
        // non-committing transaction
        SelectObserver observer = new SelectObserver();
        Transaction.externalTransaction(null).performQueries(
            ctxt,
            Collections.singletonList(q),
            observer);
        artists = observer.getResults(q);

        // check the results
        assertNotNull("Null result from StoredProcedure.", artists);
        assertEquals(1, artists.size());
        DataRow artistRow = (DataRow) artists.get(0);
        Artist a = (Artist) ctxt.objectFromDataRow(Artist.class, artistRow, false);
        Painting p = (Painting) a.getPaintingArray().get(0);

        // invalidate painting, it may have been updated in the proc
        ctxt.invalidateObjects(Collections.singletonList(p));
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    public void testSelect2() throws Exception {
        // Don't run this on MySQL
        if (!getDatabaseSetupDelegate().supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);

        ProcedureQuery q = new ProcedureQuery(SELECT_STORED_PROCEDURE);
        q.addParam("aName", "An Artist");
        q.addParam("paintingPrice", new Integer(3000));

        QueryResult result = new QueryResult();

        // since stored procedure commits its stuff, we must use an explicit 
        // non-committing transaction
        Transaction.externalTransaction(null).performQueries(
            ctxt,
            Collections.singletonList(q),
            result);

        List artists = result.getFirstRows(q);

        // check the results
        assertNotNull("Null result from StoredProcedure.", artists);
        assertEquals(1, artists.size());
        DataRow artistRow = (DataRow) artists.get(0);
        Artist a = (Artist) ctxt.objectFromDataRow(Artist.class, artistRow, false);
        Painting p = (Painting) a.getPaintingArray().get(0);

        // invalidate painting, it may have been updated in the proc
        ctxt.invalidateObjects(Collections.singletonList(p));
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    public void testSelect3() throws Exception {
        // Don't run this on MySQL
        if (!getDatabaseSetupDelegate().supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);

        // test ProcedureQuery with Procedure as root
        Procedure proc =
            ctxt.getEntityResolver().lookupProcedure(SELECT_STORED_PROCEDURE);
        ProcedureQuery q = new ProcedureQuery(proc);
        q.addParam("aName", "An Artist");
        q.addParam("paintingPrice", new Integer(3000));

        QueryResult result = new QueryResult();

        // since stored procedure commits its stuff, we must use an explicit 
        // non-committing transaction
        Transaction.externalTransaction(null).performQueries(
            ctxt,
            Collections.singletonList(q),
            result);

        List artists = result.getFirstRows(q);

        // check the results
        assertNotNull("Null result from StoredProcedure.", artists);
        assertEquals(1, artists.size());
        DataRow artistRow = (DataRow) artists.get(0);
        Artist a = (Artist) ctxt.objectFromDataRow(Artist.class, artistRow, false);
        Painting p = (Painting) a.getPaintingArray().get(0);

        // invalidate painting, it may have been updated in the proc
        ctxt.invalidateObjects(Collections.singletonList(p));
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    public void testOutParams() throws Exception {
        // Don't run this on MySQL
        if (!getDatabaseSetupDelegate().supportsStoredProcedures()) {
            return;
        }

        ProcedureQuery q = new ProcedureQuery(OUT_STORED_PROCEDURE);
        q.addParam("in_param", new Integer(20));

        QueryResult resultHolder = new QueryResult();
        // since stored procedure commits its stuff, we must use an explicit 
        // non-committing transaction
        Transaction.externalTransaction(null).performQueries(
            ctxt,
            Collections.singletonList(q),
            resultHolder);

        // check the results
        List rows = resultHolder.getFirstRows(q);
        assertNotNull(rows);

        assertEquals(1, rows.size());
        Object row = rows.get(0);
        assertNotNull(row);
        assertTrue(
            "Unexpected row class: " + row.getClass().getName(),
            row instanceof Map);
        Map outParams = (Map) row;
        Number price = (Number) outParams.get("out_param");
        assertNotNull(price);
        assertEquals(40, price.intValue());
    }

    protected void createArtist(double paintingPrice) {
        Artist a = (Artist) ctxt.createAndRegisterNewObject("Artist");
        a.setArtistName("An Artist");

        Painting p = (Painting) ctxt.createAndRegisterNewObject("Painting");
        p.setPaintingTitle("A Painting");
        p.setEstimatedPrice(new BigDecimal(paintingPrice));
        a.addToPaintingArray(p);

        ctxt.commitChanges();
    }

    protected void setUp() throws Exception {
        // Don't run this on MySQL
        if (!getDatabaseSetupDelegate().supportsStoredProcedures()) {
            return;
        }

        cleanTableData();
        ctxt = createDataContext();
    }
}
