package org.objectstyle.cayenne.dataview;

import java.util.*;
import org.objectstyle.cayenne.exp.*;
import org.objectstyle.cayenne.query.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.map.*;

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