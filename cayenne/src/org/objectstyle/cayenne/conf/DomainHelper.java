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


import java.util.*;
import java.io.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.sql.DataSource;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.util.*;


/**
 * Assists Configuration object in loading DataDomain configuration files.
 *
 * <p>
 * Note: an idea to use nested handlers for XML document parsing
 * (and code to implement it) were taken from org.apache.tools.ant.ProjectHelper 
 * from Jakarta-Ant project (Copyright: Apache Software Foundation).
 * This may not be the best way to build objects from XML, but it is rather 
 * consistent. For each nested element in the XML tree a dedicated handler
 * is created. Once the element is parsed, control is handled back to the parent handler.
 * </p>
 *
 * @author Andrei Adamchik 
 */
public class DomainHelper {
    static Logger logObj = Logger.getLogger(DomainHelper.class.getName());

    private Configuration config;
    private MapLoaderImpl loader;
    private XMLReader parser;
    private Locator locator;
    private ArrayList domains;
    private HashMap failedMaps;
    private HashMap failedAdapters;
    private HashMap failedDataSources;
    private ArrayList failedMapRefs;

    /** If set, <code>factory</code> will override
      * factory settings in domain configuration file. */
    private DataSourceFactory factory;


    /** Creates new DomainHelper. */
    public DomainHelper(Configuration config) throws java.lang.Exception {
        this.config = config;
        parser = Util.createXmlReader();
        loader = new MapLoaderImpl();
    }


    /** Returns domains loaded during the last call to "loadDomains". */
    public List getDomains() {
        return domains;
    }

    /** Returns a list of map reference names that failed to load
      * during node processing. */
    public List getFailedMapRefs() {
        return failedMapRefs;
    }

    /** Returns a map of locations for names of the data maps that
      * failed to load during the last call to "loadDomains". */
    public Map getFailedMaps() {
        return failedMaps;
    }


    /** Returns a map of DataSource locations for node names that
      * failed to load during the last call to "loadDomains". */
    public Map getFailedDataSources() {
        return failedDataSources;
    }

    /** Returns a map of adapter classes for node names that
      * failed to load during the last call to "loadDomains". */
    public Map getFailedAdapters() {
        return failedAdapters;
    }


    /** Reads domain configuration from the InputStream, returns an array
      * of initialized DataDomains. An attempt will be made to resolve and
      * load all referenced resources (data maps, data sources). 
      * <p><code>factory</code> parameter will override any factory settings
      * for the node datasources in domain configuration. This API is intended for
      * tools working with Cayenne, so that they can load objects from
      * deployment configuration files but use their own database connections. 
      * </p>
      * 
      * <p> 
      * If referenced resource is nonexistent or inaccessible, it will be 
      * inserted in one of the failed lists, available for analysis by the caller.
      * </p>
      * <p>
      * XML errors are fatal, and are being rethrown. 
      * </p>
      *
      * @param in InputStream to read configuration data. Must be not null.
      * @param factory DataSourceFactory that overrides factories specified in configuration data.
      * Could be null.
      * @return true if no failures happened during domain loading,
      * false - if at least one non-fatal failure ocurred.
      */
    public boolean loadDomains(InputStream in, DataSourceFactory factory) throws java.lang.Exception {
        this.factory = factory;

        domains = new ArrayList();
        failedMaps = new HashMap();
        failedDataSources = new HashMap();
        failedAdapters = new HashMap();
        failedMapRefs = new ArrayList();

        DefaultHandler handler = new RootHandler();
        parser.setContentHandler(handler);
        parser.setErrorHandler(handler);
        parser.parse(new InputSource(in));

        // return true if no failures
        return failedMaps.size() == 0
               && failedDataSources.size() == 0
               && failedAdapters.size() == 0
               && failedMapRefs.size() == 0;
    }


    /** Reads domain configuration from the InputStream, returns an array
      * of initialized DataDomains. An attempt will be made to resolve and
      * load all referenced resources (data maps, data sources). 
      * 
      * <p> 
      * If referenced resource is nonexistent or inaccessible, it will be 
      * inserted in one of the failed lists, available for analysis by the caller.
      * </p>
      * <p>
      * XML errors are fatal, and are being rethrown. 
      * </p>
      * @return true if no failures happened during domain loading,
      * false - if at least one non-fatal failure ocurred.
      */
    public boolean loadDomains(InputStream in) throws java.lang.Exception {
        return loadDomains(in, null);
    }


    /** Saves domains into the specified file.
      * Assumes that the maps have already been saved.*/
    public static void storeDomains(PrintWriter pw, DataDomain[] domains) {
        pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        pw.println("<domains>");
        for (int i = 0; i < domains.length; i++) {
            storeDomains(pw, domains[i]);
        }
        pw.println("</domains>");
    }


    private static void storeDomains(PrintWriter pw, DataDomain domain) {
        DataNode[] nodes = domain.getDataNodes();
        List maps = domain.getMapList();
        pw.println("<domain name=\"" + domain.getName() + "\">");
        Iterator iter = maps.iterator();
        while(iter.hasNext()) {
            StringBuffer buf = new StringBuffer();
            DataMap map = (DataMap)iter.next();
            buf.append("\t <map name=\"").append(map.getName());
            buf.append(" \" location=\"").append(map.getLocation());
            buf.append("\"/> ");
            pw.println(buf.toString());
        }// End while()

        for (int i = 0; i < nodes.length; i++) {
            pw.println("\t<node name=\"" + nodes[i].getName() + "\"" );
            pw.println("\t\t datasource=\""+ nodes[i].getDataSourceLocation() + "\"");
            pw.println("\t\t factory=\""+ nodes[i].getDataSourceFactory() + "\">");
            DataMap[] map_arr = nodes[i].getDataMaps();
            for (int j = 0; map_arr != null && j < map_arr.length; j++) {
                pw.println("\t\t\t<map-ref name=\"" + map_arr[j].getName() + "\"/>");
            }
            pw.println("\t </node>");
        }
        pw.println("</domain>");
    }


    // SAX handlers start below

    /**
     * Handler for the root element. Its only child must be the "domains" element.
     */
    private class RootHandler extends DefaultHandler {
        /**
          * Sets the locator in the project helper for future reference.
          * 
          * @param locator The locator used by the parser.
          *                Will not be <code>null</code>.
          */
        public void setDocumentLocator(Locator locator) {
            DomainHelper.this.locator = locator;
        }

        /**
         * Handles the start of a datadomains element. A domains handler is created
         * and initialised with the element name and attributes.
         * 
         * @exception SAXException if the tag given is not 
         *                              <code>"domains"</code>
         */
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
        throws SAXException {
            if (localName.equals("domains")) {
                new DomainsHandler(parser, this);
            } else {
                throw new SAXParseException("Config file is not of expected XML type", locator);
            }
        }
    }


    /**
    * Handler for the top level "project" element.
    */
    private class DomainsHandler extends AbstractHandler {

        /**
         * Constructor which just delegates to the superconstructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         */
        public DomainsHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }


        /**
         * Handles the start of a top-level element within the project. An
         * appropriate handler is created and initialised with the details
         * of the element.
         */
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
        throws SAXException {
            if (localName.equals("domain")) {
                new DomainHandler(getParser(), this).init(localName, atts);
            } else {
                throw new SAXParseException("Unexpected element \"" + localName + "\"", locator);
            }
        }
    }


    /**
      * Handler for the "domain" element.
      */
    private class DomainHandler extends AbstractHandler {
        private DataDomain domain;

        public DomainHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes attrs) throws SAXException {
            String domainName = attrs.getValue("", "name");
            if (domainName == null)
                throw new SAXParseException("Domain 'name' attribute must be not null.", locator);

            domain = new DataDomain(domainName);
            domains.add(domain);
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
        throws SAXException {
            if (localName.equals("map")) {
                new MapHandler(getParser(), this).init(localName, atts, domain);
            } else if (localName.equals("node")) {
                new NodeHandler(getParser(), this).init(localName, atts, domain);
            } else {
                throw new SAXParseException("Unexpected element \"" + localName + "\"", locator);
            }
        }
    }

    private class MapHandler extends AbstractHandler {
        public MapHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes attrs, DataDomain domain) throws SAXException {
            String mapName = attrs.getValue("", "name");
            if (mapName == null)
                throw new SAXParseException("<map> 'name' attribute must be present.", locator);

            String location = attrs.getValue("", "location");
            if (location == null)
                throw new SAXParseException("<map> 'location' attribute must be present.", locator);

            InputStream mapIn = config.getMapConfig(location);
            if (mapIn == null) {
                failedMaps.put(mapName, location);
                return ;
            }

            DataMap map = null;
            try {
                map = loader.loadDataMap(new InputSource(mapIn));
            } catch (DataMapException dmex) {
                logObj.log(Level.FINE, "Error loading map", dmex);
                failedMaps.put(mapName, location);
                return;
            }

            map.setName(mapName);
            map.setLocation(location);
            domain.addMap(map);
        }
    }

    /** Handles processing of "node" element. */
    private class NodeHandler extends AbstractHandler {
        protected DataNode node;
        protected DataDomain domain;

        /**
         * Constructor which just delegates to the superconstructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         */
        public NodeHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }


        public void init(String name, Attributes attrs, DataDomain domain) throws SAXException {
            this.domain = domain;

            String nodeName = attrs.getValue("", "name");
            if (nodeName == null)
                throw new SAXParseException("'<node name=' attribute must be present.", locator);

            String dataSrcLocation = attrs.getValue("", "datasource");
            if (dataSrcLocation == null)
                throw new SAXParseException("'<node datasource=' attribute must be present.", locator);

            String factoryName = attrs.getValue("", "factory");
            if (factoryName == null)
                throw new SAXParseException("'<node factory=' attribute must be present.", locator);

            // unlike other parameters, adapter class is optional
            // default is used when none is specified.
            String adapterClass = attrs.getValue("", "adapter");
            if (adapterClass == null)
                adapterClass = "org.objectstyle.cayenne.dba.JdbcAdapter";

            DataNode node = new DataNode(nodeName);
            node.setDataSourceFactory(factoryName);
            node.setDataSourceLocation(dataSrcLocation);
            domain.addNode(node);


            // load DataSource
            try {
                // use DomainHelper factory if it exists, if not - use factory specified
                // in configuration data
                DataSourceFactory localFactory = (factory != null)
                                                 ? factory
                                                 : (DataSourceFactory)Class.forName(factoryName).newInstance();

                node.setDataSource(localFactory.getDataSource(dataSrcLocation));
            } catch (Exception ex) {
                logObj.log(Level.FINE, "Error loading DataSource", ex);
                failedDataSources.put(nodeName, dataSrcLocation);
            }

            // load DbAdapter
            try {
                node.setAdapter((DbAdapter)Class.forName(adapterClass).newInstance());
            } catch (Exception ex) {
                failedAdapters.put(nodeName, adapterClass);
            }
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes attrs)
        throws SAXException {
            if (localName.equals("map-ref")) {
                new MapRefHandler(getParser(), this).init(localName, attrs, domain, node);
            } else {
                throw new SAXParseException("Unexpected element \"" + localName + "\"", locator);
            }
        }
    }


    private class MapRefHandler extends AbstractHandler {
        /**
         * Constructor which just delegates to the superconstructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         */
        public MapRefHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes attrs, DataDomain domain, DataNode node) throws SAXException {
            String mapName = attrs.getValue("", "name");
            if (mapName == null)
                throw new SAXParseException("'<mapref name=' attribute must be present.", locator);

            DataMap map = domain.getMap(mapName);
            if (map == null) {
                failedMapRefs.add(mapName);
            } else {
                node.addDataMap(map);
            }
        }
    }
}
