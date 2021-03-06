/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.access.jdbc;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * @since 3.0
 */
class EJBQLIdentifierColumnsTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    private Set<String> columns;

    EJBQLIdentifierColumnsTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    @Override
    public boolean visitIdentifier(EJBQLExpression expression) {

        Map<String, String> xfields = null;
        if (context.isAppendingResultColumns()) {
            xfields = context.nextEntityResult().getFields();
        }

        // assign whatever we have to a final ivar so that it can be accessed within
        // the inner class
        final Map<String, String> fields = xfields;
        final String idVar = expression.getText();

        // append all table columns ... the trick is to follow the algorithm for
        // describing the fields in the expression compiler, so that we could assign
        // columns labels from FieldResults in the order we encounter them here...
        // TODO: andrus 2008/02/17 - this is a bit of a hack, think of a better solution

        ClassDescriptor descriptor = context.getEntityDescriptor(idVar);

        PropertyVisitor visitor = new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                ObjAttribute oa = property.getAttribute();
                Iterator<?> dbPathIterator = oa.getDbPathIterator();

                EJBQLJoinAppender joinAppender = null;
                String marker = null;
                EJBQLTableId lhsId = new EJBQLTableId(idVar);

                while (dbPathIterator.hasNext()) {
                    Object pathPart = dbPathIterator.next();

                    if (pathPart == null) {
                        throw new CayenneRuntimeException(
                                "ObjAttribute has no component: " + oa.getName());
                    }
                    else if (pathPart instanceof DbRelationship) {

                        if (marker == null) {
                            marker = EJBQLJoinAppender.makeJoinTailMarker(idVar);
                            joinAppender = context
                                    .getTranslatorFactory()
                                    .getJoinAppender(context);
                        }

                        DbRelationship dr = (DbRelationship) pathPart;

                        EJBQLTableId rhsId = new EJBQLTableId(lhsId, dr.getName());
                        joinAppender.appendOuterJoin(marker, lhsId, rhsId);
                        lhsId = rhsId;
                    }
                    else if (pathPart instanceof DbAttribute) {
                        appendColumn(idVar, oa, (DbAttribute) pathPart, fields, oa
                                .getType());
                    }
                }
                return true;
            }

            public boolean visitToMany(ToManyProperty property) {
                visitRelationship(property);
                return true;
            }

            public boolean visitToOne(ToOneProperty property) {
                visitRelationship(property);
                return true;
            }

            private void visitRelationship(ArcProperty property) {
                ObjRelationship rel = property.getRelationship();
                DbRelationship dbRel = rel.getDbRelationships().get(0);

                for (DbJoin join : dbRel.getJoins()) {
                    DbAttribute src = join.getSource();
                    appendColumn(idVar, null, src, fields);
                }
            }
        };

        // EJBQL queries are polymorphic by definition - there is no distinction between
        // inheritance/no-inheritance fetch
        descriptor.visitAllProperties(visitor);

        // append id columns ... (some may have been appended already via relationships)
        DbEntity table = descriptor.getEntity().getDbEntity();
        for (DbAttribute pk : table.getPrimaryKeys()) {
            appendColumn(idVar, null, pk, fields);
        }

        // append inheritance discriminator columns...
        Iterator<ObjAttribute> discriminatorColumns = descriptor.getDiscriminatorColumns();
        while (discriminatorColumns.hasNext()) {
            
            ObjAttribute attribute = discriminatorColumns.next();
            appendColumn(idVar, attribute, attribute.getDbAttribute(), fields);
        }

        return false;
    }

    public void appendColumn(
            String identifier,
            ObjAttribute property,
            DbAttribute column,
            Map<String, String> fields) {
        appendColumn(identifier, property, column, fields, null);
    }

    public void appendColumn(
            String identifier,
            ObjAttribute property,
            DbAttribute column,
            Map<String, String> fields,
            String javaType) {

        DbEntity table = (DbEntity) column.getEntity();
        String alias = context.getTableAlias(identifier, table.getFullyQualifiedName());
        String columnName = alias + "." + column.getName();

        Set<String> columns = getColumns();

        if (columns.add(columnName)) {

            context.append(columns.size() > 1 ? ", " : " ");

            if (context.isAppendingResultColumns()) {
                context.append("#result('");
            }

            context.append(columnName);

            if (context.isAppendingResultColumns()) {
                if (javaType == null) {
                    javaType = TypesMapping.getJavaBySqlType(column.getType());
                }

                String columnLabel = fields.get(property != null ? property
                        .getDbAttributePath() : column.getName());

                // TODO: andrus 6/27/2007 - the last parameter is an unofficial "jdbcType"
                // pending CAY-813 implementation, switch to #column directive
                context
                        .append("' '")
                        .append(javaType)
                        .append("' '")
                        .append(columnLabel)
                        .append("' '")
                        .append(columnLabel)
                        .append("' " + column.getType())
                        .append(")");
            }
        }
    }

    private Set<String> getColumns() {

        if (columns == null) {
            columns = new HashSet<String>();
        }

        return columns;
    }
}
