package org.objectstyle.cayenne.conn;

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
 * 
 * Other copyright notes:
 *
 * 1. Original idea and implementation Jakarta DBCP project,  
 * copyright (c) 1999-2002 The Apache Software Foundation. 
 *
 */

import java.util.logging.Logger;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.RefAddr;
import javax.naming.spi.ObjectFactory;


/**
 * <p>Basic JNDI object factory that creates an instance of
 * <code>PoolManager</code> that has been configured based on the
 * <code>RefAddr</code> values of the specified <code>Reference</code>.</p>
 *
 * <p>Here is a sample Tomcat 4.0.x configuration that sets this class
 * as a default factory for javax.sql.DataSource objects:</p>
<code><pre>
&lt;ResourceParams name="jdbc/mydb"&gt;
    &lt;parameter&gt;
        &lt;name&gt;factory&lt;/name&gt;
        &lt;value>org.objectstyle.cayenne.conn.ContainerPoolFactory&lt;/value&gt;
    &lt;/parameter&gt;

    &lt;parameter>
        &lt;name>username&lt;/name>
        &lt;value>andrei&lt;/value>
    &lt;/parameter>
            
    &lt;parameter>
        &lt;name>password&lt;/name>
        &lt;value>bla-bla&lt;/value>
    &lt;/parameter>
                
    &lt;parameter>
        &lt;name>driver&lt;/name>
        &lt;value>org.gjt.mm.mysql.Driver&lt;/value>
    &lt;/parameter>
            
    &lt;parameter>
        &lt;name>url&lt;/name>
        &lt;value>jdbc:mysql://noise/cayenne&lt;/value>
    &lt;/parameter>
            
    &lt;parameter>
        &lt;name>min&lt;/name>
        &lt;value>1&lt;/value>
    &lt;/parameter>
            
    &lt;parameter>
        &lt;name>max&lt;/name>
        &lt;value>3&lt;/value>
    &lt;/parameter>
&lt;/ResourceParams>
</pre></code>
 *
 * @author Craig R. McClanahan
 * @author Andrei Adamchik
 */

public class ContainerPoolFactory implements ObjectFactory {
    static Logger logObj = Logger.getLogger(DataSourceObjectFactory.class.getName());


    /**
     * <p>Create and return a new <code>PoolManager</code> instance.  If no
     * instance can be created, return <code>null</code> instead.</p>
     *
     * @param obj The possibly null object containing location or
     *  reference information that can be used in creating an object
     * @param name The name of this object relative to <code>nameCtx</code>
     * @param nameCts The context relative to which the <code>name</code>
     *  parameter is specified, or <code>null</code> if <code>name</code>
     *  is relative to the default initial context
     * @param environment The possibly null environment that is used in
     *  creating this object
     *
     * @exception Exception if an exception occurs creating the instance
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
                                    Hashtable environment)
    throws Exception {
        // We only know how to deal with <code>javax.naming.Reference</code>s
        // that specify a class name of "javax.sql.DataSource"
        if ((obj == null) || !(obj instanceof Reference)) {
            logObj.fine("unsupported or null reference: " + obj);
            return null;
        }

        Reference ref = (Reference) obj;
        if (!"javax.sql.DataSource".equals(ref.getClassName())) {
            logObj.fine("unsupported type: " + ref.getClassName());
            return null;
        }

        // Create and configure a PoolManager instance based on the
        // RefAddr values associated with this Reference
        RefAddr ra = null;
        String driver = null;
        String url = null;
        int min = 1;
        int max = 1;
        String username = null;
        String password = null;
        
        ra = ref.get("min");
        if (ra != null) {
            min = Integer.parseInt(ra.getContent().toString());
        }
        
        ra = ref.get("max");
        if (ra != null) {
            max = Integer.parseInt(ra.getContent().toString());
        }


        ra = ref.get("driver");
        if (ra != null) {
            driver = ra.getContent().toString();
        }


        ra = ref.get("password");
        if (ra != null) {
            password = ra.getContent().toString();
        }

        ra = ref.get("url");
        if (ra != null) {
            url = ra.getContent().toString();
        }

        ra = ref.get("username");
        if (ra != null) {
            username = ra.getContent().toString();
        }

        logObj.warning("Loading datasource: " + driver);
        return new PoolManager(driver, url, min, max, username, password);
    }
}

