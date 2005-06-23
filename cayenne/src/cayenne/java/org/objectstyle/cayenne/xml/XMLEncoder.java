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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.util.Util;

/**
 * A helper class to encode objects to XML.
 * 
 * @since 1.2
 * @author Kevin J. Menard, Jr.
 */
public class XMLEncoder {

    /** The root of the XML document for the encoded object. */
    protected Element root;

    /**
     * Encodes object using provided mapping file.
     * 
     * @param object The object to encode.
     * @param mappingFile The mapping file that defines the encoding.
     * @return The encoded object in XML.
     * @throws CayenneRuntimeException
     */
    public String encode(Object object, String mappingFile)
            throws CayenneRuntimeException {

        this.root = new XMLMappingUtil(mappingFile).encode(object);
        return getXml();
    }

    /**
     * Retrieves the XML representation of the encoded object.
     * 
     * @return The encoded object in XML.
     */
    public String getXml() {
        Document doc = new Document(root);

        // Return the XML tree as a pretty, formatted string.
        // TODO Make this output configurable.
        XMLOutputter serializer = new XMLOutputter();
        Format format = Format.getPrettyFormat();

        format.setOmitDeclaration(true);
        format.setLineSeparator("\n");

        serializer.setFormat(format);

        String ret = serializer.outputString(doc);
        doc.detachRootElement();

        ret = stripDoubleLineBreaks(ret);

        return ret;
    }
    
    // compensates for the lack of String.replace in JDK1.3
    private String stripDoubleLineBreaks(String string) {
        if (Util.isEmptyString(string)) {
            return string;
        }

        StringBuffer buffer = new StringBuffer(string.length());
        char previous = 0;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == previous && previous == '\n') {
                continue;
            }

            buffer.append(c);
            previous = c;
        }

        return buffer.toString();
    }

    /**
     * Sets the root node for the encoded object. This must be called prior to any of the
     * encodeXXX() methods. In order to use the encoder to encoder more than one object,
     * this method should be called again.
     * 
     * @param xmlTag The name of the XML root element.
     * @param type The fully specified class name of the encoded object.
     */
    public void setRoot(String xmlTag, String type) {
        root = new Element(xmlTag);
        root.setAttribute("type", type);
    }

    /**
     * Returns the root JDOM element of the encoded object.
     * 
     * @return The root JDOM element.
     */
    public Element getRoot() {
        return root;
    }

    /**
     * Encodes an object's property value to XML.
     * 
     * @param xmlTag The name of the XML element used to represent the property.
     * @param property The object's property value to encode.
     */
    public void encodeProperty(String xmlTag, Object property) {
        Element temp;

        if (property instanceof XMLSerializable) {
            XMLSerializable element = (XMLSerializable) property;

            // Make a back-up copy of the root and then nullify it. This is done so we can
            // recycle the encoder object.
            Element rootCopy = root;
            root = null;

            // Use the current encoder object to encode the property.
            element.encodeAsXML(this);

            // Store a copy of the encoded property, which is currently stored in the
            // root, since the encoder object was recycled.
            // Also, change the element name to that which was provided as a parameter to
            // the method.
            temp = root;
            temp.setName(xmlTag);

            // Restore the old root.
            root = rootCopy;
        }
        // TODO This block operates differently from the others. Here, root can never be
        // null and we need to add a list of items. Having two points of exit is not good.
        else if (property instanceof Collection) {
            Collection c = (Collection) property;
            root.addContent(encodeCollection(xmlTag, c));

            return;
        }
        else {
            temp = new Element(xmlTag);

            temp.setAttribute("type", property.getClass().getName());
            temp.setText(property.toString());
        }

        if (null != root) {
            root.addContent(temp);
        }
        else {
            root = temp;
        }
    }

    /**
     * Encodes a collection of objects.
     * 
     * @param xmlTag The name of the root XML element for the encoded collection.
     * @param c The collection to encode.
     * @return A flat list of the encoded objects (i.e., there is no root node).
     */
    protected List encodeCollection(String xmlTag, Collection c) {
        // We need a root node to add content to, but we'll be tossing the root and return
        // only its children. Thus, it really doesn't matter what we pass to setRoot()
        // here.
        XMLEncoder encoder = new XMLEncoder();
        encoder.setRoot("root", "root");

        // Encode each of the elements in the collection.
        for (Iterator it = c.iterator(); it.hasNext();) {
            encoder.encodeProperty(xmlTag, it.next());
        }

        // Retrieve the list of encoded objects, removing their parent (the root) node.
        List ret = encoder.getRoot().removeContent();

        // If there was only a single element encoded, add the forceList attribute so that
        // the decoder will know to treat this as a collection rather than as a single
        // item.
        if (1 == ret.size()) {
            Element e = (Element) ret.get(0);

            e.setAttribute("forceList", "YES");
        }

        return ret;
    }

    /**
     * Encodes a collection of objects. This intended to be used to encode a list of
     * DataObjects returned from a query or relationship retrieval.
     * 
     * @param xmlTag The name of the root XML element for the encoded collection.
     * @param dataObjects The collection to encode.
     * @return An XML string representing the encoded collection.
     */
    public String encodeList(String xmlTag, List dataObjects) {
        // It really doesn't matter what the root tag name is, but appending list
        // to the name to be used for the elements seems like a sensible solution.
        XMLEncoder encoder = new XMLEncoder();
        encoder.setRoot(xmlTag + "List", dataObjects.getClass().getName());

        encoder.getRoot().addContent(encoder.encodeCollection(xmlTag, dataObjects));

        return encoder.getXml();
    }

    /**
     * Encodes a collection of objects. This intended to be used to encode a list of
     * DataObjects returned from a query or relationship retrieval.
     * 
     * @param xmlTag The name of the root XML element for the encoded collection.
     * @param dataObjects The collection to encode.
     * @param mappingUrl The mapping file that defines how the list elements should be
     *            encoded.
     * @return An XML string representing the encoded collection.
     */
    public String encodeList(String xmlTag, List dataObjects, String mappingUrl) {
        // It really doesn't matter what the root tag name is, but appending list
        // to the name to be used for the elements seems like a sensible solution.
        XMLEncoder encoder = new XMLEncoder();
        encoder.setRoot(xmlTag + "List", dataObjects.getClass().getName());

        final Element tempRoot = encoder.root;

        // This is a bit funky, but basically we're encoding each element individually
        // using a new MappingUtils instance each time -- this may be a performance hit,
        // we'll have to see, but it does allow straightforward code reuse.
        for (Iterator it = dataObjects.iterator(); it.hasNext();) {
            encoder.encode(it.next(), mappingUrl);
            tempRoot.addContent(encoder.root);
        }

        encoder.root = tempRoot;

        return encoder.getXml();
    }
}