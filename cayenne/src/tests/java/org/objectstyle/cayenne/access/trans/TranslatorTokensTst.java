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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.apache.oro.text.regex.MatchResult;
import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * @author Andrei Adamchik
 */
public class TranslatorTokensTst extends TestCase {

    public void testConsistency1() throws Exception {
        StringBuffer buf = new StringBuffer();
        TranslatorTokens tokenizer = new TranslatorTokens();
        tokenizer.appendDataObjectToken(buf, new HashMap());

        try {
            tokenizer.appendDataObjectToken(buf, new HashMap());
            fail("Exception excpected, tokenizer invalid.");
        } catch (CayenneRuntimeException ex) {
            // expected
        }
    }

    public void testConsistency2() throws Exception {
        StringBuffer buf = new StringBuffer();
        TranslatorTokens tokenizer = new TranslatorTokens();
        tokenizer.appendAttributesToken(buf, new ArrayList());

        try {
            tokenizer.appendAttributesToken(buf, new ArrayList());
            fail("Exception excpected, tokenizer invalid.");
        } catch (CayenneRuntimeException ex) {
            // expected
        }
    }

    public void testEmptyTokens() throws Exception {
        TestTokenizer tokenizer = new TestTokenizer();
        tokenizer.resolveTokens(new StringBuffer());
        assertEquals(0, tokenizer.getTokens().size());
    }

    public void testOneToken() throws Exception {
        StringBuffer buf = new StringBuffer();
        TestTokenizer tokenizer = new TestTokenizer();
        tokenizer.appendDataObjectToken(buf, new HashMap());
        buf.append(" = ");
        tokenizer.appendAttributesToken(buf, new ArrayList());

        assertEquals("${id0} = ${dba0}", buf.toString());

        tokenizer.resolveTokens(buf);
        assertEquals(1, tokenizer.getTokens().size());
		assertEquals("${id0} = ${dba0}", tokenizer.getTokens().get(0));
    }

    public void testTwoTokens() throws Exception {
        StringBuffer buf = new StringBuffer();
        TestTokenizer tokenizer = new TestTokenizer();
        tokenizer.appendDataObjectToken(buf, new HashMap());
        buf.append(" = ");
        tokenizer.appendAttributesToken(buf, new ArrayList());
        
        buf.append(" AND xyz = 5 OR (a < 9) OR (");
        tokenizer.appendDataObjectToken(buf, new HashMap());
        buf.append(" != ");
        tokenizer.appendAttributesToken(buf, new ArrayList());
		buf.append(")");
		
		assertEquals("${id0} = ${dba0} AND xyz = 5 OR (a < 9) OR (${id1} != ${dba1})", buf.toString());
		
		
        tokenizer.resolveTokens(buf);
        assertEquals(2, tokenizer.getTokens().size());
        assertEquals("${id0} = ${dba0}", tokenizer.getTokens().get(0));
		assertEquals("${id1} != ${dba1}", tokenizer.getTokens().get(1));
    }

    class TestTokenizer extends TranslatorTokens {
        protected List tokens = new ArrayList();

        protected void processToken(StringBuffer buf, MatchResult result, int index) {
            tokens.add(result.group(0));
        }

        public List getTokens() {
            return tokens;
        }
    }
}
