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
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.objectstyle.cayenne.CayenneRuntimeException;


/** Generates Java class source code using VTL (Velocity template engine) based on
  * template and ObjEntity.
  *
  * @author Andrei Adamchik
  */
public class ClassGenerator {
    static Logger logObj = Logger.getLogger(ClassGenerator.class.getName());


    static {
        try {
            // use ClasspathResourceLoader for velocity templates lookup
            Properties props = new Properties();
            props.put("resource.loader", "class");
            props.put("class.resource.loader.class",
                      "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            Velocity.init(props);
        } catch(Exception ex) {
            throw new CayenneRuntimeException("Can't initialize VTL", ex);
        }
    }


    protected Template classTemplate;
    protected Context velCtxt;


    protected ObjEntity entity;

    // template substitution values
    protected String packageName;
    protected String className;
    protected String superPrefix;
    protected String prop;


    /** Loads Velocity template used for class generation. */
    public ClassGenerator(String template)
    throws Exception {
        velCtxt = new VelocityContext();
        velCtxt.put("classGen", this);
        classTemplate = Velocity.getTemplate(template)
                        ;
    }


    /** Generates code for <code>entity</code> ObjEntity. Source code is written to
      * <code>out</code> Writer.*/
    public void generateClass(Writer out, ObjEntity entity) throws Exception {
        this.entity = entity;
        classTemplate.merge(velCtxt, out);
    }


    /** Returns Java package name of the class associated with this generator. */
    public String getPackageName() {
        return packageName;
    }
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /** Returns class name (without a package)
      * of the class associated with this generator. */
    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }


    public void setSuperPrefix(String superPrefix) {
        this.superPrefix = superPrefix;
    }

    /** Returns prefix used to distinguish between superclass
      * and subclass when generating classes in pairs. */
    public String getSuperPrefix() {
        return superPrefix;
    }


    /** Sets current class property name. This method
      * is calledduring template parsing for each of the 
      * class properties. */
    public void setProp(String prop) {
        this.prop = prop;
    }
    public String getProp() {
        return prop;
    }


    /** Returns current property name with capitalized first letter */
    public String getCappedProp() {
        if(prop == null || prop.length() == 0)
            return prop;

        char c = Character.toUpperCase(prop.charAt(0));
        return (prop.length() == 1) ? Character.toString(c) : c + prop.substring(1);
    }


    /** Returns true if a class associated with this generator
      * is located in a package. */
    public boolean isUsingPackage() {
        return packageName != null;
    }


    /** Returns entity for the class associated with this generator. */
    public ObjEntity getEntity() {
        return entity;
    }
}
