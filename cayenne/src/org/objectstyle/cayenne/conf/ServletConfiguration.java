package org.objectstyle.cayenne.conf;
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

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;


import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataContext;


/**
  * Subclass of Configuration that uses ServletContext to locate resources. 
  * This class can only be used in a context of a servlet/jsp container.
  * It resolves configuration file paths relative to the web application
  * "WEB-INF" directory.
  *
  * <p>It performs the following tasks:
  * <ul>
  * <li>Loads Cayenne configuration when the application is started within container.</li>
  * <li>Assigns new DataContext to every new session created within the application.</li>
  * </ul>
  * </p>
  * 
  * <p>ServletConfiguration must be configured in <code>web.xml</code> deployment
  * descriptor as a listener of context and session events:</p>
  *<pre>&lt;listener&gt;
     &lt;listener-class&gt;org.objectstyle.cayenne.conf.ServletConfiguration&lt;/listener-class&gt;
&lt;/listener&gt;</pre>
  *
  * <p>Note that to set ServletContext as a listener, you must use servlet containers 
  * compatible with Servlet Specification 2.3 (such as Tomcat 4.0).</p>
  *
  * @author Andrei Adamchik
  */
public class ServletConfiguration extends Configuration
    implements HttpSessionListener, ServletContextListener {

    public static final String DATA_CONTEXT_KEY = "cayenne.datacontext";

    protected ServletContext servletContext;


    /** Returns default Cayenne DataContext associated with session <code>s</code>. */
    public static DataContext getDefaultContext(HttpSession s) {
        return (DataContext)s.getAttribute(DATA_CONTEXT_KEY);
    }

    /** Locates domain configuration file in a web application
      * looking for "cayenne.xml" in application "WEB-INF" directory. */
    public InputStream getDomainConfig() {
        return servletContext.getResourceAsStream("/WEB-INF/" + DOMAIN_FILE);
    }

    /** Locates data map configuration file via ServletContext
      * associated with this Configuration treating 
      * <code>location</code> as relative to application "WEB-INF" directory.. */
    public InputStream getMapConfig(String location) {
        return servletContext.getResourceAsStream("/WEB-INF/" + location);
    }


    /** Returns current application context object. */
    public ServletContext getServletContext() {
        return servletContext;
    }

    /** Sets itself as a Cayenne shared Configuration object that can later
      * be obtained by calling <code>Configuration.getSharedConfig()</code>.
      * This method is a part of  ServletContextListener interface and is called
      * on application startup. */
    public void contextInitialized(ServletContextEvent sce) {
        this.servletContext = sce.getServletContext();
        Configuration.initSharedConfig(this);
    }


    /** Currently does nothing. <i>In the future it should close down
      * any database connections if they wheren't obtained via JNDI.</i>
      * This method is a part of ServletContextListener interface and is called
      * on application shutdown. */
    public void contextDestroyed(ServletContextEvent sce) {}


    /** Creates and assigns a new data context based on default domain
      * to the session object  associated with this event. This method
      * is a part of HttpSessionListener interface and is called every time
      * when a new session is created. */
    public void sessionCreated(HttpSessionEvent se) {
        se.getSession().setAttribute(DATA_CONTEXT_KEY, servletContext);
    }


    /** Does nothing. This method
      * is a part of HttpSessionListener interface and is called every time
      * when a session is destroyed. */
    public void sessionDestroyed(HttpSessionEvent se) {}
}

