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
package org.objectstyle.cayenne.dba;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/** Provides JDBC-RDBMS types mapping. Loads types info from a xml file.
  * 
  * @author Andrei Adamchik
  */
public class TypesHandler {
    private static volatile Logger logObj =
        Logger.getLogger(TypesHandler.class);

    private static Map handlerMap = new HashMap();

    /** 
     * Returns TypesHandler using XML file located in the package of
     * <code>adapterClass</code>.
     */
    public static TypesHandler getHandler(Class adapterClass) {
        return getHandler(
            Util.getPackagePath(adapterClass.getName()) + "/types.xml");
    }

    public static TypesHandler getHandler(String filePath) {
        synchronized (handlerMap) {
            TypesHandler handler = (TypesHandler) handlerMap.get(filePath);
            
            if (handler == null) {
                handler = new TypesHandler(filePath);
                handlerMap.put(filePath, handler);
            }
            
            return handler;
        }
    }

    protected Map typesMap;

    public TypesHandler(String typesConfigPath) {
        InputStream in =
            ResourceLocator.findResourceInClasspath(typesConfigPath);

        try {
            XMLReader parser = Util.createXmlReader();
            TypesParseHandler ph = new TypesParseHandler();
            parser.setContentHandler(ph);
            parser.setErrorHandler(ph);
            parser.parse(new InputSource(in));

            typesMap = ph.getTypes();
        } catch (Exception ex) {
            logObj.error(
                "Error creating TypesHandler '" + typesConfigPath + "'.",
                ex);
            throw new CayenneRuntimeException("Error parsing types", ex);
        } finally {
            try {
                in.close();
            } catch (IOException ioex) {
            }
        }
    }

    public String[] externalTypesForJdbcType(int type) {
        return (String[]) typesMap.get(new Integer(type));
    }

    /** Class helps to process Xml streams, creating DataDomain objects from
    * configuration data.*/
    final class TypesParseHandler extends DefaultHandler {
        private static final String JDBC_TYPE_TAG = "jdbc-type";
        private static final String DB_TYPE_TAG = "db-type";
        private static final String NAME_ATTR = "name";

        private Map types = new HashMap();
        private List currentTypes = new ArrayList();
        private int currentType = TypesMapping.NOT_DEFINED;

        public Map getTypes() {
            return types;
        }

        public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
            if (JDBC_TYPE_TAG.equals(localName)) {
                currentTypes.clear();
                String strType = atts.getValue("", NAME_ATTR);

                // convert to Types int value
                try {
                    currentType =
                        Types.class.getDeclaredField(strType).getInt(null);
                } catch (Exception ex) {
                    currentType = TypesMapping.NOT_DEFINED;
                    logObj.info("type not found: '" + strType + "', ignoring.");
                }
            } else if (DB_TYPE_TAG.equals(localName)) {
                currentTypes.add(atts.getValue("", NAME_ATTR));
            }
        }

        public void endElement(
            String namespaceURI,
            String localName,
            String qName)
            throws SAXException {
            if (JDBC_TYPE_TAG.equals(localName)
                && currentType != TypesMapping.NOT_DEFINED) {
                String[] typesAsArray = new String[currentTypes.size()];
                types.put(
                    new Integer(currentType),
                    currentTypes.toArray(typesAsArray));
            }
        }
    }
}
