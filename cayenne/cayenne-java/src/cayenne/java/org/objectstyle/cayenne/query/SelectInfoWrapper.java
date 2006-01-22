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

import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper for another SelectInfo allowing to override some but not all properties.
 * 
 * @author Andrus Adamchik
 */
class SelectInfoWrapper implements SelectInfo {

    SelectInfo info;
    Map overrides;

    public SelectInfoWrapper(SelectInfo info) {
        this.info = info;
    }

    void override(String key, Object value) {
        if (overrides == null) {
            overrides = new HashMap();
        }

        overrides.put(key, value);
    }

    boolean overrideExists(String key) {
        return overrides != null && overrides.containsKey(key);
    }

    public String getCachePolicy() {
        return (overrideExists(SelectInfo.CACHE_POLICY_PROPERTY)) ? (String) overrides
                .get(SelectInfo.CACHE_POLICY_PROPERTY) : info.getCachePolicy();
    }

    public boolean isFetchingDataRows() {
        if (!overrideExists(SelectInfo.FETCHING_DATA_ROWS_PROPERTY)) {
            return info.isFetchingDataRows();
        }

        Boolean b = (Boolean) overrides.get(SelectInfo.FETCHING_DATA_ROWS_PROPERTY);
        return b != null && b.booleanValue();
    }

    public boolean isRefreshingObjects() {
        if (!overrideExists(SelectInfo.REFRESHING_OBJECTS_PROPERTY)) {
            return info.isRefreshingObjects();
        }

        Boolean b = (Boolean) overrides.get(SelectInfo.REFRESHING_OBJECTS_PROPERTY);
        return b != null && b.booleanValue();
    }

    public boolean isResolvingInherited() {
        if (!overrideExists(SelectInfo.RESOLVING_INHERITED_PROPERTY)) {
            return info.isResolvingInherited();
        }

        Boolean b = (Boolean) overrides.get(SelectInfo.RESOLVING_INHERITED_PROPERTY);
        return b != null && b.booleanValue();
    }

    public int getPageSize() {
        if (!overrideExists(SelectInfo.PAGE_SIZE_PROPERTY)) {
            return info.getPageSize();
        }

        Number n = (Number) overrides.get(SelectInfo.PAGE_SIZE_PROPERTY);
        return n != null ? n.intValue() : 0;
    }

    public int getFetchLimit() {
        if (!overrideExists(SelectInfo.FETCH_LIMIT_PROPERTY)) {
            return info.getFetchLimit();
        }

        Number n = (Number) overrides.get(SelectInfo.FETCH_LIMIT_PROPERTY);
        return n != null ? n.intValue() : 0;
    }

    public PrefetchTreeNode getPrefetchTree() {
        return info.getPrefetchTree();
    }
}
