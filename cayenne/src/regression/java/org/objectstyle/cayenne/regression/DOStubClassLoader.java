package org.objectstyle.cayenne.regression;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.objectstyle.cayenne.*;

class DOStubClassLoader extends ClassLoader {
  private String superClassName = "org.objectstyle.cayenne.CayenneDataObject";

  public Class findClass(String name) {
    byte[] b = loadClassData(name);
    return defineClass(name, b, 0, b.length);
  }

  private byte[] loadClassData(String name) {
    ClassGen cg = new ClassGen(name, superClassName,
                             "<generated>", Constants.ACC_PUBLIC | Constants.ACC_SUPER,
                             null);
    cg.addEmptyConstructor(Constants.ACC_PUBLIC);
    JavaClass hw = cg.getJavaClass();
    return hw.getBytes();
  }
}