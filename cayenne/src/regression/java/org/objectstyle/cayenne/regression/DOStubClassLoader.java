package org.objectstyle.cayenne.regression;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ClassGen;

/**
 * DOStubClassLoader is a simple class loader to generate new types of DataObjects
 * (new classes) at runtime. This behavior is needed to trick Cayenne algorithms
 * based on usage of Classes of DataObject descendants for identifiers.
 *
 * @author Andriy Shapochka
 */

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