/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group
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
package org.objectstyle.cayenne.dataview;

import java.util.ArrayList;
import java.util.List;

import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;

public class BasicQueryBuilder {
  private ObjEntityView queryTarget;
  private List conditions = new ArrayList();

  public BasicQueryBuilder(ObjEntityView queryTarget) {
    this.queryTarget = queryTarget;
  }

  public void addEqual(String fieldName, Object value) {
    ObjEntityViewField field = queryTarget.getField(fieldName);
    String path = null;
    if (field.getCalcType().getValue() == CalcTypeEnum.NO_CALC_TYPE_VALUE) {
      path = field.getObjAttribute().getName();
    } else if (field.isLookup()) {
      path = field.getObjRelationship().getName();
    }
    Object rawValue = field.toRawValue(value);
    conditions.add(ExpressionFactory.matchExp(path, rawValue));
  }

  public void addRange(String fieldName, Object start, Object end) {
    ObjEntityViewField field = queryTarget.getField(fieldName);
    String path = null;
    if (field.getCalcType().getValue() == CalcTypeEnum.NO_CALC_TYPE_VALUE) {
      path = field.getObjAttribute().getName();
    } else if (field.isLookup()) {
      path = field.getObjRelationship().getName();
    }
    Object rawStart = field.toRawValue(start);
    Object rawEnd = field.toRawValue(end);
    Expression expr = null;
    if (rawStart != null && rawEnd != null)
      expr = ExpressionFactory.betweenExp(path, rawStart, rawEnd);
    else if (rawStart != null)
      expr = ExpressionFactory.greaterOrEqualExp(path, rawStart);
    else if (rawEnd != null)
      expr = ExpressionFactory.lessOrEqualExp(path, rawStart);

    if (expr != null)
      conditions.add(expr);
  }

  public void addLike(String fieldName, Object value, boolean caseSensetive) {
    ObjEntityViewField field = queryTarget.getField(fieldName);
    String path = null;
    if (field.getCalcType().getValue() == CalcTypeEnum.NO_CALC_TYPE_VALUE) {
      path = field.getObjAttribute().getName();
    } else if (field.isLookup()) {
      path = field.getObjRelationship().getName();
    }
    Object rawValue = field.toRawValue(value);
    String pattern = (rawValue != null ? rawValue.toString() : "");
    Expression expr = (caseSensetive ?
                       ExpressionFactory.likeExp(path, pattern) :
                       ExpressionFactory.likeIgnoreCaseExp(path, pattern));
    conditions.add(expr);
  }

  public SelectQuery getSelectQuery() {
    SelectQuery query = new SelectQuery(queryTarget.getObjEntity());
    if (!conditions.isEmpty()) {
      Expression qualifier = ExpressionFactory.joinExp(Expression.AND, conditions);
      query.setQualifier(qualifier);
    }
    return query;
  }
}