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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import org.objectstyle.cayenne.ConfigException;
import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.util.*;


/** Creates DataSource objects from XML configuration files that
  * describe a JDBC driver. Wraps JDBC driver in a generic DataSource 
  * implementation.
  *
  * @author Andrei Adamchik
  */
public class DriverDataSourceFactory implements DataSourceFactory {
    static Logger logObj = Logger.getLogger(DriverDataSourceFactory.class.getName());

    private XMLReader parser;
    private DataSourceInfo driverInfo;
    private Level logLevel = Level.FINER;

    public DriverDataSourceFactory() throws Exception {
        parser = Util.createXmlReader();
    }

    /** Returns DataSource object corresponding to <code>location</code>.
      * Location is expected to be a path of a file relative to the current
      * directory or a user home directory. */
    public DataSource getDataSource(String location) throws Exception {
        return getDataSource(location, Level.FINER);
    }


    public DataSource getDataSource(String location, Level logLevel) throws Exception {
        this.logLevel = logLevel;
        load(location);
        return new PoolManager(driverInfo.getJdbcDriver(),
                               driverInfo.getDataSourceUrl(),
                               driverInfo.getMinConnections(),
                               driverInfo.getMaxConnections(),
                               driverInfo.getUserName(),
                               driverInfo.getPassword());
    }


    /** Returns DataSourceInfo property. */
    protected DataSourceInfo getDriverInfo() {
        return driverInfo;
    }


	public static void store(String location, DataSourceInfo info) {
		
	}// End store

    /** Loads driver information from the file at <code>location</code>.
      * Called internally from "getDataSource" */
    protected void load(String location) throws Exception {
        logObj.log(logLevel, "loading driver information from (" + location + ").");

        InputStream in = ResourceLocator.findResourceInFileSystem(location);
        if (in == null) {
            logObj.log(logLevel, "location not found in filesystem.");
            in = ResourceLocator.findResourceInClasspath(location);
        } else {
            logObj.log(logLevel, "location found in filesystem.");
        }

        if (in == null) {
            logObj.log(logLevel, "error: location not found.");
            throw new ConfigException("Can't find DataSource configuration file at "
                                      + location);
        }

        RootHandler handler = new RootHandler();
        parser.setContentHandler(handler);
        parser.setErrorHandler(handler);
        parser.parse(new InputSource(in));
    }


    // SAX handlers start below

    /** Handler for the root element. Its only child must be the "driver" element. */
    private class RootHandler extends DefaultHandler {
        /**
         * Handles the start of a "driver" element. A driver handler is created
         * and initialized with the element name and attributes.
         * 
         * @exception SAXException if the tag given is not 
         *                              <code>"driver"</code>
         */
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
        throws SAXException {
            if (localName.equals("driver")) {
                new DriverHandler(parser, this).init(localName, atts);
            } else {
                logObj.log(logLevel, "<driver> must be the root element. <"
                           + localName + "> is unexpected.");
                throw new SAXException("Config file is not of expected XML type. '"
                                       + localName
                                       + "' unexpected.");
            }
        }
    }

    /** Handler for the "driver" element. */
    private class DriverHandler extends AbstractHandler {
        public DriverHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes attrs) throws SAXException {
            String className = attrs.getValue("", "class");
            if (className == null) {
                logObj.log(logLevel, "error: <driver> has no 'class'.");
                throw new SAXException("Driver 'class' attribute must be present.");
            }

            logObj.log(logLevel, "loading driver " + className);
            driverInfo = new DataSourceInfo();
            driverInfo.setJdbcDriver(className);
        }

        /**
          * Handles the start of a driver child element. An appropriate handler 
          * is created and initialized with the element name and attributes.
          * 
          * @exception SAXException if the tag given is not recognized.
          */
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
        throws SAXException {
            if (localName.equals("login")) {
                new LoginHandler(this.parser, this).init(localName, atts, driverInfo);
            } else if (localName.equals("url")) {
                new UrlHandler(this.parser, this).init(localName, atts, driverInfo);
            } else if (localName.equals("connectionPool")) {
                new ConnectionHandler(this.parser, this).init(localName, atts, driverInfo);
            } else {
                logObj.log(logLevel, "<login, url, connectionPool> are valid. <"
                           + localName + "> is unexpected.");
                throw new SAXException("Config file is not of expected XML type");
            }
        }

    }


    private class UrlHandler extends AbstractHandler {
        /**
         * Constructor which just delegates to the superconstructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         */
        public UrlHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes atts, DataSourceInfo driverInfo) throws SAXException {
            driverInfo.setDataSourceUrl(atts.getValue("value"));
            if(driverInfo.getDataSourceUrl() == null) {
                logObj.log(logLevel, "error: <url> has no 'value'.");
                throw new SAXException("'<url value=' attribute is required.");
            }
        }
    }

    private class LoginHandler extends AbstractHandler {
        /**
         * Constructor which just delegates to the superconstructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         */
        public LoginHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes atts, DataSourceInfo driverInfo) throws SAXException {
            logObj.log(logLevel, "loading user name and password.");
            driverInfo.setUserName(atts.getValue("userName"));
            driverInfo.setPassword(atts.getValue("password"));
        }
    }

    private class ConnectionHandler extends AbstractHandler {
        /**
         * Constructor which just delegates to the superconstructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         */
        public ConnectionHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes atts, DataSourceInfo driverInfo) throws SAXException {
            try {
                String min = atts.getValue("min");
                if(min != null)
                    driverInfo.setMinConnections(Integer.parseInt(min));

                String max = atts.getValue("max");
                if(max != null)
                    driverInfo.setMaxConnections(Integer.parseInt(max));
            } catch(NumberFormatException nfex) {
                logObj.log(logLevel, "Error loading numeric attribute", nfex);
                throw new SAXException("Error reading numeric attribute.", nfex);
            }
        }
    }
}
