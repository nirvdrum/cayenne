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

import java.util.List;

import org.objectstyle.art.FlattenedTest1;
import org.objectstyle.art.FlattenedTest2;
import org.objectstyle.art.FlattenedTest3;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextComplexQualifierTst extends CayenneTestCase {
    protected DataContext ctxt;

    protected void setUp() throws Exception {
        getDatabaseSetup().cleanTableData();
        ctxt = getDomain().createDataContext();
    }

    public void testQualifyOnToManyFlattened() throws Exception {
        FlattenedTest1 obj01 =
            (FlattenedTest1) ctxt.createAndRegisterNewObject("FlattenedTest1");
        FlattenedTest2 obj02 =
            (FlattenedTest2) ctxt.createAndRegisterNewObject("FlattenedTest2");
        FlattenedTest3 obj031 =
            (FlattenedTest3) ctxt.createAndRegisterNewObject("FlattenedTest3");
        FlattenedTest3 obj032 =
            (FlattenedTest3) ctxt.createAndRegisterNewObject("FlattenedTest3");

        FlattenedTest1 obj11 =
            (FlattenedTest1) ctxt.createAndRegisterNewObject("FlattenedTest1");
        FlattenedTest2 obj12 =
            (FlattenedTest2) ctxt.createAndRegisterNewObject("FlattenedTest2");
        FlattenedTest3 obj131 =
            (FlattenedTest3) ctxt.createAndRegisterNewObject("FlattenedTest3");

        obj01.setName("t01");
        obj02.setName("t02");
        obj031.setName("t031");
        obj032.setName("t032");
        obj02.setToFT1(obj01);
        obj02.addToFt3Array(obj031);
        obj02.addToFt3Array(obj032);

        obj11.setName("t11");
        obj131.setName("t131");
        obj12.setName("t12");
        obj12.addToFt3Array(obj131);
        obj12.setToFT1(obj11);

        ctxt.commitChanges();

        // test 1: qualify on flattened attribute
        Expression qual1 = ExpressionFactory.matchExp("ft3Array.name", "t031");
        SelectQuery query1 = new SelectQuery(FlattenedTest1.class, qual1);
        List objects1 = ctxt.performQuery(query1);

        assertEquals(1, objects1.size());
        assertSame(obj01, objects1.get(0));

        // test 2: qualify on flattened relationship
        Expression qual2 = ExpressionFactory.matchExp("ft3Array", obj131);
        SelectQuery query2 = new SelectQuery(FlattenedTest1.class, qual2);
        List objects2 = ctxt.performQuery(query2);

        assertEquals(1, objects2.size());
        assertSame(obj11, objects2.get(0));
    }

}
