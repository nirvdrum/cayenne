package org.objectstyle.cayenne.map;
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

import java.sql.*;
import java.util.*;
import java.util.logging.*;
import java.io.PrintWriter;
import org.objectstyle.util.*;
import org.objectstyle.cayenne.dba.TypesMapping;


/** Utility class that does reverse engineering of the database. 
  * It can create DataMaps using database meta data obtained via JDBC driver.
  *
  * @author Michael Shengaout
  * @author Andrei Adamchik
 */
public class DbLoader {
    static Logger logObj = Logger.getLogger(DbLoader.class.getName());

    public static final String WILDCARD = "%";

    /** Creates default name for loaded relationship */
    private static String defaultDbRelName(String dstName, boolean toMany) {
        String uglyName = (toMany) ? dstName + "_ARRAY" : "to_" + dstName;
        return NameConverter.undescoredToJava(uglyName, false);
    }

    private Connection con;
    private DatabaseMetaData metaData;


    /** Creates new DbLoader. */
    public DbLoader(Connection con) {
        this.con = con;
    }


    /** Returns DatabaseMetaData object associated with this DbLoader. */
    public DatabaseMetaData getMetaData() throws SQLException {
        if (null == metaData)
            metaData = con.getMetaData();
        return metaData;
    }

    
    /**  Retrieves catalogues for the database associated with this DbLoader.
      *
      *  @return ArrayList with the catalog names, empty Array if none found.
      */
    public ArrayList getCatalogs() throws SQLException  {
        ArrayList catalogs = new ArrayList();
        ResultSet rs = getMetaData().getCatalogs();
        while (rs.next()) {
            String catalog_name = rs.getString(1);
            catalogs.add(catalog_name);
        }
        rs.close();
        return catalogs;
    }


    /** Retrieves the schemas for the database.
     *  @return ArrayList with the schema names, empty Array if none found.*/
    public ArrayList getSchemas() throws SQLException  {
        ArrayList schemas = new ArrayList();
        ResultSet rs = getMetaData().getSchemas();
        while (rs.next()) {
            String schema_name = rs.getString(1);
            schemas.add(schema_name);
        }
        rs.close();
        return schemas;
    }



    /** Gets all the table types for the given database type.
     *  Types may be such as "TABLE", "VIEW", "SYSTEM TABLE", etc.
     *  @return ArrayList of Strings, empty array if nothing found. */
    public ArrayList getTableTypes() throws SQLException {
        ArrayList types = new ArrayList();
        ResultSet rs = getMetaData().getTableTypes();
        while(rs.next()) {
            String type = rs.getString("TABLE_TYPE");
            types.add(type);
        }
        rs.close();
        return types;
    }



    /** Gets all the table names for given combination of the criteria.
     *  @param catalog The name of the catalog, may be null.
     *  @param schemaPattern The pattern for schema name, use "%" for wildcard
     *  @param tableNamePattern The pattern for table names, % for wildcard,
     *                          if null or "" defaults to "%"
     *  @param types The types of table names to retrieve,
     *               null returns all types
     *  @return ArrayList of TableInfo objects, empty array if nothing found */
    public ArrayList getTables(String catalog,
                               String schemaPattern,
                               String tableNamePattern,
                               String[] types) throws SQLException {
        ArrayList tables = new ArrayList();
        if (null == schemaPattern || schemaPattern.equals(""))
            schemaPattern = WILDCARD;
        else if (-1 != schemaPattern.indexOf('*'))
            schemaPattern.replace('*', '%');

        if (null == tableNamePattern || tableNamePattern.equals(""))
            tableNamePattern = WILDCARD;
        else if (-1 != tableNamePattern.indexOf('*'))
            tableNamePattern.replace('*', '%');
        ResultSet rs = getMetaData().getTables(catalog, schemaPattern
                                               ,  tableNamePattern, types);
        while(rs.next() ) {
            String cat = rs.getString("TABLE_CAT");
            String schema = rs.getString("TABLE_SCHEM");
            String type = rs.getString("TABLE_TYPE");
            String name = rs.getString("TABLE_NAME");
            TableInfo info = new TableInfo(name, type, schema, catalog);
            tables.add(info);
        }
        rs.close();
        return tables;
    }


    /** Loads dbEntities for the specified tables.
     *  @param map DataMap to be populated with DbEntities. Presumed not to
     *             have duplicate entries for the specified tables.
     *             If null, a new DataMap is created
     *  @param tables The list of TableInfo object
     *              for which DbEntities must be created.
     *  @return DataMap populated with DbEntities. */
    public void loadDbEntities(DataMap map, ArrayList tables) throws SQLException {
        if (null == map)
            map = new DataMap("Untitled Map");
        Iterator iter = tables.iterator();
        while (iter.hasNext()) {
            TableInfo table = (TableInfo)iter.next();
            DbEntity dbEntity = new DbEntity();
            dbEntity.setName(table.getName());
            dbEntity.setSchema(table.getSchema());
            dbEntity.setCatalog(table.getCatalog());
            ResultSet rs = getMetaData().getColumns(table.getCatalog()
                                                    , table.getSchema()
                                                    , table.getName()
                                                    , "%");
            while (rs.next()) {
                // gets attribute's (column's) name and type,
                // create DbAttribute and add to dbEntity
                String columnName = rs.getString("COLUMN_NAME");
                int columnType = rs.getInt("DATA_TYPE");
                int columnSize = rs.getInt("COLUMN_SIZE");
                // FIXME!!! Meed to ignore this param for the param types
                // for which it is not applicable
                int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                boolean nullable = rs.getBoolean("NULLABLE");
                DbAttribute dbAttribute;
                dbAttribute = new DbAttribute(columnName, columnType, dbEntity);
                dbAttribute.setMaxLength(columnSize);
                dbAttribute.setPrecision(decimalDigits);
                dbAttribute.setMandatory(!nullable);
                dbEntity.addAttribute(dbAttribute);
            }
	        rs.close();
            map.addDbEntity(dbEntity);
        }

        // get primary keys for each table and store it in dbEntity
        Iterator i = map.getDbEntitiesAsList().iterator();
        while(i.hasNext()) {
            DbEntity dbEntity = (DbEntity)i.next();
            String tableName = dbEntity.getName();
            ResultSet rs = metaData.getPrimaryKeys(null, dbEntity.getSchema(), tableName);
            while(rs.next()) {
                String keyName = rs.getString(4);
                ((DbAttribute)dbEntity.getAttribute(keyName)).setPrimaryKey(true);
            }
            rs.close();
        }
    }

    /** Creates ObjEntities from DbEntities. Uses NameConverter class to
     *  change database table and attribute names into whatever
     *  user wants them to be, e.g. from EMPLOYEE_NAME to employeeName. */
    public void loadObjEntities(DataMap map) {
        Iterator it = map.getDbEntitiesAsList().iterator();
        while(it.hasNext()) {
            DbEntity dbEntity = (DbEntity)it.next();
            ObjEntity objEntity = new ObjEntity(NameConverter.undescoredToJava(dbEntity.getName(), true));
            objEntity.setDbEntity(dbEntity);
            objEntity.setClassName(objEntity.getName());
            map.addObjEntity(objEntity);

            Iterator colIt = dbEntity.getAttributeMap().values().iterator();
            while (colIt.hasNext()) {
                DbAttribute dbAtt = (DbAttribute)colIt.next();
                if(dbAtt.isPrimaryKey())
                    continue;

                String attName = NameConverter.undescoredToJava(dbAtt.getName(), false);
                String type = TypesMapping.getJavaBySqlType(dbAtt.getType());
                ObjAttribute objAtt = new ObjAttribute(attName, type, objEntity);
                objAtt.setDbAttribute(dbAtt);
                objEntity.addAttribute(objAtt);
            }
        }
    }


    /** Loads database relationships into a DataMap. */
    public void loadDbRelationships(DataMap map) throws SQLException {
        List dbEntities = map.getDbEntitiesAsList();
        Iterator it = dbEntities.iterator();
        while(it.hasNext()) {
            DbEntity pkEnt = (DbEntity)it.next();
            String pkEntName = pkEnt.getName();

            // Get all the foreign keys referencing this table
            ResultSet rs = getMetaData().getExportedKeys(pkEnt.getCatalog(), pkEnt.getSchema(), pkEnt.getName());
            if (!rs.next())
                continue;

            // these will be initailzed every time a new target entity
            // is found in the result set (which should be ordered by table name among other things)
            DbRelationship fwdRel = null;
            DbRelationship backRel = null;
            DbEntity fkEnt = null;

            do {
                short keySeq = rs.getShort("KEY_SEQ");
                if(keySeq == 1) {
                    // reinit variables for the new traget entity
                    String fkEntName = rs.getString("FKTABLE_NAME");

                    fkEnt = map.getDbEntity(fkEntName);

                    fwdRel = new DbRelationship(defaultDbRelName(fkEntName, true));
                    fwdRel.setToMany(true);
                    fwdRel.setSourceEntity(pkEnt);
                    fwdRel.setTargetEntity(fkEnt);
                    pkEnt.addRelationship(fwdRel);

                    backRel = new DbRelationship(defaultDbRelName(pkEntName, false));
                    backRel.setToMany(false);
                    backRel.setSourceEntity(fkEnt);
                    backRel.setTargetEntity(pkEnt);
                    fkEnt.addRelationship(backRel);
                }


                // Create and append joins
                DbAttribute pkAtt = (DbAttribute)pkEnt.getAttribute(rs.getString("PKCOLUMN_NAME"));
                DbAttribute fkAtt = (DbAttribute)fkEnt.getAttribute(rs.getString("FKCOLUMN_NAME"));

                fwdRel.addJoin(new DbAttributePair(pkAtt, fkAtt));
                backRel.addJoin(new DbAttributePair(fkAtt, pkAtt));
            } while(rs.next());
            rs.close();
        }
    }

    /** Creates ObjRelationships based on map's previously loaded DbRelationships. */
    public void loadObjRelationships(DataMap map) throws SQLException {
        Iterator it = map.getObjEntitiesAsList().iterator();
        while(it.hasNext()) {
            ObjEntity objEnt = (ObjEntity)it.next();
            DbEntity dbEnt = objEnt.getDbEntity();

            Iterator relIt = dbEnt.getRelationshipList().iterator();
            while (relIt.hasNext()) {
                DbRelationship dbRel = (DbRelationship)relIt.next();
                ObjRelationship objRel = new ObjRelationship(dbRel.getName());
                objRel.addDbRelationship(dbRel);
                objRel.setToMany(dbRel.isToMany());
                objRel.setSourceEntity(objEnt);
                objRel.setTargetEntity(map.getObjEntityByDbEntityName(dbRel.getTargetEntity().getName()));
                objEnt.addRelationship(objRel);
            }
        }
    }


    /** Performs database reverse engineering and generates DataMap object
      * that contains default mapping of the tables and views. By default will
      * read all table types including tables, views, system tables, etc. */
    public DataMap createDataMapFromDB(String schemaName) throws SQLException {
        return createDataMapFromDB(schemaName, null);
    }

    /** Performs database reverse engineering and generates DataMap object
      * that contains default mapping of the tables and views. Allows to 
      * limit types of tables to read (usually only tables and views are relevant). */
    public DataMap createDataMapFromDB(String schemaName, String[] tableTypes) throws SQLException {
        DataMap dataMap = new DataMap("Untitled Map");
        loadDbEntities(dataMap, getTables(null, schemaName, "%", tableTypes));
        loadDbRelationships(dataMap);
        loadObjEntities(dataMap);
        loadObjRelationships(dataMap);
        return dataMap;
    }
}
