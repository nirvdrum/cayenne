package org.objectstyle.cayenne.dataview;

import java.util.*;
import org.apache.commons.lang.enum.ValuedEnum;

public class CalcTypeEnum extends ValuedEnum {
  public static final int NO_CALC_TYPE_VALUE = 1;
  public static final int CALC_TYPE_VALUE = 2;
  public static final int LOOKUP_TYPE_VALUE = 3;

  public static final String NO_CALC_TYPE_NAME = "nocalc";
  public static final String CALC_TYPE_NAME = "calc";
  public static final String LOOKUP_TYPE_NAME = "lookup";

  public static final CalcTypeEnum  NO_CALC_TYPE  = new CalcTypeEnum( NO_CALC_TYPE_NAME, NO_CALC_TYPE_VALUE );
  public static final CalcTypeEnum  CALC_TYPE  = new CalcTypeEnum( CALC_TYPE_NAME, CALC_TYPE_VALUE );
  public static final CalcTypeEnum  LOOKUP_TYPE  = new CalcTypeEnum( LOOKUP_TYPE_NAME, LOOKUP_TYPE_VALUE );

  protected CalcTypeEnum(String name, int value) {
    super(name, value);
  }

  public static CalcTypeEnum getEnum(String calcType) {
     return (CalcTypeEnum) getEnum(CalcTypeEnum.class, calcType);
   }

   public static CalcTypeEnum getEnum(int calcType) {
     return (CalcTypeEnum) getEnum(CalcTypeEnum.class, calcType);
   }

   public static Map getEnumMap() {
     return getEnumMap(CalcTypeEnum.class);
   }

   public static List getEnumList() {
     return getEnumList(CalcTypeEnum.class);
   }

   public static Iterator iterator() {
     return iterator(CalcTypeEnum.class);
   }

   public final Class getEnumClass() {
     return CalcTypeEnum.class;
   }
}