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
package org.objectstyle.cayenne.project.validator;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.project.ProjectPath;

/**
 * @author Andrei Adamchik
 */
public class DataNodeValidatorTst extends ValidatorTestBase {

    /**
     * Constructor for DataNodeValidatorTst.
     * @param arg0
     */
    public DataNodeValidatorTst(String arg0) {
        super(arg0);
    }

    public void testValidateDataNodes() throws Exception {
        // should succeed
        DataDomain d1 = new DataDomain("abc");
        DataNode n1 = new DataNode("1");
        n1.setAdapter(new JdbcAdapter());
        n1.setDataSourceFactory("123");
        n1.setDataSourceLocation("qqqq");
        d1.addNode(n1);

        validator.reset();
        new DataNodeValidator().validateObject(new ProjectPath(new Object[] { project, d1, n1 }), validator);
        assertValidator(ValidationInfo.VALID);

        // should complain about no location
        DataNode n2 = new DataNode("2");
        n2.setAdapter(new JdbcAdapter());
        n2.setDataSourceFactory("123");
        d1.addNode(n2);

        validator.reset();
        new DataNodeValidator().validateObject(new ProjectPath(new Object[] { project, d1, n2 }), validator);
        assertValidator(ValidationInfo.ERROR);

        // should complain about duplicate name
        DataNode n3 = new DataNode("3");
        n3.setAdapter(new JdbcAdapter());
        n3.setDataSourceFactory("123");
        d1.addNode(n3);
        n3.setName(n1.getName());

        validator.reset();
        new DataNodeValidator().validateObject(new ProjectPath(new Object[] { project, d1, n3 }), validator);
        assertValidator(ValidationInfo.ERROR);
    }

}
