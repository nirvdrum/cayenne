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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ConfigException;
import org.objectstyle.cayenne.access.DataDomain;

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
    static Logger logObj = Logger.getLogger(Configuration.class.getName());

    public static final String DOMAIN_FILE = "cayenne.xml";
    public static final String DEFAULT_CONFIG_CLASS = "org.objectstyle.cayenne.conf.DefaultConfiguration";

    private static Configuration sharedConfig;

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
            conf = (Configuration)Class.forName(configClass).newInstance();
        } catch (java.lang.Exception ex) {
            logObj.log(Level.SEVERE, "Error initializing shared Configuration", ex);
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
            logObj.log(Level.SEVERE, "Error initializing shared Configuration", ex);
            throw new RuntimeException("Error initializing shared Configuration");
        }
    }


    // non-static code...

    /** Lookup map that stores DataDomains with names as keys. */
    protected HashMap dataDomains = new HashMap();


    /** Returns domain configuration as a stream or null if it
      * can not be found. */
    public abstract InputStream getDomainConfig();

    /** Returns DataMap configuration from a specified location or null if it
      * can not be found. */
    public abstract InputStream getMapConfig(String location);


    /** Initializes all Cayenne resources. Loads all configured domains and their
      * data maps, initializes all domain Nodes and their DataSources. */
    protected void init() throws java.lang.Exception {
        InputStream in = getDomainConfig();
        if (in == null)
            throw new ConfigException("Domain configuration file \""
                                      + DOMAIN_FILE
                                      + "\" is not found.");

        DomainHelper helper = new DomainHelper(this);
        if(!helper.loadDomains(in)) {
            throw new ConfigException("Failed to load domain and/or its maps/nodes.");
        }

        Iterator it = helper.getDomains().iterator();
        while(it.hasNext()) {
            addDomain((DataDomain)it.next());
        }
    }


    /** Adds new DataDomain to the list of registered domains. */
    public void addDomain(DataDomain domain) {
        dataDomains.put(domain.getName(), domain);
    }


    /** Returns registered domain matching <code>name</code>
      * or null if no such domain is found. */
    public DataDomain getDomain(String name) {
        return (DataDomain)dataDomains.get(name);
    }


    /** Returns default domain of this configuration. If no domains
      * are configured, null is returned. If more then 1 domain exists
      * in this configuration, an exception is thrown. In such cases
      * <code>getDomain(String name)</code> method must be used. */
    public DataDomain getDomain() {
        int size = dataDomains.size();
        if(size == 0) {
            return null;
        } else if(size == 1) {
            Iterator it = dataDomains.keySet().iterator();
            return (DataDomain)dataDomains.get(it.next());
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
        ArrayList list = new ArrayList();
        Iterator it = dataDomains.keySet().iterator();
        while (it.hasNext()) {
            list.add(dataDomains.get(it.next()));
        }
        return list;
    }
}

