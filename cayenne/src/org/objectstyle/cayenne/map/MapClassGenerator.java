package org.objectstyle.cayenne.map;
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


import java.io.Writer;
import java.util.Iterator;
import java.util.logging.Logger;

import org.objectstyle.cayenne.CayenneRuntimeException;


/** Generates Java classes source code using VTL (Velocity template engine) for
  * the ObjEntities in the DataMap. */
public abstract class MapClassGenerator {
    static Logger logObj = Logger.getLogger(MapClassGenerator.class.getName());

    public static final String SINGLE_CLASS_TEMPLATE = "dotemplates/singleclass.vm";
    public static final String SUBCLASS_TEMPLATE = "dotemplates/subclass.vm";
    public static final String SUPERCLASS_TEMPLATE = "dotemplates/superclass.vm";
    public static final String SUPERCLASS_PREFIX = "_";

    protected DataMap map;

    public MapClassGenerator(DataMap map) {
        this.map = map;
    }


    /** Provides child ClassGenerator with a Writer object
      * to store generated source code of the Java class corresponding 
      * to the <code>entity</code> parameter. 
      * @return Writer to store generated class source code or
      * null if this class generation should be skipped. */
    public abstract Writer openWriter(ObjEntity entity, String className) throws Exception ;


    /** Closes writer after class code has been successfully written by ClassGenerator. */
    public abstract void closeWriter(Writer out) throws Exception ;


    /** Runs class generation. Produces a pair of Java classes for
      * each ObjEntity in the map. Uses default Cayenne templates for classes. */
    public void generateClassPairs() throws Exception {
        generateClassPairs(SUBCLASS_TEMPLATE, SUPERCLASS_TEMPLATE, SUPERCLASS_PREFIX);
    }


    /** Runs class generation. Produces a pair of Java classes for
      * each ObjEntity in the map. This allows developers to use generated 
      * <b>subclass</b> for their custom code, while generated <b>superclass</b>
      * will contain Cayenne code. Superclass will be generated in the same package, 
      * its class name will be derived from the class name by adding a 
      * <code>superPrefix</code>. */
    public void generateClassPairs(String classTemplate, String superTemplate, String superPrefix) throws Exception {
        ClassGenerator mainGen = new ClassGenerator(classTemplate);
        ClassGenerator superGen = new ClassGenerator(superTemplate);

        // prefix is needed for both generators
        mainGen.setSuperPrefix(superPrefix);
        superGen.setSuperPrefix(superPrefix);

        Iterator it = map.getObjEntitiesAsList().iterator();
        while(it.hasNext()) {
            ObjEntity ent = (ObjEntity)it.next();

            // 1. do the superclass
            initClassGenerator(superGen, ent);


            Writer superOut = openWriter(ent, superPrefix + superGen.getClassName());
            if(superOut != null) {
                superGen.generateClass(superOut, ent);
                closeWriter(superOut);
            }

            // 2. do the main class
            initClassGenerator(mainGen, ent);
            Writer mainOut = openWriter(ent, mainGen.getClassName());
            if(mainOut != null) {
                mainGen.generateClass(mainOut, ent);
                closeWriter(mainOut);
            }
        }
    }



    /** Runs class generation. Produces a single Java class for
      * each ObjEntity in the map. Uses default Cayenne templates for classes. */
    public void generateSingleClasses() throws Exception {
        generateSingleClasses(SINGLE_CLASS_TEMPLATE);
    }


    /** Runs class generation. Produces a single Java class for
      * each ObjEntity in the map. */
    public void generateSingleClasses(String classTemplate) throws Exception {
        ClassGenerator gen = new ClassGenerator(classTemplate);

        Iterator it = map.getObjEntitiesAsList().iterator();
        while(it.hasNext()) {
            ObjEntity ent = (ObjEntity)it.next();
            initClassGenerator(gen, ent);
            Writer out = openWriter(ent, gen.getClassName());
            if(out == null)
                continue;

            gen.generateClass(out, ent);
            closeWriter(out);
        }
    }


    /** Initializes ClassGenerator with class name and package of a generated class. */
    protected void initClassGenerator(ClassGenerator gen, ObjEntity entity) {
        String fullClassName = entity.getClassName();
        int i = fullClassName.lastIndexOf(".");

        // dot in first or last position is invalid
        if(i == 0 || i + 1 == fullClassName.length())
            throw new CayenneRuntimeException("Invalid class mapping: " + fullClassName);
        else if(i < 0) {
            gen.setPackageName(null);
            gen.setClassName(fullClassName);
        } else {
            gen.setPackageName(fullClassName.substring(0, i));
            gen.setClassName(fullClassName.substring(i + 1));
        }
    }
}
