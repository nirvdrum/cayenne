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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * A convenience class for dealing with the mapping file. This can encode and decode
 * objects based upon the schema given by the map file.
 * 
 * @author Kevin J. Menard, Jr.
 * @since 1.2
 */
public class XMLMappingUtil {

    /** The root of the mapping file (the "model" tag). */
    protected Element root;

    /** Cached copies of entity items. */
    protected Map entities;

    /**
     * Creates a MappingUtils instance using a URL that points to the mapping file.
     * 
     * @param mappingUrl A URL to the mapping file that specifies the mapping model.
     * @throws CayenneException
     */
    public XMLMappingUtil(String mappingUrl) throws CayenneRuntimeException {

        // Read in the mapping file.
        SAXBuilder parser = new SAXBuilder();
        Document mapping;

        try {
            mapping = parser.build(mappingUrl);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error parsing XML", ex);
        }

        setRoot(mapping.getRootElement());
    }

    /**
     * Creates a MappingUtils instance with the mapping file provided as a JDOM document.
     * 
     * @param mapping The mapping file as a JDOM document.
     */
    public XMLMappingUtil(Document mapping) {
        setRoot(mapping.getRootElement());
    }

    /**
     * Sets the root of the mapping document.
     * 
     * @param root The root node of the mapping document.
     */
    protected void setRoot(Element root) {
        
        if (!"model".equals(root.getName())) {
            throw new CayenneRuntimeException(
                    "Root of the mapping model must be \"model\"");
        }

        this.root = root;

        entities = new HashMap();
        for (Iterator it = getEntities().iterator(); it.hasNext();) {
            Element e = (Element) it.next();

            entities.put(e.getAttributeValue("xmlTag"), e);
        }
    }

    /**
     * Returns a safe copy of the entities list.
     * 
     * @return The list of entities.
     */
    public List getEntities() {
        return Collections.unmodifiableList(root.getChildren());
    }

    /**
     * Returns a safe copy of the entity names set.
     * 
     * @return The set of entity names.
     */
    public Set getEntityNames() {
        return Collections.unmodifiableSet(entities.keySet());
    }

    /**
     * Returns the "root" entity. This entity represents the object to ultimately be
     * created.
     * 
     * @return The "root" entity.
     */
    // TODO Decide whether this should be called "primary" entity.
    public Element getRootEntity() {
        return (Element) getEntities().get(0);
    }

    /**
     * Encodes an entity to XML.
     * 
     * @param object The object to be encoded by this entity block.
     * @param entity The entity block in XML (from the mapping file).
     * @return The encoded entity.
     * @throws CayenneException
     */
    public Element encodeEntity(Object object, Element entity)
            throws CayenneRuntimeException {

        // Create the xml item to return.
        Element ret = new Element(entity.getAttributeValue("xmlTag"));

        // Each of the entity's children will correspond to a child in the returned
        // item.
        for (Iterator it = entity.getChildren().iterator(); it.hasNext();) {
            Element property = (Element) it.next();
            String xmlTag = property.getAttributeValue("xmlTag");

            // If the child refers to an entity, skip over it, since when that entity
            // is processed, it will
            // generate the appropriate xml items.
            if (getEntityNames().contains(xmlTag) == false) {
                // Otherwise, create a new child for the returned xml item, encoding
                // the passed in object's property.
                Element e = new Element(xmlTag);
                String propertyName = property.getAttributeValue("name");
                try {
                    e.setText(BeanUtils.getNestedProperty(object, propertyName));
                }
                catch (Exception ex) {
                    throw new CayenneRuntimeException("Error reading property '"
                            + propertyName
                            + "'.", ex);
                }

                // Once the property is encoded, add it to the item to be returned.
                ret.addContent(e);
            }
        }

        return ret;
    }

    /**
     * Encodes the object to XML. This object should correspond to the object specified in
     * the "root" entity in the mapping file.
     * 
     * @param object The object to be encoded.
     * @return The XML document containing the encoded object.
     */
    public Element encode(Object object) throws CayenneRuntimeException {

        Element ret = null;

        // An object encoding is simply an encoding of each of the entities specified
        // in the mapping file.
        for (Iterator it = getEntities().iterator(); it.hasNext();) {
            Element e = (Element) it.next();

            // Handle the root entity differently. We want its encoding to be at the
            // base level (i.e, the
            // root of the returned object's encoding).
            if (getRootEntity() == e) {
                ret = encodeEntity(object, e);
            }

            // If this entity is not the root entity, then append its encoding to the
            // root entity's encoding.
            else {
                String prop = getEntityRef(e.getAttributeValue("xmlTag"));
                Object o;

                try {
                    o = PropertyUtils.getProperty(object, prop);
                }
                catch (Exception ex) {
                    throw new CayenneRuntimeException("Error reading property '"
                            + prop
                            + "'.", ex);
                }
                ret.addContent(encodeEntity(o, e));
            }
        }

        return ret;
    }

    /**
     * Returns the entity XML block with the same "xmlTag" value as the passed in name.
     * 
     * @param name The name of the entity to retrieve.
     * @return The entity with "xmlTag" equal to the passed in name.
     */
    public Element getEntity(String name) {
        return (Element) entities.get(name);
    }

    /**
     * Returns the property name that is associated with the passed in entity name. This
     * is used to determine what property an entity block represents.
     * 
     * @param ref The name of the entity.
     * @return The name of the property that is associated with the named entity.
     */
    // Decide whether to change the @param tag description since "the name of the entity"
    // can be deceiving.
    public String getEntityRef(String ref) {
        for (Iterator it = getRootEntity().getChildren().iterator(); it.hasNext();) {
            Element child = (Element) it.next();

            if (child.getAttributeValue("xmlTag").equals(ref)) {
                return child.getAttributeValue("name");
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
     */
    public void decodeProperty(Object object, Element entity, Element encProperty)
            throws CayenneRuntimeException {

        List children = encProperty.getChildren();
        String xmlTag = encProperty.getName();

        // This is a "simple" encoded property. Find the associated property mapping
        // in the entity.
        if (children.isEmpty()) {
            // Scan each of the entity's property mappings to see if any of them
            // correspond to the passed in xmlTag.
            for (Iterator it = entity.getChildren().iterator(); it.hasNext();) {
                Element e = (Element) it.next();

                // If the property mapping is found . . .
                if (e.getAttributeValue("xmlTag").equals(xmlTag)) {

                    // use it to determine the actual property to be setting in the
                    // object.
                    setProperty(object, e.getAttributeValue("name"), encProperty
                            .getText());
                }
            }
        }

        // If the property has children, then it corresponds to a "helper" entity,
        // which corresponds to an embedded object.
        else {
            // Create the embedded object.
            Object o = newInstance(getEntity(xmlTag).getAttributeValue("name"));

            // Decode each of the property's children, setting values in the newly
            // created object.
            for (Iterator it = children.iterator(); it.hasNext();) {
                Element child = (Element) it.next();

                decodeProperty(o, getEntity(xmlTag), child);
            }

            // Set the property in the main object that corresponds to the newly
            // created object.
            setProperty(object, getEntityRef(xmlTag), o);
        }
    }

    /**
     * Decode the supplied XML JDOM document into an object.
     * 
     * @param data The JDOM document containing the encoded object.
     * @return The decoded object.
     */
    public Object decode(Document data) throws CayenneRuntimeException {

        // TODO: Add an error check to make sure the mapping file actually is for this
        // data file.
        List values = data.getRootElement().getChildren();

        // Create the object to be returned.
        Object ret = newInstance(getRootEntity().getAttributeValue("name"));

        // We want to read each value from the XML file and then set the corresponding
        // property value
        // in the object to be returned.
        for (Iterator it = values.iterator(); it.hasNext();) {
            Element value = (Element) it.next();

            decodeProperty(ret, getRootEntity(), value);
        }

        return ret;
    }

    /**
     * Sets object property, wrapping any exceptions in CayenneRuntimeException.
     */
    void setProperty(Object object, String property, Object value)
            throws CayenneRuntimeException {
        try {
            BeanUtils.setProperty(object, property, value);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error setting property " + property, ex);
        }
    }

    /**
     * Instantiates a new object for class name, wrapping any exceptions in
     * CayenneRuntimeException.
     */
    Object newInstance(String className) throws CayenneRuntimeException {
        try {
            return Class.forName(className).newInstance();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error creating instance of class "
                    + className, ex);
        }
    }
}