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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.util.DefaultOperationObserver;
import org.objectstyle.cayenne.examples.aggregate.Artist;
import org.objectstyle.cayenne.examples.aggregate.Painting;
import org.objectstyle.cayenne.examples.aggregate.PaintingStats;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.SqlModifyQuery;

/**
 * This is a demo showing how to use simple aggregate functions in Cayenne.
 * 
 * @author Andrei Adamchik
 */
public class Main {

    protected DataContext context;

    public static void main(String[] args) {
        Main aggregateDemo = new Main();
        aggregateDemo.prepareData();
        
        // run demo

        Artist artistB = aggregateDemo.getArtist("Artist B.");

        // note that "getPaintingStats()" is not a relationship, but arther a custom method.
        PaintingStats statsB = artistB.getPaintingStats();
        
        System.out.println(
            "Artist B.: most expensive painting price: " + statsB.getMaxPrice());
        System.out.println(
            "Artist B.: least expensive painting price: " + statsB.getMinPrice());
        System.out.println("Artist B.: total paintings: " + statsB.getPaintingsCount());



        Artist artistA = aggregateDemo.getArtist("Artist A.");

        // note that "getPaintingStats()" is not a relationship, but arther a custom method.
        PaintingStats statsA = artistA.getPaintingStats(50.00);
        if (statsA == null) {
            System.out.println("Artist A.: no paintings matching criteria.");
        }
        else {
            System.out.println(
                "Artist A.: most expensive painting price in the range: "
                    + statsA.getMaxPrice());
            System.out.println(
                "Artist A.: least expensive painting price in the range: "
                    + statsA.getMinPrice());
            System.out.println(
                "Artist A.: total paintings in the range: " + statsA.getPaintingsCount());
        }
        
    }

    public Main() {
        this.context = DataContext.createDataContext();
    }

    public Artist getArtist(String artistName) {
        Expression qualifier = ExpressionFactory.matchExp("artistName", artistName);
        List artists = context.performQuery(new SelectQuery(Artist.class, qualifier));
        return (artists.size() > 0) ? (Artist) artists.get(0) : null;
    }

    /**
     * Prepares test data set.
     */
    public void prepareData() {

        // clean up the tables
        DefaultOperationObserver observer = new DefaultOperationObserver();
        List queries = new ArrayList(2);
        queries.add(new SqlModifyQuery(Painting.class, "delete from PAINTING"));
        queries.add(new SqlModifyQuery(Artist.class, "delete from ARTIST"));
        context.performQueries(queries, observer);

        // create test data - 2 artists, one with 3 paintings, another one - with 2
        Artist artist1 = (Artist) context.createAndRegisterNewObject("Artist");
        artist1.setArtistName("Artist A.");

        Painting painting11 = (Painting) context.createAndRegisterNewObject("Painting");
        painting11.setPaintingTitle("Picture of me");
        painting11.setEstimatedPrice(new BigDecimal(1000.00));
        painting11.setArtist(artist1);

        Painting painting12 = (Painting) context.createAndRegisterNewObject("Painting");
        painting12.setPaintingTitle("Picture of him");
        painting12.setEstimatedPrice(new BigDecimal(1050.00));
        painting12.setArtist(artist1);

        Painting painting13 = (Painting) context.createAndRegisterNewObject("Painting");
        painting13.setPaintingTitle("Picture of them");
        painting13.setEstimatedPrice(new BigDecimal(30.00));
        painting13.setArtist(artist1);

        Artist artist2 = (Artist) context.createAndRegisterNewObject("Artist");
        artist2.setArtistName("Artist B.");

        Painting painting21 = (Painting) context.createAndRegisterNewObject("Painting");
        painting21.setPaintingTitle("Summer");
        painting21.setEstimatedPrice(new BigDecimal(10000.00));
        painting21.setArtist(artist2);

        Painting painting22 = (Painting) context.createAndRegisterNewObject("Painting");
        painting22.setPaintingTitle("Winter");
        painting22.setEstimatedPrice(new BigDecimal(20000.00));
        painting22.setArtist(artist2);

        context.commitChanges();
    }
}
