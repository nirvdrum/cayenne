/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.conf;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.util.WebApplicationResourceLocator;

/**
 * Configuration class that uses ServletContext to locate resources. This class is
 * intended for use in J2EE servlet containers. It is compatible with containers following
 * servlet specification version 2.2 and newer (e.g. Tomcat can be used starting from
 * version 3).
 * <p>
 * ServletConfiguration resolves configuration file locations relative to the web
 * application "WEB-INF" directory, and does not require them to be in the CLASSPATH
 * (though CLASSPATH locations such as "/WEB-INF/classes" and "/WEB-INF/lib/some.jar" are
 * supported as well). By default search for cayenne.xml is done in /WEB-INF/ folder. To
 * specify an arbitrary context path in the web application (e.g. "/WEB-INF/cayenne"), use
 * <code>cayenne.configuration.path</code> init parameters in <code>web.xml</code>.
 * </p>
 * 
 * @author Andrei Adamchik
 * @author Scott Finnerty
 * @since 1.2 renamed from BasicServletConfiguration.
 */
public class ServletConfiguration extends DefaultConfiguration {

    private static Logger logObj = Logger.getLogger(ServletConfiguration.class);

    /**
     * A name of the web application initialization parameter used to specify extra paths
     * where Cayenne XML files might be located. E.g. "/WEB-INF/cayenne".
     */
    public static final String CONFIGURATION_PATH_KEY = "cayenne.configuration.path";

    /**
     * Used by BasicServletConfiguration as a session attribute for DataContext.
     */
    public static final String DATA_CONTEXT_KEY = "cayenne.datacontext";

    protected ServletContext servletContext;

    /**
     * Creates a new ServletConfiguration and sets is as a Configuration signleton.
     */
    public synchronized static ServletConfiguration initializeConfiguration(
            ServletContext ctxt) {

        // check if this web application already has a servlet configuration
        // sometimes multiple initializations are done by mistake...

        // don't use static getter, since it will do initialization on demand!!!
        Configuration oldConfiguration = Configuration.sharedConfiguration;
        if (oldConfiguration instanceof ServletConfiguration) {
            ServletConfiguration basicConfiguration = (ServletConfiguration) oldConfiguration;
            if (basicConfiguration.getServletContext() == ctxt) {
                logObj.info("BasicServletConfiguration is already initialized, reusing.");
                return basicConfiguration;
            }
        }

        ServletConfiguration conf = new ServletConfiguration(ctxt);
        Configuration.initializeSharedConfiguration(conf);

        return conf;
    }

    /**
     * Returns default Cayenne DataContext associated with the HttpSession, creating it on
     * the spot if needed.
     */
    public static DataContext getDefaultContext(HttpSession session) {
        synchronized (session) {
            DataContext ctxt = (DataContext) session.getAttribute(DATA_CONTEXT_KEY);

            if (ctxt == null) {
                ctxt = DataContext.createDataContext();
                session.setAttribute(ServletConfiguration.DATA_CONTEXT_KEY, ctxt);
            }

            return ctxt;
        }
    }

    /**
     * Constructs an uninitialized ServletConfiguration.
     */
    public ServletConfiguration() {
        ResourceLocator locator = new WebApplicationResourceLocator();
        locator.setSkipAbsolutePath(true);
        locator.setSkipClasspath(false);
        locator.setSkipCurrentDirectory(true);
        locator.setSkipHomeDirectory(true);

        setResourceLocator(locator);
    }

    /**
     * Constructs new ServletConfiguration initializing it with web application
     * ServletContext that is used to locate configuration files.
     */
    public ServletConfiguration(ServletContext ctxt) {
        this();
        setServletContext(ctxt);
    }

    /**
     * Adds a path under the web application context to look for Cayenne configuration
     * files. This is analogous to what "cayenne.configuration.path" initialization
     * property does. Example path - "/WEB-INF/cayenne".
     */
    public void addContextPath(String path) {
        getResourceLocator().addFilesystemPath(path);
    }

    /**
     * Sets a "servletContext" property of this configuration.
     */
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;

        updateLocator();
    }

    /**
     * Returns a "servletContext" property of this configuration that is normally
     * initialized in constructor by the creators of this configuration.
     */
    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * Returns true if the servlet context is set.
     */
    public boolean canInitialize() {
        return getServletContext() != null;
    }

    /**
     * Updates ResourceLocator with current ServletContext parameters.
     */
    protected void updateLocator() {
        ResourceLocator locator = getResourceLocator();

        // setup context for resolving resources...
        // only know how to handle WebApplicationResourceLocator...
        if (locator instanceof WebApplicationResourceLocator) {
            WebApplicationResourceLocator webLocator = (WebApplicationResourceLocator) locator;

            webLocator.setServletContext(servletContext);
            String configurationPath = servletContext
                    .getInitParameter(CONFIGURATION_PATH_KEY);
            if (configurationPath != null && configurationPath.trim().length() > 0) {
                webLocator.addFilesystemPath(configurationPath.trim());
            }
        }
    }
}