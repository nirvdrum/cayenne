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
package org.objectstyle.cayenne.conf;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ConfigException;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.util.CayenneMap;
import org.objectstyle.cayenne.util.ResourceLocator;

/**
 * This class is an entry point to Cayenne. It loads all 
 * configuration files and instantiates main Cayenne objects. Used as a 
 * singleton via 'getSharedConfig' method.
 *
 * <p>To force custom subclass of Configuration, Java application must
 * call "initSharedConfig" with the name of such subclass. This will initialize
 * Configuration singleton instance with new object of a specified class.
 * By default org.objectstyle.cayenne.conf.DefaultConfiguration is instantiated.
 * </p>
 *
 * @author Andrei Adamchik
 */
public abstract class Configuration {
    private static Logger logObj = Logger.getLogger(Configuration.class);

    public static final String LOGGING_PROPS =
        ".cayenne/cayenne-log.properties";
    public static final String DOMAIN_FILE = "cayenne.xml";
    public static final String DEFAULT_CONFIG_CLASS =
        "org.objectstyle.cayenne.conf.DefaultConfiguration";

    protected static Configuration sharedConfig;
    private static boolean loggingConfigured;
    protected static Level logLevel = Level.DEBUG;

    /** 
     * Defines ClassLoader to use for resource lookup.
     * Configuration objects that are using ClassLoaders
     * to locate reosurces may need to be bootstrapped
     * explicitly.
     */
    private static ClassLoader resourceLoader =
        Configuration.class.getClassLoader();

    /** Lookup map that stores DataDomains with names as keys. */
    protected CayenneMap dataDomains = new CayenneMap(this);
    protected DataSourceFactory overrideFactory;
    protected boolean ignoringLoadFailures;
    protected ConfigStatus loadStatus;

    /** 
     * Sets <code>cl</code> class's ClassLoader to serve
     * as shared configuration resource ClassLoader.
     * If shared Configuration object does not use ClassLoader,
     * this method call will have no effect on how resources are loaded.
     */
    public static void bootstrapSharedConfig(Class cl) {
        resourceLoader = cl.getClassLoader();
    }

    /** 
     * Configures Cayenne logging properties. 
     * Search for the properties file called <code>cayenne-log.properties</code> 
     * is first done in $HOME/.cayenne, then in CLASSPATH.
     */
    public synchronized static void configCommonLogging() {
        if (!loggingConfigured) {
            ResourceLocator locator = new ResourceLocator();
            locator.setSkipAbsPath(true);
            locator.setSkipClasspath(false);
            locator.setSkipCurDir(true);
            locator.setSkipHomeDir(false);
            configCommonLogging(locator.findResource(LOGGING_PROPS));
        }
    }

    /** 
     * Configures Cayenne logging properties using properties found at specified URL. 
     */
    public synchronized static void configCommonLogging(URL propsFile) {
        if (!loggingConfigured) {
            if (propsFile != null) {
                PropertyConfigurator.configure(propsFile);
            } else {
                BasicConfigurator.configure();
            }
            loggingConfigured = true;
        }
    }

    /** Use this method as an entry point to all Cayenne access objects.
      * <p>Note that if you want to provide custom Configuration,
      * make sure you call one of <code>initSharedConfig</code> methods
      * before your application code has a chance to call this method.
      */
    public synchronized static Configuration getSharedConfig() {
        if (sharedConfig == null)
            initSharedConfig();
        return sharedConfig;
    }

    public static ClassLoader getResourceLoader() {
        return resourceLoader;
    }

    /** 
     * Returns default log level for loading configuration. 
     * Log level is made static so that applications can set it 
     * before shared Configuration object is instantaited.
     */
    public static Level getLoggingLevel() {
        return logLevel;
    }

    /** Sets default log level for loading configuration. */
    public static void setLoggingLevel(Level logLevel) {
        Configuration.logLevel = logLevel;
    }

    /** Creates and initializes shared Configuration object.
      * org.objectstyle.cayenne.conf.DefaultConfiguration will be 
      * instantiated and assigned to a singleton instance of
      * Configuration. */
    public static void initSharedConfig() {
        initSharedConfig(DEFAULT_CONFIG_CLASS);
    }

    /** Creates and initializes shared Configuration object with
      * custom Configuration subclass. */
    public static void initSharedConfig(String configClass) {
        Configuration conf = null;

        // separate instantiation exceptions from the
        // possible runtime exceptions thown in initSharedConfig
        try {
            conf = (Configuration) Class.forName(configClass).newInstance();
        } catch (Exception ex) {
            logObj.error("Error initializing shared Configuration", ex);
            throw new RuntimeException("Error initializing shared Configuration");
        }

        initSharedConfig(conf);
    }

    /** Sets shared Configuration object to a new Configuration object.
      * calls <code>init</code> method of <code>conf</code> object. */
    public static void initSharedConfig(Configuration conf) {
        try {
            sharedConfig = conf;
            sharedConfig.init();
        } catch (java.lang.Exception ex) {
            logObj.error("Error initializing shared Configuration", ex);
            throw new RuntimeException("Error initializing shared Configuration");
        }
    }

    /** Returns domain configuration as a stream or null if it
      * can not be found. */
    public abstract InputStream getDomainConfig();

    /** Returns DataMap configuration from a specified location or null if it
      * can not be found. */
    public abstract InputStream getMapConfig(String location);

    /** Initializes all Cayenne resources. Loads all configured domains and their
      * data maps, initializes all domain Nodes and their DataSources. */
    public void init() throws Exception {
        configLogging();
        InputStream in = getDomainConfig();
        if (in == null) {
            StringBuffer msg = new StringBuffer();
            msg
                .append("[")
                .append(this.getClass().getName())
                .append("] : Domain configuration file \"")
                .append(DOMAIN_FILE)
                .append("\" is not found.");

            throw new ConfigException(msg.toString());
        }

        ConfigLoader helper = new ConfigLoader(this, getLoggingLevel());
        this.loadStatus = helper;
        if (!helper.loadDomains(in)) {
            StringBuffer msg = new StringBuffer();
            msg.append("[").append(this.getClass().getName()).append(
                "] : Failed to load domain and/or its maps/nodes.");

            if (!ignoringLoadFailures) {
                throw new ConfigException(msg.toString());
            }
        }

        Iterator it = helper.getDomains().iterator();
        while (it.hasNext()) {
            addDomain((DataDomain) it.next());
        }
    }

    /**
     * Configures Log4J. This implementation calls
     * <code>configCommonLogging</code>.
     */
    protected void configLogging() {
        configCommonLogging();
    }

    /**
     * Returns an internal property for the DataSource factory that 
     * will override any settings configured in XML. 
     * Subclasses may override this method to provide a special factory for
     * DataSource creation that will take precedence over any factories
     * configured in cayenne project. 
     */
    public DataSourceFactory getOverrideFactory() {
        return overrideFactory;
    }

    public void setOverrideFactory(DataSourceFactory overrideFactory) {
        this.overrideFactory = overrideFactory;
    }

    /** Adds new DataDomain to the list of registered domains. */
    public void addDomain(DataDomain domain) {
        dataDomains.put(domain.getName(), domain);
    }

    /** Returns registered domain matching <code>name</code>
      * or null if no such domain is found. */
    public DataDomain getDomain(String name) {
        return (DataDomain) dataDomains.get(name);
    }

    /** 
     * Returns default domain of this configuration. If no domains are 
     * configured, null is returned. If more then 1 domain exists in this
     * configuration, an CayenneRuntimeException is thrown, indicating that
     * domain name must be explicitly specified. In such cases
     * <code>getDomain(String name)</code> method must be used instead.
     */
    public DataDomain getDomain() {
        int size = dataDomains.size();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            Iterator it = dataDomains.keySet().iterator();
            return (DataDomain) dataDomains.get(it.next());
        } else {
            throw new CayenneRuntimeException("More then 1 domain is configured, use 'getDomain(String name)' instead.");
        }
    }

    /** Unregisters DataDomain matching <code>name<code> from
      * this Configuration object. Note that any domain database
      * connections remain open, and it is a responsibility of a
      * caller to clean it up. */
    public void removeDomain(String name) {
        dataDomains.remove(name);
    }

    /** Returns a list of registered DataDomain objects. */
    public List getDomainList() {
        List list = new ArrayList();
        Iterator it = dataDomains.keySet().iterator();
        while (it.hasNext()) {
            list.add(dataDomains.get(it.next()));
        }
        return list;
    }

    /**
     * Returns the ignoringLoadFailures.
     * @return boolean
     */
    public boolean isIgnoringLoadFailures() {
        return ignoringLoadFailures;
    }

    /**
     * Sets the ignoringLoadFailures.
     * @param ignoringLoadFailures The ignoringLoadFailures to set
     */
    public void setIgnoringLoadFailures(boolean ignoringLoadFailures) {
        this.ignoringLoadFailures = ignoringLoadFailures;
    }
    
    /**
     * Returns the loadStatus.
     * @return ConfigStatus
     */
    public ConfigStatus getLoadStatus() {
        return loadStatus;
    }
}