/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group
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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.project.DataMapFile;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.util.PropertyComparator;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Default MapLoader. Its responsibilities include reading DataMaps
 * from XML files and saving DataMap objects back to XML.
 *
 * @author Misha Shengaout
 * @author Andrei Adamchik
 * @author Andriy Shapochka
 */
public class MapLoader extends DefaultHandler {
    private static volatile Logger logObj = Logger.getLogger(MapLoader.class);

    public static final String DATA_MAP_TAG = "data-map";
    public static final String DB_ENTITY_TAG = "db-entity";
    public static final String OBJ_ENTITY_TAG = "obj-entity";
    public static final String DB_ATTRIBUTE_TAG = "db-attribute";
    public static final String DB_ATTRIBUTE_DERIVED_TAG = "db-attribute-derived";
    public static final String DB_ATTRIBUTE_REF_TAG = "db-attribute-ref";
    public static final String OBJ_ATTRIBUTE_TAG = "obj-attribute";
    public static final String OBJ_RELATIONSHIP_TAG = "obj-relationship";
    public static final String DB_RELATIONSHIP_TAG = "db-relationship";
    public static final String DB_RELATIONSHIP_REF_TAG = "db-relationship-ref";
    public static final String DB_ATTRIBUTE_PAIR_TAG = "db-attribute-pair";
    public static final String PROCEDURE_TAG = "procedure";
    public static final String PROCEDURE_PARAMETER_TAG = "procedure-parameter";

    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public static final String DB_KEY_GENERATOR_TAG = "db-key-generator";
    public static final String DB_GENERATOR_TYPE_TAG = "db-generator-type";
    public static final String DB_GENERATOR_NAME_TAG = "db-generator-name";
    public static final String DB_KEY_CACHE_SIZE_TAG = "db-key-cache-size";

    /* Reading from XML */
    private DataMap dataMap;
    private DbEntity dbEntity;
    private ObjEntity objEntity;
    private DbRelationship dbRelationship;
    private ObjRelationship objRelationship;
    private DbAttribute attrib;
    private Map dbRelationshipMap;
    private Procedure procedure;

    private String currentTag;
    private StringBuffer charactersBuffer;

    /* Saving to XML */
    private List objRelationships;
    private List dbRelationships;
    private List dbRelationshipRefs;

    /**
     * Returns <code>true</code> if this relationship's <code>toDependentPk</code>
     * property can be potentially set to <code>true</code>.
     * This means that destination and
     * source attributes are primary keys of their corresponding entities.
     */
    public static boolean isValidForDepPk(DbRelationship rel) {
        Iterator it = rel.getJoins().iterator();
        // handle case with no joins
        if (!it.hasNext()) {
            return false;
        }

        while (it.hasNext()) {
            DbAttributePair join = (DbAttributePair) it.next();
            if (!join.getTarget().isPrimaryKey() || !join.getSource().isPrimaryKey()) {
                return false;
            }
        }

        return true;
    }

    /** Loads the data map from the input source (usually file). */
    public synchronized DataMap loadDataMap(InputSource src) throws DataMapException {
        return loadDataMap(src, Collections.EMPTY_LIST);
    }

    public synchronized DataMap loadDataMap(InputSource src, List deps)
        throws DataMapException {
        try {
            String mapName = mapNameFromLocation(src.getSystemId());
            dataMap = new DataMap(mapName);
            Iterator it = deps.iterator();
            while (it.hasNext()) {
                dataMap.addDependency((DataMap) it.next());
            }

            dbRelationshipMap = new HashMap();
            XMLReader parser = Util.createXmlReader();

            parser.setContentHandler(this);
            parser.setErrorHandler(this);
            parser.parse(src);

        }
        catch (SAXException e) {
            logObj.log(Level.INFO, "Wrapped Exception.", e.getException());
            logObj.log(Level.INFO, "SAX Exception cause.", e.getCause());

            dataMap = null;
            throw new DataMapException("Wrong DataMap format.", e);

        }
        catch (Exception e) {
            logObj.log(Level.INFO, "Exception.", e);
            dataMap = null;
            throw new DataMapException("Error loading DataMap.", e);
        }
        return dataMap;
    }

    /**
     * Helper method to guess the map name from its location.
     */
    protected String mapNameFromLocation(String location) {
        if (location == null) {
            return "Untitled";
        }

        int lastSlash = location.lastIndexOf('/');
        if (lastSlash < 0) {
            lastSlash = location.lastIndexOf('\\');
        }

        if (lastSlash >= 0 && lastSlash + 1 < location.length()) {
            location = location.substring(lastSlash + 1);
        }

        if (location.endsWith(DataMapFile.LOCATION_SUFFIX)) {
            location =
                location.substring(
                    0,
                    location.length() - DataMapFile.LOCATION_SUFFIX.length());
        }

        return location;
    }

    /**
     * Creates, configures and returns ResourceLocator object used
     * to lookup DataMap files.
     */
    protected ResourceLocator configLocator() {
        ResourceLocator locator = new ResourceLocator();
        locator.setSkipAbsolutePath(true);
        locator.setSkipClasspath(false);
        locator.setSkipCurrentDirectory(false);
        locator.setSkipHomeDirectory(false);

        // Configuration superclass statically defines what
        // ClassLoader to use for resources. This
        // allows applications to control where resources
        // are loaded from.
        locator.setClassLoader(Configuration.getResourceLoader());

        return locator;
    }

    /**
     * Loads the array a DataMap for the map file URI.
     * This is a convenience method that would resolve string URI
     * to InputSource and then call <code>loadDataMap</code>.
     *
     * @throws DataMapException if source URI does not resolve to a valid map files
     * @throws NullPointerException if <code>src</code> parameter is null.
     */
    public DataMap loadDataMap(String src) throws DataMapException {
        // configure resource locator
        ResourceLocator locator = configLocator();
        InputStream in = locator.findResourceStream(src);
        if (in == null) {
            throw new DataMapException("Can't find data map " + src);
        }

        try {
            InputSource inSrc = new InputSource(in);
            inSrc.setSystemId(src);
            return loadDataMap(inSrc);
        }
        finally {
            try {
                in.close();
            }
            catch (IOException ioex) {
            }
        }

    }

    public void startElement(
        String namespace_uri,
        String local_name,
        String q_name,
        Attributes atts)
        throws SAXException {

        rememberCurrentTag(local_name);
        if (local_name.equals(DATA_MAP_TAG)) {
        }
        else if (local_name.equals(DB_ENTITY_TAG)) {
            processStartDbEntity(atts);
        }
        else if (local_name.equals(DB_ATTRIBUTE_TAG)) {
            processStartDbAttribute(atts);
        }
        else if (local_name.equals(DB_ATTRIBUTE_DERIVED_TAG)) {
            processStartDerivedDbAttribute(atts);
        }
        else if (local_name.equals(DB_ATTRIBUTE_REF_TAG)) {
            processStartDbAttributeRef(atts);
        }
        else if (local_name.equals(OBJ_ENTITY_TAG)) {
            processStartObjEntity(atts);
        }
        else if (local_name.equals(OBJ_ATTRIBUTE_TAG)) {
            processStartObjAttribute(atts);
        }
        else if (local_name.equals(DB_RELATIONSHIP_TAG)) {
            processStartDbRelationship(atts);
        }
        else if (local_name.equals(DB_ATTRIBUTE_PAIR_TAG)) {
            processStartDbAttributePair(atts);
        }
        else if (local_name.equals(OBJ_RELATIONSHIP_TAG)) {
            processStartObjRelationship(atts);
        }
        else if (local_name.equals(DB_RELATIONSHIP_REF_TAG)) {
            processStartDbRelationshipRef(atts);
        }
        else if (local_name.equals(PROCEDURE_PARAMETER_TAG)) {
            processStartProcedureParameter(atts);
        }
        else if (local_name.equals(PROCEDURE_TAG)) {
            processStartProcedure(atts);
        }
        else if (local_name.equals(DB_KEY_GENERATOR_TAG)) {
            processStartDbKeyGenerator(atts);
        }
        else if (local_name.equals(DB_GENERATOR_TYPE_TAG)) {
            charactersBuffer = new StringBuffer();
        }
        else if (local_name.equals(DB_GENERATOR_NAME_TAG)) {
            charactersBuffer = new StringBuffer();
        }
        else if (local_name.equals(DB_KEY_CACHE_SIZE_TAG)) {
            charactersBuffer = new StringBuffer();
        }
    }

    public void endElement(String namespaceURI, String local_name, String qName)
        throws SAXException {
        if (local_name.equals(DATA_MAP_TAG)) {
        }
        else if (local_name.equals(DB_ENTITY_TAG)) {
            processEndDbEntity();
        }
        else if (local_name.equals(OBJ_ENTITY_TAG)) {
            processEndObjEntity();
        }
        else if (local_name.equals(DB_ATTRIBUTE_TAG)) {
            processEndDbAttribute();
        }
        else if (local_name.equals(DB_ATTRIBUTE_DERIVED_TAG)) {
            processEndDbAttribute();
        }
        else if (local_name.equals(DB_RELATIONSHIP_TAG)) {
            processEndDbRelationship();
        }
        else if (local_name.equals(OBJ_RELATIONSHIP_TAG)) {
            processEndObjRelationship();
        }
        else if (local_name.equals(DB_KEY_GENERATOR_TAG)) {
        }
        else if (local_name.equals(DB_GENERATOR_TYPE_TAG)) {
            processEndDbGeneratorType();
        }
        else if (local_name.equals(DB_GENERATOR_NAME_TAG)) {
            processEndDbGeneratorName();
        }
        else if (local_name.equals(DB_KEY_CACHE_SIZE_TAG)) {
            processEndDbKeyCacheSize();
        }
        else if (local_name.equals(PROCEDURE_PARAMETER_TAG)) {
            processEndProcedureParameter();
        }
        else if (local_name.equals(PROCEDURE_TAG)) {
            processEndProcedure();
        }

        resetCurrentTag();
        charactersBuffer = null;
    }

    public void warning(SAXParseException e) throws SAXException {
        logObj.warn(
            "**Parsing warning**\n"
                + "Line:"
                + e.getLineNumber()
                + "\nMessage:"
                + e.getMessage());
        throw new SAXException("Warning!");
    }

    public void error(SAXParseException e) throws SAXException {
        logObj.error(
            "**Parsing error**\n"
                + "Line:"
                + e.getLineNumber()
                + "\nMessage:"
                + e.getMessage());
        throw new SAXException("Warning!");
    }

    public void fatalError(SAXParseException e) throws SAXException {
        logObj.fatal(
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
        out.println(
            "<data-map project-version=\"" + Project.CURRENT_PROJECT_VERSION + "\">");
        storeProcedures(out, map);
        storeDbEntities(out, map);
        storeObjEntities(out, map);
        storeDbRelationships(out);
        storeObjRelationships(out);
        out.println("</data-map>");
        objRelationships = null;
        dbRelationships = null;
        dbRelationshipRefs = null;
    }

    private void storeProcedures(PrintWriter out, DataMap map) {

        Iterator iter = sortedProcedures(map).iterator();
        while (iter.hasNext()) {
            Procedure procedure = (Procedure) iter.next();
            out.print("\t<procedure name=\"" + procedure.getName() + '\"');
            if (procedure.getSchema() != null
                && procedure.getSchema().trim().length() > 0) {
                out.print(" schema=\"");
                out.print(procedure.getSchema());
                out.print('\"');
            }

            if (procedure.getCatalog() != null
                && procedure.getCatalog().trim().length() > 0) {
                out.print(" catalog=\"");
                out.print(procedure.getCatalog());
                out.print('\"');
            }

            if (procedure.isReturningValue()) {
                out.print(" returningValue=\"true\"");
            }

            out.println('>');

            storeProcedureParameters(out, procedure);
            out.println("\t</procedure>");
        }
    }

    private void storeProcedureParameters(PrintWriter out, Procedure procedure) {

        Iterator iter = procedure.getCallParameters().iterator();

        while (iter.hasNext()) {
            ProcedureParameter parameter = (ProcedureParameter) iter.next();
            out.print("\t\t<procedure-parameter name=\"" + parameter.getName() + '\"');

            String type = TypesMapping.getSqlNameByType(parameter.getType());
            if (type != null) {
                out.print(" type=\"" + type + '\"');
            }

            if (parameter.getMaxLength() > 0) {
                out.print(" length=\"");
                out.print(parameter.getMaxLength());
                out.print('\"');
            }

            if (parameter.getPrecision() > 0) {
                out.print(" precision=\"");
                out.print(parameter.getPrecision());
                out.print('\"');
            }

            int direction = parameter.getDirection();
            if (direction == ProcedureParameter.IN_PARAMETER) {
                out.print(" direction=\"in\"");
            }
            else if (direction == ProcedureParameter.IN_OUT_PARAMETER) {
                out.print(" direction=\"in_out\"");
            }
            else if (direction == ProcedureParameter.OUT_PARAMETER) {
                out.print(" direction=\"out\"");
            }

            out.println("/>");
        }
    }

    private void storeDbEntities(PrintWriter out, DataMap map) {

        Iterator iter = sortedRegularDbEntities(map).iterator();
        while (iter.hasNext()) {
            DbEntity dbe = (DbEntity) iter.next();
            out.print("\t<db-entity name=\"" + dbe.getName() + '\"');
            if (dbe.getSchema() != null && dbe.getSchema().trim().length() > 0) {
                out.print(" schema=\"");
                out.print(dbe.getSchema());
                out.print('\"');
            }
            if (dbe.getCatalog() != null && dbe.getCatalog().trim().length() > 0) {
                out.print(" catalog=\"");
                out.print(dbe.getCatalog());
                out.print('\"');
            }

            out.println('>');

            storeDbAttribute(out, dbe);
            storeDbKeyGenerator(out, dbe.getPrimaryKeyGenerator());
            out.println("\t</db-entity>");
            dbRelationships.addAll(dbe.getRelationships());
        }

        Iterator diter = sortedDerivedDbEntities(map).iterator();
        while (diter.hasNext()) {
            DerivedDbEntity dbe = (DerivedDbEntity) diter.next();
            out.print("\t<db-entity name=\"" + dbe.getName() + '\"');
            if (dbe.getSchema() != null && dbe.getSchema().trim().length() > 0) {
                out.print(" schema=\"");
                out.print(dbe.getSchema());
                out.print('\"');
            }
            if (dbe.getCatalog() != null && dbe.getCatalog().trim().length() > 0) {
                out.print(" catalog=\"");
                out.print(dbe.getCatalog());
                out.print('\"');
            }

            DbEntity parent = dbe.getParentEntity();
            String name = (parent != null) ? parent.getName() : "";
            out.print(" parentName=\"");
            out.print(name);
            out.print('\"');

            out.println('>');

            storeDbAttribute(out, dbe);
            out.println("\t</db-entity>");
            dbRelationships.addAll(dbe.getRelationships());
        }
    }

    private void storeDbAttribute(PrintWriter out, DbEntity dbe) {
        Iterator iter = this.sortedAttributes(dbe).iterator();

        while (iter.hasNext()) {
            DbAttribute attr = (DbAttribute) iter.next();
            if (attr instanceof DerivedDbAttribute) {
                storeDerivedDbAttribute(out, (DerivedDbAttribute) attr);
            }
            else {
                storeRegularDbAttribute(out, attr);
            }
        }

    }

    private void storeDerivedDbAttribute(PrintWriter out, DerivedDbAttribute attr) {
        out.print("\t\t<db-attribute-derived name=\"" + attr.getName() + '\"');

        String type = TypesMapping.getSqlNameByType(attr.getType());
        if (type != null) {
            out.print(" type=\"" + type + '\"');
        }

        // If attribute is part of primary key
        if (attr.isPrimaryKey()) {
            out.print(" isPrimaryKey=\"true\"");
        }

        if (attr.isMandatory())
            out.print(" isMandatory=\"true\"");

        if (attr.getMaxLength() > 0) {
            out.print(" length=\"");
            out.print(attr.getMaxLength());
            out.print('\"');
        }

        if (attr.getPrecision() > 0) {
            out.print(" precision=\"");
            out.print(attr.getPrecision());
            out.print('\"');
        }

        if (((DerivedDbEntity) attr.getEntity()).getGroupByAttributes().contains(attr)) {
            out.print(" isGroupBy=\"true\"");
        }

        String spec = attr.getExpressionSpec();
        if (spec != null && spec.trim().length() > 0) {
            out.print(" spec=\"");
            out.print(spec);
            out.print('\"');
        }

        List params = attr.getParams();

        if (params.size() > 0) {
            out.println(">");

            Iterator refs = params.iterator();
            while (refs.hasNext()) {
                DbAttribute ref = (DbAttribute) refs.next();
                out.println("\t\t\t<db-attribute-ref name=\"" + ref.getName() + "\"/>");
            }
            out.println("\t\t</db-attribute-derived>");
        }
        else {
            out.println("/>");
        }
    }

    private void storeRegularDbAttribute(PrintWriter out, DbAttribute attr) {
        out.print("\t\t<db-attribute name=\"" + attr.getName() + '\"');

        String type = TypesMapping.getSqlNameByType(attr.getType());
        if (type != null) {
            out.print(" type=\"" + type + '\"');
        }

        // If attribute is part of primary key
        if (attr.isPrimaryKey()) {
            out.print(" isPrimaryKey=\"true\"");
        }

        if (attr.isMandatory()) {
            out.print(" isMandatory=\"true\"");
        }

        if (attr.getMaxLength() > 0) {
            out.print(" length=\"");
            out.print(attr.getMaxLength());
            out.print('\"');
        }

        if (attr.getPrecision() > 0) {
            out.print(" precision=\"");
            out.print(attr.getPrecision());
            out.print('\"');
        }

        out.println("/>");
    }

    private void storeDbKeyGenerator(PrintWriter out, DbKeyGenerator pkGenerator) {
        if (pkGenerator == null)
            return;
        String type = pkGenerator.getGeneratorType();
        if (type == null)
            return;
        String name = pkGenerator.getGeneratorName();
        Integer cacheSize = pkGenerator.getKeyCacheSize();
        out.println("\t\t<" + DB_KEY_GENERATOR_TAG + '>');
        out.print("\t\t\t<" + DB_GENERATOR_TYPE_TAG + '>');
        out.print(type);
        out.println("</" + DB_GENERATOR_TYPE_TAG + '>');
        if (name != null) {
            out.print("\t\t\t<" + DB_GENERATOR_NAME_TAG + '>');
            out.print(name);
            out.println("</" + DB_GENERATOR_NAME_TAG + '>');
        }
        if (cacheSize != null) {
            out.print("\t\t\t<" + DB_KEY_CACHE_SIZE_TAG + '>');
            out.print(cacheSize);
            out.println("</" + DB_KEY_CACHE_SIZE_TAG + '>');
        }
        out.println("\t\t</" + DB_KEY_GENERATOR_TAG + '>');
    }

    private void storeObjEntities(PrintWriter out, DataMap map) {
        Iterator iter = this.sortedObjEntities(map).iterator();
        while (iter.hasNext()) {
            ObjEntity temp = (ObjEntity) iter.next();
            out.print("\t<obj-entity name=\"");
            out.print(temp.getName());

            if (temp.getClassName() != null) {
                out.print("\" className=\"");
                out.print(temp.getClassName());
            }

            if (temp.isReadOnly()) {
                out.print("\" readOnly=\"true");
            }

            out.print('\"');

            if (temp.getDbEntity() != null) {
                out.print(" dbEntityName=\"");
                out.print(temp.getDbEntity().getName());
                out.print('\"');
            }

            if (temp.getSuperClassName() != null) {
                out.print(" superClassName=\"");
                out.print(temp.getSuperClassName());
                out.print("\"");
            }

            out.println('>');
            storeObjAttribute(out, temp);

            out.println("\t</obj-entity>");
            Collection objRels = temp.getRelationshipMap().values();
            objRelationships.addAll(objRels);

            Iterator relIt = objRels.iterator();
            while (relIt.hasNext()) {
                ObjRelationship objRel = (ObjRelationship) relIt.next();
                dbRelationshipRefs.addAll(objRel.getDbRelationships());
            }
        }
    }

    private void storeObjAttribute(PrintWriter out, ObjEntity obj_entity) {
        Iterator iter = this.sortedAttributes(obj_entity).iterator();
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
                out.print(" db-attribute-path=\"");
                out.print(temp.getDbAttributePath());
                out.print('\"');
            }
            out.println("/>");
        }
    }

    private void storeObjRelationships(PrintWriter out) throws DataMapException {
        Iterator iter = this.sortedRelationships(objRelationships).iterator();
        while (iter.hasNext()) {
            ObjRelationship rel = (ObjRelationship) iter.next();
            ObjEntity srcEnt = (ObjEntity) rel.getSourceEntity();
            if (srcEnt == null) {
                logObj.warn(
                    "No source entity, ignoring ObjRelationship " + rel.getName());
                return;
            }

            ObjEntity targetEnt = (ObjEntity) rel.getTargetEntity();
            if (targetEnt == null) {
                logObj.warn(
                    "No target entity, ignoring ObjRelationship " + rel.getName());
                return;
            }

            out.print("\t<obj-relationship name=\"" + rel.getName() + '\"');
            out.print(" source=\"" + srcEnt.getName() + '\"');
            out.print(" target=\"" + targetEnt.getName() + '\"');
            out.print(" toMany=\"" + (rel.isToMany() ? TRUE : FALSE) + '\"');

            String deleteRule = DeleteRule.deleteRuleName(rel.getDeleteRule());
            if (rel.getDeleteRule() != DeleteRule.NO_ACTION && deleteRule != null) {
                out.print(" deleteRule=\"" + deleteRule + '\"');
            }
            out.println('>');
            this.storeDbRelationshipRef(out, rel);
            out.println("\t</obj-relationship>");
        }
    }

    private void storeDbRelationshipRef(PrintWriter out, ObjRelationship objRelationship)
        throws DataMapException {

        // do the first empty run, to see that the chain of
        // relationships is valid
        boolean validChain = true;
        Iterator dryRun = objRelationship.getDbRelationships().iterator();
        while (dryRun.hasNext()) {
            DbRelationship relationship = (DbRelationship) dryRun.next();
            if (!dbRelationships.contains(relationship)) {
                validChain = false;
                break;
            }
        }

        if (!validChain) {
            return;
        }

        Iterator iter = objRelationship.getDbRelationships().iterator();
        while (iter.hasNext()) {
            DbRelationship rel = (DbRelationship) iter.next();

            out.print("\t\t<");
            out.print(DB_RELATIONSHIP_REF_TAG);
            out.print(" source=\"");
            out.print(rel.getSourceEntity().getName());
            out.print("\" target=\"");
            out.print(rel.getTargetEntityName());
            out.print("\" name=\"");
            out.print(rel.getName());
            out.println("\"/>");
        } 
    }

    private void storeDbRelationships(PrintWriter out) throws DataMapException {
        Iterator iter = this.sortedRelationships(dbRelationships).iterator();
        while (iter.hasNext()) {
            DbRelationship temp = (DbRelationship) iter.next();
            out.print("\t<");
            out.print(DB_RELATIONSHIP_TAG);
            out.print(" name=\"");
            out.print(temp.getName());
            out.print("\" source=\"");
            out.print(temp.getSourceEntity().getName());
            out.print("\" target=\"");
            out.print(temp.getTargetEntityName());
            out.print("\" toDependentPK=\"");
            out.print(temp.isToDependentPK() ? TRUE : FALSE);
            out.print("\" toMany=\"");
            out.print(temp.isToMany() ? TRUE : FALSE);
            out.println("\">");
            this.storeDbAttributePair(out, temp);
            out.print("\t</");
            out.print(DB_RELATIONSHIP_TAG);
            out.println('>');
        }
    }

    private void storeDbAttributePair(PrintWriter out, DbRelationship dbRel)
        throws DataMapException {
        Iterator iter = dbRel.getJoins().iterator();
        while (iter.hasNext()) {
            DbAttributePair pair = (DbAttributePair) iter.next();
            out.print("\t\t<");
            out.print(DB_ATTRIBUTE_PAIR_TAG);

            // sanity check
            if (pair.getSource() != null) {
                out.print(" source=\"");
                out.print(pair.getSource().getName());
                out.print("\"");
            }

            if (pair.getTarget() != null) {
                out.print(" target=\"");
                out.print(pair.getTarget().getName());
                out.print("\"");
            }

            out.println("/>");
        }
    }

    private void processStartDbEntity(Attributes atts) throws SAXException {
        String name = atts.getValue("", "name");
        String parentName = atts.getValue("", "parentName");

        if (parentName != null) {
            dbEntity = new DerivedDbEntity(name);
            ((DerivedDbEntity) dbEntity).setParentEntityName(parentName);
        }
        else {
            dbEntity = new DbEntity(name);
        }

        if (!(dbEntity instanceof DerivedDbEntity)) {
            dbEntity.setSchema(atts.getValue("", "schema"));
            dbEntity.setCatalog(atts.getValue("", "catalog"));
        }

        dataMap.addDbEntity(dbEntity);
    }

    private void processStartDbAttributeRef(Attributes atts) throws SAXException {
        String name = atts.getValue("", "name");
        if ((attrib instanceof DerivedDbAttribute)
            && (dbEntity instanceof DerivedDbEntity)) {
            DbEntity parent = ((DerivedDbEntity) dbEntity).getParentEntity();
            DbAttribute ref = (DbAttribute) parent.getAttribute(name);
            ((DerivedDbAttribute) attrib).addParam(ref);
        }
        else {
            throw new SAXException(
                "Referenced attributes are not supported by regular DbAttributes. "
                    + " Offending attribute name '"
                    + attrib.getName()
                    + "'.");
        }
    }

    private void processStartDbAttribute(Attributes atts) throws SAXException {
        String name = atts.getValue("", "name");
        String type = atts.getValue("", "type");

        attrib = new DbAttribute(name);
        attrib.setType(TypesMapping.getSqlTypeByName(type));
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
    }

    private void processStartDerivedDbAttribute(Attributes atts) throws SAXException {
        String name = atts.getValue("", "name");
        String type = atts.getValue("", "type");
        String spec = atts.getValue("", "spec");

        attrib = new DerivedDbAttribute(name);
        attrib.setType(TypesMapping.getSqlTypeByName(type));
        ((DerivedDbAttribute) attrib).setExpressionSpec(spec);
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
            ((DerivedDbAttribute) attrib).setGroupBy(true);
        }
    }

    private void processStartDbKeyGenerator(Attributes atts) throws SAXException {
        DbKeyGenerator pkGenerator = new DbKeyGenerator();
        dbEntity.setPrimaryKeyGenerator(pkGenerator);
    }

    private void processStartObjEntity(Attributes atts) {
        objEntity = new ObjEntity(atts.getValue("", "name"));
        objEntity.setClassName(atts.getValue("", "className"));

        String readOnly = atts.getValue("", "readOnly");
        objEntity.setReadOnly(TRUE.equalsIgnoreCase(readOnly));

        String temp = atts.getValue("", "dbEntityName");
        if (null != temp) {
            DbEntity db_temp = dataMap.getDbEntity(temp);
            objEntity.setDbEntity(db_temp);
        }

        temp = atts.getValue("", "superClassName");
        if (null != temp) {
            objEntity.setSuperClassName(temp);
        }
        dataMap.addObjEntity(objEntity);
    }

    private void processStartObjAttribute(Attributes atts) {
        String name = atts.getValue("", "name");
        String type = atts.getValue("", "type");

        ObjAttribute oa = new ObjAttribute(name);
        oa.setType(type);
        objEntity.addAttribute(oa);
        String dbPath = atts.getValue("", "db-attribute-path");
        if (dbPath == null) {
            dbPath = atts.getValue("", "db-attribute-name");
        }
        oa.setDbAttributePath(dbPath);
    }

    private void processStartDbRelationship(Attributes atts) throws SAXException {
        String temp = atts.getValue("", "source");
        if (null == temp) {
            throw new SAXException(
                "MapLoaderImpl::processStartDbRelationship(),"
                    + " Unable to parse source. Attributes:\n"
                    + printAttributes(atts).toString());
        }

        DbEntity source = dataMap.getDbEntity(temp);
        if (null == source) {
            logObj.debug(
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

        DbEntity target = dataMap.getDbEntity(temp, true);
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

    private void processStartDbRelationshipRef(Attributes atts) throws SAXException {
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

    private void processStartDbAttributePair(Attributes atts) throws SAXException {
        String source = atts.getValue("", "source");
        String target = atts.getValue("", "target");
        DbAttribute dbSrc = null;
        DbAttribute dbTarget = null;

        if (source != null) {
            dbSrc = (DbAttribute) dbRelationship.getSourceEntity().getAttribute(source);
        }

        if (target != null) {
            dbTarget =
                (DbAttribute) dbRelationship.getTargetEntity().getAttribute(target);
        }

        DbAttributePair pair = new DbAttributePair(dbSrc, dbTarget);
        dbRelationship.addJoin(pair);
    }

    private void processStartObjRelationship(Attributes atts) throws SAXException {
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
        ObjEntity target = dataMap.getObjEntity(temp, true);
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

        int deleteRule = DeleteRule.NO_ACTION;
        String deleteRuleName = atts.getValue("", "deleteRule");
        if (null != deleteRuleName) {
            deleteRule = DeleteRule.deleteRuleForName(deleteRuleName);
        }

        objRelationship = new ObjRelationship(source, target, to_many);
        objRelationship.setName(name);
        objRelationship.setDeleteRule(deleteRule);
        source.addRelationship(objRelationship);
    }

    private void processStartProcedure(Attributes attributes) throws SAXException {

        String name = attributes.getValue("", "name");
        if (null == name) {
            throw new SAXException(
                "MapLoaderImpl::processStartProcedure()," + " no procedure name.");
        }

        String schema = attributes.getValue("", "schema");
        String catalog = attributes.getValue("", "catalog");
        String returningValue = attributes.getValue("", "returningValue");

        procedure = new Procedure(name);
        procedure.setReturningValue(
            returningValue != null && returningValue.equalsIgnoreCase(TRUE));
        procedure.setSchema(schema);
        procedure.setCatalog(catalog);
        dataMap.addProcedure(procedure);
    }

    private void processStartProcedureParameter(Attributes attributes)
        throws SAXException {

        String name = attributes.getValue("", "name");
        if (name == null) {
            throw new SAXException(
                "MapLoaderImpl::processStartProcedureParameter(),"
                    + " no procedure parameter name.");
        }

        ProcedureParameter parameter = new ProcedureParameter(name);

        String type = attributes.getValue("", "type");
        if (type != null) {
            parameter.setType(TypesMapping.getSqlTypeByName(type));
        }

        String length = attributes.getValue("", "length");
        if (length != null) {
            parameter.setMaxLength(Integer.parseInt(length));
        }

        String precision = attributes.getValue("", "precision");
        if (precision != null) {
            attrib.setPrecision(Integer.parseInt(precision));
        }

        String direction = attributes.getValue("", "direction");
        if ("in".equals(direction)) {
            parameter.setDirection(ProcedureParameter.IN_PARAMETER);
        }
        else if ("out".equals(direction)) {
            parameter.setDirection(ProcedureParameter.OUT_PARAMETER);
        }
        else if ("in_out".equals(direction)) {
            parameter.setDirection(ProcedureParameter.IN_OUT_PARAMETER);
        }

        procedure.addCallParameter(parameter);
    }

    private void processEndDbAttribute() throws SAXException {
        attrib = null;
    }

    private void processEndDbEntity() {
        dbEntity = null;
    }

    private void processEndProcedure() {
        procedure = null;
    }

    private void processEndProcedureParameter() {
    }

    private void processEndDbGeneratorType() {
        if (dbEntity == null)
            return;
        DbKeyGenerator pkGenerator = dbEntity.getPrimaryKeyGenerator();
        if (pkGenerator == null)
            return;
        pkGenerator.setGeneratorType(charactersBuffer.toString());
        if (pkGenerator.getGeneratorType() == null) {
            dbEntity.setPrimaryKeyGenerator(null);
        }
    }

    private void processEndDbGeneratorName() {
        if (dbEntity == null)
            return;
        DbKeyGenerator pkGenerator = dbEntity.getPrimaryKeyGenerator();
        if (pkGenerator == null)
            return;
        pkGenerator.setGeneratorName(charactersBuffer.toString());
    }

    private void processEndDbKeyCacheSize() {
        if (dbEntity == null)
            return;
        DbKeyGenerator pkGenerator = dbEntity.getPrimaryKeyGenerator();
        if (pkGenerator == null)
            return;
        try {
            pkGenerator.setKeyCacheSize(new Integer(charactersBuffer.toString().trim()));
        }
        catch (Exception ex) {
            pkGenerator.setKeyCacheSize(null);
        }
    }

    private void processEndObjEntity() {
        objEntity = null;
    }

    private void processEndDbRelationship() {
        // validation check: if "toDepPK" is set and source target is NOT a PK, unset it
        if (dbRelationship.isToDependentPK() && !isValidForDepPk(dbRelationship)) {
            logObj.warn(
                "Relationship '"
                    + dbRelationship.getName()
                    + "': 'toDependentPK' is incorrectly set to true, unsetting...");
            dbRelationship.setToDependentPK(false);
        }

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

    protected List sortedProcedures(DataMap map) {
        List list = new ArrayList(map.getProcedures());
        Collections.sort(list, new PropertyComparator("name", ObjEntity.class));
        return list;
    }

    protected List sortedRegularDbEntities(DataMap map) {
        Iterator it = map.getDbEntities().iterator();
        List derived = new ArrayList();
        while (it.hasNext()) {
            Object ent = it.next();
            if (!(ent instanceof DerivedDbEntity)) {
                derived.add(ent);
            }
        }
        if (derived.size() > 1) {
            Collections.sort(derived, new PropertyComparator("name", DbEntity.class));
        }
        return derived;
    }

    protected List sortedDerivedDbEntities(DataMap map) {
        Iterator it = map.getDbEntities().iterator();
        List derived = new ArrayList();
        while (it.hasNext()) {
            Object ent = it.next();
            if (ent instanceof DerivedDbEntity) {
                derived.add(ent);
            }
        }
        if (derived.size() > 1) {
            Collections.sort(derived, new PropertyComparator("name", DbEntity.class));
        }
        return derived;
    }

    protected List sortedObjEntities(DataMap map) {
        List list = new ArrayList(map.getObjEntities());
        Collections.sort(list, new PropertyComparator("name", ObjEntity.class));
        return list;
    }

    protected List sortedAttributes(Entity ent) {
        List list = new ArrayList(ent.getAttributes());
        Collections.sort(list, new PropertyComparator("name", Attribute.class));
        return list;
    }

    protected List sortedRelationships(Entity ent) {
        List list = new ArrayList(ent.getRelationships());
        Collections.sort(list, new PropertyComparator("name", Relationship.class));
        return list;
    }

    protected List sortedRelationships(List rels) {
        List list = new ArrayList(rels);
        Collections.sort(list, new PropertyComparator("name", Relationship.class));
        return list;
    }

    public void characters(char[] text, int start, int length)
        throws org.xml.sax.SAXException {
        if (charactersBuffer != null) {
            charactersBuffer.append(text, start, length);
        }
    }

    private void rememberCurrentTag(String tag) {
        currentTag = tag;
    }

    private void resetCurrentTag() {
        currentTag = null;
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
            int code = source.hashCode() * 100000 + target.hashCode() * 10000;
            if (null != name) {
                code += name.hashCode();
            }
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
}
