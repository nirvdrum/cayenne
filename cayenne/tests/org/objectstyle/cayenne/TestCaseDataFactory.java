package org.objectstyle.cayenne;
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

import java.sql.Connection;
import java.sql.PreparedStatement;

public class TestCaseDataFactory {

    public static void createArtistWithPainting(
        String artistName,
        String[] paintingNames,
        boolean paintingInfo)
        throws Exception {
        String insertArtist =
            "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) VALUES (?,?,?)";

        Connection conn = org.objectstyle.TestMain.getSharedConnection();

        try {   
            conn.setAutoCommit(false);
                     
            PreparedStatement stmt = conn.prepareStatement(insertArtist);
            long dateBase = System.currentTimeMillis();

            stmt.setInt(1, 1);
            stmt.setString(2, artistName);
            stmt.setDate(3, new java.sql.Date(dateBase - 1000 * 60 * 60 * 24 * 365 * 30));
            stmt.executeUpdate();

            stmt.close();
            conn.commit();

            String insertPt =
                "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, ESTIMATED_PRICE, PAINTING_TITLE) VALUES (?,?,?,?)";
            stmt = conn.prepareStatement(insertPt);

            int len = paintingNames.length;
            if (len > 0) {
                for (int i = 0; i < len; i++) {
                    stmt.setInt(1, i + 1);
                    stmt.setInt(2, 1);
                    stmt.setFloat(3, 1000 * i);
                    stmt.setString(4, paintingNames[i]);
                    stmt.executeUpdate();
                }
                stmt.close();
                conn.commit();

                if (paintingInfo) {
                    String insertPtI =
                        "INSERT INTO PAINTING_INFO (PAINTING_ID, TEXT_REVIEW) VALUES (?,?)";
                    stmt = conn.prepareStatement(insertPtI);
                    for (int i = 0; i < len; i++) {
                        stmt.setInt(1, i + 1);
                        stmt.setString(2, "text: " + paintingNames[i]);
                        stmt.executeUpdate();
                    }

                    stmt.close();
                    conn.commit();
                }

            }
        }
        finally {
            conn.close();
        }
    }
}