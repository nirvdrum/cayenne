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

package org.objectstyle.cayenne.access;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.objectstyle.cayenne.TestOperationObserver;
import org.objectstyle.cayenne.unittest.CayenneTestCase;
import org.objectstyle.cayenne.unittest.CayenneTestDatabaseSetup;

/**
 * @author Andrei Adamchik
 */
public class DataContextTestBase extends CayenneTestCase {

    public static final int artistCount = 25;
    public static final int galleryCount = 10;

    protected DataContext ctxt;
    protected TestOperationObserver opObserver;

    protected void setUp() throws java.lang.Exception {
        super.setUp();

        CayenneTestDatabaseSetup setup = getDatabaseSetup();
        setup.cleanTableData();
        populateTables();

        DataDomain dom = getDomain();
        setup.createPkSupportForMapEntities(
            (DataNode) dom.getDataNodes().iterator().next());

        ctxt = dom.createDataContext();
        opObserver = new TestOperationObserver();
    }

    public String artistName(int ind) {
        return "artist" + ind;
    }

    /** Give each artist a single painting. */
    public void populatePaintings() throws Exception {
        String insertPaint =
            "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) VALUES (?, ?, ?, ?)";

        Connection conn = getConnection();

        try {
            conn.setAutoCommit(false);

            PreparedStatement stmt = conn.prepareStatement(insertPaint);

            for (int i = 1; i <= artistCount; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "P_" + artistName(i));
                stmt.setInt(3, i);
                stmt.setBigDecimal(4, new BigDecimal(i * 1000));
                stmt.executeUpdate();
            }

            stmt.close();
            conn.commit();
        }
        finally {
            conn.close();
        }
    }

    public void populateTables() throws Exception {
        String insertArtist =
            "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) VALUES (?,?,?)";

        Connection conn = getConnection();

        try {
            conn.setAutoCommit(false);

            PreparedStatement stmt = conn.prepareStatement(insertArtist);
            long dateBase = System.currentTimeMillis();

            for (int i = 1; i <= artistCount; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, artistName(i));
                stmt.setDate(3, new java.sql.Date(dateBase + 1000 * 60 * 60 * 24 * i));
                stmt.executeUpdate();
            }

            stmt.close();
            conn.commit();

            String insertGal =
                "INSERT INTO GALLERY (GALLERY_ID, GALLERY_NAME) VALUES (?,?)";
            stmt = conn.prepareStatement(insertGal);

            for (int i = 1; i <= galleryCount; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "gallery" + i);
                stmt.executeUpdate();
            }

            stmt.close();
            conn.commit();
        }
        finally {
            conn.close();
        }
    }
}
