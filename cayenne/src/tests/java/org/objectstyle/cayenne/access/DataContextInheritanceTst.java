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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.access.util.DefaultOperationObserver;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.SqlModifyQuery;
import org.objectstyle.cayenne.testdo.inherit.AbstractPerson;
import org.objectstyle.cayenne.testdo.inherit.CustomerRepresentative;
import org.objectstyle.cayenne.testdo.inherit.Employee;
import org.objectstyle.cayenne.testdo.inherit.Manager;
import org.objectstyle.cayenne.unit.PeopleTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextInheritanceTst extends PeopleTestCase {
    protected DataContext context;

    protected void setUp() throws Exception {
        super.setUp();

        context = createDataContext();
        deleteTestData();
        setupData();
    }

    public void testNoInheritanceResolving() throws Exception {
        // select Abstract Ppl
        SelectQuery query = new SelectQuery(AbstractPerson.class);
        assertFalse(query.isResolvingInherited());
        List abstractPpl = context.performQuery(query);
        assertEquals(6, abstractPpl.size());

        Iterator it = abstractPpl.iterator();
        while (it.hasNext()) {
            Object next = it.next();
            assertTrue(
                "Unknown object class: " + next.getClass().getName(),
                next.getClass() == AbstractPerson.class);
        }
    }

    public void testInheritanceResolving() throws Exception {
        List customerReps = new ArrayList();
        List employees = new ArrayList();
        List managers = new ArrayList();

        // select Abstract Ppl
        SelectQuery query = new SelectQuery(AbstractPerson.class);
        query.setResolvingInherited(true);
        assertTrue(query.isResolvingInherited());
        List abstractPpl = context.performQuery(query);
        assertEquals(6, abstractPpl.size());

        Iterator it = abstractPpl.iterator();
        while (it.hasNext()) {
            Object next = it.next();

            if (next instanceof CustomerRepresentative) {
                customerReps.add(next);
            }

            if (next instanceof Employee) {
                employees.add(next);
            }

            if (next instanceof Manager) {
                managers.add(next);
            }
        }

        assertEquals(1, customerReps.size());
        assertEquals(5, employees.size());
        assertEquals(2, managers.size());
    }

    private void setupData() throws Exception {
        List queries = new ArrayList();
        queries.add(
            new SqlModifyQuery(
                AbstractPerson.class,
                "insert into PERSON (CLIENT_COMPANY_ID, CLIENT_CONTACT_TYPE, DEPARTMENT_ID, NAME, PERSON_ID, PERSON_TYPE, SALARY) "
                    + "values (null, null, null, 'e1', 1, 'EE', 20000)"));
        queries.add(
            new SqlModifyQuery(
                AbstractPerson.class,
                "insert into PERSON (CLIENT_COMPANY_ID, CLIENT_CONTACT_TYPE, DEPARTMENT_ID, NAME, PERSON_ID, PERSON_TYPE, SALARY) "
                    + "values (null, null, null, 'e2', 2, 'EE', 25000)"));
        queries.add(
            new SqlModifyQuery(
                AbstractPerson.class,
                "insert into PERSON (CLIENT_COMPANY_ID, CLIENT_CONTACT_TYPE, DEPARTMENT_ID, NAME, PERSON_ID, PERSON_TYPE, SALARY) "
                    + "values (null, null, null, 'e3', 3, 'EE', 28000)"));
        queries.add(
            new SqlModifyQuery(
                AbstractPerson.class,
                "insert into PERSON (CLIENT_COMPANY_ID, CLIENT_CONTACT_TYPE, DEPARTMENT_ID, NAME, PERSON_ID, PERSON_TYPE, SALARY) "
                    + "values (null, null, null, 'm1', 4, 'EM', 30000)"));
        queries.add(
            new SqlModifyQuery(
                AbstractPerson.class,
                "insert into PERSON (CLIENT_COMPANY_ID, CLIENT_CONTACT_TYPE, DEPARTMENT_ID, NAME, PERSON_ID, PERSON_TYPE, SALARY) "
                    + "values (null, null, null, 'm2', 5, 'EM', 40000)"));
        queries.add(
            new SqlModifyQuery(
                AbstractPerson.class,
                "insert into PERSON (CLIENT_COMPANY_ID, CLIENT_CONTACT_TYPE, DEPARTMENT_ID, NAME, PERSON_ID, PERSON_TYPE, SALARY)"
                    + "values (null, null, null, 'c1', 6, 'C', null)"));
        context.performQueries(queries, new DefaultOperationObserver());
    }
}
