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
package org.objectstyle.cayenne.access.trans;

import java.sql.Connection;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.UpdateBatchQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockPreparedStatement;

/**
 * @author Andrei Adamchik, Mike Kienenberger
 */
public class UpdateBatchQueryBuilderTst extends CayenneTestCase {

    protected ObjEntity artistObjEntity;
    protected DbEntity artistDbEntity;
    protected String dBArtistName;
    protected String dBArtistDOB;
    protected Map idKeys1;
    protected Map snapshot1;
    protected Map idKeys2;
    protected Map snapshot2;
    protected List updatedDbAttributeNamesList;
    protected List lockingDbAttributeNamesList;
    protected ObjAttribute artistNameObjAttribute;
    protected ObjAttribute artistDOBObjAttribute;
    protected Map lockingIdKeys1;
    protected Map lockingIdKeys2;

    protected void setUp() throws Exception {
        artistObjEntity = getDomain().getEntityResolver().lookupObjEntity("Artist");
        artistDbEntity = artistObjEntity.getDbEntity();
        artistNameObjAttribute = (ObjAttribute)artistObjEntity.getAttribute(Artist.ARTIST_NAME_PROPERTY);
        artistDOBObjAttribute = (ObjAttribute)artistObjEntity.getAttribute(Artist.DATE_OF_BIRTH_PROPERTY);

        dBArtistName = artistNameObjAttribute.getDbAttributeName();
        dBArtistDOB = artistDOBObjAttribute.getDbAttributeName();

        idKeys1 = new HashMap();
        idKeys1.put(Artist.ARTIST_ID_PK_COLUMN, new Integer(98));
        snapshot1 = new HashMap();
        snapshot1.put(dBArtistName, "Bob Putter");
        snapshot1.put(dBArtistDOB, (new GregorianCalendar(1970, 8, 22)).getTime());

        idKeys2 = new HashMap();
        idKeys2.put(Artist.ARTIST_ID_PK_COLUMN, new Integer(99));
        snapshot2 = new HashMap();
        snapshot2.put(dBArtistName, "Joe Cool");
        snapshot2.put(dBArtistDOB, (new GregorianCalendar(1955, 11, 14)).getTime());

        lockingIdKeys1 = new HashMap();
        lockingIdKeys1.put(Artist.ARTIST_ID_PK_COLUMN, new Integer(98));
        lockingIdKeys1.put(dBArtistName, "Bob Potter");
        lockingIdKeys1.put(dBArtistDOB, (new GregorianCalendar(1971, 8, 22)).getTime());

        lockingIdKeys2 = new HashMap();
        lockingIdKeys2.put(Artist.ARTIST_ID_PK_COLUMN, new Integer(99));
        lockingIdKeys2.put(dBArtistName, "Joe Cool");
        lockingIdKeys2.put(dBArtistDOB, null);

        updatedDbAttributeNamesList = Arrays.asList(new Object[]
            {dBArtistName, dBArtistDOB});
        
        lockingDbAttributeNamesList = Arrays.asList(new Object[]
            {(DbAttribute)artistDbEntity.getAttribute(Artist.ARTIST_ID_PK_COLUMN), artistNameObjAttribute.getDbAttribute(), artistDOBObjAttribute.getDbAttribute()});
    }

	public void testConstructor() throws Exception {
		DbAdapter adapter = new JdbcAdapter();

		UpdateBatchQueryBuilder builder =
			new UpdateBatchQueryBuilder(adapter);

		assertSame(adapter, builder.getAdapter());
	}

    public void testCreateSqlString() throws Exception {
        UpdateBatchQuery updateQuery = new UpdateBatchQuery(artistDbEntity, updatedDbAttributeNamesList, 1);
        updateQuery.add(idKeys1, snapshot1);
        updateQuery.add(idKeys2, snapshot2);

        DbAdapter adapter = new JdbcAdapter();
        UpdateBatchQueryBuilder builder = new UpdateBatchQueryBuilder(adapter);
        String generatedSql = builder.createSqlString(updateQuery);

        // do some simple assertions to make sure all parts are in
        assertNotNull(generatedSql);
        assertTrue(generatedSql.startsWith("UPDATE "));
        assertTrue(generatedSql.indexOf(" SET ") > 0);
        assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql.indexOf(" SET "));
        
        // Test specific sql update statement
        assertEquals("UPDATE " + artistDbEntity.getName()
            + " SET " 
            + dBArtistName + " = ?"
            + ", " + dBArtistDOB + " = ?"
            + " WHERE " + Artist.ARTIST_ID_PK_COLUMN + " = ?", generatedSql);
    }

    public void testCreateSqlStringOptimisticLocking() throws Exception {
        UpdateBatchQuery updateQuery = new UpdateBatchQuery(artistDbEntity, updatedDbAttributeNamesList, 1);
        updateQuery.setIdDbAttributes(lockingDbAttributeNamesList);
        updateQuery.setUsingOptimisticLocking(true);
        updateQuery.add(lockingIdKeys1, snapshot1);
        updateQuery.add(lockingIdKeys2, snapshot2);
        
        DbAdapter adapter = new JdbcAdapter();
        UpdateBatchQueryBuilder builder = new UpdateBatchQueryBuilder(adapter);

        updateQuery.reset();
        updateQuery.next();
        
        String generatedSql = builder.createSqlString(updateQuery);

        // do some simple assertions to make sure all parts are in
        assertNotNull(generatedSql);
        assertTrue(generatedSql.startsWith("UPDATE "));
        assertTrue(generatedSql.indexOf(" SET ") > 0);
        assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql.indexOf(" SET "));
        
        // Test specific sql update statement
        assertEquals("UPDATE " + artistDbEntity.getName()
            + " SET " 
            + dBArtistName + " = ?"
            + ", " + dBArtistDOB + " = ?"
            + " WHERE " + Artist.ARTIST_ID_PK_COLUMN + " = ? AND "
            + dBArtistName + " = ? AND " + dBArtistDOB + " = ?", generatedSql);
 
 
        updateQuery.next();
        
        generatedSql = builder.createSqlString(updateQuery);

        // do some simple assertions to make sure all parts are in
        assertNotNull(generatedSql);
        assertTrue(generatedSql.startsWith("UPDATE "));
        assertTrue(generatedSql.indexOf(" SET ") > 0);
        assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql.indexOf(" SET "));
        
        // Test specific sql update statement
        assertEquals("UPDATE " + artistDbEntity.getName()
            + " SET " 
            + dBArtistName + " = ?"
            + ", " + dBArtistDOB + " = ?"
            + " WHERE " + Artist.ARTIST_ID_PK_COLUMN + " = ? AND "
            + dBArtistName + " = ? AND " + dBArtistDOB + " IS NULL", generatedSql);
    }

    public void testBindParameters() throws Exception {
        UpdateBatchQuery updateQuery = new UpdateBatchQuery(artistDbEntity, updatedDbAttributeNamesList, 1);
        updateQuery.add(idKeys1, snapshot1);
        updateQuery.add(idKeys2, snapshot2);

        DbAdapter adapter = new JdbcAdapter();
        UpdateBatchQueryBuilder builder = new UpdateBatchQueryBuilder(adapter);

        Connection connection = new MockConnection();

        String generatedSql = builder.createSqlString(updateQuery);
        MockPreparedStatement st = new MockPreparedStatement(connection, generatedSql);

        updateQuery.reset();


        updateQuery.next();

        // dbAttributes not used in an UpdateQuery
        List dbAttributes = null;
        builder.bindParameters(st, updateQuery, dbAttributes);
        
        assertEquals(3, st.getIndexedParameterMap().size());

        Object param1 = st.getParameter(1);
        assertNotNull(param1);
        assertEquals(snapshot1.get(dBArtistName), param1);
        
        Object param2 = st.getParameter(2);
        assertNotNull(param2);
        assertEquals(snapshot1.get(dBArtistDOB), param2);

        Object param3 = st.getParameter(3);
        assertNotNull(param3);
        assertEquals(idKeys1.get(Artist.ARTIST_ID_PK_COLUMN), param3);


        updateQuery.next();

        // dbAttributes not used in an UpdateQuery
        dbAttributes = null;
        builder.bindParameters(st, updateQuery, dbAttributes);
        
        assertEquals(3, st.getIndexedParameterMap().size());

        param1 = st.getParameter(1);
        assertNotNull(param1);
        assertEquals(snapshot2.get(dBArtistName), param1);
        
        param2 = st.getParameter(2);
        assertNotNull(param2);
        assertEquals(snapshot2.get(dBArtistDOB), param2);

        param3 = st.getParameter(3);
        assertNotNull(param3);
        assertEquals(idKeys2.get(Artist.ARTIST_ID_PK_COLUMN), param3);
    }
   
    public void testBindParametersOptimisticLocking() throws Exception {
        UpdateBatchQuery updateQuery = new UpdateBatchQuery(artistDbEntity, updatedDbAttributeNamesList, 1);
        updateQuery.setIdDbAttributes(lockingDbAttributeNamesList);
        updateQuery.setUsingOptimisticLocking(true);
        updateQuery.add(lockingIdKeys1, snapshot1);
        updateQuery.add(lockingIdKeys2, snapshot2);
        

        DbAdapter adapter = new JdbcAdapter();
        UpdateBatchQueryBuilder builder = new UpdateBatchQueryBuilder(adapter);

        Connection connection = new MockConnection();

        updateQuery.reset();


        updateQuery.next();

        String generatedSql = builder.createSqlString(updateQuery);
        MockPreparedStatement st = new MockPreparedStatement(connection, generatedSql);

        // dbAttributes not used in an UpdateQuery
        List dbAttributes = null;
        builder.bindParameters(st, updateQuery, dbAttributes);
        
        assertEquals(5, st.getIndexedParameterMap().size());
 
        Object param1 = st.getParameter(1);
        assertNotNull(param1);
        assertEquals(snapshot1.get(dBArtistName), param1);
        
        Object param2 = st.getParameter(2);
        assertNotNull(param2);
        assertEquals(snapshot1.get(dBArtistDOB), param2);

        Object param3 = st.getParameter(3);
        assertNotNull(param3);
        assertEquals(lockingIdKeys1.get(Artist.ARTIST_ID_PK_COLUMN), param3);
        
        Object param4 = st.getParameter(4);
        assertNotNull(param4);
        assertEquals(lockingIdKeys1.get(dBArtistName), param4);

        Object param5 = st.getParameter(5);
        assertNotNull(param5);
        assertEquals(lockingIdKeys1.get(dBArtistDOB), param5);

        updateQuery.next();

        generatedSql = builder.createSqlString(updateQuery);
        st = new MockPreparedStatement(connection, generatedSql);

        // dbAttributes not used in an UpdateQuery
        dbAttributes = null;
        builder.bindParameters(st, updateQuery, dbAttributes);
        
        assertEquals(4, st.getIndexedParameterMap().size());

        param1 = st.getParameter(1);
        assertNotNull(param1);
        assertEquals(snapshot2.get(dBArtistName), param1);
        
        param2 = st.getParameter(2);
        assertNotNull(param2);
        assertEquals(snapshot2.get(dBArtistDOB), param2);

        param3 = st.getParameter(3);
        assertNotNull(param3);
        assertEquals(lockingIdKeys2.get(Artist.ARTIST_ID_PK_COLUMN), param3);
        
        param4 = st.getParameter(4);
        assertNotNull(param4);
        assertEquals(lockingIdKeys2.get(dBArtistName), param4);
    }
   
}
