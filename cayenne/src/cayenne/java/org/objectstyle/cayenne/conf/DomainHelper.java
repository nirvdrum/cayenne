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

import java.io.*;
import java.util.*;

import javax.sql.*;

import org.apache.log4j.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.dba.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.project.*;
import org.objectstyle.cayenne.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

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

	private Level logLevel = Level.DEBUG;
	private Configuration config;
	private MapLoader loader;
	private XMLReader parser;
	private Locator locator;
	private List domains;
	private Map failedMaps;
	private Map failedAdapters;
	private Map failedDataSources;
	private List failedMapRefs;

	/** 
	 * If set, <code>factory</code> will override
	 * factory settings in domain configuration file. 
	 */
	private DataSourceFactory factory;

	/** Creates new DomainHelper. */
	public DomainHelper(Configuration config) throws Exception {
		this(config, Level.DEBUG);
	}

	/** Creates new DomainHelper that uses specified level of verbosity. */
	public DomainHelper(Configuration config, Level level) throws Exception {
		this.logLevel = level;
		this.config = config;
		parser = Util.createXmlReader();
		loader = new MapLoader();
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

	/** 
	 * Returns a map of DataSource locations for node names that
	 * failed to load during the last call to "loadDomains". 
	 */
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
	public boolean loadDomains(InputStream in, DataSourceFactory factory)
		throws Exception {
		this.factory = factory;

		logObj.log(logLevel, "start configuration loading.");
		if (factory != null) {
			logObj.log(
				logLevel,
				"factory "
					+ factory.getClass().getName()
					+ " will override any configured DataSourceFactory.");
		}

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
		return !hasFailures();
	}

	/** Returns true if any of the "failed.." collections
	  * is non-empty. */
	private boolean hasFailures() {
		return (failedMaps != null && failedMaps.size() > 0)
			|| (failedDataSources != null && failedDataSources.size() > 0)
			|| (failedAdapters != null && failedAdapters.size() > 0)
			|| (failedMapRefs != null && failedMapRefs.size() > 0);
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
		pw.println("<domains project-version=\"" + Project.CURRENT_PROJECT_VERSION + "\">");
		for (int i = 0; i < domains.length; i++) {
			storeDomain(pw, domains[i]);
		}
		pw.println("</domains>");
	}

	private static void storeDomain(PrintWriter pw, DataDomain domain) {
		pw.println("<domain name=\"" + domain.getName().trim() + "\">");

		DataNode[] nodes = domain.getDataNodes();
		List maps = domain.getMapList();

		// sort to satisfy dependecies
		OperationSorter.sortMaps(maps);

		Iterator iter = maps.iterator();
		while (iter.hasNext()) {
			DataMap map = (DataMap) iter.next();
			List depMaps = map.getDependencies();

			pw.print("\t<map name=\"" + map.getName().trim());
			pw.print("\" location=\"" + map.getLocation().trim());

			if (depMaps.size() == 0) {
				pw.println("\"/>");
			} else {
				pw.println("\">");
				Iterator dit = depMaps.iterator();
				while (dit.hasNext()) {
					DataMap dep = (DataMap) dit.next();
					pw.println(
						"\t\t<dep-map-ref name=\""
							+ dep.getName().trim()
							+ "\"/>");
				}

				pw.println("\t</map>");
			}
		}

		for (int i = 0; i < nodes.length; i++) {
			pw.println("\t<node name=\"" + nodes[i].getName().trim() + "\"");
			String datasource = nodes[i].getDataSourceLocation();
			if (null != datasource)
				datasource = datasource.trim();
			else
				datasource = "";
			pw.println("\t\t datasource=\"" + datasource + "\"");
			if (nodes[i].getAdapter() != null) {
				pw.println(
					"\t\t adapter=\""
						+ nodes[i].getAdapter().getClass().getName()
						+ "\"");
			}
			
			String factory = nodes[i].getDataSourceFactory();
			if (null != factory)
				factory = factory.trim();
			else
				factory = "";
			pw.println("\t\t factory=\"" + factory + "\">");
			DataMap[] map_arr = nodes[i].getDataMaps();
			for (int j = 0; map_arr != null && j < map_arr.length; j++) {
				pw.println(
					"\t\t\t<map-ref name=\""
						+ map_arr[j].getName().trim()
						+ "\"/>");
			}
			pw.println("\t </node>");
		}
		pw.println("</domain>");
	}

	/** 
	 * Stores DataSolurceInfo to the specified PrintWriter. 
	 * <code>info</code> object may contain full or partial information.
	 */
	public static void storeDataNode(PrintWriter out, DataSourceInfo info) {
		out.print("<driver");
		if (info.getJdbcDriver() != null) {
			out.print(" class=\"" + info.getJdbcDriver() + "\"");
		}
		out.println(">");

		if (info.getDataSourceUrl() != null) {
			String encoded = Util.encodeXmlAttribute(info.getDataSourceUrl());
			out.println("\t<url value=\"" + encoded + "\"/>");
		}

		out.println(
			"\t<connectionPool min=\""
				+ info.getMinConnections()
				+ "\" max=\""
				+ info.getMaxConnections()
				+ "\" />");

		if (info.getUserName() != null || info.getPassword() != null) {
			out.print("\t<login");
			if (info.getUserName() != null) {
				out.print(" userName=\"" + info.getUserName() + "\"");
			}
			if (info.getPassword() != null) {
				out.print(" password=\"" + info.getPassword() + "\"");
			}
			out.println("/>");
		}

		out.println("</driver>");
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
		public void startElement(
			String namespaceURI,
			String localName,
			String qName,
			Attributes atts)
			throws SAXException {
			if (localName.equals("domains")) {
				new DomainsHandler(parser, this);
			} else {
				logObj.log(
					logLevel,
					"<domains> should be the root element. <"
						+ localName
						+ "> is unexpected.");
				throw new SAXParseException(
					"Config file is not of expected XML type",
					locator);
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
		public void startElement(
			String namespaceURI,
			String localName,
			String qName,
			Attributes atts)
			throws SAXException {
			if (localName.equals("domain")) {
				new DomainHandler(getParser(), this).init(localName, atts);
			} else {
				logObj.log(
					logLevel,
					"<domain> should be the only child of <domains>. <"
						+ localName
						+ "> is unexpected.");
				throw new SAXParseException(
					"Unexpected element \"" + localName + "\"",
					locator);
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
			if (domainName == null) {
				logObj.log(logLevel, "error: unnamed <domain>.");
				throw new SAXParseException(
					"Domain 'name' attribute must be not null.",
					locator);
			}

			logObj.log(logLevel, "loaded domain: " + domainName);
			domain = new DataDomain(domainName);
			domains.add(domain);
		}

		public void startElement(
			String namespaceURI,
			String localName,
			String qName,
			Attributes atts)
			throws SAXException {
			if (localName.equals("map")) {
				new MapHandler(getParser(), this).init(localName, atts, domain);
			} else if (localName.equals("node")) {
				new NodeHandler(getParser(), this).init(
					localName,
					atts,
					domain);
			} else {
				logObj.log(
					logLevel,
					"<node> or <map> should be the children of <domain>. <"
						+ localName
						+ "> is unexpected.");
				throw new SAXParseException(
					"Unexpected element \"" + localName + "\"",
					locator);
			}
		}
	}

	private class MapHandler extends AbstractHandler {
		protected DataDomain domain;
		protected List depMaps = new ArrayList();
		protected String mapName;
		protected String location;

		public MapHandler(XMLReader parser, ContentHandler parentHandler) {
			super(parser, parentHandler);
		}

		public void init(String name, Attributes attrs, DataDomain domain)
			throws SAXException {
			this.domain = domain;
			mapName = attrs.getValue("", "name");
			if (mapName == null) {
				logObj.log(logLevel, "error: <map> without 'name'.");
				throw new SAXParseException(
					"<map> 'name' attribute must be present.",
					locator);
			}

			location = attrs.getValue("", "location");
			if (location == null) {
				logObj.log(logLevel, "error: <map> without 'location'.");
				throw new SAXParseException(
					"<map> 'location' attribute must be present.",
					locator);
			}

			logObj.log(
				logLevel,
				"loading <map name='"
					+ mapName
					+ "' location='"
					+ location
					+ "'>.");
		}

		public void startElement(
			String namespaceURI,
			String localName,
			String qName,
			Attributes attrs)
			throws SAXException {
			if (localName.equals("dep-map-ref")) {
				new DepMapRefHandler(getParser(), this).init(
					localName,
					attrs,
					domain,
					depMaps);
			} else {
				logObj.log(
					logLevel,
					"<dep-map-ref> should be the only node child. <"
						+ localName
						+ "> is unexpected.");
				throw new SAXParseException(
					"Unexpected element \"" + localName + "\"",
					locator);
			}
		}

		protected void finished() {
			// do actual loading after all references are initialized
			InputStream mapIn = config.getMapConfig(location);
			if (mapIn == null) {
				logObj.log(logLevel, "warning: map location not found.");
				failedMaps.put(mapName, location);
				return;
			}

			DataMap map = null;
			try {
				map = loader.loadDataMap(new InputSource(mapIn), depMaps);
			} catch (DataMapException dmex) {
				logObj.log(logLevel, "warning: map loading failed.", dmex);
				failedMaps.put(mapName, location);
				return;
			}

			logObj.log(
				logLevel,
				"loaded <map name='"
					+ mapName
					+ "' location='"
					+ location
					+ "'>.");
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

		public void init(String name, Attributes attrs, DataDomain domain)
			throws SAXException {
			this.domain = domain;

			String nodeName = attrs.getValue("", "name");
			if (nodeName == null) {
				logObj.log(logLevel, "error: <node> without 'name'.");
				throw new SAXParseException(
					"'<node name=' attribute must be present.",
					locator);
			}

			String dataSrcLocation = attrs.getValue("", "datasource");
			if (dataSrcLocation == null) {
				logObj.log(logLevel, "error: <map> without 'datasource'.");
				throw new SAXParseException(
					"'<node datasource=' attribute must be present.",
					locator);
			}

			String factoryName = attrs.getValue("", "factory");
			if (factoryName == null) {
				logObj.log(logLevel, "error: <map> without 'factory'.");
				throw new SAXParseException(
					"'<node factory=' attribute must be present.",
					locator);
			}

			logObj.log(
				logLevel,
				"loading <node name='"
					+ nodeName
					+ "' datasource='"
					+ dataSrcLocation
					+ "' factory='"
					+ factoryName
					+ "'>.");

			// unlike other parameters, adapter class is optional
			// default is used when none is specified.
			String adapterClass = attrs.getValue("", "adapter");
			if (adapterClass == null) {
				adapterClass = "org.objectstyle.cayenne.dba.JdbcAdapter";
			}

			logObj.log(logLevel, "node DbAdapter: " + adapterClass);

			this.node = new DataNode(nodeName);
			node.setDataSourceFactory(factoryName);
			node.setDataSourceLocation(dataSrcLocation);

			// load DataSource
			try {
				// use DomainHelper factory if it exists, if not - use factory specified
				// in configuration data
				DataSourceFactory localFactory =
					(factory != null)
						? factory
						: (DataSourceFactory) Class
							.forName(factoryName)
							.newInstance();

				logObj.log(
					logLevel,
					"using factory: " + localFactory.getClass().getName());
					
				localFactory.setParentConfig(DomainHelper.this.config);
				DataSource ds =
					localFactory.getDataSource(dataSrcLocation, logLevel);
				if (ds != null) {
					logObj.log(logLevel, "loaded datasource.");
					node.setDataSource(ds);
				} else {
					logObj.log(logLevel, "warning: null datasource.");
					failedDataSources.put(nodeName, dataSrcLocation);
				}
			} catch (Exception ex) {
				logObj.log(logLevel, "error: DataSource load failed", ex);
				failedDataSources.put(nodeName, dataSrcLocation);
			}

			// load DbAdapter
			try {
				node.setAdapter(
					(DbAdapter) Class.forName(adapterClass).newInstance());
			} catch (Exception ex) {
				logObj.log(logLevel, "instantiating adapter failed.", ex);
				failedAdapters.put(nodeName, adapterClass);
			}
		}

		public void startElement(
			String namespaceURI,
			String localName,
			String qName,
			Attributes attrs)
			throws SAXException {
			if (localName.equals("map-ref")) {
				new MapRefHandler(getParser(), this).init(
					localName,
					attrs,
					domain,
					node);
			} else {
				logObj.log(
					logLevel,
					"<map-ref> should be the only node child. <"
						+ localName
						+ "> is unexpected.");
				throw new SAXParseException(
					"Unexpected element \"" + localName + "\"",
					locator);
			}
		}

		protected void finished() {
			// it is important to add node to domain after all node maps
			// are initialized..
			domain.addNode(node);
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

		public void init(
			String name,
			Attributes attrs,
			DataDomain domain,
			DataNode node)
			throws SAXException {
			String mapName = attrs.getValue("", "name");
			if (mapName == null) {
				logObj.log(logLevel, "<map-ref> has no 'name'.");
				throw new SAXParseException(
					"'<map-ref name=' attribute must be present.",
					locator);
			}

			DataMap map = domain.getMap(mapName);
			if (map == null) {
				logObj.log(
					logLevel,
					"warning: unknown map-ref: " + mapName + ".");
				failedMapRefs.add(mapName);
			} else {
				logObj.log(logLevel, "loaded map-ref: " + mapName + ".");
				node.addDataMap(map);
			}
		}
	}

	private class DepMapRefHandler extends AbstractHandler {

		public DepMapRefHandler(
			XMLReader parser,
			ContentHandler parentHandler) {
			super(parser, parentHandler);
		}

		public void init(
			String name,
			Attributes attrs,
			DataDomain domain,
			List depMaps)
			throws SAXException {
			String mapName = attrs.getValue("", "name");
			if (mapName == null) {
				logObj.log(logLevel, "<map-ref> has no 'name'.");
				throw new SAXParseException(
					"'<map-ref name=' attribute must be present.",
					locator);
			}

			DataMap depMap = domain.getMap(mapName);
			if (depMap == null) {
				logObj.log(
					logLevel,
					"warning: unknown map-ref: " + mapName + ".");
				failedMapRefs.add(mapName);
			} else {
				logObj.log(logLevel, "loaded dep-map-ref: " + mapName + ".");
				depMaps.add(depMap);
			}
		}
	}
}
