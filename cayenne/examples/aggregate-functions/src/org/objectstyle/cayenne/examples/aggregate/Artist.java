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
package org.objectstyle.cayenne.examples.aggregate;

import java.math.BigDecimal;
import java.util.List;

import org.objectstyle.cayenne.examples.aggregate.auto._Artist;
import org.objectstyle.cayenne.examples.aggregate.auto._Painting;
import org.objectstyle.cayenne.examples.aggregate.auto._PaintingStats;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;

public class Artist extends _Artist {

    /**
     * Returns stats for all paintings of this artist.
     */
    public PaintingStats getPaintingStats() {
        SelectQuery query =
            new SelectQuery(
                PaintingStats.class,
                ExpressionFactory.matchExp(_PaintingStats.ARTIST_PROPERTY, this));

        List stats = getDataContext().performQuery(query);
        return (stats.size() > 0) ? (PaintingStats) stats.get(0) : null;
    }

    /**
     * Returns stats for all paintings at least as expensive as
     * the supplied doubel parameter.
     */
    public PaintingStats getPaintingStats(double atLeastAsExpensive) {
        SelectQuery query = new SelectQuery(PaintingStats.class);

        // note how we are building a "parent" qualifier (WHERE clause, not HAVING)
        query.setParentObjEntityName("Painting");
        query.setParentQualifier(
            ExpressionFactory.greaterOrEqualExp(
                _Painting.ESTIMATED_PRICE_PROPERTY,
                new BigDecimal(atLeastAsExpensive)));
        query.andParentQualifier(
            ExpressionFactory.matchExp(_PaintingStats.ARTIST_PROPERTY, this));

        List stats = getDataContext().performQuery(query);
        return (stats.size() > 0) ? (PaintingStats) stats.get(0) : null;
    }
}
