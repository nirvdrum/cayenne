/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.art.CompoundFkTestEntity;
import org.apache.art.CompoundPkTestEntity;
import org.apache.art.Painting;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.openbase.OpenBaseAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.SQLResultSetMapping;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneCase;

/**
 * @author Andrus Adamchik
 */
public class DataContextSQLTemplateTest extends CayenneCase {

    protected DataContext context;

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
        context = createDataContext();
    }

    public void testSQLResultSetMappingScalar() throws Exception {
        createTestData("testSQLResultSetMappingScalar");

        String sql = "SELECT count(1) AS X FROM ARTIST";

        DataMap map = getDomain().getMap("testmap");
        SQLTemplate query = new SQLTemplate(map, sql);
        query.setTemplate(
                FrontBaseAdapter.class.getName(),
                "SELECT COUNT(ARTIST_ID) X FROM ARTIST");
        query.setTemplate(
                OpenBaseAdapter.class.getName(),
                "SELECT COUNT(ARTIST_ID) X FROM ARTIST");
        query.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);

        SQLResultSetMapping rsMap = new SQLResultSetMapping();
        rsMap.addColumnResult("X");
        query.setResultSetMapping(rsMap);

        List objects = createDataContext().performQuery(query);
        assertEquals(1, objects.size());

        Object o = objects.get(0);
        assertTrue("Expected Number: " + o, o instanceof Number);
        assertEquals(4, ((Number) o).intValue());
    }

    public void testSQLResultSetMappingScalarArray() throws Exception {
        createTestData("testSQLResultSetMappingScalar");

        String sql = "SELECT count(1) AS X, 77 AS Y FROM ARTIST";

        DataMap map = getDomain().getMap("testmap");
        SQLTemplate query = new SQLTemplate(map, sql);
        query.setTemplate(
                FrontBaseAdapter.class.getName(),
                "SELECT COUNT(ARTIST_ID) X, 77 Y FROM ARTIST GROUP BY Y");
        query.setTemplate(
                OpenBaseAdapter.class.getName(),
                "SELECT COUNT(ARTIST_ID) X, 77 Y FROM ARTIST GROUP BY 77");
        query.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);

        SQLResultSetMapping rsMap = new SQLResultSetMapping();
        rsMap.addColumnResult("X");
        rsMap.addColumnResult("Y");
        query.setResultSetMapping(rsMap);

        List objects = createDataContext().performQuery(query);
        assertEquals(1, objects.size());

        Object o = objects.get(0);
        assertTrue(o instanceof Object[]);

        Object[] row = (Object[]) o;
        assertEquals(2, row.length);

        assertEquals(4, ((Number) row[0]).intValue());
        assertEquals(77, ((Number) row[1]).intValue());
    }

    public void testColumnNamesCapitalization() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Artist.class, template);
        query.setColumnNamesCapitalization(SQLTemplate.LOWERCASE_COLUMN_NAMES);
        query.setFetchingDataRows(true);

        List rows = context.performQuery(query);

        DataRow row1 = (DataRow) rows.get(0);
        assertFalse(row1.containsKey("ARTIST_ID"));
        assertTrue(row1.containsKey("artist_id"));

        DataRow row2 = (DataRow) rows.get(1);
        assertFalse(row2.containsKey("ARTIST_ID"));
        assertTrue(row2.containsKey("artist_id"));

        query.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);

        List rowsUpper = context.performQuery(query);

        DataRow row3 = (DataRow) rowsUpper.get(0);
        assertFalse(row3.containsKey("artist_id"));
        assertTrue(row3.containsKey("ARTIST_ID"));

        DataRow row4 = (DataRow) rowsUpper.get(1);
        assertFalse(row4.containsKey("artist_id"));
        assertTrue(row4.containsKey("ARTIST_ID"));
    }

    public void testFetchDataRows() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Artist.class, template);

        getSQLTemplateBuilder().updateSQLTemplate(query);

        query.setFetchingDataRows(true);

        List rows = context.performQuery(query);
        assertEquals(DataContextCase.artistCount, rows.size());
        assertTrue(
                "Expected DataRow, got this: " + rows.get(1),
                rows.get(1) instanceof DataRow);

        DataRow row2 = (DataRow) rows.get(1);
        assertEquals(3, row2.size());
        assertEquals(new Integer(33002), row2.get("ARTIST_ID"));
    }

    public void testFetchObjects() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);

        query.setFetchingDataRows(false);

        List objects = context.performQuery(query);
        assertEquals(DataContextCase.artistCount, objects.size());
        assertTrue(objects.get(1) instanceof Artist);

        Artist artist2 = (Artist) objects.get(1);
        assertEquals("artist2", artist2.getArtistName());
    }

    public void testBindObjectEqualShort() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        Artist a = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 33002);

        String template = "SELECT * FROM PAINTING "
                + "WHERE #bindObjectEqual($a) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);
        query.setParameters(Collections.singletonMap("a", a));

        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p = (Painting) objects.get(0);
        assertEquals(33002, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectNotEqualShort() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        Artist a = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 33002);

        String template = "SELECT * FROM PAINTING "
                + "WHERE #bindObjectNotEqual($a) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);
        query.setParameters(Collections.singletonMap("a", a));

        List objects = context.performQuery(query);

        // null comparison is unpredictable across DB's ... some would return true on null
        // <> value, some - false
        assertTrue(objects.size() == 1 || objects.size() == 2);

        Painting p = (Painting) objects.get(0);
        assertEquals(33001, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectEqualFull() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        Artist a = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 33002);

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectEqual($a [ 't0.ARTIST_ID' ] [ 'ARTIST_ID' ] ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);
        query.setParameters(Collections.singletonMap("a", a));

        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p = (Painting) objects.get(0);
        assertEquals(33002, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectEqualFullNonArray() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        Artist a = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 33002);

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectEqual($a 't0.ARTIST_ID' 'ARTIST_ID' ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);
        query.setParameters(Collections.singletonMap("a", a));

        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p = (Painting) objects.get(0);
        assertEquals(33002, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectEqualNull() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectEqual($a [ 't0.ARTIST_ID' ] [ 'ARTIST_ID' ] ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);
        query.setParameters(Collections.singletonMap("a", null));

        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p = (Painting) objects.get(0);
        assertEquals(33003, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectNotEqualFull() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        Artist a = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 33002);

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectNotEqual($a [ 't0.ARTIST_ID' ] [ 'ARTIST_ID' ] ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);
        query.setParameters(Collections.singletonMap("a", a));

        List objects = context.performQuery(query);
        // null comparison is unpredictable across DB's ... some would return true on null
        // <> value, some - false
        assertTrue(objects.size() == 1 || objects.size() == 2);

        Painting p = (Painting) objects.get(0);
        assertEquals(33001, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectEqualCompound() throws Exception {
        createTestData("testBindObjectEqualCompound");

        ObjectContext context = createDataContext();

        Map pk = new HashMap();
        pk.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "a1");
        pk.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "a2");

        CompoundPkTestEntity a = (CompoundPkTestEntity) DataObjectUtils.objectForPK(
                context,
                CompoundPkTestEntity.class,
                pk);

        String template = "SELECT * FROM COMPOUND_FK_TEST t0"
                + " WHERE #bindObjectEqual($a [ 't0.F_KEY1', 't0.F_KEY2' ] [ 'KEY1', 'KEY2' ] ) ORDER BY PKEY";
        SQLTemplate query = new SQLTemplate(CompoundFkTestEntity.class, template);
        query.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);
        query.setParameters(Collections.singletonMap("a", a));

        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        CompoundFkTestEntity p = (CompoundFkTestEntity) objects.get(0);
        assertEquals(33001, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectNotEqualCompound() throws Exception {
        createTestData("testBindObjectEqualCompound");

        ObjectContext context = createDataContext();

        Map pk = new HashMap();
        pk.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "a1");
        pk.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "a2");

        CompoundPkTestEntity a = (CompoundPkTestEntity) DataObjectUtils.objectForPK(
                context,
                CompoundPkTestEntity.class,
                pk);

        String template = "SELECT * FROM COMPOUND_FK_TEST t0"
                + " WHERE #bindObjectNotEqual($a [ 't0.F_KEY1', 't0.F_KEY2' ] [ 'KEY1', 'KEY2' ] ) ORDER BY PKEY";
        SQLTemplate query = new SQLTemplate(CompoundFkTestEntity.class, template);
        query.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);
        query.setParameters(Collections.singletonMap("a", a));

        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        CompoundFkTestEntity p = (CompoundFkTestEntity) objects.get(0);
        assertEquals(33002, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectNotEqualNull() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectNotEqual($a [ 't0.ARTIST_ID' ] [ 'ARTIST_ID' ] ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);
        query.setParameters(Collections.singletonMap("a", null));

        List objects = context.performQuery(query);
        assertEquals(2, objects.size());

        Painting p1 = (Painting) objects.get(0);
        assertEquals(33001, DataObjectUtils.intPKForObject(p1));

        Painting p2 = (Painting) objects.get(1);
        assertEquals(33002, DataObjectUtils.intPKForObject(p2));
    }

    public void testFetchLimit() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        int fetchLimit = 3;

        // sanity check
        assertTrue(fetchLimit < DataContextCase.artistCount);
        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);
        query.setFetchLimit(fetchLimit);

        List objects = context.performQuery(query);
        assertEquals(fetchLimit, objects.size());
        assertTrue(objects.get(0) instanceof Artist);
    }

    public void testPageSize() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        int pageSize = 3;

        // sanity check
        assertTrue(pageSize < DataContextCase.artistCount);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);

        query.setPageSize(pageSize);

        List objects = context.performQuery(query);
        assertEquals(DataContextCase.artistCount, objects.size());
        assertTrue(objects.get(0) instanceof Artist);

        assertTrue(objects instanceof IncrementalFaultList);
        IncrementalFaultList pagedList = (IncrementalFaultList) objects;
        assertEquals(DataContextCase.artistCount - pageSize, pagedList
                .getUnfetchedObjects());

        // check if we can resolve subsequent pages
        Artist artist = (Artist) objects.get(pageSize);

        int expectUnresolved = DataContextCase.artistCount - pageSize - pageSize;
        if (expectUnresolved < 0) {
            expectUnresolved = 0;
        }
        assertEquals(expectUnresolved, pagedList.getUnfetchedObjects());
        assertEquals("artist" + (pageSize + 1), artist.getArtistName());
    }

    public void testIteratedQuery() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);

        ResultIterator it = context.performIteratedQuery(query);

        try {
            int i = 0;

            while (it.hasNextRow()) {
                i++;

                Map row = it.nextDataRow();
                assertEquals(3, row.size());
                assertEquals(new Integer(33000 + i), row.get("ARTIST_ID"));
            }

            assertEquals(DataContextCase.artistCount, i);
        }
        finally {
            it.close();
        }
    }

    public void testQueryWithLineBreakAfterMacroCAY726() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        // see CAY-726 for details
        String template = "SELECT #result('count(*)' 'int' 'X')"
                + System.getProperty("line.separator")
                + "FROM ARTIST";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);
        query.setFetchingDataRows(true);

        List result = context.performQuery(query);

        assertEquals(new Integer(25), ((Map) result.get(0)).get("X"));
    }
}
