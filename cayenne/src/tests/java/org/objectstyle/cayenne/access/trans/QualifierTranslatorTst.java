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
package org.objectstyle.cayenne.access.trans;

import org.apache.log4j.Logger;

import org.objectstyle.cayenne.CayenneTestCase;
import org.objectstyle.cayenne.exp.*;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.QualifiedQuery;
import org.objectstyle.cayenne.query.Query;

public class QualifierTranslatorTst extends CayenneTestCase {
	static Logger logObj =
		Logger.getLogger(QualifierTranslatorTst.class.getName());

	protected TstQueryAssembler qa;

	public QualifierTranslatorTst(String name) {
		super(name);
	}

	protected void setUp() throws java.lang.Exception {
		qa = TstQueryAssembler.assembler(getSharedDomain(), Query.SELECT_QUERY);
	}

	public void testNonQualifiedQuery() throws java.lang.Exception {
		qa.dispose();
		qa = TstQueryAssembler.assembler(getSharedDomain(), Query.INSERT_QUERY);

		try {
			new QualifierTranslator(qa).doTranslation();
			fail();
		} catch (ClassCastException ccex) {
			// exception expected
		} finally {
			qa.dispose();
		}
	}

	public void testNullQualifier() throws java.lang.Exception {
		try {
			assertNull(new QualifierTranslator(qa).doTranslation());
		} finally {
			qa.dispose();
		}
	}

	public void testUnary() throws java.lang.Exception {
		doExpressionTest(new TstUnaryExpSuite());
	}

	public void testBinary() throws java.lang.Exception {
		doExpressionTest(new TstBinaryExpSuite());
	}

	public void testTernary() throws java.lang.Exception {
		doExpressionTest(new TstTernaryExpSuite());
	}

	private void doExpressionTest(TstExpressionSuite suite)
		throws java.lang.Exception {

		try {
			TstExpressionCase[] cases = suite.cases();

			int len = cases.length;
			for (int i = 0; i < len; i++) {
				try {
					((QualifiedQuery) qa.getQuery()).setQualifier(
						cases[i].getCayenneExp());
					ObjEntity ent =
						getSharedDomain().lookupEntity(
							cases[i].getRootEntity());
					assertNotNull(ent);
					qa.getQuery().setObjEntityName(ent.getName());
					cases[i].assertTranslatedWell(
						new QualifierTranslator(qa).doTranslation(),
						false);
				} catch (Exception ex) {
					System.out.println("Failed case: [" + i + "]: " + cases[i]);
					throw ex;
				}
			}
		} finally {
			qa.dispose();
		}
	}
}