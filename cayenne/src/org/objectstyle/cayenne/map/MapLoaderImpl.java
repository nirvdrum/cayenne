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
package org.objectstyle.cayenne.map;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.util.Util;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/** 
 * Default implementation of MapLoader interface.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class MapLoaderImpl extends DefaultHandler implements MapLoader {
	static Logger logObj = Logger.getLogger(MapLoaderImpl.class.getName());

	private static final String DATA_MAP_TAG = "data-map";
	private static final String DB_ENTITY_TAG = "db-entity";
	private static final String OBJ_ENTITY_TAG = "obj-entity";
	private static final String DB_ATTRIBUTE_TAG = "db-attribute";
	private static final String DB_ATTRIBUTE_REF_TAG = "db-attribute-ref";
	private static final String OBJ_ATTRIBUTE_TAG = "obj-attribute";
	private static final String OBJ_RELATIONSHIP_TAG = "obj-relationship";
	private static final String DB_RELATIONSHIP_TAG = "db-relationship";
	private static final String DB_RELATIONSHIP_REF_TAG = "db-relationship-ref";
	private static final String DB_ATTRIBUTE_PAIR_TAG = "db-attribute-pair";
	private static final String TRUE = "true";
	private static final String FALSE = "false";

	/* Reading from XML */
	private DataMap dataMap;
	private DbEntity dbEntity;
	private ObjEntity objEntity;
	private DbRelationship dbRelationship;
	private ObjRelationship objRelationship;
	private DbAttribute attrib;
	private HashMap dbRelationshipMap;

	/* Saving to XML */
	private ArrayList objRelationships;
	private ArrayList dbRelationships;
	private ArrayList dbRelationshipRefs;

	/** Loads the data map from the input source (usually file). */
	public synchronized DataMap loadDataMap(InputSource src)
		throws DataMapException {
		try {
			String system_id = src.getSystemId();
			if (null == system_id)
				system_id = "Untitled";

			String mapName =
				system_id.substring(system_id.lastIndexOf('/') + 1);
			dataMap = new DataMap(mapName);
			dbRelationshipMap = new HashMap();
			XMLReader parser = Util.createXmlReader();

			parser.setContentHandler(this);
			parser.setErrorHandler(this);

			parser.parse(src);
		} catch (SAXException e) {
			logObj.log(Level.INFO, "SAX Exception.", e);

			Exception wrappedEx = e.getException();
			if (e.getCause() != null) {
				logObj.log(Level.INFO, "SAX Exception cause.", e.getCause());
			}

			dataMap = null;
			throw new DataMapException(
				"Wrong DataMap format: " + e.getMessage());
		} catch (Exception e) {
			logObj.log(Level.INFO, "Exception.", e);
			dataMap = null;
			throw new DataMapException(
				"DataMap could not be loaded: " + e.getMessage());
		}
		return dataMap;
	}

	/** Load multiple DataMaps at once. */
	public DataMap[] loadDataMaps(InputSource[] src) throws DataMapException {
		int len = src.length;
		DataMap[] maps = new DataMap[len];
		for (int i = 0; i < src.length; i++) {
			maps[i] = loadDataMap(src[i]);
		}
		return maps;
	}

	/** 
	 * Creates, configures and returns ResourceLocator object used 
	 * to lookup DataMap files.
	 */
	protected ResourceLocator configLocator() {
		ResourceLocator locator = new ResourceLocator();
		locator.setSkipAbsPath(true);
		locator.setSkipClasspath(false);
		locator.setSkipCurDir(false);
		locator.setSkipHomeDir(false);

		// Configuration superclass statically defines what 
		// ClassLoader to use for resources. This
		// allows applications to control where resources 
		// are loaded from.
		locator.setClassLoader(Configuration.getResourceLoader());

		return locator;
	}

	/** Loads the array of data maps per array of map file URI's.
	 * This is a convenience method that would resolve string URI's
	 * to InputSources and then call <code>loadDataMap</code> for each one of them.
	 *
	 * @throws DataMapException if source URI's do not resolve to valid map files
	 * @throws NullPointerException if <code>src</code> parameter is null.
	 */
	public DataMap[] loadDataMaps(String[] src) throws DataMapException {
		int len = src.length;
		DataMap[] dataMaps = new DataMap[len];

		// configure resource locator
		ResourceLocator locator = configLocator();

		for (int i = 0; i < len; i++) {

			InputStream in = locator.findResourceStream(src[i]);
			if (in == null) {
				throw new DataMapException("Can't find data map " + src[i]);
			}

			try {
				dataMaps[i] = loadDataMap(new InputSource(in));
			} finally {
				try {
					in.close();
				} catch (IOException ioex) {
				}
			}
		}
		return dataMaps;
	}

	public void startElement(
		String namespace_uri,
		String local_name,
		String q_name,
		Attributes atts)
		throws SAXException {
		if (local_name.equals(DATA_MAP_TAG)) {
		} else if (local_name.equals(DB_ENTITY_TAG)) {
			processStartDbEntity(atts);
		} else if (local_name.equals(DB_ATTRIBUTE_TAG)) {
			processStartDbAttribute(atts);
		} else if (local_name.equals(DB_ATTRIBUTE_REF_TAG)) {
			processStartDbAttributeRef(atts);
		} else if (local_name.equals(OBJ_ENTITY_TAG)) {
			processStartObjEntity(atts);
		} else if (local_name.equals(OBJ_ATTRIBUTE_TAG)) {
			processStartObjAttribute(atts);
		} else if (local_name.equals(DB_RELATIONSHIP_TAG)) {
			processStartDbRelationship(atts);
		} else if (local_name.equals(DB_ATTRIBUTE_PAIR_TAG)) {
			processStartDbAttributePair(atts);
		} else if (local_name.equals(OBJ_RELATIONSHIP_TAG)) {
			processStartObjRelationship(atts);
		} else if (local_name.equals(DB_RELATIONSHIP_REF_TAG)) {
			processStartDbRelationshipRef(atts);
		}
	}

	public void endElement(
		String namespaceURI,
		String local_name,
		String qName)
		throws SAXException {
		if (local_name.equals(DATA_MAP_TAG)) {
		} else if (local_name.equals(DB_ENTITY_TAG)) {
			processEndDbEntity();
		} else if (local_name.equals(OBJ_ENTITY_TAG)) {
			processEndObjEntity();
		} else if (local_name.equals(DB_ATTRIBUTE_TAG)) {
			processEndDbAttribute();
		} else if (local_name.equals(DB_RELATIONSHIP_TAG)) {
			processEndDbRelationship();
		} else if (local_name.equals(OBJ_RELATIONSHIP_TAG)) {
			processEndObjRelationship();
		}
	}

	public void warning(SAXParseException e) throws SAXException {
		System.out.println(
			"**Parsing warning**\n"
				+ "Line:"
				+ e.getLineNumber()
				+ "\nMessage:"
				+ e.getMessage());
		throw new SAXException("Warning!");
	}

	public void error(SAXParseException e) throws SAXException {
		System.out.println(
			"**Parsing error**\n"
				+ "Line:"
				+ e.getLineNumber()
				+ "\nMessage:"
				+ e.getMessage());
		throw new SAXException("Warning!");
	}

	public void fatalError(SAXParseException e) throws SAXException {
		System.out.println(
			"**Parsing fatal error**\n"
				+ "Line:"
				+ e.getLineNumber()
				+ "\nMessage:"
				+ e.getMessage());
		throw new SAXException("Warning!");
	}

	/* * * STORE TO XML PART * * */

	/** Archives provided <code>map</code> to XML and stores it to <code>out</code> PrintStream. */
	public synchronized void storeDataMap(PrintWriter out, DataMap map)
		throws DataMapException {
		objRelationships = new ArrayList();
		dbRelationshipRefs = new ArrayList();
		dbRelationships = new ArrayList();
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<data-map>");
		storeDbEntities(out, map);
		storeObjEntities(out, map);
		storeDbRelationships(out);
		storeObjRelationships(out);
		out.println("</data-map>");
		objRelationships = null;
		dbRelationships = null;
		dbRelationshipRefs = null;
	}

	private void storeDbEntities(PrintWriter out, DataMap map) {
		Collection db_entities = map.getDbEntityMap().values();
		Iterator iter = db_entities.iterator();
		while (iter.hasNext()) {
			DbEntity temp = (DbEntity) iter.next();
			out.print("\t<db-entity name=\"" + temp.getName() + '\"');
			if (null != temp.getSchema()) {
				out.print(" schema=\"");
				out.print(temp.getSchema());
				out.print('\"');
			}
			if (null != temp.getCatalog()) {
				out.print(" catalog=\"");
				out.print(temp.getCatalog());
				out.print('\"');
			}
			out.println('>');

			storeDbAttribute(out, temp);
			out.println("\t</db-entity>");
			dbRelationships.addAll(temp.getRelationshipList());
		} // End while()
	}

	private void storeDbAttribute(PrintWriter out, DbEntity db_entity) {
		Collection db_attributes = db_entity.getAttributeMap().values();
		Iterator iter = db_attributes.iterator();
		while (iter.hasNext()) {
			DbAttribute temp = (DbAttribute) iter.next();
			out.print("\t\t<db-attribute name=\"" + temp.getName() + '\"');

			String type = TypesMapping.getSqlNameByType(temp.getType());
			if (type != null) {
				out.print(" type=\"" + type + '\"');
			}

			// If attribute is part of primary key
			if (temp.isPrimaryKey())
				out.print(" isPrimaryKey=\"true\"");

			if (temp.isMandatory())
				out.print(" isMandatory=\"true\"");

			if (temp.getMaxLength() > 0) {
				out.print(" length=\"");
				out.print(temp.getMaxLength());
				out.print('\"');
			}

			if (temp.getPrecision() > 0) {
				out.print(" precision=\"");
				out.print(temp.getPrecision());
				out.print('\"');
			}
			out.println("/>");
		}

	}

	private void storeObjEntities(PrintWriter out, DataMap map) {
		Collection obj_entities = map.getObjEntityMap().values();
		Iterator iter = obj_entities.iterator();
		while (iter.hasNext()) {
			ObjEntity temp = (ObjEntity) iter.next();
			out.print("\t<obj-entity name=\"");
			out.print(temp.getName());

			if (temp.getClassName() != null) {
				out.print("\" className=\"");
				out.print(temp.getClassName());
			}

			out.print('\"');

			if (temp.getDbEntity() != null) {
				out.print(" dbEntityName=\"");
				out.print(temp.getDbEntity().getName());
				out.print('\"');
			}

			out.println('>');
			storeObjAttribute(out, temp);

			out.println("\t</obj-entity>");
			Collection objRels = temp.getRelationshipMap().values();
			objRelationships.addAll(objRels);

			ArrayList dbRels = new ArrayList();
			Iterator relIt = objRels.iterator();
			while (relIt.hasNext()) {
				ObjRelationship objRel = (ObjRelationship) relIt.next();
				dbRelationshipRefs.addAll(objRel.getDbRelationshipList());
			}
		}
	}

	private void storeObjAttribute(PrintWriter out, ObjEntity obj_entity) {
		Collection obj_attributes = obj_entity.getAttributeMap().values();
		Iterator iter = obj_attributes.iterator();
		while (iter.hasNext()) {
			ObjAttribute temp = (ObjAttribute) iter.next();
			out.print("\t\t<obj-attribute name=\"" + temp.getName() + '\"');

			if (temp.getType() != null) {
				out.print(" type=\"");
				out.print(temp.getType());
				out.print('\"');
			}

			// If this obj attribute is mapped to db attribute
			if (temp.getDbAttribute() != null) {
				out.print(" db-attribute-name=\"");
				out.print(temp.getDbAttribute().getName());
				out.print('\"');
			}
			out.println("/>");
		}
	}

	private void storeObjRelationships(PrintWriter out)
		throws DataMapException {
		Iterator iter = objRelationships.iterator();
		while (iter.hasNext()) {
			ObjRelationship temp = (ObjRelationship) iter.next();
			out.print("\t<obj-relationship name=\"" + temp.getName() + '\"');
			out.print(" source=\"" + temp.getSourceEntity().getName() + '\"');
			out.print(" target=\"" + temp.getTargetEntity().getName() + '\"');
			out.print(" toMany=\"" + (temp.isToMany() ? TRUE : FALSE) + '\"');
			out.println('>');
			storeDbRelationshipRef(out, temp);
			out.println("\t</obj-relationship>");
		}
	}

	private void storeDbRelationshipRef(
		PrintWriter out,
		ObjRelationship obj_rel)
		throws DataMapException {
		Iterator iter = obj_rel.getDbRelationshipList().iterator();
		while (iter.hasNext()) {
			DbRelationship rel = (DbRelationship) iter.next();
			if (!dbRelationships.contains(rel)) {
				throw new DataMapException(
					"Broken reference. Obj Relationship "
						+ obj_rel.getSourceEntity().getName()
						+ "->"
						+ obj_rel.getTargetEntity().getName()
						+ " uses DbRelationship "
						+ rel.getSourceEntity().getName()
						+ "->"
						+ rel.getTargetEntity().getName()
						+ " which doesn't exist anymore.");
			}

			out.print("\t\t<");
			out.print(DB_RELATIONSHIP_REF_TAG);
			out.print(" source=\"");
			out.print(rel.getSourceEntity().getName());
			out.print("\" target=\"");
			out.print(rel.getTargetEntity().getName());
			out.print("\" name=\"");
			out.print(rel.getName());
			out.println("\"/>");
		} // End while()
	}

	private void storeDbRelationships(PrintWriter out) {
		Iterator iter = dbRelationships.iterator();
		while (iter.hasNext()) {
			DbRelationship temp = (DbRelationship) iter.next();
			out.print("\t<");
			out.print(DB_RELATIONSHIP_TAG);
			out.print(" name=\"");
			out.print(temp.getName());
			out.print("\" source=\"");
			out.print(temp.getSourceEntity().getName());
			out.print("\" target=\"");
			out.print(temp.getTargetEntity().getName());
			out.print("\" toDependentPK=\"");
			out.print(temp.isToDependentPK() ? TRUE : FALSE);
			out.print("\" toMany=\"");
			out.print(temp.isToMany() ? TRUE : FALSE);
			out.println("\">");
			storeDbAttributePair(out, temp);
			out.print("\t</");
			out.print(DB_RELATIONSHIP_TAG);
			out.println('>');
		} // End while()
	}

	private void storeDbAttributePair(PrintWriter out, DbRelationship db_rel) {
		Iterator iter = db_rel.getJoins().iterator();
		while (iter.hasNext()) {
			DbAttributePair pair = (DbAttributePair) iter.next();
			out.print("\t\t<");
			out.print(DB_ATTRIBUTE_PAIR_TAG);
			out.print(" source=\"");
			out.print(pair.getSource().getName());
			out.println("\" target=\"" + pair.getTarget().getName() + "\"/>");
		}
	}

	private void processStartDbEntity(Attributes atts) throws SAXException {
		String name = atts.getValue("", "name");
		String parentName = atts.getValue("", "parentName");

		if (parentName != null) {
			DbEntity parent = dataMap.getDbEntity(parentName);
			if (parent == null) {
				throw new SAXException(
					"Can't find parent DbEntity '"
						+ parentName
						+ "' for derived entity '"
						+ name
						+ "'.");
			}
			dbEntity = new DerivedDbEntity(name);
			((DerivedDbEntity) dbEntity).setParentEntity(parent);
		} else {
			dbEntity = new DbEntity(name);
		}

		dbEntity.setSchema(atts.getValue("", "schema"));
		dbEntity.setCatalog(atts.getValue("", "catalog"));
	}

	private void processStartDbAttributeRef(Attributes atts)
		throws SAXException {
		String name = atts.getValue("", "name");
		if ((attrib instanceof DerivedDbAttribute) && (dbEntity instanceof DerivedDbEntity)) {
            DbEntity parent = ((DerivedDbEntity)dbEntity).getParentEntity();
            DbAttribute ref = (DbAttribute)parent.getAttribute(name);
            ((DerivedDbAttribute)attrib).addParam(ref);
		} else {
			throw new SAXException(
				"Referenced attributes are not supported by regular DbAttributes. Offending attribute name '"
					+ attrib.getName()
					+ "'.");
		}
	}

	private void processStartDbAttribute(Attributes atts) throws SAXException {
		String name = atts.getValue("", "name");
		String type = atts.getValue("", "type");
		String spec = atts.getValue("", "spec");

		attrib =
			(dbEntity instanceof DerivedDbEntity)
				? new DerivedDbAttribute(
					name,
					TypesMapping.getSqlTypeByName(type),
					dbEntity,
					spec)
				: new DbAttribute(
					name,
					TypesMapping.getSqlTypeByName(type),
					dbEntity);

		dbEntity.addAttribute(attrib);

		String temp = atts.getValue("", "length");
		if (temp != null) {
			attrib.setMaxLength(Integer.parseInt(temp));
		}
		temp = atts.getValue("", "precision");
		if (temp != null) {
			attrib.setPrecision(Integer.parseInt(temp));
		}
		temp = atts.getValue("", "isPrimaryKey");
		if (temp != null && temp.equalsIgnoreCase(TRUE)) {
			attrib.setPrimaryKey(true);
		}
		temp = atts.getValue("", "isMandatory");
		if (temp != null && temp.equalsIgnoreCase(TRUE)) {
			attrib.setMandatory(true);
		}

		temp = atts.getValue("", "isGroupBy");
		if (temp != null && temp.equalsIgnoreCase(TRUE)) {
			if (dbEntity instanceof DerivedDbEntity) {
				((DerivedDbEntity) dbEntity).addGroupByAttribute(attrib);
			} else {
				throw new SAXException(
					"'isGroupBy' not supported for regular DbEntities. Offending attribute name '"
						+ name
						+ "'.");
			}
		}
	}

	private void processStartObjEntity(Attributes atts) {
		objEntity = new ObjEntity(atts.getValue("", "name"));
		objEntity.setClassName(atts.getValue("", "className"));
		String temp = atts.getValue("", "dbEntityName");
		if (null != temp) {
			DbEntity db_temp = dataMap.getDbEntity(temp);
			objEntity.setDbEntity(db_temp);
		}
	}

	private void processStartObjAttribute(Attributes atts) {
		String name = atts.getValue("", "name");
		String type = atts.getValue("", "type");

		ObjAttribute attrib = new ObjAttribute(name, type, objEntity);
		objEntity.addAttribute(attrib);

		String temp = atts.getValue("", "db-attribute-name");
		// Navigate to db attribute in the corresponding table
		if (null != temp) {
			if (null != objEntity.getDbEntity()) {
				DbAttribute db_temp =
					(DbAttribute) objEntity.getDbEntity().getAttribute(temp);
				attrib.setDbAttribute(db_temp);
			}
		}
	}

	private void processStartDbRelationship(Attributes atts)
		throws SAXException {

		String temp = atts.getValue("", "source");
		if (null == temp) {
			throw new SAXException(
				"MapLoaderImpl::processStartDbRelationship(),"
					+ " Unable to parse source. Attributes:\n"
					+ printAttributes(atts).toString());
		}

		DbEntity source = dataMap.getDbEntity(temp);
		if (null == source) {
			System.out.println(
				"MapLoaderImpl::processStartDbRelationship(),"
					+ " Unable to find source "
					+ temp);
			return;
		}
		temp = atts.getValue("", "target");
		if (null == temp) {
			throw new SAXException(
				"MapLoaderImpl::processStartDbRelationship(),"
					+ " Unable to parse target. Attributes:\n"
					+ printAttributes(atts).toString());
		}

		DbEntity target = dataMap.getDbEntity(temp);
		if (null == target) {
			throw new SAXException(
				"MapLoaderImpl::processStartDbRelationship(),"
					+ " Unable to find target "
					+ temp);
		}

		temp = atts.getValue("", "toMany");
		boolean toMany = temp != null && temp.equalsIgnoreCase(TRUE);

		temp = atts.getValue("", "toDependentPK");
		boolean toDependentPK = temp != null && temp.equalsIgnoreCase(TRUE);

		String name = atts.getValue("", "name");
		if (null == name) {
			throw new SAXException(
				"MapLoaderImpl::processStartDbRelationship(),"
					+ " Unable to parse name. Attributes:\n"
					+ printAttributes(atts).toString());
		}

		dbRelationship = new DbRelationship();
		dbRelationship.setSourceEntity(source);
		dbRelationship.setTargetEntity(target);
		dbRelationship.setToMany(toMany);
		dbRelationship.setName(name);
		dbRelationship.setToDependentPK(toDependentPK);
		// Save the reference to this db relationship for later resolution
		// in the ObjRelationship
		dbRelationshipMap.put(
			new SourceTarget(source.getName(), target.getName(), name),
			dbRelationship);

		source.addRelationship(dbRelationship);
	}

	private void processStartDbRelationshipRef(Attributes atts)
		throws SAXException {
		String source = atts.getValue("", "source");
		if (null == source) {
			throw new SAXException(
				"MapLoaderImpl::processStartDbRelationshipRef(),"
					+ " Unable to parse source. Attributes:\n"
					+ printAttributes(atts).toString());
		}
		String target = atts.getValue("", "target");
		if (null == target) {
			throw new SAXException(
				"MapLoaderImpl::processStartDbRelationshipRef(),"
					+ " Unable to parse target. Attributes:\n"
					+ printAttributes(atts).toString());
		}

		String name = atts.getValue("", "name");

		SourceTarget key = new SourceTarget(source, target, name);
		DbRelationship temp = (DbRelationship) dbRelationshipMap.get(key);
		if (null == temp) {
			throw new SAXException(
				"MapLoaderImpl::processStartDbRelationshipRef()"
					+ ", Unresolved reference from "
					+ source
					+ " to "
					+ target);
		}
		objRelationship.addDbRelationship(temp);
	}

	private void processStartDbAttributePair(Attributes atts)
		throws SAXException {
		String source = atts.getValue("", "source");
		if (null == source) {
			throw new SAXException(
				"MapLoaderImpl::processStartDbAttributePair(),"
					+ " Unable to parse target. Attributes:\n"
					+ printAttributes(atts).toString());
		}
		String target = atts.getValue("", "target");
		if (null == target) {
			throw new SAXException(
				"MapLoaderImpl::processStartDbAttributePair(),"
					+ " Unable to parse source. Attributes:\n"
					+ printAttributes(atts).toString());
		}
		DbAttribute db_source =
			(DbAttribute) dbRelationship.getSourceEntity().getAttribute(source);
		DbAttribute db_target =
			(DbAttribute) dbRelationship.getTargetEntity().getAttribute(target);

		DbAttributePair pair = new DbAttributePair(db_source, db_target);
		dbRelationship.addJoin(pair);
	}

	private void processStartObjRelationship(Attributes atts)
		throws SAXException {
		String temp = atts.getValue("", "source");
		if (null == temp) {
			throw new SAXException(
				"MapLoaderImpl::processStartObjRelationship(),"
					+ " Unable to parse source. Attributes:\n"
					+ printAttributes(atts).toString());
		}
		ObjEntity source = dataMap.getObjEntity(temp);
		if (null == source) {
			throw new SAXException(
				"MapLoaderImpl::processStartObjRelationship(),"
					+ " Unable to find source "
					+ temp);
		}
		temp = atts.getValue("", "target");
		if (null == temp) {
			throw new SAXException(
				"MapLoaderImpl::processStartObjRelationship(),"
					+ " Unable to parse target. Attributes:\n"
					+ printAttributes(atts).toString());
		}
		ObjEntity target = dataMap.getObjEntity(temp);
		if (null == target) {
			throw new SAXException(
				"MapLoaderImpl::processStartObjRelationship(),"
					+ " Unable to find target "
					+ temp);
		}
		temp = atts.getValue("", "toMany");
		boolean to_many = false;
		if (temp != null && temp.equalsIgnoreCase(TRUE))
			to_many = true;
		String name = atts.getValue("", "name");
		if (null == name) {
			throw new SAXException(
				"MapLoaderImpl::processStartObjRelationship(),"
					+ " Unable to parse target. Attributes:\n"
					+ printAttributes(atts).toString());
		}
		objRelationship = new ObjRelationship(source, target, to_many);
		objRelationship.setName(name);
		source.addRelationship(objRelationship);
	}

	private void processEndDbAttribute() throws SAXException {
		attrib = null;
	}

	private void processEndDbEntity() {
		dataMap.addDbEntity(dbEntity);
		dbEntity = null;
	}

	private void processEndObjEntity() {
		dataMap.addObjEntity(objEntity);
		objEntity = null;
	}

	private void processEndDbRelationship() {
		dbRelationship = null;
	}

	private void processEndObjRelationship() {
		objRelationship = null;
	}

	/** Prints the attributes. Used for error reporting purposes.*/
	private StringBuffer printAttributes(Attributes atts) {
		StringBuffer sb = new StringBuffer();
		String name, value;
		for (int i = 0; i < atts.getLength(); i++) {
			value = atts.getQName(i);
			name = atts.getValue(i);
			sb.append("Name: " + name + "\tValue: " + value + "\n");
		}
		return sb;
	}
}

/** Used for creating the key in DbRelationship map */
class SourceTarget {
	public String source;
	public String target;
	public String name;

	public SourceTarget(String temp1, String temp2, String temp3) {
		source = temp1;
		target = temp2;
		name = temp3;
	}

	public int hashCode() {
		int code = +source.hashCode() * 100000 + target.hashCode() * 10000;
		if (null != name)
			code += name.hashCode();
		return code;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof SourceTarget))
			return false;
		SourceTarget other = (SourceTarget) obj;
		if (source.equals(other.source)
			&& target.equals(other.target)
			&& name.equals(other.name)) {
			return true;
		}
		return false;
	}
}