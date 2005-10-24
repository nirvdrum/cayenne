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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.property.PropertyUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A convenience class for dealing with the mapping file. This can encode and decode
 * objects based upon the schema given by the map file.
 * 
 * @author Kevin J. Menard, Jr.
 * @since 1.2
 */
final class XMLMappingDescriptor {

    private SerializableEntity rootEntity;
    private Map entities;

    /**
     * Creates new XMLMappingDescriptor using a URL that points to the mapping file.
     * 
     * @param mappingUrl A URL to the mapping file that specifies the mapping model.
     * @throws CayenneRuntimeException
     */
    XMLMappingDescriptor(String mappingUrl) throws CayenneRuntimeException {

        // Read in the mapping file.
        DocumentBuilder builder = XMLUtil.newBuilder();

        Document document;
        try {
            document = builder.parse(mappingUrl);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error parsing XML at " + mappingUrl, ex);
        }

        Element root = document.getDocumentElement();

        if (!"model".equals(root.getNodeName())) {
            throw new CayenneRuntimeException(
                    "Root of the mapping model must be \"model\"");
        }

        Map entities = new HashMap();
        Iterator it = XMLUtil.getChildren(root).iterator();
        while (it.hasNext()) {
            Element e = (Element) it.next();

            SerializableEntity entity = new SerializableEntity(this, e);
            String tag = e.getAttribute("xmlTag");
            entities.put(tag, entity);

            if (rootEntity == null) {
                rootEntity = entity;
            }
        }

        this.entities = entities;
    }

    SerializableEntity getRootEntity() {
        return rootEntity;
    }

    /**
     * Decode the supplied XML JDOM document into an object.
     * 
     * @param xml The JDOM document containing the encoded object.
     * @return The decoded object.
     * @throws CayenneRuntimeException
     */
    Object decode(Element xml) throws CayenneRuntimeException {

        // TODO: Add an error check to make sure the mapping file actually is for this
        // data file.

        // Create the object to be returned.
        Object ret = newInstance(rootEntity.getName());

        // We want to read each value from the XML file and then set the corresponding
        // property value in the object to be returned.
        for (Iterator it = XMLUtil.getChildren(xml).iterator(); it.hasNext();) {
            Element value = (Element) it.next();
            decodeProperty(ret, rootEntity.getDescriptor(), value);
        }

        return ret;
    }

    /**
     * Returns the entity XML block with the same "xmlTag" value as the passed in name.
     * 
     * @param name The name of the entity to retrieve.
     * @return The entity with "xmlTag" equal to the passed in name.
     */
    SerializableEntity getEntity(String name) {
        return (SerializableEntity) entities.get(name);
    }

    /**
     * Returns the property name that is associated with the passed in entity name. This
     * is used to determine what property an entity block represents.
     * 
     * @param rootEntity The root to which the reference to find is relative to.
     * @param ref The name of the entity.
     * @return The name of the property that is associated with the named entity.
     */
    // TODO Decide whether to change the @param tag description since "the name of the
    // entity" can be deceiving.
    private String getEntityRef(Element rootEntity, String ref) {
        for (Iterator it = XMLUtil.getChildren(rootEntity).iterator(); it.hasNext();) {
            Element child = (Element) it.next();

            if (child.getAttribute("xmlTag").equals(ref)) {
                return child.getAttribute("name");
            }
        }

        return null;
    }

    /**
     * Decodes a property.
     * 
     * @param object The object to be updated with the decoded property's value.
     * @param entity The entity block that contains the property mapping for the value.
     * @param encProperty The encoded property.
     * @throws CayenneRuntimeException
     */
    private void decodeProperty(Object object, Element entity, Element encProperty)
            throws CayenneRuntimeException {

        List children = XMLUtil.getChildren(encProperty);
        String xmlTag = encProperty.getNodeName();

        // This is a "simple" encoded property. Find the associated property mapping
        // in the entity.
        if (children.isEmpty()) {
            // Scan each of the entity's property mappings to see if any of them
            // correspond to the passed in xmlTag.
            for (Iterator it = XMLUtil.getChildren(entity).iterator(); it.hasNext();) {
                Element e = (Element) it.next();

                // If the property mapping is found . . .
                if (e.getAttribute("xmlTag").equals(xmlTag)) {

                    // use it to determine the actual property to be setting in the
                    // object.
                    PropertyUtils.setProperty(object, e.getAttribute("name"), XMLUtil
                            .getText(encProperty));
                }
            }
        }
        // If the property has children, then it corresponds to a "helper" entity,
        // which corresponds to an embedded object.
        else {
            // Create the embedded object.
            Object o = newInstance(getEntity(xmlTag).getDescriptor().getAttribute("name"));

            // Decode each of the property's children, setting values in the newly
            // created object.
            for (Iterator it = children.iterator(); it.hasNext();) {
                Element child = (Element) it.next();

                decodeProperty(o, getEntity(xmlTag).getDescriptor(), child);
            }

            // Set the property in the main object that corresponds to the newly
            // created object.
            Object property = PropertyUtils.getProperty(object, getEntityRef(
                    entity,
                    xmlTag));

            if (property instanceof Collection) {
                Collection c = (Collection) property;
                c.add(o);
            }
            else {
                PropertyUtils.setProperty(object, getEntityRef(entity, xmlTag), o);
            }
        }
    }

    /**
     * Instantiates a new object for class name, wrapping any exceptions in
     * CayenneRuntimeException.
     * 
     * @param className The type to create a new instance of.
     * @return The newly created object.
     * @throws CayenneRuntimeException
     */
    private Object newInstance(String className) throws CayenneRuntimeException {
        try {
            return Class.forName(
                    className,
                    true,
                    Thread.currentThread().getContextClassLoader()).newInstance();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error creating instance of class "
                    + className, ex);
        }
    }
}