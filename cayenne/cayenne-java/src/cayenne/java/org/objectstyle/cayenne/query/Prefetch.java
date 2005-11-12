/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.query;

import java.io.Serializable;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.util.Util;

/**
 * An object that defines a path and semantics of a single query prefetch. Note that
 * semanticsHint properrty is just that - a hint. Cayenne may choose to use different
 * prefetching strategy if the one configured in prefetch is not supported for any reason.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class Prefetch implements Serializable {

    public static final int UNDEFINED_SEMANTICS = 0;
    public static final int JOINT_PREFETCH_SEMANTICS = 1;
    public static final int DISJOINT_PREFETCH_SEMANTICS = 2;

    protected String path;
    protected int semanticsHint;

    // keep private constructor for Hessian serialization
    private Prefetch() {

    }

    /**
     * Creates a new Prefetch object with semantics hint set to
     * <em>Prefetch.UNDEFINED_SEMANTICS</em>, meaning that join semantics will be
     * resolved during query execution.
     */
    public Prefetch(String path) {
        this(path, UNDEFINED_SEMANTICS);
    }

    public Prefetch(String path, int semanticsHint) {
        if (path == null) {
            throw new IllegalArgumentException("Null 'path'");
        }

        this.path = path;
        this.semanticsHint = semanticsHint;
    }

    public String getPath() {
        return path;
    }

    public boolean isMultiStep() {
        return path.indexOf(Entity.PATH_SEPARATOR) > 0;
    }

    public int getSemanticsHint() {
        return semanticsHint;
    }

    public boolean isJointPrefetch() {
        return semanticsHint == JOINT_PREFETCH_SEMANTICS;
    }

    public boolean isDisjointPrefetch() {
        return semanticsHint == DISJOINT_PREFETCH_SEMANTICS;
    }

    /**
     * Implements checking for object equality. Two prefetches are considered equal if
     * their paths are equals, semantics hint is ignored in comparison.
     */
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof Prefetch)) {
            return false;
        }

        return Util.nullSafeEquals(path, ((Prefetch) object).getPath());
    }

    /**
     * Overrides super hashCode implementation to return hashCode compatible with
     * 'equals'.
     */
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder(43, 47);

        if (path != null) {
            builder.append(path);
        }

        return builder.toHashCode();
    }
}
