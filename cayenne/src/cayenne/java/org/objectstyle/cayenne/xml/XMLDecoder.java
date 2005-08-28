/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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

package org.objectstyle.cayenne.xml;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.access.DataContext;

/**
 * XMLDecoder is used to decode XML into objects.
 * 
 * @author Kevin J. Menard, Jr.
 * @since 1.2
 */
public class XMLDecoder {

    static final Map classMapping = new HashMap();

    static {
        classMapping.put("boolean", Boolean.class);
        classMapping.put("int", Integer.class);
        classMapping.put("char", Character.class);
        classMapping.put("float", Float.class);
        classMapping.put("byte", Byte.class);
        classMapping.put("short", Short.class);
        classMapping.put("long", Long.class);
        classMapping.put("double", Double.class);
    }

    /** The root of the XML document being decoded. */
    private Element root;

    /** The data context to register decoded DataObjects with. */
    private DataContext dc;

    // TODO: H to the A to the C to the K
    private List decodedCollections = new ArrayList();

    /**
     * Default constructor. This will create an XMLDecoder instance that will decode
     * objects from XML, but will not register them with any DataContext.
     * 
     * @see XMLDecoder#XMLDecoder(DataContext)
     */
    public XMLDecoder() {
        this(null);
    }

    /**
     * Creates an XMLDecoder that will register decoded DataObjects with the specified
     * DataContext.
     * 
     * @param dc The DataContext to register decoded DataObjects with.
     */
    public XMLDecoder(DataContext dc) {
        this.dc = dc;
    }

    /**
     * Decodes an XML element to a Boolean.
     * 
     * @param xmlTag The tag identifying the element.
     * @return The tag's value.
     */
    public Boolean decodeBoolean(String xmlTag) {
        String val = decodeString(xmlTag);

        if (null == val) {
            return null;
        }

        return Boolean.valueOf(val);
    }

    /**
     * Decodes an XML element to a Double.
     * 
     * @param xmlTag The tag identifying the element.
     * @return The tag's value.
     */
    public Double decodeDouble(String xmlTag) {
        String val = decodeString(xmlTag);

        if (null == val) {
            return null;
        }

        return Double.valueOf(val);
    }

    /**
     * Decodes an XML element to a Float.
     * 
     * @param xmlTag The tag identifying the element.
     * @return The tag's value.
     */
    public Float decodeFloat(String xmlTag) {
        String val = decodeString(xmlTag);

        if (null == val) {
            return null;
        }

        return Float.valueOf(val);
    }

    /**
     * Decodes an XML element to an Integer.
     * 
     * @param xmlTag The tag identifying the element.
     * @return The tag's value.
     */
    public Integer decodeInteger(String xmlTag) {
        String val = decodeString(xmlTag);

        if (null == val) {
            return null;
        }

        return Integer.valueOf(val);
    }

    /**
     * Decodes an object from XML.
     * 
     * @param xmlTag The XML tag corresponding to the root of the encoded object.
     * @return The decoded object.
     */
    public Object decodeObject(String xmlTag) {
        // Find the XML element corresponding to the supplied tag.
        Element child = root.getChild(xmlTag);

        return decodeObject(child);
    }

    /**
     * Decodes an XML element to an Object.
     * 
     * @param child The XML element.
     * @return The tag's value.
     */
    private Object decodeObject(Element child) {

        if (null == child) {
            return null;
        }

        String type = child.getAttributeValue("type");
        if (null == type) {
            // TODO should we use String by default? Or guess from the property type?
            throw new CayenneRuntimeException("No type specified for tag '"
                    + child.getName()
                    + "'.");
        }

        // temp hack to support primitives...
        Class objectClass = (Class) classMapping.get(type);
        if (null == objectClass) {
            try {
                objectClass = Class.forName(type);
            }
            catch (Exception e) {
                throw new CayenneRuntimeException("Unrecognized class '"
                        + objectClass
                        + "'", e);
            }
        }

        try {
            // This crazy conditional checks if we're decoding a collection.  There are two ways
            // to enter into this body:
            // 1) If there are two elements at the same level with the same name, then they should
            // part of a collection.
            // 2) If a single occurring element has the "forceList" attribute set to "YES", then it
            // too should be treated as a collection.
            // 
            // The final part checks that we have not previously attempted to decode this collection,
            // which is necessary to prevent infinite loops .
            if ((((null != child.getParentElement()) && (child
                    .getParentElement()
                    .getChildren(child.getName())
                    .size() > 1)) || ((null != child.getAttributeValue("forceList")) && (child
                    .getAttributeValue("forceList")
                    .toUpperCase().equals("YES"))))
                    && (false == decodedCollections.contains(child))) {
                return decodeCollection(child);
            }

            // If the object implements XMLSerializable, delegate decoding to the class's
            // implementation of decodeFromXML().
            else if (XMLSerializable.class.isAssignableFrom(objectClass)) {
                XMLSerializable ret = (XMLSerializable) objectClass.newInstance();
                ret.decodeFromXML(this);

                return ret;
            }

            // If we hit here, then we should be encoding "simple" properties, which are basically
            // objects that take a single arg String constructor.
            else if (ConstructorUtils.getAccessibleConstructor(objectClass, String.class) != null) {
                // Create a new object of the type supplied as the "type" attribute
                // in the XML element that
                // represents the XML element's text value.
                // E.g., for <count type="java.lang.Integer">13</count>, this is
                // equivalent to new Integer("13");
                return ConstructorUtils.invokeConstructor(objectClass, child.getText());
            }

            // If we hit here, then we're trying to decode something we're not equipped to handle.
            // E.g., a complex object that does not implement XMLSerializable.
            else {
                throw new CayenneRuntimeException(
                        "Error decoding tag '"
                                + child.getName()
                                + "': "
                                + "specified class does not have a constructor taking either a String or an XMLDecoder");
            }
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error decoding tag '"
                    + child.getName()
                    + "'", e);
        }
    }

    /**
     * Decodes an XML element to a String.
     * 
     * @param xmlTag The tag identifying the element.
     * @return The tag's value.
     */
    public String decodeString(String xmlTag) {
        // Find the XML element corresponding to the supplied tag, and simply
        // return its text.
        return root.getChildText(xmlTag);
    }

    /**
     * Decodes XML wrapped by a Reader into an object.
     * 
     * @param xml Wrapped XML.
     * @return A new instance of the object represented by the XML.
     * @throws CayenneRuntimeException
     */
    public Object decode(Reader xml) throws CayenneRuntimeException {

        // Parse the XML into a JDOM representation.
        Document data = parse(xml);

        // Delegate to the decode() method that works on JDOM elements.
        return decodeElement(data.getRootElement());
    }

    /**
     * Decodes XML wrapped by a Reader into an object, using the supplied mapping file to
     * guide the decoding process.
     * 
     * @param xml Wrapped XML.
     * @param mappingUrl Mapping file describing how the XML elements and object
     *            properties correlate.
     * @return A new instance of the object represented by the XML.
     * @throws CayenneRuntimeException
     * @see XMLMappingUtil#decode(org.jdom.Element)
     */
    public Object decode(Reader xml, String mappingUrl) throws CayenneRuntimeException {
        // Parse the XML document into a JDOM representation.
        Document data = parse(xml);

        // MappingUtils will really do all the work.
        XMLMappingUtil mu = new XMLMappingUtil(mappingUrl);
        Object ret = mu.decode(data.getRootElement());

        if (null != dc) {
            dc.registerNewObject((DataObject) ret);
        }

        return ret;
    }

    /**
     * Decodes the XML element to an object. If the supplied DataContext is not null, the
     * object will be registered with it and committed to the database.
     * 
     * @param element The XML element.
     * @return The decoded object.
     * @throws CayenneRuntimeException
     */
    private Object decodeElement(Element element) throws CayenneRuntimeException {

        // Update root to be the supplied xml element. This is necessary as
        // root is used for decoding properties.
        Element oldRoot = root;
        root = element;

        // Create the object we're ultimately returning. It is represented
        // by the root element of the XML.
        Object object;

        try {
            object = decodeObject(element);
        }
        catch (Throwable th) {
            throw new CayenneRuntimeException("Error instantiating object", th);
        }

        if ((null != dc) && (object instanceof DataObject)) {
            dc.registerNewObject((DataObject) object);
        }

        root = oldRoot;
        decodedCollections.clear();

        return object;
    }

    /**
     * Decodes a Collection represented by XML wrapped by a Reader into a List of objects.
     * Each object will be registered with the supplied DataContext.
     * 
     * @param xml The XML element representing the elements in the collection to decode.
     * @return A List of all the decoded objects.
     * @throws CayenneRuntimeException
     */
    private Collection decodeCollection(Element xml) throws CayenneRuntimeException {

        Collection ret;
        try {
            String parentClass = xml.getParentElement().getAttributeValue("type");
            Object property = Class.forName(parentClass).newInstance();
            Collection c = (Collection) PropertyUtils.getNestedProperty(property, xml
                    .getName());

            ret = (Collection) c.getClass().newInstance();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException(
                    "Could not create collection with no-arg constructor.",
                    ex);
        }

        // Each child of the root corresponds to an XML representation of
        // the object. The idea is decode each of those into an object and add them to the
        // list to be returned.
        for (Iterator it = xml.getParentElement().getChildren(xml.getName()).iterator(); it
                .hasNext();) {
            // Decode the object.
            Element e = (Element) it.next();
            decodedCollections.add(e);
            Object o = decodeElement(e);

            // Add it to the output list.
            ret.add(o);
        }

        return ret;
    }

    /**
     * Decodes a list of objects. This intended to be used to decode a list of encdoded
     * DataObjects.
     * 
     * @param xml The wrapped XML encoding of the list of objects.
     * @return The list of decoded objects.
     */
    public List decodeList(Reader xml) {
        return decodeList(xml, null);
    }

    /**
     * Decodes a list of objects using the specified mapping. This intended to be used to
     * decode a list of encdoded DataObjects.
     * 
     * @param xml The wrapped XML encoding of the list of objects.
     * @param mappingUrl The mapping file that defines how the list elements should be
     *            decoded.
     * @return The list of decoded objects.
     * @throws CayenneRuntimeException
     */
    public List decodeList(Reader xml, String mappingUrl) throws CayenneRuntimeException {
        Element listRoot = parse(xml).getRootElement();

        List ret;
        try {
            String parentClass = listRoot.getAttributeValue("type");
            ret = (List) Class.forName(parentClass).newInstance();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException(
                    "Could not create collection with no-arg constructor.",
                    ex);
        }

        XMLMappingUtil mu = null;
        if (null != mappingUrl) {
            mu = new XMLMappingUtil(mappingUrl);
        }

        // Each child of the root corresponds to an XML representation of
        // the object. The idea is decode each of those into an object and add them to the
        // list to be returned.
        for (Iterator it = listRoot.getChildren().iterator(); it.hasNext();) {
            // Decode the object.
            Element e = (Element) it.next();
            decodedCollections.add(e);
            Object o;

            // Decode the item using the appropriate decoding method.
            if (null == mu) {
                o = decodeElement(e);
            }
            else {
                o = mu.decode(e);
            }

            // Register the decoded object with the data context if necessary.
            if ((null != dc) && (o instanceof DataObject)) {
                dc.registerNewObject((DataObject) o);
            }

            // Add it to the output list.
            ret.add(o);
        }

        return ret;
    }

    /**
     * Takes the XML wrapped in a Reader and returns a JDOM Document representation of it.
     * 
     * @param in Wrapped XML.
     * @return JDOM Document wrapping the XML for use throughout the rest of the decoder.
     * @throws CayenneRuntimeException
     */
    private Document parse(Reader in) throws CayenneRuntimeException {

        // Read in the XML file holding the data to be constructed into an
        // object.
        SAXBuilder parser = new SAXBuilder();

        try {
            return parser.build(in);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error parsing XML", ex);
        }
    }
}