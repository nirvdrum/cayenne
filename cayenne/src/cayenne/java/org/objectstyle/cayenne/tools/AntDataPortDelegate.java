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
package org.objectstyle.cayenne.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.oro.text.perl.Perl5Util;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.objectstyle.cayenne.access.DataPort;
import org.objectstyle.cayenne.access.DataPortDelegate;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Query;

/**
 * DataPortDelegate implementation that works in the context of Ant DataPortTask task
 * execution, performing entity filtering and logging functions.
 * 
 * @author Andrei Adamchik
 * @since 1.2: Prior to 1.2 DataPort classes were a part of cayenne-examples package.
 */
class AntDataPortDelegate implements DataPortDelegate {

    private static final Perl5Util regexUtil = new Perl5Util();
    private static final String[] emptyArray = new String[0];

    protected Task parentTask;
    protected String[] mapFilters;
    protected String[] entityIncludeFilters;
    protected String[] entityExcludeFilters;

    protected long timestamp;
    protected DbEntity lastEntity;

    public AntDataPortDelegate(Task parentTask, String mapsPattern,
            String includeEntitiesPattern, String excludeEntitiesPattern) {
        this.parentTask = parentTask;

        mapFilters = tokenizePattern(mapsPattern);
        entityIncludeFilters = tokenizePattern(includeEntitiesPattern);
        entityExcludeFilters = tokenizePattern(excludeEntitiesPattern);
    }

    /**
     * Returns an array of valid Jakarta ORO regular expressions. Takes a comma-separated
     * list of patterns, attempting to convert them to the ORO syntax. E.g.
     * <p>
     * <code>"billing_*,user?"</code> will become a set of two expressions:
     * <p>
     * <code>/billing_.* /</code><br>
     * <code>/user.?/</code><br>
     */
    protected String[] tokenizePattern(String pattern) {
        if (pattern != null && pattern.length() > 0) {
            StringTokenizer toks = new StringTokenizer(pattern, ",");

            int len = toks.countTokens();
            if (len == 0) {
                return emptyArray;
            }

            List patterns = new ArrayList(len);
            for (int i = 0; i < len; i++) {
                String nextPattern = toks.nextToken();
                StringBuffer buffer = new StringBuffer();

                // convert * into regex syntax
                // e.g. abc*x becomes /^abc.*x$/
                // or abc?x becomes /^abc.?x$/
                buffer.append("/^");
                for (int j = 0; j < nextPattern.length(); j++) {
                    char nextChar = nextPattern.charAt(j);
                    if (nextChar == '*' || nextChar == '?') {
                        buffer.append('.');
                    }
                    buffer.append(nextChar);
                }
                buffer.append("$/");

                String finalPattern = buffer.toString();

                // test the pattern
                try {
                    regexUtil.match(finalPattern, "abc_123");
                }
                catch (Exception e) {
                    parentTask.log("Ignoring invalid pattern ["
                            + nextPattern
                            + "], reason: "
                            + e.getMessage(), Project.MSG_WARN);
                    continue;
                }

                patterns.add(finalPattern);
            }

            return (String[]) patterns.toArray(new String[patterns.size()]);
        }
        else {
            return emptyArray;
        }
    }

    /**
     * Applies preconfigured list of filters to the list, removing entities that do not
     * pass the filter.
     */
    protected List filterEntities(List entities) {
        if (entities == null || entities.isEmpty()) {
            return entities;
        }

        if (mapFilters.length == 0
                && entityIncludeFilters.length == 0
                && entityExcludeFilters.length == 0) {
            return entities;
        }

        Iterator it = entities.iterator();
        while (it.hasNext()) {
            DbEntity entity = (DbEntity) it.next();

            if (!passedDataMapFilter(entity)) {
                it.remove();
                continue;
            }

            if (!passedEntityIncludeFilter(entity)) {
                it.remove();
                continue;
            }

            if (!passedEntityExcludeFilter(entity)) {
                it.remove();
                continue;
            }
        }

        return entities;
    }

    /**
     * Returns true if the entity passes a set of DataMap filters or if there is no
     * DataMap filters.
     */
    protected boolean passedDataMapFilter(DbEntity entity) {
        if (mapFilters.length == 0) {
            return true;
        }

        DataMap map = entity.getDataMap();
        if (map == null) {
            return true;
        }

        String mapName = map.getName();
        for (int i = 0; i < mapFilters.length; i++) {
            if (regexUtil.match(mapFilters[i], mapName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the entity matches any one of the "include" patterns, or if there
     * is no "include" patterns defined.
     */
    protected boolean passedEntityIncludeFilter(DbEntity entity) {
        if (entityIncludeFilters.length == 0) {
            return true;
        }

        String entityName = entity.getName();
        for (int i = 0; i < entityIncludeFilters.length; i++) {
            if (regexUtil.match(entityIncludeFilters[i], entityName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the entity does not match any one of the "exclude" patterns, or if
     * there is no "exclude" patterns defined.
     */
    protected boolean passedEntityExcludeFilter(DbEntity entity) {
        if (entityExcludeFilters.length == 0) {
            return true;
        }

        String entityName = entity.getName();
        for (int i = 0; i < entityExcludeFilters.length; i++) {
            if (regexUtil.match(entityExcludeFilters[i], entityName)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Implements the delegate method to filter the list of entities applying filtering
     * rules encapsulated by this object.
     */
    public List willPortEntities(DataPort portTool, List entities) {
        return filterEntities(entities);
    }

    /**
     * Logs entity porting event using Ant logger.
     */
    public GenericSelectQuery willPortEntity(
            DataPort portTool,
            DbEntity entity,
            GenericSelectQuery query) {
        parentTask.log("  Porting '" + entity.getName() + "'");
        lastEntity = entity;
        timestamp = System.currentTimeMillis();

        return query;
    }

    public void didPortEntity(DataPort portTool, DbEntity entity, int rowCount) {
        String timestampLabel = "";
        if (lastEntity == entity) {
            timestampLabel = " in " + (System.currentTimeMillis() - timestamp) + " ms.";
        }

        String label = (rowCount == 1) ? "1 row transferred" : rowCount
                + " rows transferred";
        parentTask.log("  Done porting "
                + entity.getName()
                + ", "
                + label
                + timestampLabel, Project.MSG_VERBOSE);
    }

    public List willCleanData(DataPort portTool, List entities) {
        return filterEntities(entities);
    }

    public Query willCleanData(DataPort portTool, DbEntity entity, Query query) {
        parentTask.log("  Deleting " + entity.getName(), Project.MSG_VERBOSE);
        lastEntity = entity;
        timestamp = System.currentTimeMillis();
        return query;
    }

    public void didCleanData(DataPort portTool, DbEntity entity, int rowCount) {
        String timestampLabel = "";
        if (lastEntity == entity) {
            timestampLabel = " in " + (System.currentTimeMillis() - timestamp) + " ms.";
        }

        String label = (rowCount == 1) ? "1 row deleted" : rowCount + " rows deleted";
        parentTask.log("  Done deleting "
                + entity.getName()
                + ", "
                + label
                + timestampLabel, Project.MSG_VERBOSE);
    }
}