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
import java.util.List;
import java.util.Map;

import org.apache.oro.text.perl.Perl5Util;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * Helper class to process tokens in the translated queries. Tokens are inserted 
 * in the query string in the situations when translator can't create valid SQL due 
 * to lookahead limitations, e.g. in case of compound keys used in joins. On the long run 
 * a more reliable and integrated solution is needed.
 * 
 * @author Andrei Adamchik
 */
public class TranslatorTokens {
    protected static final Perl5Util matcher = new Perl5Util();
    protected static final String TOKEN_PAIR_RE =
        "/(\\$\\{[\\w]+\\}\\s[\\S]+\\s\\$\\{[\\w]+\\})/";

    protected List attributesSets;
    protected List valueSets;

    public TranslatorTokens() {
        attributesSets = new ArrayList(12);
        valueSets = new ArrayList(12);
    }

    public void appendDataObjectToken(StringBuffer buf, Map idSnapshot) {
        if (Math.abs(attributesSets.size() - 1 - valueSets.size()) > 1) {
            throw new CayenneRuntimeException(
                "No matching token, delta: "
                    + (attributesSets.size() - 1 - valueSets.size()));
        }

        String tokenId = "${id" + valueSets.size() + "}";
        buf.append(tokenId);
        valueSets.add(idSnapshot);
    }

    public void appendAttributesToken(StringBuffer buf, List attributes) {
        if (Math.abs(attributesSets.size() + 1 - valueSets.size()) > 1) {
            throw new CayenneRuntimeException(
                "No matching token, delta: "
                    + (attributesSets.size() + 1 - valueSets.size()));
        }

        String tokenId = "${dba" + attributesSets.size() + "}";
        buf.append(tokenId);
        attributesSets.add(attributes);
    }

    /**
     * Substitutes all tokens in the buffer with valid SQL content.
     */
    public void resolveTokens(StringBuffer buf) {
        int size = attributesSets.size();
        if (size != valueSets.size()) {
            throw new CayenneRuntimeException(
                "Token counts don't match, delta: "
                    + (attributesSets.size() - valueSets.size()));
        }

        // no tokens
        if (size == 0) {
            return;
        }

        String str = buf.toString();
        PatternMatcherInput input = new PatternMatcherInput(str);

        synchronized (matcher) {
            for (int i = 0; i < size; i++) {

                if (!matcher.match(TOKEN_PAIR_RE, input)) {
                    throw new CayenneRuntimeException(
                        "Unexpectedly few tokens: "
                            + i
                            + ". Expected: "
                            + size
                            + ". Content: "
                            + input);
                }

                processToken(buf, matcher.getMatch(), i);
            }

            if (matcher.match(TOKEN_PAIR_RE, input)) {
                throw new CayenneRuntimeException(
                    "Unexpectedly many tokens, at least "
                        + (size + 1)
                        + ". Expected: "
                        + size);
            }
        }
    }

    protected void processToken(StringBuffer buf, MatchResult result, int index) {

    }
}