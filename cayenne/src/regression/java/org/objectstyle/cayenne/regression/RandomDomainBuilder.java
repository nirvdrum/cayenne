package org.objectstyle.cayenne.regression;

import java.util.*;
import java.io.*;
import java.sql.*;
import org.apache.commons.lang.*;
import org.objectstyle.ashwood.dbutil.*;
import org.objectstyle.ashwood.graph.*;
import org.objectstyle.cayenne.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.util.*;
import org.objectstyle.cayenne.dba.*;

public class RandomDomainBuilder {
  private DataDomain domain;
  private RandomSchema randomSchema = new RandomSchema();
  private Map objEntitiesByTable;
  private List createTableStatements;
  private List createSequenceStatements;
  private List dropTableStatements;
  private List dropSequenceStatements;
  private boolean schemaGenerated;

  public RandomDomainBuilder(DataDomain domain) {
    this.domain = domain;
  }

  public void generate(File generatedScriptDir) throws CayenneException {
    generateSchema(generatedScriptDir);
  }

  public void drop() throws CayenneException {
    dropSchema();
  }

  private void generateSchema(File dir) throws CayenneException {
    if (schemaGenerated) throw new CayenneException("Schema already exists");
    randomSchema.generate();
    List tables = randomSchema.getTables();
    Map sequencesByTable = randomSchema.getSequencesByTable();
    createTableStatements = new ArrayList(tables.size());
    createSequenceStatements = new ArrayList(sequencesByTable.size());
    dropTableStatements = new ArrayList(tables.size());
    dropSequenceStatements = new ArrayList(sequencesByTable.size());
    for (Iterator i = tables.iterator(); i.hasNext();) {
      StringWriter buffer = new StringWriter();
      PrintWriter out = new PrintWriter(buffer);
      Table table = (Table)i.next();
      table.toCreateSQL(out);
      out.flush();
      createTableStatements.add(buffer.toString());
      Sequence sequence = (Sequence)sequencesByTable.get(table);
      if (sequence != null) {
        StringWriter buffer1 = new StringWriter();
        PrintWriter out1 = new PrintWriter(buffer1);
        sequence.toCreateSQL(out1);
        out1.flush();
        createSequenceStatements.add(buffer1.toString());
        buffer1 = new StringWriter();
        out1 = new PrintWriter(buffer1);
        sequence.toDropSQL(out1);
        out1.flush();
        dropSequenceStatements.add(buffer1.toString());
      }
    }
    for (ListIterator i = tables.listIterator(tables.size()); i.hasPrevious();) {
      StringWriter buffer = new StringWriter();
      PrintWriter out = new PrintWriter(buffer);
      Table table = (Table)i.previous();
      table.toDropSQL(out);
      out.flush();
      dropTableStatements.add(buffer.toString());
    }
    if (dir != null) {
      try {
        FileWriter createSchemaScript = new FileWriter(new File(dir, "create_schema.sql"));
        PrintWriter out = new PrintWriter(createSchemaScript);
        script(createSequenceStatements, out);
        script(createTableStatements, out);
        out.close();
        createSchemaScript.close();
        FileWriter dropSchemaScript = new FileWriter(new File(dir, "drop_schema.sql"));
        out = new PrintWriter(dropSchemaScript);
        script(dropSequenceStatements, out);
        script(dropTableStatements, out);
        out.close();
        dropSchemaScript.close();
      }
      catch (IOException ex) {
        throw new CayenneException(ex);
      }
    }
    generateDataMap(dir);
    Connection connection = null;
    try {
      DataNode[] nodes = domain.getDataNodes();
      if (nodes == null || nodes.length == 0) throw new CayenneException("No data nodes configured.");
      DataNode node = nodes[0];
      connection = node.getDataSource().getConnection();
      schemaGenerated = true;
      executeStatements(createTableStatements, connection);
      executeStatements(createSequenceStatements, connection);
      try {connection.close();}
      catch (Exception exc) {}
    }
    catch (SQLException ex) {
      try {connection.close();}
      catch (Exception exc) {}
      dropSchema();
      throw new CayenneException(ex);
    }
  }

  private void dropSchema() throws CayenneException {
    Connection connection = null;
    try {
      DataNode[] nodes = domain.getDataNodes();
      if (nodes == null || nodes.length == 0) throw new CayenneException("No data nodes configured.");
      DataNode node = nodes[0];
      connection = node.getDataSource().getConnection();
      executeAllStatements(dropTableStatements, connection);
      executeAllStatements(dropSequenceStatements, connection);
      schemaGenerated = false;
      try {connection.close();}
      catch (Exception exc) {}
    }
    catch (SQLException ex) {
      try {connection.close();}
      catch (Exception exc) {}
      throw new CayenneException(ex);
    }
  }

  private void executeStatements(List statements, Connection c) throws SQLException {
    for (Iterator i = statements.iterator(); i.hasNext();) {
      PreparedStatement ps = c.prepareStatement(i.next().toString());
      ps.execute();
      ps.close();
    }
  }

  private void executeAllStatements(List statements, Connection c) {
    for (Iterator i = statements.iterator(); i.hasNext();) {
      try {
        PreparedStatement ps = c.prepareStatement(i.next().toString());
        ps.execute();
        ps.close();
      }
      catch (SQLException ex) {
      }
    }
  }

  private void script(List statements, PrintWriter out) {
    for (Iterator i = statements.iterator(); i.hasNext();) {
      out.print(i.next());
      out.println(';');
    }
  }

  private void generateDataMap(File dir) throws CayenneException {
    DataNode[] nodes = domain.getDataNodes();
    if (nodes == null || nodes.length == 0) throw new CayenneException("No data nodes configured.");
    DataNode node = nodes[0];
    List tables = randomSchema.getTables();
    Map sequencesByTable = randomSchema.getSequencesByTable();
    DataMap dataMap = new DataMap("DataMap-" + StringUtils.defaultString(randomSchema.getSchemaName()));
    List objEntities = new ArrayList(tables.size());
    objEntitiesByTable = new HashMap(tables.size());
    for (Iterator i = tables.iterator(); i.hasNext();) {
      Table table = (Table)i.next();
      DbEntity dbEntity = createDbEntity(table, (Sequence)sequencesByTable.get(table));
      ObjEntity objEntity = createObjEntity(dbEntity);
      objEntities.add(objEntity);
      objEntitiesByTable.put(table, objEntity);
    }
    Collections.shuffle(objEntities);
    for (Iterator i = objEntities.iterator(); i.hasNext();) {
      ObjEntity entity = (ObjEntity)i.next();
      entity.getDbEntity().setParent(dataMap);
      dataMap.addDbEntity(entity.getDbEntity());
      entity.setParent(dataMap);
      dataMap.addObjEntity(entity);
    }
    Digraph refDigraph = randomSchema.getSchemaGraph();
    for (Iterator i = tables.iterator(); i.hasNext();) {
      Table pkTable = (Table)i.next();
      for (ArcIterator j = refDigraph.outgoingIterator(pkTable); j.hasNext();) {
        j.next();
        Table fkTable = (Table)j.getDestination();
        createRelationship(pkTable, fkTable);
      }
    }

    node.getAdapter().getPkGenerator().reset();
    node.setDataMaps(Collections.singletonList(dataMap));
    domain.reset();
    domain.clearDataMaps();
    domain.addNode(node);

    if (dir != null) {
      try {
        FileWriter dataMapXml = new FileWriter(new File(dir, dataMap.getName() + ".xml"));
        PrintWriter out = new PrintWriter(dataMapXml);
        MapLoader mapSaver = new MapLoader();
        mapSaver.storeDataMap(out, dataMap);
        out.close();
        dataMapXml.close();
      }
      catch (IOException ex) {
        throw new CayenneException(ex);
      }
    }
  }

  private DbEntity createDbEntity(Table table, Sequence sequence) {
    DbEntity dbEntity = new DbEntity(table.getName());
    dbEntity.setCatalog(table.getCatalog());
    dbEntity.setSchema(table.getSchema());
    if (sequence != null) {
      DbKeyGenerator pkGenerator = new DbKeyGenerator();
      pkGenerator.setGeneratorName(sequence.getName());
      pkGenerator.setGeneratorType(DbKeyGenerator.ORACLE_TYPE);
      pkGenerator.setKeyCacheSize(new Integer(sequence.getIncrement()));
      dbEntity.setPrimaryKeyGenerator(pkGenerator);
    }
    Collection columns = table.getColumns();
    Collection primaryKeys = table.getPrimaryKeys();
    for (Iterator i = columns.iterator(); i.hasNext();) {
      Column column = (Column)i.next();
      DbAttribute attribute = new DbAttribute(column.getName(), Types.INTEGER, dbEntity);
      attribute.setMandatory(true);
      for (Iterator j = primaryKeys.iterator(); j.hasNext();) {
        PrimaryKey pk = (PrimaryKey)j.next();
        if (pk.getColumnName().equals(attribute.getName()))
          attribute.setPrimaryKey(true);
      }
      dbEntity.addAttribute(attribute);
    }
    return dbEntity;
  }

  private void createRelationship(Table pkTable, Table fkTable) {
    ObjEntity pkObjEntity = (ObjEntity)objEntitiesByTable.get(pkTable);
    ObjEntity fkObjEntity = (ObjEntity)objEntitiesByTable.get(fkTable);
    DbEntity pkEntity = pkObjEntity.getDbEntity();
    DbEntity fkEntity = fkObjEntity.getDbEntity();
    Collection foreignKeys = fkTable.getForeignKeys();
    Collection primaryKeys = fkTable.getPrimaryKeys();
    for (Iterator i = foreignKeys.iterator(); i.hasNext();) {
      ForeignKey fk = (ForeignKey)i.next();
      if (fk.getPkTableName().equals(pkTable.getName())) {
        DbRelationship forwardRelation = new DbRelationship(pkEntity.getName() + '_' + fkEntity.getName() + '_' + fk.getName());
        forwardRelation.setToMany(true);
        for (Iterator j = primaryKeys.iterator(); j.hasNext();) {
          PrimaryKey pk = (PrimaryKey)j.next();
          if (pk.getColumnName().equals(fk.getColumnName())) {
            forwardRelation.setToDependentPK(true);
            break;
          }
        }
        forwardRelation.setSourceEntity(pkEntity);
        forwardRelation.setTargetEntity(fkEntity);
        pkEntity.addRelationship(forwardRelation);
        DbRelationship backwardRelation = new DbRelationship(fkEntity.getName() + '_' + fk.getName() + '_' + pkEntity.getName());
        backwardRelation.setToMany(false);
        backwardRelation.setSourceEntity(fkEntity);
        backwardRelation.setTargetEntity(pkEntity);
        fkEntity.addRelationship(backwardRelation);
        DbAttribute pkAtt = (DbAttribute)pkEntity.getAttribute(fk.getPkColumnName());
        DbAttribute fkAtt = (DbAttribute)fkEntity.getAttribute(fk.getColumnName());
        forwardRelation.addJoin(new DbAttributePair(pkAtt, fkAtt));
        backwardRelation.addJoin(new DbAttributePair(fkAtt, pkAtt));

        ObjRelationship objForwardRel = new ObjRelationship(forwardRelation.getName());
        objForwardRel.addDbRelationship(forwardRelation);
        objForwardRel.setToMany(forwardRelation.isToMany());
        objForwardRel.setSourceEntity(pkObjEntity);
        objForwardRel.setTargetEntity(fkObjEntity);
        pkObjEntity.addRelationship(objForwardRel);
        ObjRelationship objBackwardRel = new ObjRelationship(backwardRelation.getName());
        objBackwardRel.addDbRelationship(backwardRelation);
        objBackwardRel.setToMany(backwardRelation.isToMany());
        objBackwardRel.setSourceEntity(fkObjEntity);
        objBackwardRel.setTargetEntity(pkObjEntity);
        fkObjEntity.addRelationship(objBackwardRel);
        //return;
      }
    }
  }

  private ObjEntity createObjEntity(DbEntity dbEntity) {
    ObjEntity objEntity = new ObjEntity(NameConverter.undescoredToJava(dbEntity.getName(), true));
    objEntity.setDbEntity(dbEntity);
    objEntity.setClassName(objEntity.getName());
    Iterator colIt = dbEntity.getAttributeMap().values().iterator();
    while (colIt.hasNext()) {
      DbAttribute dbAtt = (DbAttribute) colIt.next();
      if (dbAtt.isPrimaryKey()) continue;
      String attName = NameConverter.undescoredToJava(dbAtt.getName(), false);
      String type = TypesMapping.getJavaBySqlType(dbAtt.getType());
      ObjAttribute objAtt = new ObjAttribute(attName, type, objEntity);
      objAtt.setDbAttribute(dbAtt);
      objEntity.addAttribute(objAtt);
    }
    return objEntity;
  }
  public RandomSchema getRandomSchema() {
    return randomSchema;
  }
  public DataDomain getDomain() {
    return domain;
  }
  public boolean isSchemaGenerated() {
    return schemaGenerated;
  }
}