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

package org.objectstyle.cayenne.access;

import java.util.List;

import org.objectstyle.art.BlobTest;
import org.objectstyle.cayenne.access.types.ByteArrayTypeTst;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextBlobTst extends CayenneTestCase {
    protected DataContext ctxt;

    protected void setUp() throws Exception {
        getDatabaseSetup().cleanTableData();
        ctxt = getDomain().createDataContext();
    }

    protected boolean skipTests() {
        return !super.getDatabaseSetupDelegate().supportsLobs();
    }

    public void testBlob() throws Exception {
        if (skipTests()) {
            return;
        }

        byte[] bytes1 = new byte[] { 1, 2, 3, 4 };
        byte[] bytes2 = new byte[] { 'a', 'b', 'c', 'd' };

        // insert new blob
        BlobTest blobObj1 =
            (BlobTest) ctxt.createAndRegisterNewObject("BlobTest");

        blobObj1.setBlobCol(bytes1);
        ctxt.commitChanges();

        // read the BLOB in the new context
        DataContext ctxt2 = getDomain().createDataContext();
        List objects2 = ctxt2.performQuery(new SelectQuery(BlobTest.class));
        assertEquals(1, objects2.size());

        BlobTest blobObj2 = (BlobTest) objects2.get(0);
        ByteArrayTypeTst.assertByteArraysEqual(blobObj1.getBlobCol(), blobObj2.getBlobCol());

        // update and save Blob
        blobObj2.setBlobCol(bytes2);
        ctxt2.commitChanges();

        // read into yet another context and check for changes
        DataContext ctxt3 = getDomain().createDataContext();
        List objects3 = ctxt3.performQuery(new SelectQuery(BlobTest.class));
        assertEquals(1, objects3.size());

        BlobTest blobObj3 = (BlobTest) objects3.get(0);
        ByteArrayTypeTst.assertByteArraysEqual(blobObj2.getBlobCol(), blobObj3.getBlobCol());
    }
}
