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
package org.objectstyle.cayenne.xml;

import java.util.Collection;
import java.util.Iterator;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.objectstyle.cayenne.CayenneException;

/**
 * A helper class to encode objects to XML.
 * 
 * @since 1.2
 * @author Kevin J. Menard, Jr.
 */
public class XMLEncoder {

    /** The root of the XML document for the encoded object. */
    protected Element root = null;

    public String encode(Object object, String mappingFile) throws CayenneException {
        XMLMappingUtil mu = new XMLMappingUtil(mappingFile);
        root = mu.encode(object);

        return getXml();
    }

    public String getXml() {
        Document doc = new Document(root);

        // Return the XML tree as a pretty, formatted string.
        // TODO Make this output configurable.
        XMLOutputter serializer = new XMLOutputter();
        serializer.setFormat(Format.getPrettyFormat());

        return serializer.outputString(doc);
    }

    public void setRoot(String xmlTag, String type) {
        root = new Element(xmlTag);
        root.setAttribute("type", type);
    }

    public void encodeProperty(String xmlTag, Object property) {
        Element temp = new Element(xmlTag);
        temp.setAttribute("type", property.getClass().getName());
        temp.setText(property.toString());

        root.addContent(temp);
    }

    public void encodeCollection(String xmlTag, Collection c) {
        final Element temp = new Element(xmlTag);

        for (Iterator it = c.iterator(); it.hasNext();) {
            XMLSerializable element = (XMLSerializable) it.next();
            element.encodeAsXML(this);
            temp.addContent(root);
        }

        root = temp;
    }
}