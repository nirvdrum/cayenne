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

package org.objectstyle.cayenne.access;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.ashwood.dbutil.Table;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbAttributePair;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.project.NamedObjectFactory;
import org.objectstyle.cayenne.util.NameConverter;

/** Utility class that does reverse engineering of the database. 
  * It can create DataMaps using database meta data obtained via JDBC driver.
  *
  * @author Michael Shengaout
  * @author Andrei Adamchik
 */
public class DbLoader {
    private static Logger logObj = Logger.getLogger(DbLoader.class);

    public static final String WILDCARD = "%";

    /** List of db entities to process.*/
    private List dbEntityList = new ArrayList();

    /** Creates default name for loaded relationship */
    private static String defaultDbRelName(String dstName, boolean toMany) {
        String uglyName = (toMany) ? dstName + "_ARRAY" : "to_" + dstName;
        return NameConverter.undescoredToJava(uglyName, false);
    }

    /** Creates a unique name for loaded relationship on the given entity.  Uses defaultDbRelName*/
    private static String uniqueDbRelName(
        DbEntity entity,
        String dstName,
        boolean toMany) {
        int currentSuffix = 1;
        String baseRelName = defaultDbRelName(dstName, true);
        String relName = baseRelName;

        while (entity.getRelationship(relName) != null) {
            relName = baseRelName + currentSuffix;
            currentSuffix++;
        }
        return relName;
    }

    protected Connection con;
    protected DbAdapter adapter;
    protected DatabaseMetaData metaData;
    protected DbLoaderDelegate delegate;

    /** Creates new DbLoader. */
    public DbLoader(
        Connection con,
        DbAdapter adapter,
        DbLoaderDelegate delegate) {
        this.adapter = adapter;
        this.con = con;
        this.delegate = delegate;
    }

    /** Returns DatabaseMetaData object associated with this DbLoader. */
    public DatabaseMetaData getMetaData() throws SQLException {
        if (null == metaData)
            metaData = con.getMetaData();
        return metaData;
    }

    /** 
     * Returns database connection used by this DbLoader.
     */
    public Connection getCon() {
        return con;
    }

    /** 
     * Retrieves catalogues for the database associated with this DbLoader.
     *
     * @return List with the catalog names, empty Array if none found.
     */
    public List getCatalogs() throws SQLException {
        List catalogs = new ArrayList();
        ResultSet rs = getMetaData().getCatalogs();

        try {
            while (rs.next()) {
                String catalog_name = rs.getString(1);
                catalogs.add(catalog_name);
            }
        } finally {
            rs.close();
        }
        return catalogs;
    }

    /** 
     * Retrieves the schemas for the database.
     * 
     * @return List with the schema names, empty Array if none found.
     */
    public List getSchemas() throws SQLException {
        List schemas = new ArrayList();
        ResultSet rs = getMetaData().getSchemas();

        try {
            while (rs.next()) {
                String schema_name = rs.getString(1);
                schemas.add(schema_name);
            }
        } finally {
            rs.close();
        }
        return schemas;
    }

    /** 
     * Returns all the table types for the given database.
     * Types may be such as "TABLE", "VIEW", "SYSTEM TABLE", etc.
     * 
     * @return List of Strings, empty array if nothing found. 
     */
    public List getTableTypes() throws SQLException {
        List types = new ArrayList();
        ResultSet rs = getMetaData().getTableTypes();

        try {
            while (rs.next()) {
                types.add(rs.getString("TABLE_TYPE").trim());
            }
        } finally {
            rs.close();
        }
        return types;
    }

    /** 
     * Returns all table names for given combination of the criteria.
     * 
     * @param catalog The name of the catalog, may be null.
     * @param schemaPattern The pattern for schema name, use "%" for wildcard.
     * @param tableNamePattern The pattern for table names, % for wildcard,
     * if null or "" defaults to "%".
     * @param types The types of table names to retrieve, null returns all types.
     * 
     * @return List of TableInfo objects, empty array if nothing found.
     */
    public List getTables(
        String catalog,
        String schemaPattern,
        String tableNamePattern,
        String[] types)
        throws SQLException {

        List tables = new ArrayList();
        if (null == schemaPattern || schemaPattern.equals("")) {
            schemaPattern = WILDCARD;
        } else if (schemaPattern.indexOf('*') >= 0) {
            schemaPattern.replace('*', '%');
        }

        if (null == tableNamePattern || tableNamePattern.equals("")) {
            tableNamePattern = WILDCARD;
        } else if (tableNamePattern.indexOf('*') >= 0) {
            tableNamePattern.replace('*', '%');
        }

        if (logObj.isDebugEnabled()) {
            logObj.debug(
                "Read tables: catalog="
                    + catalog
                    + ", schema="
                    + schemaPattern
                    + ", tableNames="
                    + tableNamePattern);

            if (types != null && types.length > 0) {
                for (int i = 0; i < types.length; i++) {
                    logObj.debug("Read tables: table type=" + types[i]);
                }
            }
        }

        ResultSet rs =
            getMetaData().getTables(
                catalog,
                schemaPattern,
                tableNamePattern,
                types);

        try {
            while (rs.next()) {
                String cat = rs.getString("TABLE_CAT");
                String schema = rs.getString("TABLE_SCHEM");
                String name = rs.getString("TABLE_NAME");
                Table info = new Table(cat, schema, name);
                tables.add(info);
            }
        } finally {
            rs.close();
        }
        return tables;
    }

    /** 
     * Loads dbEntities for the specified tables.
     * 
     * @param map DataMap to be populated with DbEntities.
     * 
     * @param tables The list of org.objectstyle.ashwood.dbutil.Table objects
     * for which DbEntities must be created.
     * 
     * @return true if need to continue, false if must stop loading. 
     */
    public boolean loadDbEntities(DataMap map, List tables)
        throws SQLException {
        boolean ret_code = true;
        dbEntityList = new ArrayList();
        Iterator iter = tables.iterator();
        while (iter.hasNext()) {
            Table table = (Table) iter.next();

            // Check if there already is db entity under such name
            // if so, consult the delegate what to do
            DbEntity oldEnt = map.getDbEntity(table.getName());
            if (oldEnt != null) {
                if (delegate == null) {
                    // no delegate, don't know what to do, cancel import
                    return false;
                }

                try {
                    if (delegate.overwriteDbEntity(oldEnt)) {
                        logObj.info("Overwrite: " + oldEnt.getName());
                        map.deleteDbEntity(oldEnt.getName());
                        delegate.dbEntityRemoved(oldEnt);
                    } else {
                        logObj.info("Keep old: " + oldEnt.getName());
                        continue;
                    }
                } catch (CayenneException ex) {
                    logObj.info("Load canceled.");

                    // cancel immediately
                    return false;
                }
            }

            DbEntity dbEntity = new DbEntity();
            dbEntityList.add(dbEntity);
            dbEntity.setName(table.getName());

            if (delegate != null) {
                delegate.setSchema(dbEntity, table.getSchema());
            } else {
                dbEntity.setSchema(table.getSchema());
            }
            dbEntity.setCatalog(table.getCatalog());

            // --  Create DbAttributes from column information  --
            ResultSet rs =
                getMetaData().getColumns(
                    table.getCatalog(),
                    table.getSchema(),
                    table.getName(),
                    "%");

            try {
                while (rs.next()) {
                    // gets attribute's (column's) information
                    String columnName = rs.getString("COLUMN_NAME");
                    boolean allowNulls = rs.getBoolean("NULLABLE");
                    int columnType = rs.getInt("DATA_TYPE");
                    int columnSize = rs.getInt("COLUMN_SIZE");

                    // ignore precision of non-decimal columns
                    int decimalDigits = -1;
                    if (TypesMapping.isDecimal(columnType)) {
                        decimalDigits = rs.getInt("DECIMAL_DIGITS");
                        if (rs.wasNull()) {
                            decimalDigits = -1;
                        }
                    }

                    // create attribute delegating this task to adapter
                    DbAttribute attr =
                        adapter.buildAttribute(
                            columnName,
                            columnType,
                            columnSize,
                            decimalDigits,
                            allowNulls);
                    attr.setEntity(dbEntity);
                    dbEntity.addAttribute(attr);
                }
            } finally {
                rs.close();
            }

            map.addDbEntity(dbEntity);

            // notify delegate
            if (delegate != null) {
                delegate.dbEntityAdded(dbEntity);
            }
        }

        // get primary keys for each table and store it in dbEntity
        Iterator i = map.getDbEntitiesAsList().iterator();
        while (i.hasNext()) {
            DbEntity dbEntity = (DbEntity) i.next();
            String tableName = dbEntity.getName();
            ResultSet rs =
                metaData.getPrimaryKeys(null, dbEntity.getSchema(), tableName);

            try {
                while (rs.next()) {
                    String keyName = rs.getString(4);
                    (
                        (DbAttribute) dbEntity.getAttribute(
                            keyName)).setPrimaryKey(
                        true);
                }
            } finally {
                rs.close();
            }
        }
        return ret_code;
    }

    /** 
     * Creates ObjEntities from DbEntities. Uses NameConverter class to
     * change database table and attribute names into whatever
     * user wants them to be, e.g. from EMPLOYEE_NAME to employeeName. 
     */
    public void loadObjEntities(DataMap map) {
        Iterator it = dbEntityList.iterator();
        while (it.hasNext()) {
            DbEntity dbEntity = (DbEntity) it.next();
            ObjEntity objEntity =
                new ObjEntity(
                    NameConverter.undescoredToJava(dbEntity.getName(), true));
            objEntity.setDbEntity(dbEntity);
            objEntity.setClassName(objEntity.getName());
            map.addObjEntity(objEntity);

            Iterator colIt = dbEntity.getAttributeMap().values().iterator();
            while (colIt.hasNext()) {
                DbAttribute dbAtt = (DbAttribute) colIt.next();
                if (dbAtt.isPrimaryKey())
                    continue;

                String attName =
                    NameConverter.undescoredToJava(dbAtt.getName(), false);
                String type = TypesMapping.getJavaBySqlType(dbAtt.getType());

                if (logObj.isDebugEnabled()) {
                    if (type == null || type.trim().length() == 0) {
                        logObj.debug(
                            "DbLoader::loadObjEntities(), Entity "
                                + dbEntity.getName()
                                + ", attribute "
                                + attName
                                + " db type "
                                + dbAtt.getType());
                    }

                    if (dbAtt.getType() == Types.CLOB) {
                        logObj.debug(
                            "DbLoader::loadObjEntities(), Entity "
                                + dbEntity.getName()
                                + ", attribute "
                                + attName
                                + " type "
                                + type);
                    }
                }

                ObjAttribute objAtt =
                    new ObjAttribute(attName, type, objEntity);
                objAtt.setDbAttribute(dbAtt);
                objEntity.addAttribute(objAtt);
            }

            if (delegate != null) {
                delegate.objEntityAdded(objEntity);
            }
        }
    }

    /** Loads database relationships into a DataMap. */
    public void loadDbRelationships(DataMap map) throws SQLException {
        Iterator it = dbEntityList.iterator();
        while (it.hasNext()) {
            DbEntity pkEnt = (DbEntity) it.next();
            String pkEntName = pkEnt.getName();

            // Get all the foreign keys referencing this table
            ResultSet rs =
                getMetaData().getExportedKeys(
                    pkEnt.getCatalog(),
                    pkEnt.getSchema(),
                    pkEnt.getName());
            try {
                if (!rs.next())
                    continue;

                // these will be initailzed every time a new target entity
                // is found in the result set (which should be ordered by table name among other things)
                DbRelationship fwdRel = null;
                DbRelationship backRel = null;
                DbEntity fkEnt = null;

                do {
                    short keySeq = rs.getShort("KEY_SEQ");
                    if (keySeq == 1) {
                        // reinit variables for the new traget entity
                        String fkEntName = rs.getString("FKTABLE_NAME");

                        fkEnt = map.getDbEntity(fkEntName);

                        if (fkEnt == null) {
                            logObj.debug(
                                "FK warning: no entity found for name '"
                                    + fkEntName
                                    + "'");
                        } else {
                            fwdRel =
                                new DbRelationship(
                                    uniqueDbRelName(pkEnt, fkEntName, true));
                            fwdRel.setToMany(true);
                            fwdRel.setSourceEntity(pkEnt);
                            fwdRel.setTargetEntity(fkEnt);
                            pkEnt.addRelationship(fwdRel);

                            backRel =
                                new DbRelationship(
                                    uniqueDbRelName(fkEnt, pkEntName, false));
                            backRel.setToMany(false);
                            backRel.setSourceEntity(fkEnt);
                            backRel.setTargetEntity(pkEnt);
                            fkEnt.addRelationship(backRel);
                        }
                    }

                    if (fkEnt != null) {
                        // Create and append joins
                        DbAttribute pkAtt =
                            (DbAttribute) pkEnt.getAttribute(
                                rs.getString("PKCOLUMN_NAME"));
                        DbAttribute fkAtt =
                            (DbAttribute) fkEnt.getAttribute(
                                rs.getString("FKCOLUMN_NAME"));

                        fwdRel.addJoin(new DbAttributePair(pkAtt, fkAtt));
                        backRel.addJoin(new DbAttributePair(fkAtt, pkAtt));
                    }
                } while (rs.next());
            } finally {
                rs.close();
            }
        }
    }

    /** Creates ObjRelationships based on map's previously loaded DbRelationships. */
    public void loadObjRelationships(DataMap map) throws SQLException {
        Iterator it = map.getObjEntitiesAsList().iterator();
        while (it.hasNext()) {
            ObjEntity objEnt = (ObjEntity) it.next();
            DbEntity dbEnt = objEnt.getDbEntity();

            // bug #578419: no assumptions should be made about current state of the model,
            // it might as well contain ObjEntities without DbEntities
            if (dbEnt == null) {
                continue;
            }

            Iterator relIt = dbEnt.getRelationshipList().iterator();
            while (relIt.hasNext()) {
                DbRelationship dbRel = (DbRelationship) relIt.next();
                ObjRelationship objRel = new ObjRelationship(dbRel.getName());
                objRel.addDbRelationship(dbRel);
                objRel.setToMany(dbRel.isToMany());
                objRel.setSourceEntity(objEnt);
                objRel.setTargetEntity(
                    (Entity) map.getMappedEntities(
                        (DbEntity) dbRel.getTargetEntity()).get(
                        0));
                objEnt.addRelationship(objRel);
            }
        }
    }

    /** 
     * Performs database reverse engineering and generates DataMap
     * that contains default mapping of the tables and views. 
     * By default will include regular tables and views.
     */
    public DataMap createDataMapFromDB(String schemaName) throws SQLException {

        String viewType = adapter.tableTypeForView();
        String tableType = adapter.tableTypeForTable();

        // use types that are not null
        List list = new ArrayList();
        if (viewType != null) {
            list.add(viewType);
        }
        if (tableType != null) {
            list.add(tableType);
        }

        if (list.size() == 0) {
            throw new SQLException("No supported table types found.");
        }

        String[] types = new String[list.size()];
        list.toArray(types);

        return createDataMapFromDB(schemaName, types);
    }

    /** 
     * Performs database reverse engineering and generates DataMap object
     * that contains default mapping of the tables and views. 
     * Allows to limit types of tables to read. 
     */
    public DataMap createDataMapFromDB(String schemaName, String[] tableTypes)
        throws SQLException {
        DataMap dataMap =
            (DataMap) NamedObjectFactory.createObject(
                DataMap.class,
                new DataDomain());
        return loadDataMapFromDB(schemaName, tableTypes, dataMap);
    }

    /** 
     * Performs database reverse engineering and generates DataMap object
     * that contains default mapping of the tables and views. Allows to 
     * limit types of tables to read. 
     */
    public DataMap loadDataMapFromDB(
        String schemaName,
        String[] tableTypes,
        DataMap dataMap)
        throws SQLException {

        if (!loadDbEntities(dataMap,
            getTables(null, schemaName, "%", tableTypes))) {
            return dataMap;
        }

        loadDbRelationships(dataMap);
        loadObjEntities(dataMap);
        loadObjRelationships(dataMap);
        return dataMap;
    }
}